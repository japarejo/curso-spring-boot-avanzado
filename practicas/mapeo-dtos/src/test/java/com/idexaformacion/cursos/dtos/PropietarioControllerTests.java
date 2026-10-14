package com.idexaformacion.cursos.dtos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La aplicacion completa con los datos de data.sql: el mapper es un bean
 * inyectado en el controlador y los DTOs salen como JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API de propietarios con DTOs")
class PropietarioControllerTests {

	@Autowired
	MockMvc mvc;

	@Test
	@DisplayName("El listado devuelve el resumen, no la entidad")
	void listado() throws Exception {
		mvc.perform(get("/propietarios"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].nombreCompleto").value("Betty Davis"))
			.andExpect(jsonPath("$[1].numeroMascotas").value(2))
			.andExpect(jsonPath("$[0].telefono").doesNotExist());
	}

	@Test
	@DisplayName("El detalle trae mascotas y username, pero nunca el password")
	void detalle() throws Exception {
		mvc.perform(get("/propietarios/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("george"))
			.andExpect(jsonPath("$.mascotas", hasSize(2)))
			.andExpect(jsonPath("$.mascotas[0].fechaNacimiento").value("07/09/2020"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.version").doesNotExist());
	}

	@Test
	@DisplayName("Un id que no existe da 404")
	void noExiste() throws Exception {
		mvc.perform(get("/propietarios/99")).andExpect(status().isNotFound());
	}

	@Test
	@DirtiesContext
	@DisplayName("Alta: el id que manda el cliente se ignora")
	void alta() throws Exception {
		mvc.perform(post("/propietarios").contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"id": 1, "version": 7, "nombre": "Jean", "apellidos": "Coleman", "ciudad": "Monona"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/propietarios/3"))
			.andExpect(jsonPath("$.id").value(3))
			.andExpect(jsonPath("$.mascotas", hasSize(0)));
	}

	@Test
	@DirtiesContext
	@DisplayName("PATCH: cambia el telefono y conserva el resto")
	void modificacionParcial() throws Exception {
		mvc.perform(patch("/propietarios/2").contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"telefono": "600000000"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.telefono").value("600000000"))
			.andExpect(jsonPath("$.nombre").value("Betty"))
			.andExpect(jsonPath("$.mascotas", hasSize(1)));
	}

}
