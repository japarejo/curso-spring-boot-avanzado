package org.springframework.samples.petclinic.web.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.service.OwnerService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OwnerViewsDemoRestController.class)
@WithMockUser
class OwnerViewsDemoRestControllerMockMvcTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	OwnerService ownerService;

	@Test
	void listadoUsesPublicView() throws Exception {
		given(ownerService.findOwnerByLastName("")).willReturn(List.of(george()));

		mockMvc.perform(get("/api/v1/views-demo/owners").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].lastName").value("Franklin"))
			.andExpect(jsonPath("$[0].city").value("Madison"))
			.andExpect(jsonPath("$[0].telephone").doesNotExist())
			.andExpect(jsonPath("$[0].username").doesNotExist());
	}

	@Test
	void detalleUsesReceptionView() throws Exception {
		given(ownerService.findOwnerById(1)).willReturn(george());

		mockMvc.perform(get("/api/v1/views-demo/owners/{id}", 1).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.city").value("Madison"))
			.andExpect(jsonPath("$.telephone").value("6085551023"))
			.andExpect(jsonPath("$.version").doesNotExist())
			.andExpect(jsonPath("$.username").doesNotExist());
	}

	@Test
	void porRolDefaultsToPublicView() throws Exception {
		given(ownerService.findOwnerById(1)).willReturn(george());

		mockMvc.perform(get("/api/v1/views-demo/owners/{id}/por-rol", 1).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.city").value("Madison"))
			.andExpect(jsonPath("$.address").doesNotExist());
	}

	@Test
	void porRolAdminShowsEverything() throws Exception {
		given(ownerService.findOwnerById(1)).willReturn(george());

		mockMvc.perform(get("/api/v1/views-demo/owners/{id}/por-rol", 1).param("rol", "ADMIN")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.telephone").value("6085551023"))
			.andExpect(jsonPath("$.version").value(3))
			.andExpect(jsonPath("$.username").value("owner1"));
	}

	@Test
	void porRolRejectsUnknownRole() throws Exception {
		mockMvc.perform(get("/api/v1/views-demo/owners/{id}/por-rol", 1).param("rol", "jefe")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest());
	}

	@Test
	void returns404WhenOwnerDoesNotExist() throws Exception {
		given(ownerService.findOwnerById(99)).willReturn(null);

		mockMvc.perform(get("/api/v1/views-demo/owners/{id}", 99).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}

	private static Owner george() {
		User user = new User();
		user.setUsername("owner1");
		Owner owner = new Owner();
		owner.setId(1);
		owner.setVersion(3);
		owner.setFirstName("George");
		owner.setLastName("Franklin");
		owner.setAddress("110 W. Liberty St.");
		owner.setCity("Madison");
		owner.setTelephone("6085551023");
		owner.setUser(user);
		return owner;
	}

}
