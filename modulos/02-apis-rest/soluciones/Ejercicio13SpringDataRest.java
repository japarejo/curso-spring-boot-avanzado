// =============================================================================
// Modulo 2 - Ejercicio 13: Spring Data REST a fondo.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================

// ---------- apps/petclinic-api/pom.xml ----------
//   <dependency>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-starter-data-rest</artifactId>
//   </dependency>
//   <!-- Opcional: explorador HAL en /data-api -->
//   <dependency>
//     <groupId>org.springframework.data</groupId>
//     <artifactId>spring-data-rest-hal-explorer</artifactId>
//   </dependency>
//
// ---------- application.properties ----------
//   spring.data.rest.base-path=/data-api
//   # ANNOTATED: solo se exponen los repositorios anotados. Con DEFAULT, al anadir
//   # el starter se publicarian TODOS los repositorios publicos del proyecto,
//   # incluido UserRepository con los hashes de las contrasenas.
//   spring.data.rest.detection-strategy=annotated
//
// ---------- SecurityConfiguration: antes del denyAll() final ----------
//   .requestMatchers(HttpMethod.GET, "/data-api/**").permitAll()
//   .requestMatchers("/data-api/**").hasAuthority("admin")
// y anadir "/data-api/**" a csrf.ignoringRequestMatchers(...).

// ---------- repository/VetRepository.java ----------
package org.springframework.samples.petclinic.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.samples.petclinic.model.Vet;

@RepositoryRestResource(path = "veterinarios", collectionResourceRel = "veterinarios",
		excerptProjection = VetResumen.class)
public interface VetRepository extends CrudRepository<Vet, Integer> {

	// ... los metodos que ya tenia el repositorio se mantienen ...

	// Aparece como /data-api/veterinarios/search/findByLastNameStartingWithIgnoreCase?apellido=...
	List<Vet> findByLastNameStartingWithIgnoreCase(@Param("apellido") String apellido);

	// Redeclarar un metodo heredado permite anotarlo. exported = false lo retira
	// de la API: DELETE /data-api/veterinarios/1 pasa a responder 405.
	@Override
	@RestResource(exported = false)
	void deleteById(Integer id);

	@Override
	@RestResource(exported = false)
	void delete(Vet entity);

}

// ---------- repository/VetResumen.java ----------
/*
package org.springframework.samples.petclinic.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.samples.petclinic.model.Vet;

// Proyeccion ABIERTA: @Value evalua una expresion SpEL sobre la entidad.
// Se aplica sola en los listados por ser excerptProjection; en el detalle se
// pide explicitamente con ?projection=resumen.
@Projection(name = "resumen", types = Vet.class)
public interface VetResumen {

	String getFirstName();

	String getLastName();

	@Value("#{target.nrOfSpecialties}")
	int getEspecialidades();
}
*/

// ---------- repository/VetEventHandler.java ----------
/*
package org.springframework.samples.petclinic.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.stereotype.Component;

// Los eventos de Spring Data REST SOLO se disparan cuando la operacion entra por
// la API generada, no cuando el codigo llama al repositorio. Es la diferencia con
// los callbacks de JPA (@PrePersist) o los eventos de Spring Data (@DomainEvents).
@Component
@RepositoryEventHandler
public class VetEventHandler {

	private static final Logger log = LoggerFactory.getLogger(VetEventHandler.class);

	@HandleAfterCreate
	public void alCrear(Vet vet) {
		log.info("Alta de veterinario por la API: {} {}", vet.getFirstName(), vet.getLastName());
	}

	@HandleBeforeSave
	public void antesDeModificar(Vet vet) {
		log.info("Se va a modificar el veterinario {}", vet.getId());
	}
}
*/

// Comprobacion:
//   curl -s localhost:8080/data-api | jq
//   curl -s localhost:8080/data-api/veterinarios | jq '._embedded.veterinarios[0]'
//   curl -s localhost:8080/data-api/veterinarios/1 | jq
//   curl -s "localhost:8080/data-api/veterinarios/1?projection=resumen" | jq
//   curl -s "localhost:8080/data-api/veterinarios/search/findByLastNameStartingWithIgnoreCase?apellido=or" | jq
//   curl -i -X DELETE -u admin1:4dm1n localhost:8080/data-api/veterinarios/1      # 405
//
// Para el POST con autenticacion basica hay que activar httpBasic() en la
// cadena de filtros; con el formulario de login, curl no puede autenticarse
// facilmente. Es una buena excusa para adelantar el modulo 3.
//
// Nota: Vet extiende Person, que tiene getFullName(). Si aparece en el JSON y no
// se quiere, @JsonIgnore en el getter o una proyeccion.
