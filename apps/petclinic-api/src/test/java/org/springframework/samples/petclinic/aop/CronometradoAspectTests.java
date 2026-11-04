package org.springframework.samples.petclinic.aop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

/**
 * MODULO 1 - Comprueba {@link CronometradoAspect} sobre un servicio de
 * demostracion, sin base de datos.
 */
@SpringBootTest(classes = CronometradoAspectTests.App.class,
		properties = { "eureka.client.enabled=false", "spring.boot.admin.client.enabled=false" })
@ExtendWith(OutputCaptureExtension.class)
class CronometradoAspectTests {

	@Autowired
	Lento lento;

	@Test
	void laAnotacionDelMetodoGanaALaDeLaClase(CapturedOutput salida) throws Exception {
		lento.rapidoConUmbralCero();   // umbral del metodo: 0 ms -> siempre LENTO
		lento.dormir(30);              // umbral de la clase: 10 ms -> LENTO
		lento.noTanLento();            // umbral del metodo: 1000 ms -> no

		assertThat(salida).contains("LENTO Lento.rapidoConUmbralCero()")
			.contains("LENTO Lento.dormir(..)")
			.doesNotContain("LENTO Lento.noTanLento()");
		// Una sola linea por llamada: el consejo de clase no se suma al del metodo
		assertThat(salida.getOut().split("LENTO Lento.rapidoConUmbralCero").length - 1).isEqualTo(1);
	}

	@Test
	void unMetodoPrivadoNoSeMide(CapturedOutput salida) {
		lento.llamaAPrivado();         // la clase mide la llamada publica (umbral 10 ms -> no)

		assertThat(salida).doesNotContain("LENTO Lento.privado()");
	}

	@Service
	@Cronometrado(umbralMs = 10)
	static class Lento {

		@Cronometrado(umbralMs = 0)
		public void rapidoConUmbralCero() {
		}

		public void dormir(long ms) throws InterruptedException {
			Thread.sleep(ms);
		}

		@Cronometrado(umbralMs = 1000)
		public void noTanLento() throws InterruptedException {
			Thread.sleep(30);
		}

		public void llamaAPrivado() {
			privado();
		}

		@Cronometrado(umbralMs = 0)
		private void privado() {
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
			SqlInitializationAutoConfiguration.class })
	@Import({ CronometradoAspect.class, Lento.class })
	static class App {

	}

}
