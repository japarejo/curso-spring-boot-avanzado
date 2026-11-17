package org.springframework.samples.petclinic.testingexamples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AlternativePetAppointmentAdvisorTests {

	PetAppointmentAdvisor advisor;
	int standardPetAge=5;
	String basicSymptom="nerviosismo"; 
	// Arrangement:		
	@BeforeEach 
	public void setup() {
		advisor = new PetAppointmentAdvisor(java.time.Clock.systemDefaultZone());
	}

	// Arrangement:		
	@ParameterizedTest
	@CsvSource({
		"Bufi, 5, vomitando sangre, EMERGENCY",
		"Max, 3, no respira, EMERGENCY",
		"Rocky, 12, tos persistente, SAME_DAY",
		"Buddy, 8, convulsion, EMERGENCY",
		"Charlie, 10, no come, SAME_DAY",
		"Lucy, 2, estornudos, ROUTINE_CHECKUP"
	})
	public void testUrgencyWhenSymptomContainsEmergencyKeyword(String petName, int petAge, String symptom, PetAppointmentAdvisor.AppointmentType oracle) {				
		// Act:
		PetAppointmentAdvisor.AppointmentType result = advisor.classify(petName, petAge, symptom);
		// Assert:		
		assertEquals(oracle,result,"El resultado de la clasificacion no es el esperado, debería ser EMERGENCY");
	}
	@ParameterizedTest
	@CsvSource ({
		"''",
		"'  '",
		"'   '"
	})
	public void negativeTest(String petName) {
		// Arrangement:				
		
		// Act & Assert:
		assertThrows(IllegalArgumentException.class, () -> {
			advisor.classify(petName, standardPetAge, basicSymptom);			
		}, "Se esperaba una excepcion de tipo IllegalArgumentException");
	}
	
}
