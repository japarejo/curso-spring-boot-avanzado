// =============================================================================
// Modulo 4 - Ejercicio 18: dobles de servicios externos, y sus limites.
//
// Prueba lista para ejecutar. Copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/apiclient/
// con el nombre BillsGatewayTests.java.
// =============================================================================
package org.springframework.samples.petclinic.apiclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.apiclients.BillClient;
import org.springframework.samples.petclinic.apiclients.BillsGateway;
import org.springframework.samples.petclinic.configuration.BillsProperties;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Prueba UNITARIA: sin contexto de Spring. MockRestServiceServer sustituye la
 * capa HTTP del RestClient: no se abre ningun puerto.
 *
 * Compara con BillApiClientTest, que levanta un servidor HTTP de verdad con la
 * JDK para probar el cliente Feign. Son dos niveles distintos de la piramide.
 */
@DisplayName("BillsGateway con un servicio de facturas simulado")
class BillsGatewayTests {

	private static final String BASE = "http://facturas.test";

	MockRestServiceServer servidor;

	BillsGateway gateway;

	@BeforeEach
	void preparar() {
		RestClient.Builder builder = RestClient.builder();
		// bindTo ANTES de que BillsGateway llame a build(): el builder queda
		// configurado para mandar las peticiones al servidor simulado.
		servidor = MockRestServiceServer.bindTo(builder).build();
		BillsProperties propiedades = new BillsProperties(BASE, Duration.ofSeconds(1), Duration.ofSeconds(1));
		gateway = new BillsGateway(propiedades, mock(BillClient.class), builder, new RestTemplateBuilder());
	}

	@Test
	@DisplayName("convierte la respuesta del servicio en facturas")
	void respuestaCorrecta() {
		servidor.expect(requestTo(BASE + "/api/v1/bills"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
					[{"id": 1, "amount": 30.0, "concept": "Vacuna"},
					 {"id": 2, "amount": 45.5, "concept": "Revision"}]
					""", MediaType.APPLICATION_JSON));

		List<Bill> facturas = gateway.conRestClient();

		assertThat(facturas).hasSize(2);
		assertThat(facturas).extracting(Bill::getConcept).containsExactly("Vacuna", "Revision");
		servidor.verify();
	}

	@Test
	@DisplayName("con new no hay proxy: el cortocircuito NO actua")
	void sinProxyNoHayCortocircuito() {
		servidor.expect(requestTo(BASE + "/api/v1/bills")).andRespond(withServerError());

		// Dentro de Spring, @CircuitBreaker devolveria lista vacia. Aqui no:
		// el objeto se ha creado con `new` (modulo 1, ejercicio 9).
		assertThrows(HttpServerErrorException.class, () -> gateway.conRestClient());
	}

	@Test
	@DisplayName("el doble no se entera de que el servicio ha cambiado")
	void elDobleSeQuedaAtras() {
		// El equipo de facturas renombra `concept` a `concepto`...
		servidor.expect(requestTo(BASE + "/api/v1/bills"))
			.andRespond(withSuccess("[{\"id\": 1, \"amount\": 30.0, \"concepto\": \"Vacuna\"}]",
					MediaType.APPLICATION_JSON));

		List<Bill> facturas = gateway.conRestClient();

		// ...y la prueba sigue en verde mientras no se compruebe el campo:
		assertThat(facturas).hasSize(1);
		// El dato se ha perdido en silencio:
		assertThat(facturas.get(0).getConcept()).isNull();
	}

}

// Notas:
//  - La tercera prueba depende de que Jackson IGNORE los campos desconocidos.
//    RestClient.builder() usa la configuracion por defecto de Spring, que los
//    ignora. Con FAIL_ON_UNKNOWN_PROPERTIES activado, la prueba fallaria: la
//    discusion de clase es si eso es mejor (detecta el cambio) o peor (rompe
//    ante cualquier campo nuevo, aunque sea compatible).
