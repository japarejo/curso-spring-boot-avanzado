// =============================================================================
// Modulo 4 - Ejercicio 14: pruebas que se leen como una especificacion.
//
// Prueba lista para ejecutar. Copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/testingexamples/
// =============================================================================
package org.springframework.samples.petclinic.testingexamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.testingexamples.PetAppointmentAdvisor.AppointmentType;

@DisplayName("PetAppointmentAdvisor")
class Ejercicio14PruebasAnidadasTests {

	/** 12 de octubre de 2026: lunes. */
	private static final Clock LUNES = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC);

	/** 17 de octubre de 2026: sabado. */
	private static final Clock SABADO = Clock.fixed(Instant.parse("2026-10-17T10:00:00Z"), ZoneOffset.UTC);

	PetAppointmentAdvisor advisor;

	@Nested
	@DisplayName("cuando los datos no son validos")
	class DatosNoValidos {

		@BeforeEach
		void preparar() {
			advisor = new PetAppointmentAdvisor(LUNES);
		}

		@Test
		@DisplayName("rechaza un nombre vacio, con un mensaje que lo explica")
		void nombreVacio() {
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
					() -> advisor.classify("  ", 3, "tos"));
			assertEquals("El nombre de la mascota es obligatorio", ex.getMessage());
		}

		@Test
		@DisplayName("rechaza una edad negativa")
		void edadNegativa() {
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
					() -> advisor.classify("Leo", -1, "tos"));
			assertThat(ex.getMessage()).contains("negativa");
		}

	}

	@Nested
	@DisplayName("cuando hay sintomas de urgencia")
	@Tag("critico")
	class Urgencias {

		@BeforeEach
		void preparar() {
			advisor = new PetAppointmentAdvisor(LUNES);
		}

		@ParameterizedTest(name = "\"{0}\" -> EMERGENCY")
		@ValueSource(strings = { "sangra mucho", "NO RESPIRA bien", "tuvo una convulsion" })
		@DisplayName("clasifica como EMERGENCY")
		void urgencia(String sintoma) {
			assertEquals(AppointmentType.EMERGENCY, advisor.classify("Leo", 3, sintoma));
		}

	}

	@Nested
	@DisplayName("en un dia laborable")
	class DiaLaborable {

		@BeforeEach
		void preparar() {
			advisor = new PetAppointmentAdvisor(LUNES);
		}

		@Test
		@DisplayName("una mascota de 11 anios va a revision")
		void onceAnios() {
			assertEquals(AppointmentType.ROUTINE_CHECKUP, advisor.classify("Leo", 11, "tos leve"));
		}

		@Test
		@DisplayName("una mascota de 12 anios va el mismo dia")
		void doceAnios() {
			assertEquals(AppointmentType.SAME_DAY, advisor.classify("Leo", 12, "tos leve"));
		}

		@Test
		@DisplayName("el resumen dice lo mismo que la clasificacion (assertAll)")
		void resumen() {
			// Si fallan dos, JUnit informa de LOS DOS. Con assertEquals seguidos,
			// solo veriamos el primero.
			assertAll("resumenes",
					() -> assertEquals("Leo puede esperar a una revision rutinaria",
							advisor.friendlySummary("  leo ", 3, "tos")),
					() -> assertEquals("Rosy necesita cita hoy", advisor.friendlySummary("ROSY", 13, "tos")),
					() -> assertEquals("Basil debe pasar a urgencias", advisor.friendlySummary("basil", 2, "sangra")));
		}

	}

	@Nested
	@DisplayName("en fin de semana")
	class FinDeSemana {

		@BeforeEach
		void preparar() {
			advisor = new PetAppointmentAdvisor(SABADO);
		}

		@Test
		@DisplayName("todo lo que no es urgente va el mismo dia")
		void mismoDia() {
			assertEquals(AppointmentType.SAME_DAY, advisor.classify("Leo", 2, "tos leve"));
		}

	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	@DisplayName("solo en Windows: aparece como 'skipped' en Linux y macOS")
	void soloEnWindows() {
		assertTrue(System.getProperty("os.name").startsWith("Windows"));
	}

}
