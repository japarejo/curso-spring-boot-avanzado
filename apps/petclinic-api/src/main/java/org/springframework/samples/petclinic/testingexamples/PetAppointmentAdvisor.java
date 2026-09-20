package org.springframework.samples.petclinic.testingexamples;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

public class PetAppointmentAdvisor {

	public enum AppointmentType {

		ROUTINE_CHECKUP, SAME_DAY, EMERGENCY

	}

	private final Clock clock;

	private final String DOES_NOT_EAT ="no come";

	public PetAppointmentAdvisor(Clock clock) {
		this.clock = clock;
	}

	public AppointmentType classify(String petName, int age, String symptom) {
		if (isBlank(petName)) {
			throw new IllegalArgumentException("El nombre de la mascota es obligatorio");
		}
		if (age < 0) {
			throw new IllegalArgumentException("La edad no puede ser negativa");
		}
		if (containsEmergencyKeyword(symptom)) {
			return AppointmentType.EMERGENCY;
		}
		if (age >= 10 && containsDoesNotEatKeyword(symptom)) {
			return AppointmentType.SAME_DAY;
		}
		if (age >= 12 || isWeekend()) {
			return AppointmentType.SAME_DAY;
		}
		return AppointmentType.ROUTINE_CHECKUP;
	}

	public boolean containsDoesNotEatKeyword(String symptom){
		return symptom.contains(DOES_NOT_EAT);
	}

	public String friendlySummary(String petName, int age, String symptom) {
		AppointmentType appointmentType = classify(petName, age, symptom);
		String normalizedName = normalizePetName(petName);
		return switch (appointmentType) {
			case EMERGENCY -> normalizedName + " debe pasar a urgencias";
			case SAME_DAY -> normalizedName + " necesita cita hoy";
			case ROUTINE_CHECKUP -> normalizedName + " puede esperar a una revision rutinaria";
		};
	}

	public String normalizePetName(String petName) {
		if (isBlank(petName)) {
			return "Mascota sin nombre";
		}
		String lowerCaseName = petName.trim().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowerCaseName.charAt(0)) + lowerCaseName.substring(1);
	}

	private boolean containsEmergencyKeyword(String symptom) {
		if (isBlank(symptom)) {
			return false;
		}
		String lowerCaseSymptom = symptom.toLowerCase(Locale.ROOT);
		return lowerCaseSymptom.contains("sangra") || lowerCaseSymptom.contains("no respira")
				|| lowerCaseSymptom.contains("convulsion");
	}

	private boolean isWeekend() {
		DayOfWeek dayOfWeek = LocalDate.now(this.clock).getDayOfWeek();
		return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}
