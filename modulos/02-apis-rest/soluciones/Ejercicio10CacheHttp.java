// =============================================================================
// Modulo 2 - Ejercicio 10: ResponseEntity, cabeceras y cache HTTP.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Son los tres metodos que sustituyen a findbyId, update y delete en
// web/api/PetRestController (ya movido a /api/v1/pets si se hizo el ejercicio 1;
// si no, la ruta es /api/pets y los curl cambian en consecuencia).
// =============================================================================
package org.springframework.samples.petclinic.web.api;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

class PetRestControllerConEtag {

	private PetService petService; // el que ya tiene PetRestController

	/** La ETag es la version de @Version entre comillas, como exige la RFC 9110. */
	private static String etag(Pet pet) {
		return "\"" + pet.getVersion() + "\"";
	}

	// --- 1 y 2. GET con ETag, Cache-Control y 304 ------------------------------
	// El @Parameter solo DOCUMENTA la cabecera, para que Swagger UI ofrezca un campo
	// donde escribirla. El valor lo lee WebRequest.checkNotModified por dentro.
	@GetMapping("/{id}")
	@Parameter(in = ParameterIn.HEADER, name = "If-None-Match", required = false,
			description = "ETag recibida en un GET anterior, con comillas: \"1\"",
			schema = @Schema(type = "string"))
	public ResponseEntity<Pet> findById(@PathVariable("id") int id, WebRequest request) {
		Pet pet = petService.findPetById(id);
		if (pet == null) {
			throw new ResourceNotFoundException("No existe la mascota " + id);
		}
		// checkNotModified compara con If-None-Match y, si coincide, deja la
		// respuesta preparada como 304. Hay que devolver SIN cuerpo.
		if (request.checkNotModified(etag(pet))) {
			return null;
		}
		return ResponseEntity.ok()
			.eTag(etag(pet))
			.cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate())
			.body(pet);
	}

	// --- 3. PUT condicional: If-Match obligatorio --------------------------------
	@PutMapping("/{id}")
	public ResponseEntity<Void> update(@PathVariable("id") int id, @RequestBody Pet resource,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch)
			throws DuplicatedPetNameException {
		Pet actual = petService.findPetById(id);
		if (actual == null) {
			throw new ResourceNotFoundException("No existe la mascota " + id);
		}
		if (ifMatch == null) {
			// 428: el servidor exige peticiones condicionales (RFC 6585).
			throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
					"Envia If-Match con la ETag que obtuviste en el GET");
		}
		if (!ifMatch.equals(etag(actual))) {
			// 412: alguien ha modificado la mascota desde que la leiste.
			throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
					"La mascota ha cambiado. Version actual: " + etag(actual));
		}
		// El id de la ruta manda sobre el del cuerpo, y la version es la comprobada.
		resource.setId(id);
		resource.setVersion(actual.getVersion());
		petService.savePet(resource);
		Pet guardada = petService.findPetById(id);
		return ResponseEntity.noContent().eTag(etag(guardada)).build();
	}

	// --- 4. DELETE con cabecera propia (diapositiva 57) --------------------------
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") int id) {
		petService.deletePet(id);
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Mascota-Borrada", String.valueOf(id));
		// 204 en lugar del 200 de la diapositiva: no hay cuerpo que devolver.
		return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
	}

}

// --- Alternativa sin codigo: ShallowEtagHeaderFilter ---------------------------
// En configuration/WebConfig.java:
/*
	@Bean
	FilterRegistrationBean<ShallowEtagHeaderFilter> etags() {
		FilterRegistrationBean<ShallowEtagHeaderFilter> registro =
				new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
		registro.addUrlPatterns("/api/*");
		return registro;
	}
*/
// Calcula la ETag como hash MD5 del cuerpo YA GENERADO. Ahorra red (304) pero no
// CPU ni base de datos, y no sirve para If-Match: no sabe nada de versiones.
//
// Nota: ResponseStatusException es un ErrorResponseException, y el
// GlobalExceptionHandler del proyecto ya lo respeta (handleErrorResponse), asi
// que 412 y 428 salen como ProblemDetail sin tocar nada mas.
