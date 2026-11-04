package org.springframework.samples.petclinic.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Service;

/**
 * MODULO 1 - Comprueba el aspecto {@link AuditoriaAspect} sobre un servicio de
 * demostracion, sin base de datos.
 */
@SpringBootTest(classes = AuditoriaAspectTests.AuditoriaTestApplication.class,
		properties = { "eureka.client.enabled=false", "spring.boot.admin.client.enabled=false" })
class AuditoriaAspectTests {

	@Autowired
	ServicioDemo servicio;

	@Autowired
	RegistroAuditoria registro;

	@BeforeEach
	void limpiar() {
		registro.limpiar();
	}

	@Test
	void elServicioAnotadoEsUnProxy() {
		assertThat(AopUtils.isAopProxy(servicio)).isTrue();
	}

	@Test
	@WithMockUser(username = "george")
	void registraAccionMetodoUsuarioYArgumentos() {
		servicio.reservar("Leo", 3);

		assertThat(registro.entradas()).singleElement().satisfies(e -> {
			assertThat(e.accion()).isEqualTo("reserva");
			assertThat(e.metodo()).isEqualTo("ServicioDemo.reservar(..)");
			assertThat(e.usuario()).isEqualTo("george");
			assertThat(e.argumentos()).isEqualTo("[Leo, 3]");
			assertThat(e.correcta()).isTrue();
		});
	}

	@Test
	void unaExcepcionSeRegistraYSeVuelveALanzar() {
		assertThatThrownBy(() -> servicio.fallar()).isInstanceOf(IllegalStateException.class);

		assertThat(registro.entradas()).singleElement()
			.extracting(EntradaAuditoria::resultado)
			.isEqualTo("ERROR IllegalStateException");
	}

	@Test
	void conArgumentosFalseNoGuardaLosArgumentos() {
		servicio.cambiarClave("secreto");

		assertThat(registro.entradas()).singleElement()
			.extracting(EntradaAuditoria::argumentos)
			.isEqualTo("[ocultos]");
	}

	@Test
	void sinAnotacionNoSeAudita() {
		servicio.sinAuditar();

		assertThat(registro.entradas()).isEmpty();
	}

	@Test
	void laAutoinvocacionNoPasaPorElAspecto() {
		servicio.reservarDosVeces("Leo");

		// Solo la llamada externa; las dos this.reservar(...) no atraviesan el proxy
		assertThat(registro.entradas()).extracting(EntradaAuditoria::accion).containsExactly("reserva doble");
	}

	@Test
	@WithMockUser(username = "intruso", authorities = "owner")
	void comoEsElAspectoMasExternoTambienRegistraLosAccesosDenegados() {
		assertThatThrownBy(() -> servicio.borrarTodo()).isInstanceOf(AccessDeniedException.class);

		assertThat(registro.entradas()).singleElement().satisfies(e -> {
			assertThat(e.usuario()).isEqualTo("intruso");
			assertThat(e.resultado()).isEqualTo("ERROR AuthorizationDeniedException");
		});
	}

	@Service
	static class ServicioDemo {

		@Auditado("reserva")
		public String reservar(String mascota, int dias) {
			return mascota + " reservado " + dias + " dias";
		}

		@Auditado("reserva doble")
		public void reservarDosVeces(String mascota) {
			reservar(mascota, 1);
			this.reservar(mascota, 2);
		}

		@Auditado("operacion que falla")
		public void fallar() {
			throw new IllegalStateException("fallo simulado");
		}

		@Auditado(value = "cambio de clave", conArgumentos = false)
		public void cambiarClave(String nueva) {
		}

		public void sinAuditar() {
		}

		@Auditado("borrado masivo")
		@PreAuthorize("hasAuthority('admin')")
		public void borrarTodo() {
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
			SqlInitializationAutoConfiguration.class })
	@EnableMethodSecurity
	@Import({ AuditoriaAspect.class, RegistroAuditoria.class, ServicioDemo.class })
	static class AuditoriaTestApplication {

	}

}
