package org.springframework.samples.petclinic.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MODULO 5 - Ejercicio 14. Comprueba que el servidor sigue abierto sin el
 * perfil `secure` y que con el perfil aplica las reglas de SecurityConfiguration.
 */
class ConfigServerSecurityTests {

	static final String PROPS = "eureka.client.enabled=false";

	@Nested
	@SpringBootTest(properties = PROPS)
	@AutoConfigureMockMvc
	@ActiveProfiles("native")
	class SinPerfilSecure {

		@Autowired
		MockMvc mvc;

		@Test
		void laConfiguracionSeSirveSinCredenciales() throws Exception {
			mvc.perform(get("/bills-service/development")).andExpect(status().isOk());
		}
	}

	@Nested
	@SpringBootTest(properties = { PROPS,
			"CONFIG_CLIENT_PASSWORD=clave-cliente", "CONFIG_ADMIN_PASSWORD=clave-admin",
			"encrypt.key=clave-de-cifrado-de-los-tests" })
	@AutoConfigureMockMvc
	@ActiveProfiles({ "native", "secure" })
	class ConPerfilSecure {

		@Autowired
		MockMvc mvc;

		@Test
		void sinCredencialesDevuelve401() throws Exception {
			mvc.perform(get("/bills-service/development")).andExpect(status().isUnauthorized());
		}

		@Test
		void conClaveIncorrectaDevuelve401() throws Exception {
			mvc.perform(get("/bills-service/development").with(httpBasic("config-client", "mala")))
				.andExpect(status().isUnauthorized());
		}

		@Test
		void elClienteLeeSuConfiguracion() throws Exception {
			mvc.perform(get("/bills-service/development").with(httpBasic("config-client", "clave-cliente")))
				.andExpect(status().isOk());
		}

		@Test
		void elClienteNoPuedeDescifrar() throws Exception {
			mvc.perform(post("/decrypt").contentType(MediaType.TEXT_PLAIN).content("AQB...").with(httpBasic("config-client", "clave-cliente")))
				.andExpect(status().isForbidden());
		}

		@Test
		void elAdministradorPuedeCifrar() throws Exception {
			mvc.perform(post("/encrypt").contentType(MediaType.TEXT_PLAIN).content("secreto").with(httpBasic("config-admin", "clave-admin")))
				.andExpect(status().isOk());
		}

		@Test
		void laSaludSigueSiendoPublica() throws Exception {
			mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		}

		@Test
		void elEnvDeActuatorExigeAdministrador() throws Exception {
			mvc.perform(get("/actuator/env").with(httpBasic("config-client", "clave-cliente")))
				.andExpect(status().isForbidden());
		}
	}
}
