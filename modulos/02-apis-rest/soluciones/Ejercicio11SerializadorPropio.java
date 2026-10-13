// =============================================================================
// Modulo 2 - Ejercicio 11: serializador y deserializador propios.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================

// ---------- web/api/SpecialtyJson.java ----------
package org.springframework.samples.petclinic.web.api;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.samples.petclinic.model.Specialty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @JsonComponent registra las dos clases internas en el ObjectMapper que crea
 * Spring Boot. A partir de ese momento, CUALQUIER Specialty que se serialice en
 * CUALQUIER respuesta sale como texto. Es comodo y es el riesgo: no hay forma de
 * verlo mirando la entidad o el DTO.
 *
 * Comparacion con PetTypeSerializer/Deserializer:
 *   - aquellos se activan campo a campo con @JsonSerialize(using = ...) en Pet.type;
 *   - este se activa para el TIPO, en toda la aplicacion.
 */
@JsonComponent
public class SpecialtyJson {

	public static class Serializer extends JsonSerializer<Specialty> {

		@Override
		public void serialize(Specialty value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeString(value.getName());
		}

	}

	/**
	 * Deserializa SOLO el nombre. Si se necesitara la entidad gestionada habria que
	 * buscarla por nombre (como hace PetTypeDeserializer con PetTypeFormatter).
	 * Aqui se deja sin id a proposito para discutirlo: ¿que pasaria al guardar un
	 * Vet con una Specialty sin id? (Hibernate intentaria insertarla como nueva.)
	 */
	public static class Deserializer extends JsonDeserializer<Specialty> {

		@Override
		public Specialty deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			Specialty specialty = new Specialty();
			specialty.setName(p.getValueAsString());
			return specialty;
		}

	}

}

// ---------- web/api/VetRestController.java ----------
/*
package org.springframework.samples.petclinic.web.api;

import java.util.Collection;

import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.service.VetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vets")
public class VetRestController {

	private final VetService vetService;

	public VetRestController(VetService vetService) {
		this.vetService = vetService;
	}

	// Vet.specialties es EAGER, por eso se puede serializar fuera de la
	// transaccion sin LazyInitializationException. Con LAZY habria que usar un
	// DTO o un @EntityGraph (modulo 6).
	@GetMapping
	public Collection<Vet> listar() {
		return vetService.findVets();
	}
}
*/

// Antes:  {"id":2,"firstName":"Helen","lastName":"Leary","specialties":[{"id":1,"name":"radiology","version":1}], ...}
// Despues: {"id":2,"firstName":"Helen","lastName":"Leary","specialties":["radiology"], ...}
//
// Configuracion global de Jackson: una alternativa a las propiedades
// spring.jackson.* es un bean, que se combina con ellas:
/*
	@Bean
	Jackson2ObjectMapperBuilderCustomizer jacksonDelCurso() {
		return builder -> builder
			.serializationInclusion(JsonInclude.Include.NON_NULL)
			.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}
*/
