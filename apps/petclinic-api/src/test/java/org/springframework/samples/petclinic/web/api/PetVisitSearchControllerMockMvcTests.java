package org.springframework.samples.petclinic.web.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PetVisitSearchController.class)
@WithMockUser()
class PetVisitSearchControllerMockMvcTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	PetService petService;

	@Test
	void shouldReturnPetsWithLeftJoinExplanation() throws Exception {
		// ARRANGEMENT:
		Pet petWithMatchingVisit = pet(1, "Leo", "cat", "George Franklin",
				visit(11, LocalDate.of(2026, 2, 1), "annual rabies shot"));
		Pet petWithoutMatchingVisit = pet(2, "Molly", "dog", "Helen Franklin",
				visit(12, LocalDate.of(2026, 3, 1), "dental cleaning"));
		given(petService.searchPetsWithVisits(eq(null), eq("Franklin"), eq("rabies"), eq(true)))
			.willReturn(List.of(petWithMatchingVisit, petWithoutMatchingVisit));
		// ACT:
		mockMvc.perform(get("/api/v2/pets")
				.param("ownerLastName", "Franklin")
				.param("visitDescription", "rabies")
				.param("joinMode", "left")
				.accept(MediaType.APPLICATION_JSON))
		// ASSERTION:
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.joinMode").value("left"))
			.andExpect(jsonPath("$.visitDescriptionFilter").value("rabies"))
			.andExpect(jsonPath("$.resultCount").value(2))
			.andExpect(jsonPath("$.sqlApproximado").value(org.hamcrest.Matchers.containsString("LEFT JOIN")))
			.andExpect(jsonPath("$.sqlApproximado").value(org.hamcrest.Matchers.containsString("OR v.id IS NULL")))
			.andExpect(jsonPath("$.pets[0].petName").value("Leo"))
			.andExpect(jsonPath("$.pets[0].ownerFullName").value("George Franklin"))
			.andExpect(jsonPath("$.pets[0].hasMatchingVisit").value(true))
			.andExpect(jsonPath("$.pets[0].visits[0].description").value("annual rabies shot"	))
			.andExpect(jsonPath("$.pets[1].petName").value("Molly"))
			.andExpect(jsonPath("$.pets[1].hasMatchingVisit").value(false));

		verify(petService).searchPetsWithVisits(null, "Franklin", "rabies", true);
	}

	@Test
	void shouldReturnOnlyMatchingPetsInInnerJoinMode() throws Exception {
		Pet petWithMatchingVisit = pet(1, "Leo", "cat", "George Franklin",
				visit(11, LocalDate.of(2026, 2, 1), "annual rabies shot"));
		given(petService.searchPetsWithVisits(eq("Leo"), eq(null), eq("rabies"), eq(false)))
			.willReturn(List.of(petWithMatchingVisit));

		mockMvc.perform(get("/api/v2/pets")
				.param("petName", "Leo")
				.param("visitDescription", "rabies")
				.param("joinMode", "inner")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.joinMode").value("inner"))
			.andExpect(jsonPath("$.resultCount").value(1))
			.andExpect(jsonPath("$.sqlApproximado").value(org.hamcrest.Matchers.containsString("Sin OR")))
			.andExpect(jsonPath("$.pets[0].hasMatchingVisit").value(true));

		verify(petService).searchPetsWithVisits("Leo", null, "rabies", false);
	}

	@Test
	void shouldRejectInvalidJoinMode() throws Exception {
		mockMvc.perform(get("/api/v2/pets")
				.param("visitDescription", "rabies")
				.param("joinMode", "outer")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Peticion no valida"))
			.andExpect(jsonPath("$.detail").value("joinMode debe ser 'left' o 'inner', valor recibido: outer"));
	}

	private Pet pet(int id, String name, String typeName, String ownerFullName, Visit... visits) {
		String[] ownerNames = ownerFullName.split(" ", 2);
		Owner owner = new Owner();
		owner.setId(id + 100);
		owner.setFirstName(ownerNames[0]);
		owner.setLastName(ownerNames[1]);
		Pet pet = new Pet();
		pet.setId(id);
		pet.setName(name);
		PetType type = new PetType();
		type.setName(typeName);
		pet.setType(type);
		owner.addPet(pet);
		for (Visit visit : visits) {
			pet.addVisit(visit);
		}
		return pet;
	}

	private Visit visit(int id, LocalDate date, String description) {
		Visit visit = new Visit();
		visit.setId(id);
		visit.setDate(date);
		visit.setDescription(description);
		return visit;
	}

}
