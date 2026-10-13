// =============================================================================
// Modulo 4 - Ejercicio 15: probar la seguridad.
//
// Prueba lista para ejecutar. Copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/web/security/
// con el nombre ReglasDeAccesoTests.java.
// =============================================================================
package org.springframework.samples.petclinic.web.security;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Diferencias con la diapositiva 132:
 *   - @AutoConfigureMockMvc ya aplica springSecurity() al MockMvc: no hace falta
 *     construirlo a mano con MockMvcBuilders;
 *   - no hace falta RANDOM_PORT ni @DirtiesContext: MockMvc no abre ningun puerto.
 *
 * @Transactional deshace el alta de propietario al terminar cada prueba. Con
 * MockMvc funciona porque la peticion se ejecuta en el MISMO hilo que la prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Reglas de acceso de SecurityConfiguration")
class ReglasDeAccesoTests {

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("anonimo -> redirigido al login")
	void anonimoVaAlLogin() throws Exception {
		mockMvc.perform(get("/owners/1"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	@WithMockUser(authorities = "vet")
	@DisplayName("veterinario -> 403 en propietarios")
	void veterinarioNoVePropietarios() throws Exception {
		mockMvc.perform(get("/owners/1")).andExpect(status().isForbidden());
	}

	/** @WithUserDetails carga owner1 de la base de datos con su autoridad real. */
	@Test
	@WithUserDetails("owner1")
	@DisplayName("owner1 ve su propia ficha")
	void propietarioVeLaSuya() throws Exception {
		mockMvc.perform(get("/owners/1")).andExpect(status().isOk());
	}

	@Test
	@WithUserDetails("owner1")
	@DisplayName("owner1 NO ve la ficha de otro (hoy: 200 sin datos)")
	void propietarioNoVeLaAjena() throws Exception {
		mockMvc.perform(get("/owners/2"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("owner", nullValue()));
		// Mejora propuesta: que responda 403 o 404. Cambia esta prueba primero.
	}

	@Test
	@DisplayName("POST sin token CSRF -> 403, aunque el usuario pueda")
	void sinCsrf() throws Exception {
		mockMvc.perform(post("/owners/new").with(user("admin1").authorities(() -> "admin")))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("POST con CSRF y datos validos -> redireccion a la ficha")
	void conCsrf() throws Exception {
		mockMvc.perform(post("/owners/new")
				.with(user("admin1").authorities(() -> "admin"))
				.with(csrf())
				.param("firstName", "Ada")
				.param("lastName", "Lovelace")
				.param("address", "Calle Mayor 1")
				.param("city", "Sevilla")
				.param("telephone", "954000000")
				.param("user.username", "ada")
				.param("user.password", "clave"))
			.andExpect(status().is3xxRedirection());
	}

	@Test
	@WithMockUser(authorities = "owner")
	@DisplayName("propietario -> 403 en administracion")
	void propietarioNoAdministra() throws Exception {
		mockMvc.perform(get("/admin/cualquiera")).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("health es publico")
	void healthPublico() throws Exception {
		// Si hiciste el ejercicio 8 del modulo 1 (FacturasHealthIndicator) y
		// facturas no esta arrancado, aqui veras 503: justo el problema que
		// resuelven los grupos de health.
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

}

// Con el ejercicio 6 o 13 del modulo 3 hecho, dos pruebas mas para la API:
/*
	@Test
	void apiSinToken() throws Exception {
		mockMvc.perform(get("/api/v1/visits")).andExpect(status().isUnauthorized());
	}

	// Solo con oauth2ResourceServer (ejercicio 6). jwt() fabrica el token sin firma.
	// Para el filtro a mano (ejercicio 13) hay que generar un token de verdad con
	// jjwt y el mismo secreto, y mandarlo en la cabecera Authorization.
	@Test
	void apiConToken() throws Exception {
		mockMvc.perform(get("/api/v1/visits")
				.with(SecurityMockMvcRequestPostProcessors.jwt()
					.authorities(new SimpleGrantedAuthority("vet"))))
			.andExpect(status().isOk());
	}
*/
