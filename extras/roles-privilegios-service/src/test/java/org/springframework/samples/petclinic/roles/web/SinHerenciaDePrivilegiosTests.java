package org.springframework.samples.petclinic.roles.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La trampa del modelo "ingenuo": jerarquia de roles por un lado, privilegios
 * de los roles ASIGNADOS por otro. La jerarquia funciona para hasRole(...),
 * pero los privilegios no se heredan.
 *
 * <p>Base de datos propia para no pisar la del contexto de las otras pruebas.
 */
@SpringBootTest(properties = {
		"demo.roles.heredar-privilegios=false",
		"spring.datasource.url=jdbc:h2:mem:roles-sin-herencia;DB_CLOSE_DELAY=-1" })
@AutoConfigureMockMvc
class SinHerenciaDePrivilegiosTests {

	@Autowired
	MockMvc mvc;

	@Test
	void porRolLaJerarquiaSigueFuncionando() throws Exception {
		mvc.perform(get("/api/turnos").with(httpBasic("admin", "demo"))).andExpect(status().isOk());
		mvc.perform(get("/api/turnos").with(httpBasic("rita", "demo"))).andExpect(status().isOk());
	}

	@Test
	void ritaEsEmpleadaPeroNoPuedeLeerMascotas() throws Exception {
		// hasRole('EMPLEADO') diria que si; hasAuthority('MASCOTA_LEER') dice que no.
		mvc.perform(get("/api/mascotas").with(httpBasic("rita", "demo"))).andExpect(status().isForbidden());
	}

	@Test
	void adminEsVeterinarioPeroNoPuedeRegistrarVisitas() throws Exception {
		mvc.perform(post("/api/visitas").with(httpBasic("admin", "demo"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"mascota": "Leo (gato)", "motivo": "vacuna"}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void evaNoNotaNadaPorqueNoHeredaDeNadie() throws Exception {
		mvc.perform(get("/api/mascotas").with(httpBasic("eva", "demo"))).andExpect(status().isOk());
	}

}
