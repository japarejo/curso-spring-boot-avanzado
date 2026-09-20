package org.springframework.samples.petclinic.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MODULO 3 y 4 - Prueba de integracion del flujo de autenticacion.
 *
 * Recorre el camino completo: credenciales -> token -> validacion del token.
 * Se ejecuta con el perfil por defecto (HS512); la variante asimetrica se
 * comprueba en {@link AuthControllerRsaTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static final String LOGIN = """
			{"username":"%s","password":"%s"}""";

	@Test
	@DisplayName("Con credenciales correctas devuelve un token firmado")
	void devuelveTokenConCredencialesCorrectas() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN.formatted("admin1", "4dm1n")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.algorithm").value("HS512 (simetrico)"))
			.andExpect(jsonPath("$.expiresIn").value(3600));
	}

	@Test
	@DisplayName("Con contrasena incorrecta devuelve 401 y no filtra el motivo")
	void rechazaCredencialesIncorrectas() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN.formatted("admin1", "incorrecta")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Credenciales no validas"))
			// El detalle es generico a proposito: no debe distinguir entre
			// "no existe el usuario" y "la contrasena no coincide".
			.andExpect(jsonPath("$.detail").value("El usuario o la contrasena no son correctos"));
	}

	@Test
	@DisplayName("Con un usuario inexistente responde igual que con contrasena incorrecta")
	void rechazaUsuarioInexistente() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN.formatted("noexiste", "loquesea")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("El usuario o la contrasena no son correctos"));
	}

	@Test
	@DisplayName("El token emitido se valida correctamente y conserva las autoridades")
	void validaElTokenEmitido() throws Exception {
		String token = obtenerToken("vet1", "v3t");

		mockMvc.perform(post("/api/v1/auth/validate")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.username").value("vet1"))
			.andExpect(jsonPath("$.authorities[0]").value("vet"));
	}

	@Test
	@DisplayName("Un token manipulado se rechaza: la firma no cuadra")
	void rechazaTokenManipulado() throws Exception {
		String token = obtenerToken("owner1", "0wn3r");
		// Se altera un caracter del cuerpo manteniendo la firma original.
		String manipulado = token.substring(0, 25) + "X" + token.substring(26);

		mockMvc.perform(post("/api/v1/auth/validate")
				.header("Authorization", "Bearer " + manipulado))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false));
	}

	@Test
	@DisplayName("Sin cabecera Authorization el token no es valido")
	void rechazaPeticionSinCabecera() throws Exception {
		mockMvc.perform(post("/api/v1/auth/validate"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false));
	}

	@Test
	@DisplayName("El token tiene las tres partes del estandar: cabecera, cuerpo y firma")
	void elTokenTieneTresPartes() throws Exception {
		String token = obtenerToken("admin1", "4dm1n");
		// cabecera.cuerpo.firma -> dos puntos separadores
		assertThat(token.chars().filter(caracter -> caracter == '.').count()).isEqualTo(2);
	}

	private String obtenerToken(String usuario, String clave) throws Exception {
		String respuesta = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(LOGIN.formatted(usuario, clave)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(respuesta);
		return json.get("token").asText();
	}
}
