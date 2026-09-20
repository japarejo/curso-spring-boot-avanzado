package org.springframework.samples.petclinic.testingexamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.testingexamples.PetAppointmentAdvisor.AppointmentType;

@DisplayName("Ejemplos JUnit 5 para cobertura y ciclo de vida")
class PetAppointmentAdvisorTests {

	private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

	private static List<String> classDiary;

	private List<String> testDiary;

	private PetAppointmentAdvisor weekdayAdvisor;

	@BeforeAll
	static void beforeAll() {
		classDiary = new ArrayList<>();
		classDiary.add("La clinica abre una vez antes de todos los tests");
	}

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		this.testDiary = new ArrayList<>();
		this.testDiary.add("Preparando: " + testInfo.getDisplayName());
		Clock fixedWednesday = Clock.fixed(Instant.parse("2026-05-13T10:15:30Z"), MADRID);
		this.weekdayAdvisor = new PetAppointmentAdvisor(fixedWednesday);
	}

	@AfterEach
	void afterEach() {
		this.testDiary.add("Limpiando datos temporales del test");
		assertThat(this.testDiary).hasSize(2);
	}

	@AfterAll
	static void afterAll() {
		classDiary.add("La clinica cierra una vez despues de todos los tests");
		assertThat(classDiary).containsExactly("La clinica abre una vez antes de todos los tests",
				"La clinica cierra una vez despues de todos los tests");
	}

	@Test
	@DisplayName("Una urgencia se detecta por palabras clave del sintoma")
	void shouldClassifyEmergencyBySymptomKeywords() {
		AppointmentType result = this.weekdayAdvisor.classify("Nala", 4, "Sangra por una pata");

		assertThat(result).isEqualTo(AppointmentType.EMERGENCY);
	}

	@ParameterizedTest(name = "{index}: {0}, {1} anios y sintoma \"{2}\" -> {3}")
	//@CsvSource(value={"Rufo,20,no come, SAME_DAY"})
	@CsvFileSource(resources = "/testingexamples/appointment-classification-scenarios.csv", numLinesToSkip = 1)
	@DisplayName("Clasificacion parametrizada de citas")
	void shouldClassifyDifferentAppointmentScenarios(String petName, int age, String symptom,
			AppointmentType expectedType) {
		assertThat(this.weekdayAdvisor.classify(petName, age, symptom)).isEqualTo(expectedType);
	}

	@ParameterizedTest(name = "Normaliza \"{0}\"")
	@ValueSource(strings = { " luna ", "MAX", "mika" })
	@DisplayName("Normalizacion de nombres con ValueSource")
	void shouldNormalizePetNames(String rawName) {
		assertThat(this.weekdayAdvisor.normalizePetName(rawName)).startsWith(rawName.trim().substring(0, 1).toUpperCase());
	}

	@ParameterizedTest(name = "Nombre vacio #{index}")
	@NullAndEmptySource
	@ValueSource(strings = { " ", "   " })
	@DisplayName("Nombres vacios usan un texto seguro")
	void shouldUseFallbackForBlankPetNames(String rawName) {
		assertThat(this.weekdayAdvisor.normalizePetName(rawName)).isEqualTo("Mascota sin nombre");
	}

	@Test
	@DisplayName("Un fin de semana convierte una cita normal en cita para hoy")
	void shouldRecommendSameDayAppointmentDuringWeekend() {
		Clock fixedSaturday = Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), MADRID);
		PetAppointmentAdvisor weekendAdvisor = new PetAppointmentAdvisor(fixedSaturday);

		assertThat(weekendAdvisor.classify("Trufa", 5, "revision")).isEqualTo(AppointmentType.SAME_DAY);
	}

	@Test
	@DisplayName("Los datos invalidos fallan rapido con un mensaje entendible")
	void shouldRejectInvalidAge() {
		assertThatThrownBy(() -> this.weekdayAdvisor.classify("Kira", -1, "revision"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("La edad no puede ser negativa");
	}

	@Test
	@Disabled("Ejemplo didactico: aparece como skipped en los informes de Surefire")
	@DisplayName("Ejemplo deshabilitado mientras negocio decide el criterio de cachorros")
	void disabledExampleForPendingBusinessRule() {
		assertThat(this.weekdayAdvisor.classify("Taco", 0, "primer chequeo")).isEqualTo(AppointmentType.SAME_DAY);
	}

	@Test
	@DisplayName("Ejemplo de ejeución con datos inválidos y nombre de mascota vacío")
	void executeWithBlankPetName()
	{
		assertThatThrownBy(() -> this.weekdayAdvisor.classify("",5,"prueba rutinaria"))
				.isInstanceOf(IllegalArgumentException.class);
	}


	@DisplayName("Si la mascota tiene 10 años o más y la descripción de los sintomas contiene no come, debe darsele cita el mismo dia")
	@CsvFileSource(resources = "/testingexamples/old-pet-eating-scenarios.csv", numLinesToSkip = 1)
	@ParameterizedTest
	void oldPetThatDoestNotEatGetSameDay(String petName, int edad, String sintomas, String resultAsString){
		AppointmentType expectedResult = AppointmentType.valueOf(resultAsString);
		// ACT
		AppointmentType result = this.weekdayAdvisor.classify(petName,edad,sintomas);
		// ASSERTION
		Assertions.assertEquals(result, expectedResult);
	}

}
