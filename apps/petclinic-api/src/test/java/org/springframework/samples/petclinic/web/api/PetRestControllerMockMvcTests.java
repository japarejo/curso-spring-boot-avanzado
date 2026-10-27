package org.springframework.samples.petclinic.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.configuration.GenericIdToEntityConverter;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.samples.petclinic.web.PetTypeFormatter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PetRestController.class)
@WithMockUser
class PetRestControllerMockMvcTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	PetService petService;

	// Los deserializadores JSON de Pet dependen de estos beans, que @WebMvcTest
	// no carga. El dueño llega como id ("owner": 1) y lo resuelve el conversor;
	// sin él quedaría a null y fallaría el @NotNull de Pet.owner.
	@MockitoBean
	GenericIdToEntityConverter idToEntityConverter;

	@MockitoBean
	PetTypeFormatter petTypeFormatter;

	@BeforeEach
	void ownerOneExists() {
		Owner owner = new Owner();
		owner.setId(1);
		given(idToEntityConverter.convert(1, Owner.class)).willReturn(owner);
	}

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
	void shouldReturn404WhenPetDoesNotExist() throws Exception {
		given(petService.findPetById(99999)).willReturn(null);

		mockMvc.perform(get("/api/pets/{id}", 99999).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Recurso no encontrado"));
	}

	@Test
	void shouldReturn400WhenIdIsNotANumber() throws Exception {
		// Antes caia en el catch-all de GlobalExceptionHandler y respondia 500.
		mockMvc.perform(get("/api/pets/{id}", "abc").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Peticion no valida"));
	}

	@Test
	void shouldReturn400WhenBodyIsNotJson() throws Exception {
		// Antes caia en el catch-all de GlobalExceptionHandler y respondia 500.
		mockMvc.perform(post("/api/pets")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content("{ esto no es json"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldCreatePetAndPassBodyToService() throws Exception {
		Pet request = new Pet();
		request.setName("Max");
		request.setBirthDate(LocalDate.of(2022, 1, 5));
		// El servicio es un mock: se imita a JPA asignando el id al guardar.
		willAnswer(invocation -> {
			invocation.<Pet>getArgument(0).setId(42);
			return null;
		}).given(petService).savePet(any(Pet.class));

		mockMvc.perform(post("/api/pets")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(withOwner(request, 1)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/pets/42"))
			.andExpect(jsonPath("$.id").value(42))
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
					{"name":"Bo","owner":1}
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

	private String withOwner(Pet pet, int ownerId) {
		ObjectNode body = objectMapper.valueToTree(pet);
		body.put("owner", ownerId);
		return body.toString();
	}
}
