package org.springframework.samples.petclinic.testingexamples.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyString;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.service.AuthoritiesService;
import org.springframework.samples.petclinic.service.OwnerService;
import org.springframework.samples.petclinic.service.UserService;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mockito: pruebas aisladas sin contexto de Spring")
class OwnerServiceIsolatedMockTests {

	@Mock
	private OwnerRepository ownerRepository;

	@Mock
	private UserService userService;

	@Mock
	private AuthoritiesService authoritiesService;

	@Mock
	private Principal principal;

	private OwnerService ownerService;

	@BeforeEach
	void setUp() {
		this.ownerService = new OwnerService(this.ownerRepository);
		ReflectionTestUtils.setField(this.ownerService, "userService", this.userService);
		ReflectionTestUtils.setField(this.ownerService, "authoritiesService", this.authoritiesService);
	}

	@Test
	@DisplayName("Simula el repositorio y verifica que devuelve el propietario autenticado")
	void shouldFindAuthenticatedOwnerByIdWithMockedRepository() {
		Owner authenticatedOwner = owner(1, "George", "george");
		given(this.principal.getName()).willReturn("george");
		given(this.ownerRepository.findByUserName("george")).willReturn(authenticatedOwner);
		given(this.ownerRepository.findById(1)).willReturn(authenticatedOwner);

		Owner result = this.ownerService.findOwnerById(1, this.principal);

		assertThat(result).isSameAs(authenticatedOwner);
		verify(this.ownerRepository,times(1)).findByUserName(any(String.class));
		verify(this.ownerRepository).findById(1);
	}

	@Test
	@DisplayName("Comprueba una rama de seguridad sin tocar la base de datos")
	void shouldNotLoadOwnerDetailsWhenPrincipalDoesNotOwnRequestedId() {
		Owner authenticatedOwner = owner(1, "George", "george");
		given(this.principal.getName()).willReturn("george");
		given(this.ownerRepository.findByUserName("george")).willReturn(authenticatedOwner);

		Owner result = this.ownerService.findOwnerById(2, this.principal);

		assertThat(result).isNull();
		verify(this.ownerRepository).findByUserName("george");
		verify(this.ownerRepository, never()).findById(2);
	}

	@Test
	@DisplayName("Verifica colaboraciones al guardar propietario, usuario y permisos")
	void shouldSaveOwnerUserAndAuthoritiesWithMocks() {
		Owner owner = owner(null, "Clara", "clara");

		this.ownerService.saveOwner(owner);

		verify(this.ownerRepository).save(owner);
		verify(this.userService).saveUser(owner.getUser());
		verify(this.authoritiesService).saveAuthorities("clara", "owner");
	}

	private static Owner owner(Integer id, String firstName, String username) {
		User user = new User();
		user.setUsername(username);
		user.setPassword("testing");
		user.setEnabled(true);

		Owner owner = new Owner();
		owner.setId(id);
		owner.setFirstName(firstName);
		owner.setLastName("Testing");
		owner.setAddress("Calle Mayor 1");
		owner.setCity("Madrid");
		owner.setTelephone("600123123");
		owner.setUser(user);
		return owner;
	}

}
