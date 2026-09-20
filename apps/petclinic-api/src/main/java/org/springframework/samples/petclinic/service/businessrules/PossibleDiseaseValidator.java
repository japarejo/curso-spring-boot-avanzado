package org.springframework.samples.petclinic.service.businessrules;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.Diagnose;
import org.springframework.samples.petclinic.model.Disease;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.stereotype.Component;

@Component
public class PossibleDiseaseValidator implements ConstraintValidator<ValidatePossibleDisease, Diagnose>{

	@Autowired
    VisitRepository repo;

	@Override
	public boolean isValid(Diagnose diagnose, ConstraintValidatorContext context) {
		Disease disease=diagnose.getDisease();
		PetType petType=diagnose.getVisit().getPet().getType();		
		return disease.getPetTypeswithPrevalence().contains(petType);
	}

}
