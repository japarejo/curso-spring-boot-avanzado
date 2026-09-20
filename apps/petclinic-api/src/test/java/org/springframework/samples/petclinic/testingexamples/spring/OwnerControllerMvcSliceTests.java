package org.springframework.samples.petclinic.testingexamples.spring;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.samples.petclinic.configuration.SecurityConfiguration;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.service.AuthoritiesService;
import org.springframework.samples.petclinic.service.OwnerService;
import org.springframework.samples.petclinic.service.UserService;
import org.springframework.samples.petclinic.web.OwnerController;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

@WebMvcTest(controllers = OwnerController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfigurer.class),
		excludeAutoConfiguration = SecurityConfiguration.class)
@DisplayName("@WebMvcTest: slice MVC con MockMvc")
class OwnerControllerMvcSliceTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerService ownerService;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private AuthoritiesService authoritiesService;

	//@ParameterizedTest
	/*@CsvSource(value={
			"owner, 200",
			"vet, 403",
			"admin, 200"
	})*/
	@Test
	@WithMockUser(username = "japarejo",authorities = {"owner","ver"})
	@DisplayName("Prueba una ruta MVC sin arrancar base de datos ni servidor HTTP real")
	//void shouldRenderOwnerDetailsWithMockedService(String authority, int responseCode) throws Exception {
	void shouldRenderOwnerDetailsWithMockedService() throws Exception {
		//String[] authorities={authority};
		//SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("japarejo","1234",authorities));

		// ARRANGEMENT:
		Owner owner = new Owner();
		owner.setId(1);
		owner.setFirstName("George");
		owner.setLastName("Franklin");
		owner.setAddress("110 W. Liberty St.");
		owner.setCity("Madison");
		owner.setTelephone("6085551023");
		given(this.ownerService.findOwnerById(eq(1), any())).willReturn(owner);
		// ACT
		this.mockMvc.perform(get("/owners/{ownerId}", 1))
		// ASSERTIONS:
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownerDetails"))
			.andExpect(model().attribute("owner", hasProperty("firstName", is("George"))))
			.andExpect(model().attribute("owner", hasProperty("lastName", is("Franklin"))));
		verify(this.ownerService).findOwnerById(anyInt(),any(Principal.class));
	}

}
