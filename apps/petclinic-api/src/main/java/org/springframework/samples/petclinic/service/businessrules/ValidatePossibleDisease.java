package org.springframework.samples.petclinic.service.businessrules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {PossibleDiseaseValidator.class})
public @interface ValidatePossibleDisease {
	String message() default "According to our vademecum such pet type cannot develop that disease";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
