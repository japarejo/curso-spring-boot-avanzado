package org.springframework.samples.petclinic.web.api;

import java.net.URI;
import java.util.Optional;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Diagnose;
import org.springframework.samples.petclinic.model.Disease;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.DiseaseService;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.samples.petclinic.service.VetService;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.samples.petclinic.web.api.dto.CreateDiagnosisRequest;
import org.springframework.samples.petclinic.web.api.dto.CreateVisitRequest;
import org.springframework.samples.petclinic.web.api.dto.VisitResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Visits", description = "Visit retrieval REST API")
@RestController
@RequestMapping("/api/v1/visits")
public class RestfulVisitController {

	VisitService visitService;

	PetService petService;

	DiseaseService diseaseService;

	VetService vetService;
	
	@Autowired
	public RestfulVisitController(VisitService visitService, PetService petService, DiseaseService diseaseService,
			VetService vetService) {
		this.visitService=visitService;
		this.petService = petService;
		this.diseaseService = diseaseService;
		this.vetService = vetService;
	}
	
	@Operation(summary = "Get visit by ID", description = "Returns the details for a single visit.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Visit returned successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Visit.class))),
		@ApiResponse(responseCode = "404", description = "Visit not found", content = @Content)
	})
	@GetMapping("/{id}")
	public Visit getVisit(@Parameter(description = "Visit identifier", required = true) @PathVariable("id")Integer id) {
		Optional<Visit> visit=visitService.findById(id);
		if(visit.isPresent())			
			return visit.get();
		else
			throw new ResourceNotFoundException();
	}

	@Operation(summary = "Create a visit", description = "Creates a visit and, optionally, an associated diagnosis. "
			+ "When a diagnosis is supplied, the disease must be feasible for the pet type.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Visit created successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisitResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid payload or infeasible disease diagnosis",
			content = @Content),
		@ApiResponse(responseCode = "404", description = "Referenced pet, disease, or vet not found",
			content = @Content)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Visit creation payload", required = true,
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateVisitRequest.class)))
	@PostMapping
	public ResponseEntity<VisitResponse> createVisit(@RequestBody @Valid CreateVisitRequest request) {
		Pet pet = petService.findPetById(request.getPetId());
		if (pet == null) {
			throw new ResourceNotFoundException();
		}

		Visit visit = toVisit(request, pet);
		Diagnose diagnose = toDiagnose(request.getDiagnosis());
		visit.setDiagnose(diagnose);

		visitService.create(visit);

		URI location = URI.create("/api/v1/visits/" + visit.getId());
		return ResponseEntity.created(location).body(VisitResponse.from(visit, visit.getDiagnose()));
	}

	private Visit toVisit(CreateVisitRequest request, Pet pet) {
		Visit visit = new Visit();
		if (request.getDate() != null) {
			visit.setDate(request.getDate());
		}
		visit.setDescription(request.getDescription());
		visit.setPet(pet);
		return visit;
	}

	private Diagnose toDiagnose(CreateDiagnosisRequest request) {
		if (request == null) {
			return null;
		}

		Disease disease = diseaseService.findById(request.getDiseaseId())
			.orElseThrow(ResourceNotFoundException::new);
		Vet vet = vetService.findById(request.getVetId())
			.orElseThrow(ResourceNotFoundException::new);

		Diagnose diagnose = new Diagnose();
		diagnose.setDescription(request.getDescription());
		diagnose.setDisease(disease);
		diagnose.setVet(vet);
		return diagnose;
	}
}
