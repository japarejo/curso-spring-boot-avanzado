// =============================================================================
// Modulo 2 - Ejercicio 14: OpenAPI a fondo.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Sustituye a configuration/OpenApi30Config.java.
// =============================================================================
package org.springframework.samples.petclinic.configuration;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Dos estilos que conviven y se complementan:
 *   - anotaciones (@SecurityScheme, @OpenAPIDefinition): declarativas, junto al codigo;
 *   - bean OpenAPI: programatico, puede leer propiedades (version, URL del servidor).
 *
 * La diapositiva 69 usa @OpenAPIDefinition; aqui se usa el bean para poder tomar
 * la version de la configuracion, y la anotacion solo para el esquema de seguridad.
 */
@Configuration
@SecurityScheme(name = "bearer-jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
		description = "Token emitido por auth-service: POST http://localhost:8060/api/v1/auth/login")
public class OpenApi30Config {

	@Bean
	OpenAPI customOpenAPI(@Value("${api.version:1.0.0}") String version,
			@Value("${petclinic.public-url:http://localhost:8080}") String urlPublica) {
		return new OpenAPI()
			.info(new Info()
				.title("PetClinic API")
				.version(version)
				.description("""
						API del curso **Spring Boot avanzado**.

						- Errores con `application/problem+json` (RFC 9457).
						- Las operaciones de escritura exigen un JWT (boton *Authorize*).
						""")
				.contact(new Contact().name("Equipo del curso").email("tutoria@idexaformacion.com"))
				.license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
			.servers(List.of(new Server().url(urlPublica).description("Entorno de clase")));
	}

	/** /api/v1/**: la API que se mantiene. */
	@Bean
	GroupedOpenApi apiV1() {
		return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1/**").build();
	}

	/** Lo que queda por migrar (ejercicio 1). Separarlo deja claro que no es el camino. */
	@Bean
	GroupedOpenApi apiLegado() {
		return GroupedOpenApi.builder().group("legado").pathsToMatch("/api/pets/**", "/api/v2/**").build();
	}

}

// ---------- En los controladores ----------
/*
	@Operation(summary = "Borra una mascota", security = @SecurityRequirement(name = "bearer-jwt"))
	@ApiResponse(responseCode = "204", description = "Borrada")
	@ApiResponse(responseCode = "404", description = "No existe",
		content = @Content(mediaType = "application/problem+json",
			schema = @Schema(implementation = ProblemDetail.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable int id) { ... }

	@Hidden                       // fuera de la documentacion
	@GetMapping("/api/v1/demo/pets/{pet}")
	public String demo(@PathVariable("pet") Pet pet) { ... }
*/

// ---------- application.properties ----------
//   springdoc.api-docs.path=/api-docs
//   springdoc.swagger-ui.path=/docs
//   springdoc.swagger-ui.operations-sorter=method
//   springdoc.swagger-ui.tags-sorter=alpha
//
// ---------- SecurityConfiguration ----------
//   .requestMatchers("/docs", "/docs/**", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll()
//   (Swagger UI sigue sirviendo sus recursos desde /swagger-ui/**, aunque la
//    pagina de entrada se mueva a /docs.)
//
// Con grupos, cada uno tiene su propio documento:
//   curl -s localhost:8080/api-docs/v1 | jq '.paths | keys'
//   curl -s localhost:8080/api-docs.yaml/v1 > openapi-v1.yaml
//   npx @redocly/cli lint openapi-v1.yaml
