package org.springframework.samples.petclinic.service.transacciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

/**
 * LABORATORIO DE TRANSACCIONES AVANZADAS (modulo 6, material adicional).
 *
 * Continuacion de TransactionalExamplesVerificationTests, que cubre lo que entra
 * en el temario obligatorio: frontera, rollbackFor, readOnly, propagacion,
 * aislamiento y timeout. Aqui van las cinco trampas que aparecen despues, ya
 * trabajando:
 *
 *   1. autoinvocacion
 *   2. actualizacion perdida y bloqueo optimista
 *   3. bloqueo pesimista
 *   4. NESTED
 *   5. eventos ligados a la transaccion
 *
 * Igual que en el otro, @Transactional(NOT_SUPPORTED) desactiva la transaccion
 * que Spring pone alrededor de cada prueba. Sin eso los metodos del servicio se
 * engancharian a la transaccion de la prueba, nunca confirmarian y ninguna de
 * estas demostraciones funcionaria. Es la primera cosa que se rompe al copiar
 * estas pruebas a otro sitio.
 *
 * Cada bloque trabaja sobre una visita distinta (data.sql trae 4) y restaura el
 * valor original al terminar, para que el orden de ejecucion no importe.
 */
@DataJpaTest
@Import({ AutoinvocacionService.class, AutoinvocacionService.ColaboradorTransaccional.class,
		EdicionConcurrenteService.class, RegistroDeVisitasService.class, NotificacionesDeVisita.class })
@TestPropertySource(properties = {
		"spring.jpa.show-sql=true",
		"logging.level.org.hibernate.SQL=DEBUG",
		"logging.level.org.springframework.transaction.interceptor=TRACE",
		"logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG"
})
@ExtendWith(OutputCaptureExtension.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Transacciones avanzadas: las cinco trampas")
class TransaccionesAvanzadasVerificationTests {

	@Autowired
	private AutoinvocacionService autoinvocacion;

	@Autowired
	private AutoinvocacionService.ColaboradorTransaccional colaborador;

	@Autowired
	private EdicionConcurrenteService edicion;

	@Autowired
	private RegistroDeVisitasService registro;

	@Autowired
	private NotificacionesDeVisita notificaciones;

	@Nested
	@DisplayName("1. Autoinvocacion: el proxy que se queda fuera")
	class Autoinvocacion {

		@Test
		@DisplayName("Llamar con this NO abre transaccion, aunque el metodo este anotado")
		void llamarConThisNoAbreTransaccion() {
			assertThat(autoinvocacion.llamandoConThis()).isFalse();
		}

		@Test
		@DisplayName("Los tres arreglos si la abren")
		void losTresArreglosFuncionan() {
			assertThat(autoinvocacion.llamandoAlProxy()).isTrue();
			assertThat(autoinvocacion.usandoTransactionTemplate()).isTrue();
			assertThat(autoinvocacion.llamandoAOtroBean(colaborador)).isTrue();
		}

		@Test
		@DisplayName("El REQUIRES_NEW invocado con this se queda en la transaccion de fuera")
		void elRequiresNewInvocadoConThisNoSeparaNada(CapturedOutput salida) {
			String sinProxy = autoinvocacion.auditoriaQueNoSobrevive();
			String conProxy = autoinvocacion.auditoriaQueSiSobrevive();

			// Sin proxy, el nombre de la transaccion sigue siendo el del metodo de
			// fuera: no se ha creado ninguna transaccion nueva.
			assertThat(sinProxy).endsWith("auditoriaQueNoSobrevive");
			assertThat(conProxy).endsWith("nombreDeLaTransaccionActual");

			// Y el log lo confirma: solo hay una suspension, la del caso bueno.
			assertThat(salida).contains("Suspending current transaction");
		}

	}

	@Nested
	@DisplayName("2. Actualizacion perdida y bloqueo optimista")
	class BloqueoOptimista {

		@Test
		@DisplayName("Con UPDATE masivo la actualizacion se pierde en silencio")
		void elUpdateMasivoPierdeLaActualizacion() {
			// El usuario B escribe y confirma primero; el usuario A escribe
			// despues, sobre una lectura anterior. No hay excepcion.
			edicion.editarConUpdateMasivo(1, "texto del usuario A", "texto del usuario B");

			assertThat(edicion.descripcionActual(1)).isEqualTo("texto del usuario A");

			edicion.restaurarDescripcion(1, "rabies shot");
		}

		@Test
		@DisplayName("Escribiendo por la entidad, @Version detecta el conflicto al confirmar")
		void laEntidadGestionadaDetectaElConflicto() {
			assertThatThrownBy(
					() -> edicion.editarConEntidadGestionada(2, "texto del usuario A", "texto del usuario B"))
				.isInstanceOf(OptimisticLockingFailureException.class);

			// Gana quien confirmo primero. El segundo se queda sin escribir, pero
			// al menos lo sabe: puede recargar y reintentar.
			assertThat(edicion.descripcionActual(2)).isEqualTo("texto del usuario B");

			edicion.restaurarDescripcion(2, "rabies shot");
		}

		@Test
		@DisplayName("Tras un UPDATE masivo la entidad en memoria se queda obsoleta")
		void elUpdateMasivoDejaElContextoDesincronizado() {
			EdicionConcurrenteService.EstadoDelContexto estado = edicion
				.desincronizacionTrasUpdateMasivo(4, "descripcion nueva");

			assertThat(estado.antes()).isEqualTo("spayed");
			// Esta es la linea del laboratorio: la base de datos ya dice otra cosa.
			assertThat(estado.enMemoria()).isEqualTo("spayed");
			assertThat(estado.trasRefrescar()).isEqualTo("descripcion nueva");

			edicion.restaurarDescripcion(4, "spayed");
		}

	}

	@Nested
	@DisplayName("3. Bloqueo pesimista")
	class BloqueoPesimista {

		@Test
		@DisplayName("PESSIMISTIC_WRITE bloquea la fila y genera SELECT ... FOR UPDATE")
		void laLecturaConLockBloqueaLaFila(CapturedOutput salida) {
			LockModeType modo = edicion.editarConBloqueoPesimista(3, "con bloqueo pesimista");

			assertThat(modo).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
			assertThat(salida.getAll().toLowerCase()).contains("for update");

			edicion.restaurarDescripcion(3, "neutered");
		}

		@Test
		@DisplayName("Una lectura normal no bloquea nada")
		void laLecturaNormalNoBloquea() {
			assertThat(edicion.leerSinBloqueo(3)).isEqualTo(LockModeType.NONE);
		}

	}

	@Nested
	@DisplayName("4. NESTED con JPA")
	class PropagacionNested {

		@Test
		@DisplayName("JpaTransactionManager no admite NESTED: falla al entrar, no al confirmar")
		void nestedNoEstaSoportado() {
			assertThatThrownBy(() -> edicion.intentarNested(4, "con nested"))
				.isInstanceOf(NestedTransactionNotSupportedException.class);

			// Ni siquiera llego a escribir: el gestor rechaza la propagacion antes
			// de ejecutar el metodo anidado.
			assertThat(edicion.descripcionActual(4)).isEqualTo("spayed");
		}

	}

	@Nested
	@DisplayName("5. Eventos ligados a la transaccion")
	class EventosTransaccionales {

		@BeforeEach
		void limpiarNotificaciones() {
			notificaciones.limpiar();
		}

		@Test
		@DisplayName("Si la transaccion confirma, los dos escuchadores se ejecutan")
		void alConfirmarSeEntreganLosDos() {
			registro.registrar(1, "cita confirmada");

			assertThat(notificaciones.getEntregadasAlPublicar()).containsExactly("cita confirmada");
			assertThat(notificaciones.getEntregadasAlConfirmar()).containsExactly("cita confirmada");
			assertThat(notificaciones.getEntregadasAlRevertir()).isEmpty();

			edicion.restaurarDescripcion(1, "rabies shot");
		}

		@Test
		@DisplayName("Si se revierte, el @EventListener normal ya ha enviado el correo fantasma")
		void alRevertirSoloSeEntregaElNoTransaccional() {
			assertThatThrownBy(() -> registro.registrarYFallar(1, "cita fantasma"))
				.isInstanceOf(IllegalStateException.class);

			// El dato no esta en la base de datos...
			assertThat(edicion.descripcionActual(1)).isEqualTo("rabies shot");

			// ...pero el escuchador normal ya actuo. Este es el bug.
			assertThat(notificaciones.getEntregadasAlPublicar()).containsExactly("cita fantasma");

			// El transaccional, en cambio, no se ejecuto: es lo que se buscaba.
			assertThat(notificaciones.getEntregadasAlConfirmar()).isEmpty();

			// Y el de AFTER_ROLLBACK si, que es lo que sirve para compensar.
			assertThat(notificaciones.getEntregadasAlRevertir()).containsExactly("cita fantasma");
		}

		@Test
		@DisplayName("Publicar sin transaccion descarta el evento transaccional en silencio")
		void sinTransaccionElEventoTransaccionalSePierde() {
			registro.registrarSinTransaccion(1, "sin transaccion");

			assertThat(notificaciones.getEntregadasAlPublicar()).containsExactly("sin transaccion");
			assertThat(notificaciones.getEntregadasAlConfirmar()).isEmpty();
			assertThat(notificaciones.getEntregadasAlRevertir()).isEmpty();
		}

	}

}
