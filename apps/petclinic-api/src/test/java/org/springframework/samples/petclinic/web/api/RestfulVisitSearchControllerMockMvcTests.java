package org.springframework.samples.petclinic.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RestfulVisitSearchController.class)
@WithMockUser
class RestfulVisitSearchControllerMockMvcTests {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	VisitService visitService;

	@Test
	void shouldSearchVisitsAndReturnPagedJson() throws Exception {
		Visit visit = visit(5, LocalDate.of(2026, 5, 18), "annual rabies shot", "Leo", "cat", "Franklin");
		PageRequest expectedPage = PageRequest.of(1, 1, Sort.by(Sort.Direction.ASC, "pet.name"));
		given(visitService.search(eq(null), eq("Franklin"), eq("Leo"), eq("cat"), eq("rabies"),
				eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31)), eq(expectedPage)))
			.willReturn(new PageImpl<>(List.of(visit), expectedPage, 3));

		mockMvc.perform(get("/api/v1/visits")
				.param("ownerLastName", " Franklin ")
				.param("petName", "Leo")
				.param("petType", "cat")
				.param("description", "rabies")
				.param("fromDate", "2026-01-01")
				.param("toDate", "2026-12-31")
				.param("page", "1")
				.param("size", "1")
				.param("sort", "petName,asc")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(5))
			.andExpect(jsonPath("$.content[0].date").value("2026-05-18"))
			.andExpect(jsonPath("$.content[0].description").value("annual rabies shot"))
			.andExpect(jsonPath("$.content[0].pet.name").value("Leo"))
			.andExpect(jsonPath("$.content[0].pet.type").value("cat"))
			.andExpect(jsonPath("$.content[0].owner.lastName").value("Franklin"))
			.andExpect(jsonPath("$.page.number").value(1))
			.andExpect(jsonPath("$.page.size").value(1))
			.andExpect(jsonPath("$.page.totalElements").value(3))
			.andExpect(jsonPath("$.page.totalPages").value(3))
			.andExpect(jsonPath("$.filters.ownerLastName").value("Franklin"))
			.andExpect(jsonPath("$.filters.petName").value("Leo"))
			.andExpect(jsonPath("$.sort").value("petName,asc"))
			.andExpect(jsonPath("$.links.self").exists())
			.andExpect(jsonPath("$.links.previous").exists())
			.andExpect(jsonPath("$.links.next").exists());
	}

	@Test
	void shouldSendTranslatedSortToVisitService() throws Exception {
		given(visitService.search(eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		mockMvc.perform(get("/api/v1/visits")
				.param("sort", "ownerLastName,desc")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		verify(visitService).search(eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
				pageableCaptor.capture());
		Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("pet.owner.lastName");
		assertThat(order).isNotNull();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void shouldRejectUnsupportedSortField() throws Exception {
		mockMvc.perform(get("/api/v1/visits")
				.param("sort", "unknown,asc")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Peticion no valida"))
			.andExpect(jsonPath("$.detail").value("Unsupported sort field: unknown"));
	}

	@Test
	void shouldRejectInvalidDateRange() throws Exception {
		mockMvc.perform(get("/api/v1/visits")
				.param("fromDate", "2026-12-31")
				.param("toDate", "2026-01-01")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("fromDate must be before or equal to toDate"));
	}

	private Visit visit(int id, LocalDate date, String description, String petName, String petTypeName,
			String ownerLastName) {
		Owner owner = new Owner();
		owner.setId(10);
		owner.setFirstName("George");
		owner.setLastName(ownerLastName);
		Pet pet = new Pet();
		pet.setId(20);
		pet.setName(petName);
		PetType petType = new PetType();
		petType.setName(petTypeName);
		pet.setType(petType);
		owner.addPet(pet);
		Visit visit = new Visit();
		visit.setId(id);
		visit.setDate(date);
		visit.setDescription(description);
		visit.setPet(pet);
		return visit;
	}

}
