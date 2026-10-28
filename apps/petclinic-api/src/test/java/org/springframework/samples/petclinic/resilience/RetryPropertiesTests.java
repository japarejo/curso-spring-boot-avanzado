package org.springframework.samples.petclinic.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.resilience.RegistroIntentos.Intento;

/**
 * MODULO 5 - Comprueba que las propiedades resilience4j.retry.* de
 * application.properties llegan a las anotaciones @Retry.
 *
 * Se carga solo lo imprescindible (sin base de datos): resilience4j, AOP y
 * las clases de la demostracion. Las esperas son las reales de
 * application.properties, por eso se eligen pocos fallos.
 */
@SpringBootTest(classes = RetryPropertiesTests.RetryTestApplication.class,
		properties = { "eureka.client.enabled=false", "spring.boot.admin.client.enabled=false" })
class RetryPropertiesTests {

	@Autowired
	RetryDemoGateway gateway;

	@Test
	void laPoliticaEfectivaSaleDeLasPropiedades() {
		assertThat(gateway.configuracion("demoEsperaFija").maxAttempts()).isEqualTo(4);
		assertThat(gateway.configuracion("demoEsperaFija").esperasEntreIntentosMs()).containsExactly(1000L, 1000L, 1000L);
		// El tope exponential-max-wait-duration=1s corta la cuarta espera (1600 ms)
		assertThat(gateway.configuracion("demoExponencial").esperasEntreIntentosMs()).containsExactly(200L, 400L, 800L,
				1000L);
		assertThat(gateway.configuracion("bills").esperasEntreIntentosMs()).containsExactly(500L, 1000L);
		// Sin propiedades propias de intentos ni espera: todo se hereda de configs.comun
		assertThat(gateway.configuracion("demoExcepciones").esperasEntreIntentosMs()).containsExactly(300L, 300L,
				300L);
		assertThat(gateway.configuracion("demoResultado").failAfterMaxAttempts()).isTrue();
	}

	@Test
	void conEsperaFijaSeReintentaHastaQueResponde() {
		RegistroIntentos registro = new RegistroIntentos(1, TipoFallo.TRANSITORIO);

		String resultado = gateway.esperaFija(registro);

		assertThat(resultado).isEqualTo("Respuesta correcta en el intento 2");
		assertThat(registro.intentos()).extracting(Intento::resultado)
			.containsExactly("FALLO FalloTransitorioException", "OK");
		assertThat(registro.intentos().get(1).msDesdeAnterior()).isGreaterThanOrEqualTo(1000L);
	}

	@Test
	void conBackoffExponencialCadaEsperaDuplicaLaAnterior() {
		RegistroIntentos registro = new RegistroIntentos(2, TipoFallo.TRANSITORIO);

		gateway.exponencial(registro);

		List<Intento> intentos = registro.intentos();
		assertThat(intentos).hasSize(3);
		assertThat(intentos.get(1).msDesdeAnterior()).isBetween(200L, 399L);
		assertThat(intentos.get(2).msDesdeAnterior()).isGreaterThanOrEqualTo(400L);
	}

	@Test
	void unaExcepcionIgnoradaNoSeReintenta() {
		RegistroIntentos registro = new RegistroIntentos(2, TipoFallo.PERMANENTE);

		String resultado = gateway.excepciones(registro);

		assertThat(registro.intentos()).hasSize(1);
		assertThat(resultado).startsWith("FALLBACK");
		assertThat(registro.causaFallback()).startsWith("FalloPermanenteException");
	}

	@Test
	void siSeAgotanLosIntentosRespondeElFallback() {
		RegistroIntentos registro = new RegistroIntentos(10, TipoFallo.TRANSITORIO);

		String resultado = gateway.excepciones(registro);

		assertThat(registro.intentos()).hasSize(4); // max-attempts=4
		assertThat(resultado).isEqualTo("FALLBACK tras 4 intento(s)");
	}

	@Test
	void seReintentaPorResultadoYSeFallaAlAgotarLosIntentos() {
		RegistroIntentos pocasVeces = new RegistroIntentos(2, TipoFallo.TRANSITORIO);
		assertThat(gateway.porResultado(pocasVeces)).isEqualTo(ServicioRemotoSimulado.COMPLETADO);
		assertThat(pocasVeces.intentos()).hasSize(3);

		RegistroIntentos siemprePendiente = new RegistroIntentos(10, TipoFallo.TRANSITORIO);
		assertThat(gateway.porResultado(siemprePendiente)).startsWith("FALLBACK");
		// fail-after-max-attempts=true: en vez de devolver PENDIENTE, se lanza esta excepcion
		assertThat(siemprePendiente.causaFallback()).startsWith("MaxRetriesExceededException");
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
			SqlInitializationAutoConfiguration.class })
	@Import({ RetryDemoGateway.class, ServicioRemotoSimulado.class, RetryTrazasConfiguration.class })
	static class RetryTestApplication {

	}

}
