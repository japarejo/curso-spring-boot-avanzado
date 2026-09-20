package org.springframework.samples.petclinic.apiclient;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("external-api")
class ExternalApiRestAssuredTests {

	private static final RestAssuredConfig TIMEOUTS = RestAssuredConfig.config()
		.httpClient(HttpClientConfig.httpClientConfig()
			.setParam("http.connection.timeout", 3000)
			.setParam("http.socket.timeout", 3000));

	@Test
	void shouldReadPikachuFromPokemonApi() {
		getOrSkip("https://pokeapi.co/api/v2/pokemon/pikachu")
		.then().assertThat()
			.statusCode(200)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.body("id", equalTo(25))
			.body("name", equalTo("pikachu"))
			.body("types.type.name", hasItem("electric"));
	}


	@Test
	void lukeEsRubio(){
		Response response = getOrSkip("https://swapi.info/api/people/1");

		response.then().assertThat()
				.statusCode(200)
				.contentType(MediaType.APPLICATION_JSON_VALUE)

				.body("name", equalTo("Luke Skywalker"))
				.body("height", equalTo("172"))
				.body("birth_year", equalTo("19BBY"));
	}

	private Response getOrSkip(String url) {
		try {
			Response response = given()
				.config(TIMEOUTS)
				.accept(MediaType.APPLICATION_JSON_VALUE)
				.when()
				.get(url);
			assumeTrue(response.statusCode() == 200, "External API returned HTTP " + response.statusCode());
			return response;
		}
		catch (RuntimeException ex) {
			assumeTrue(false, "External API is not available: " + ex.getMessage());
			throw ex;
		}
	}

}
