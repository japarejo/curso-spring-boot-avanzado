package org.springframework.samples.petclinic.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * @author Michael Isvy Simple test to make sure that Bean Validation is working (useful
 * when upgrading to a new version of Hibernate Validator/ Bean Validation)
 */
class ValidatorTests {

	private Validator createValidator() {
		LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
		localValidatorFactoryBean.afterPropertiesSet();
		return localValidatorFactoryBean;
	}

	@Test
	void shouldNotValidateWhenSomethingIsEmpty() {
		// ARRANGEMENT:
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		Person person = new Person();
		person.setFirstName("");
		person.setLastName("smith");
		Validator validator = createValidator();
		// ACT:
		Set<ConstraintViolation<Person>> constraintViolations = validator.validate(person);
		// ASSERTIONs:
		assertThat(constraintViolations.size()).isEqualTo(1);
		ConstraintViolation<Person> violation = constraintViolations.iterator().next();
		assertThat(violation.getPropertyPath().toString()).isEqualTo("firstName");
		assertThat(violation.getMessage()).isEqualTo("must not be empty");

		// ARRANGEMENT:
		person.setFirstName("Manueeeeeee!!!");
		person.setLastName("");
		// ACT:
		constraintViolations = validator.validate(person);
		// ASSERTIONs:
		assertThat(constraintViolations.size()).isEqualTo(1);
		violation = constraintViolations.iterator().next();
		assertThat(violation.getPropertyPath().toString()).isEqualTo("lastName");
		assertThat(violation.getMessage()).isEqualTo("must not be empty");

	}


	@Test
	void pruebaDeValidador(){
		// ARRANGEMENT:
		Person japarejo=new Person();
		japarejo.setFirstName("José Antonio");
		japarejo.setLastName("Parejo Maestre");
		Validator validator = createValidator();
		// ACT:
		Set<ConstraintViolation<Person>> constraintViolations = validator.validate(japarejo);
		// ASSERTIONs:
		Assertions.assertTrue(constraintViolations.isEmpty());
	}

}
