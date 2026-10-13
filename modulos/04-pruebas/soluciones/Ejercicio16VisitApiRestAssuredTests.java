// =============================================================================
// Modulo 4 - Ejercicio 16: REST Assured contra la API propia.
//
// Prueba lista para ejecutar. Copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/web/api/
// con el nombre VisitApiRestAssuredTests.java.
//
// Supone que /api/** sigue abierto. Si ya se protegio en el modulo 3, anade
// .header("Authorization", "Bearer " + token) o .auth().preemptive()... segun el caso.
// =============================================================================
package org.springframework.samples.petclinic.web.api;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Diferencias con las diapositivas 125-127:
 *   - RANDOM_PORT en lugar de DEFINED_PORT: no choca con una aplicacion que ya
 *     este corriendo en el 8080 ni con otra ejecucion en paralelo;
 *   - el puerto se configura una vez (RestAssured.port) y las URL son relativas;
 *   - el registro de peticion/respuesta solo aparece cuando algo falla.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API de visitas por HTTP real")
class VisitApiRestAssuredTests {

	@LocalServerPort
	int port;

	@BeforeEach
	void configurar() {
		RestAssured.port = port;
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	@DisplayName("1. el listado pagina")
	void listadoPaginado() {
		given()
			.queryParam("size", 2)
		.when()
			.get("/api/v1/visits")
		.then()
			.statusCode(200)
			.body("content", hasSize(2))
			.body("page.size", equalTo(2))
			.body("page.totalElements", greaterThan(2));
	}

	@Test
	@DisplayName("2. ordenar por un campo no permitido da 400 con ProblemDetail")
	void ordenacionNoPermitida() {
		given()
			.queryParam("sort", "unknown")
		.when()
			.get("/api/v1/visits")
		.then()
			.statusCode(400)
			.contentType("application/problem+json")
			.body("status", equalTo(400));
	}

	@Test
	@DisplayName("3. crear devuelve 201 y un Location que lleva a la visita creada")
	void crearYSeguirLocation() {
		String location =
			given()
				.contentType(ContentType.JSON)
				.body("""
					{"description": "Revision anual REST Assured", "petId": 1}
					""")
			.when()
				.post("/api/v1/visits")
			.then()
				.statusCode(201)
				.header("Location", notNullValue())
				.body("description", equalTo("Revision anual REST Assured"))
				.extract().header("Location");

		when()
			.get(location)
		.then()
			.statusCode(200)
			.body("description", equalTo("Revision anual REST Assured"));
	}

	@Test
	@DisplayName("4. sin descripcion -> 400 que menciona el campo")
	void validacion() {
		given()
			.contentType(ContentType.JSON)
			.body("{\"petId\": 1}")
		.when()
			.post("/api/v1/visits")
		.then()
			.statusCode(400)
			.body("detail", containsString("description"));
	}

	@Test
	@DisplayName("5. el listado responde en menos de 2 s")
	void tiempoDeRespuesta() {
		// Mide ida y vuelta completa, no solo el servidor (diapositiva 130):
		// sirve para detectar regresiones gordas, no para medir rendimiento.
		when()
			.get("/api/v1/visits")
		.then()
			.time(lessThan(2000L));
	}

}

// Opcional, paso 4: formulario de Spring Security con CSRF.
/*
	@Test
	void fichaDePropietarioConLogin() {
		given()
			.auth().form("owner1", "0wn3r",
				FormAuthConfig.springSecurity().withAutoDetectionOfCsrf())
		.when()
			.get("/owners/1")
		.then()
			.statusCode(200)
			.body(containsString("Franklin"));
	}
*/
// FormAuthConfig.springSecurity() supone /login con campos username y password,
// que es exactamente la pagina que genera Spring Security en este proyecto.
