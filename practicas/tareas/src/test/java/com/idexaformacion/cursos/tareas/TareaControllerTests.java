package com.idexaformacion.cursos.tareas;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La prueba de la diapositiva 30, actualizada a JUnit 5 y Spring Boot 3.5:
 *
 *   diapositiva (JUnit 4, Spring Boot 1.x)       hoy
 *   ------------------------------------------   -------------------------------
 *   @RunWith(SpringJUnit4ClassRunner.class)      nada: @WebMvcTest ya trae la extension
 *   @SpringApplicationConfiguration              @WebMvcTest (solo la capa web)
 *   MockMvcBuilders.webAppContextSetup(...)      MockMvc inyectado
 *   @MockBean                                    @MockitoBean (desde Spring Boot 3.4)
 *
 * @WebMvcTest carga SOLO el controlador indicado y la infraestructura MVC. El
 * repositorio no existe en este contexto: hay que darle un doble (modulo 4).
 */
@WebMvcTest(TareaController.class)
class TareaControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TareaRepository repository;

	@Test
	void listaTodasLasTareas() throws Exception {
		given(repository.findAll()).willReturn(List.of(
			new Tarea("Uno", LocalDate.of(2026, 1, 1)),
			new Tarea("Dos", LocalDate.of(2026, 1, 2))));

		mockMvc.perform(get("/api/v1/tareas"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].descripcion", is("Uno")));
	}

	@Test
	void devuelve404SiLaTareaNoExiste() throws Exception {
		given(repository.findById(99)).willReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/tareas/99"))
			.andExpect(status().isNotFound());
	}

	@Test
	void crearDevuelve201ConLocation() throws Exception {
		Tarea guardada = new Tarea("Nueva", LocalDate.of(2026, 3, 1));
		guardada.setId(7);
		given(repository.save(any(Tarea.class))).willReturn(guardada);

		mockMvc.perform(post("/api/v1/tareas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"descripcion": "Nueva", "fechaLimite": "2026-03-01"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/tareas/7"))
			.andExpect(jsonPath("$.id", is(7)));
	}

	@Test
	void rechazaUnaTareaSinDescripcion() throws Exception {
		mockMvc.perform(post("/api/v1/tareas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"descripcion\": \"\"}"))
			.andExpect(status().isBadRequest());

		// La validacion corta la peticion antes de llegar al repositorio.
		verify(repository, never()).save(any());
	}

}
