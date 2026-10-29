package org.springframework.samples.petclinic.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.repository.criteria.FichaMascota;
import org.springframework.samples.petclinic.repository.criteria.FiltroMascotas;
import org.springframework.samples.petclinic.service.BusquedaMascotasService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ESCENARIO DE CRITERIA API: lo que llega de la pantalla se convierte en el
 * FiltroMascotas esperado. La consulta se prueba en BusquedaMascotasRepositoryTests.
 */
@WebMvcTest(BusquedaMascotasRestController.class)
@WithMockUser
class BusquedaMascotasRestControllerMockMvcTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	BusquedaMascotasService servicio;

	@Test
	void losParametrosDeLaPantallaLleganAlFiltro() throws Exception {
		FichaMascota max = new FichaMascota(8, "Max", "cat", LocalDate.of(2012, 9, 4), "Jean", "Coleman", "Monona",
				2L, LocalDate.of(2013, 1, 3));
		given(servicio.buscar(any(), any())).willAnswer(i -> new PageImpl<>(List.of(max), i.getArgument(1), 1));

		mockMvc
			.perform(get("/api/v1/busqueda-mascotas").param("tipos", "cat", "dog")
				.param("ciudades", "Monona")
				.param("visitadaDesde", "2013-01-01")
				.param("minVisitas", "1")
				.param("enfermedades", "2")
				.param("sort", "ultimaVisita,desc")
				.param("size", "500"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contenido[0].nombre").value("Max"))
			.andExpect(jsonPath("$.contenido[0].numeroVisitas").value(2))
			.andExpect(jsonPath("$.totalElementos").value(1));

		ArgumentCaptor<FiltroMascotas> filtro = ArgumentCaptor.forClass(FiltroMascotas.class);
		ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
		verify(servicio).buscar(filtro.capture(), pagina.capture());

		assertThat(filtro.getValue()).isEqualTo(FiltroMascotas.builder()
			.tipos(List.of("cat", "dog"))
			.ciudades(List.of("Monona"))
			.visitadaDesde(LocalDate.of(2013, 1, 1))
			.minVisitas(1)
			.enfermedades(List.of(2))
			.build());
		assertThat(pagina.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("ultimaVisita")));
		assertThat(pagina.getValue().getPageSize()).isEqualTo(50);
	}

	@Test
	void ordenarPorUnaColumnaNoPermitidaEsUn400() throws Exception {
		mockMvc.perform(get("/api/v1/busqueda-mascotas").param("sort", "owner.user.password"))
			.andExpect(status().isBadRequest());
		verify(servicio, never()).buscar(any(), any());
	}

}
