package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.samples.petclinic.model.Diagnose;
import org.springframework.samples.petclinic.model.Disease;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@SpringJUnitConfig(VisitServiceTests.Config.class)
class VisitServiceTests {

	@Autowired
	VisitService service;

	@Autowired
	VisitRepository visitRepository;

	@Test
	void shouldRejectDiagnosisWhenDiseaseIsNotFeasibleForPetType() {
		Visit visit = visitWithPetType(petType(1, "cat"));
		Diagnose diagnose = diagnoseWithDiseaseForPetTypes(Set.of(petType(2, "dog")));
		visit.setDiagnose(diagnose);

		assertThatThrownBy(() -> service.create(visit))
			.isInstanceOf(ConstraintViolationException.class)
			.hasMessageContaining("According to our vademecum");

		verify(visitRepository, never()).save(visit);
	}

	private Visit visitWithPetType(PetType type) {
		Pet pet = new Pet();
		pet.setType(type);
		Visit visit = new Visit();
		visit.setPet(pet);
		visit.setDescription("Routine check");
		return visit;
	}

	private Diagnose diagnoseWithDiseaseForPetTypes(Set<PetType> feasibleTypes) {
		Disease disease = new Disease();
		disease.setName("Diabetes");
		disease.setDescription("Complex disease caused by insufficient insulin response.");
		disease.setPetTypeswithPrevalence(feasibleTypes);

		Diagnose diagnose = new Diagnose();
		diagnose.setDescription("Persistent clinical signs compatible with the disease.");
		diagnose.setDisease(disease);
		diagnose.setVet(new Vet());
		return diagnose;
	}

	private PetType petType(int id, String name) {
		PetType petType = new PetType();
		petType.setId(id);
		petType.setName(name);
		return petType;
	}

	static class Config {

		@Bean
		VisitService visitService() {
			return new VisitService();
		}

		@Bean
		VisitRepository visitRepository() {
			return mock(VisitRepository.class);
		}

		@Bean
		static MethodValidationPostProcessor methodValidationPostProcessor() {
			MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
			processor.setProxyTargetClass(true);
			return processor;
		}

	}

}
