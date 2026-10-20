package org.springframework.samples.petclinic.web.api;

import java.net.URI;
import java.util.Collection;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.configuration.AuditorAwareImpl;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException.Unauthorized;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Pets", description = "Pet management REST API")
@RestController
@RequestMapping("/api/pets")
public class PetRestController {
	
	private final AuditorAwareImpl auditorAwareImpl;
    @Autowired
	private PetService petService;

    PetRestController(AuditorAwareImpl auditorAwareImpl) {
        this.auditorAwareImpl = auditorAwareImpl;
    }
	
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
		Pet pet = petService.findPetById(id);
		// Sin esta comprobacion, un id inexistente devolvia 200 con el cuerpo vacio
		// (Spring serializa un null como respuesta sin contenido), aunque la
		// documentacion de arriba promete un 404.
		if (pet == null) {
			throw new ResourceNotFoundException("No existe la mascota " + id);
		}
		return pet;
	}
	
	@Operation(summary = "Create a pet", description = "Creates a new pet from a JSON payload.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Pet created successfully",
			headers = @Header(name = "Location", description = "URL of the new pet", schema = @Schema(type = "string")),
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class))),
		@ApiResponse(responseCode = "400", description = "Invalid pet payload", content = @Content)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pet object to create", required = true,
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = Pet.class)))
	@PostMapping
	public ResponseEntity<Pet> create(@RequestBody @Valid Pet resource) throws DuplicatedPetNameException{
		
		if(resource == null) {
			throw new BadRequestException();
		} else {
				petService.savePet(resource);
		}

		// savePet() asigna el id al persistir. La URL se construye a partir de la
		// peticion actual (esquema, host, puerto y ruta), asi que vale tras un proxy
		// o si cambia el @RequestMapping: Location: http://host:puerto/api/pets/{id}
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(resource.getId())
			.toUri();
		return ResponseEntity.created(location).body(resource);
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
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication!=null) {			
			System.out.println("Usuario logueado: " + authentication.getName());
			if(authentication.getAuthorities()!=null && !authentication.getAuthorities().isEmpty()) {
				System.out.println("Roles: " + authentication.getAuthorities());
				for(GrantedAuthority authority : authentication.getAuthorities()) {
					System.out.println(" - " + authority.getAuthority());
					if(authority.getAuthority().equals("owner")) {
						Pet pet = petService.findPetById(id);
						if(pet != null && pet.getOwner() != null) {
							String username = authentication.getName();
							if(!username.equals(pet.getOwner().getUser().getUsername())) {
								throw new AccessDeniedException("El propietario logueado no es el dueño de la mascota " + id);	
							}
						}
					}	
				}
			}
		}
		petService.deletePet(id);
	}

}
