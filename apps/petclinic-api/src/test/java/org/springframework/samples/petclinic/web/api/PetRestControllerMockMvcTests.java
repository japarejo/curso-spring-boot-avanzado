package org.springframework.samples.petclinic.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PetRestController.class)
@WithMockUser
class PetRestControllerMockMvcTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	PetService petService;

	@Test
	void shouldListPetsUsingMockMvc() throws Exception {
		given(petService.findAllPets()).willReturn(List.of(pet(1, "Leo", "cat"), pet(2, "Bella", "dog")));

		mockMvc.perform(get("/api/pets").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].name").value("Leo"))
			.andExpect(jsonPath("$[0].type").value("cat"))
			.andExpect(jsonPath("$[1].id").value(2))
			.andExpect(jsonPath("$[1].name").value("Bella"))
			.andExpect(jsonPath("$[1].type").value("dog"));
	}

	@Test
	void shouldFindPetByIdUsingMockMvc() throws Exception {
		given(petService.findPetById(1)).willReturn(pet(1, "Leo", "cat"));

		mockMvc.perform(get("/api/pets/{id}", 1).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("Leo"))
			.andExpect(jsonPath("$.birthDate").value("2021/03/14"))
			.andExpect(jsonPath("$.type").value("cat"));
	}

	@Test
	void shouldCreatePetAndPassBodyToService() throws Exception {
		Pet request = new Pet();
		request.setName("Max");
		request.setBirthDate(LocalDate.of(2022, 1, 5));

		mockMvc.perform(post("/api/pets")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Max"))
			.andExpect(jsonPath("$.birthDate").value("2022/01/05"));

		ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
		verify(petService).savePet(petCaptor.capture());
		org.assertj.core.api.Assertions.assertThat(petCaptor.getValue().getName()).isEqualTo("Max");
	}

	@Test
	void shouldRejectInvalidPetBody() throws Exception {
		mockMvc.perform(post("/api/pets")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content("""
					{"name":"Bo"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Peticion no valida"))
			.andExpect(jsonPath("$.detail").value("name: size must be between 3 and 50"));
	}

	@Test
	void shouldDeletePetUsingMockMvc() throws Exception {
		mockMvc.perform(delete("/api/pets/{id}", 7).with(csrf()))
			.andExpect(status().isOk());

		verify(petService).deletePet(7);
	}

	private Pet pet(int id, String name, String typeName) {
		Pet pet = new Pet();
		pet.setId(id);
		pet.setName(name);
		pet.setBirthDate(LocalDate.of(2021, 3, 14));
		PetType type = new PetType();
		type.setId(id);
		type.setName(typeName);
		pet.setType(type);
		return pet;
	}

}
