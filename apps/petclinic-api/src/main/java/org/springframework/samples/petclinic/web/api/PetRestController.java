package org.springframework.samples.petclinic.web.api;

import java.util.Collection;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Pets", description = "Pet management REST API")
@RestController
@RequestMapping("/api/pets")
public class PetRestController {
	
	@Autowired
	private PetService petService;
	
	@Operation(summary = "List all pets", description = "Returns a collection of all registered pets.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Pets returned successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class)))
	})
	@GetMapping
	public Collection<Pet> findAll() {
		return petService.findAllPets();
	}
	
	@Operation(summary = "Get pet by ID", description = "Returns the pet for the requested identifier.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Pet returned successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class))),
		@ApiResponse(responseCode = "404", description = "Pet not found", content = @Content)
	})
	@GetMapping("/{id}")
	public Pet findbyId(@Parameter(description = "Pet identifier", required = true) @PathVariable("id") int id) {
		return petService.findPetById(id);
	}
	
	@Operation(summary = "Create a pet", description = "Creates a new pet from a JSON payload.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Pet created successfully",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class))),
		@ApiResponse(responseCode = "400", description = "Invalid pet payload", content = @Content)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pet object to create", required = true,
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class)))
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Pet create(@RequestBody @Valid Pet resource) throws DuplicatedPetNameException{
		if(resource == null) {
			throw new BadRequestException();
		} else {
				petService.savePet(resource);
		}
		
		return resource;
	}
	
	@Operation(summary = "Update a pet", description = "Updates an existing pet with the given ID.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Pet updated successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid pet payload", content = @Content),
		@ApiResponse(responseCode = "404", description = "Pet not found", content = @Content)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pet object to update", required = true,
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class)))
	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public void update(@RequestBody Pet resource, @Parameter(description = "Pet identifier", required = true) @PathVariable("id") int id) {
		if(resource == null) {
			throw new BadRequestException();
		} else if(petService.findPetById(id) == null) {
			throw new ResourceNotFoundException();
		} else {
			try {
				petService.savePet(resource);
			} catch (DuplicatedPetNameException ex) {
				throw new BadRequestException();
			}
		}
	}
	
	@Operation(summary = "Delete a pet", description = "Deletes the pet identified by the given ID.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Pet deleted successfully"),
		@ApiResponse(responseCode = "404", description = "Pet not found", content = @Content)
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public void delete(@Parameter(description = "Pet identifier", required = true) @PathVariable("id") int id) {
		petService.deletePet(id);
	}

}
