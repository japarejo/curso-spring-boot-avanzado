package org.springframework.samples.petclinic.web.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Disease;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.DiseaseService;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.samples.petclinic.service.VetService;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.samples.petclinic.web.api.dto.CreateDiagnosisRequest;
import org.springframework.samples.petclinic.web.api.dto.CreateVisitRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RestfulVisitController.class)
class RestfulVisitControllerTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	VisitService visitService;

	@MockBean
	PetService petService;

	@MockBean
	DiseaseService diseaseService;

	@MockBean
	VetService vetService;

	@Test
	void shouldCreateVisitWithDiagnosis() throws Exception {
		Pet pet = pet(1);
		Disease disease = disease(2);
		Vet vet = vet(3);
		given(petService.findPetById(1)).willReturn(pet);
		given(diseaseService.findById(2)).willReturn(Optional.of(disease));
		given(vetService.findById(3)).willReturn(Optional.of(vet));
		given(visitService.create(any(Visit.class))).willAnswer(invocation -> {
			Visit visit = invocation.getArgument(0);
			visit.setId(5);
			visit.getDiagnose().setId(6);
			return visit;
		});

		mockMvc.perform(post("/api/v1/visits")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.with(user("api-test"))
				.content(objectMapper.writeValueAsString(createRequest())))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/visits/5"))
			.andExpect(jsonPath("$.id").value(5))
			.andExpect(jsonPath("$.petId").value(1))
			.andExpect(jsonPath("$.diagnosis.id").value(6))
			.andExpect(jsonPath("$.diagnosis.diseaseId").value(2))
			.andExpect(jsonPath("$.diagnosis.vetId").value(3));

		ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
		verify(visitService).create(visitCaptor.capture());
		Visit visit = visitCaptor.getValue();
		org.assertj.core.api.Assertions.assertThat(visit.getDiagnose().getDisease()).isSameAs(disease);
		org.assertj.core.api.Assertions.assertThat(visit.getDiagnose().getVet()).isSameAs(vet);
		org.assertj.core.api.Assertions.assertThat(visit.getDiagnose().getVisit()).isSameAs(visit);
	}

	@Test
	void shouldReturnCreatedVisitJsonContent() throws Exception {
		Pet pet = pet(1);
		Disease disease = disease(2);
		Vet vet = vet(3);
		CreateVisitRequest request = createRequest();
		request.setDate(LocalDate.of(2026, 5, 18));
		given(petService.findPetById(1)).willReturn(pet);
		given(diseaseService.findById(2)).willReturn(Optional.of(disease));
		given(vetService.findById(3)).willReturn(Optional.of(vet));
		given(visitService.create(any(Visit.class))).willAnswer(invocation -> {
			Visit visit = invocation.getArgument(0);
			visit.setId(5);
			visit.getDiagnose().setId(6);
			return visit;
		});

		mockMvc.perform(post("/api/v1/visits")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.with(user("api-test"))
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(5))
			.andExpect(jsonPath("$.date").value("2026-05-18"))
			.andExpect(jsonPath("$.description").value("Routine check with mild respiratory symptoms"))
			.andExpect(jsonPath("$.petId").value(1))
			.andExpect(jsonPath("$.diagnosis.id").value(6))
			.andExpect(jsonPath("$.diagnosis.description").value("Persistent coughing and respiratory inflammation"))
			.andExpect(jsonPath("$.diagnosis.diseaseId").value(2))
			.andExpect(jsonPath("$.diagnosis.vetId").value(3));
	}

	@Test
	void shouldReturnNotFoundWhenPetDoesNotExist() throws Exception {
		given(petService.findPetById(1)).willReturn(null);

		mockMvc.perform(post("/api/v1/visits")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.with(user("api-test"))
				.content(objectMapper.writeValueAsString(createRequest())))
			.andExpect(status().isNotFound());
	}

	@Test
	void shouldRejectInvalidVisitPayload() throws Exception {
		CreateVisitRequest request = createRequest();
		request.setDescription("");

		mockMvc.perform(post("/api/v1/visits")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.with(user("api-test"))
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("description:")));
	}

	@Test
	void shouldReturnReadableDetailWhenServiceValidationFails() throws Exception {
		Pet pet = pet(1);
		Disease disease = disease(2);
		Vet vet = vet(3);
		given(petService.findPetById(1)).willReturn(pet);
		given(diseaseService.findById(2)).willReturn(Optional.of(disease));
		given(vetService.findById(3)).willReturn(Optional.of(vet));

		Path path = mock(Path.class);
		given(path.toString()).willReturn("create.visit.diagnose");
		ConstraintViolation<?> violation = mock(ConstraintViolation.class);
		given(violation.getPropertyPath()).willReturn(path);
		given(violation.getMessage()).willReturn("According to our vademecum such pet type cannot develop that disease");
		ConstraintViolationException validationException = new ConstraintViolationException(Set.of(violation));
		given(visitService.create(any(Visit.class))).willThrow(validationException);

		mockMvc.perform(post("/api/v1/visits")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.with(user("api-test"))
				.content(objectMapper.writeValueAsString(createRequest())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(
				"create.visit.diagnose: According to our vademecum such pet type cannot develop that disease"));
	}

	private CreateVisitRequest createRequest() {
		CreateVisitRequest request = new CreateVisitRequest();
		request.setDescription("Routine check with mild respiratory symptoms");
		request.setPetId(1);
		CreateDiagnosisRequest diagnosis = new CreateDiagnosisRequest();
		diagnosis.setDescription("Persistent coughing and respiratory inflammation");
		diagnosis.setDiseaseId(2);
		diagnosis.setVetId(3);
		request.setDiagnosis(diagnosis);
		return request;
	}

	private Pet pet(int id) {
		Pet pet = new Pet();
		pet.setId(id);
		PetType type = new PetType();
		type.setId(1);
		type.setName("cat");
		pet.setType(type);
		return pet;
	}

	private Disease disease(int id) {
		Disease disease = new Disease();
		disease.setId(id);
		disease.setName("Diabetes");
		return disease;
	}

	private Vet vet(int id) {
		Vet vet = new Vet();
		vet.setId(id);
		return vet;
	}

}
