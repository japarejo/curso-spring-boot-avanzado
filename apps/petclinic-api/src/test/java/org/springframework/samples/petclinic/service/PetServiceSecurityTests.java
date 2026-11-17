package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.web.api.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Service;

/**
 * Seguridad por metodo de {@link PetService#deletePet(int)}.
 * Datos de data.sql: la mascota 1 (Leo) es de owner1 y la 2 (Basil) de owner2.
 */
@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
@Import({ ValidationAutoConfiguration.class, PetSecurity.class, PetServiceSecurityTests.MethodSecurity.class })
class PetServiceSecurityTests {

	@Autowired
	PetService petService;

	@Autowired
	PetRepository petRepository;

	@Test
	@WithMockUser(username = "owner2", authorities = "owner")
	void unOwnerNoPuedeBorrarLaMascotaDeOtroYEstaSigueExistiendo() {
		assertThatThrownBy(() -> petService.deletePet(1)).isInstanceOf(AccessDeniedException.class);

		assertThat(petRepository.findById(1)).isNotNull();
	}

	@Test
	@WithMockUser(username = "owner1", authorities = "owner")
	void unOwnerPuedeBorrarSuPropiaMascota() {
		petService.deletePet(1);

		assertThat(petRepository.findById(1)).isNull();
	}

	@Test
	@WithMockUser(username = "admin1", authorities = "admin")
	void unAdminPuedeBorrarCualquierMascota() {
		petService.deletePet(2);

		assertThat(petRepository.findById(2)).isNull();
	}

	@Test
	@WithMockUser(username = "admin1", authorities = "admin")
	void unAdminRecibeNotFoundSiLaMascotaNoExiste() {
		assertThatThrownBy(() -> petService.deletePet(9999)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@WithMockUser(username = "owner1", authorities = "owner")
	void unOwnerRecibeAccesoDenegadoSiLaMascotaNoExiste() {
		assertThatThrownBy(() -> petService.deletePet(9999)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(username = "vet1", authorities = "vet")
	void otrasAutoridadesNoPuedenBorrar() {
		assertThatThrownBy(() -> petService.deletePet(1)).isInstanceOf(AccessDeniedException.class);

		assertThat(petRepository.findById(1)).isNotNull();
	}

	@TestConfiguration
	@EnableMethodSecurity
	static class MethodSecurity {

	}

}
