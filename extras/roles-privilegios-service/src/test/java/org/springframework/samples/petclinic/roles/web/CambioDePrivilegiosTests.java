package org.springframework.samples.petclinic.roles.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambiar QUIEN puede hacer algo sin tocar el codigo: basta con una fila en
 * {@code roles_privilegios}.
 *
 * <p>{@code @Transactional}: MockMvc ejecuta la peticion en el mismo hilo que
 * la prueba, asi que todo comparte transaccion y el cambio se deshace al
 * terminar, sin contaminar las demas pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CambioDePrivilegiosTests {

	@Autowired
	MockMvc mvc;

	@Test
	void concederUnPrivilegioAUnRolSeNotaEnLaSiguientePeticion() throws Exception {
		mvc.perform(registrarVisitaComo("rita")).andExpect(status().isForbidden());

		mvc.perform(put("/api/admin/roles/RECEPCION/privilegios/VISITA_REGISTRAR").with(httpBasic("admin", "demo")))
				.andExpect(status().isOk());

		mvc.perform(registrarVisitaComo("rita")).andExpect(status().isCreated());
	}

	@Test
	void retirarUnPrivilegioDeEmpleadoAfectaATodaLaJerarquia() throws Exception {
		mvc.perform(get("/api/mascotas").with(httpBasic("victor", "demo"))).andExpect(status().isOk());

		mvc.perform(delete("/api/admin/roles/EMPLEADO/privilegios/MASCOTA_LEER").with(httpBasic("admin", "demo")))
				.andExpect(status().isOk());

		// victor lo heredaba de EMPLEADO: lo pierde aunque su rol no se haya tocado.
		mvc.perform(get("/api/mascotas").with(httpBasic("victor", "demo"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/mascotas").with(httpBasic("admin", "demo"))).andExpect(status().isForbidden());
	}

	@Test
	void soloQuienTieneElPrivilegioPuedeGestionarRoles() throws Exception {
		// pablo pasa por /api/admin? No: la regla por ruta lo para antes (403).
		mvc.perform(put("/api/admin/roles/RECEPCION/privilegios/VISITA_REGISTRAR").with(httpBasic("pablo", "demo")))
				.andExpect(status().isForbidden());
	}

	private static MockHttpServletRequestBuilder registrarVisitaComo(String usuario) {
		return post("/api/visitas").with(httpBasic(usuario, "demo"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"mascota": "Leo (gato)", "motivo": "vacuna"}
						""");
	}

}
