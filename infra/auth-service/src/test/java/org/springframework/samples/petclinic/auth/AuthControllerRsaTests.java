package org.springframework.samples.petclinic.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MODULO 3 - La misma API, con firma asimetrica.
 *
 * Solo cambia el perfil activo. Ni el controlador ni el servicio de usuarios se
 * enteran de que ha cambiado el algoritmo: esa es la ventaja de haber puesto una
 * interfaz (JwtTokenService) entre medias.
 *
 * MODULO 4 - Ojo al coste: @ActiveProfiles crea un contexto de aplicacion
 * DISTINTO del de AuthControllerTests, asi que Spring no puede reutilizarlo.
 * Es una de las causas habituales de que una bateria de pruebas tarde minutos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rsa")
class AuthControllerRsaTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Con el perfil rsa el token se firma con RS256")
	void firmaConRsa() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"admin1","password":"4dm1n"}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.algorithm").value("RS256 (asimetrico)"));
	}
}
