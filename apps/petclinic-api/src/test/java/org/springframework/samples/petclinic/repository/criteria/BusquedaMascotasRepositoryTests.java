package org.springframework.samples.petclinic.repository.criteria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.repository.PetRepository;

/**
 * ESCENARIO DE CRITERIA API: busqueda avanzada de pacientes (modulo 6).
 *
 * Una prueba por criterio y unas cuantas de combinaciones, contra data.sql:
 *
 *   - 13 mascotas. Gatos: Leo, Samantha, Max y Sly. Perros: Rosy, Jewel, Mulligan, Lucky (12).
 *   - Propietarios de Madison: Franklin, McTavish, Escobito y Schroeder.
 *   - Solo Samantha (7) y Max (8), de Jean Coleman (Monona), tienen visitas:
 *       visita 1  Samantha  2013-01-01  diagnostico Diabetes  (Dra. Leary, vet 2)
 *       visita 2  Max       2013-01-02  diagnostico COVID-19  (Dr. Carter, vet 1)
 *       visita 3  Max       2013-01-03
 *       visita 4  Samantha  2013-01-04
 */
@DataJpaTest
@DisplayName("Criteria API: busqueda avanzada de mascotas")
class BusquedaMascotasRepositoryTests {

	private static final Pageable TODO = PageRequest.of(0, 50, Sort.by("nombre"));

	@Autowired
	PetRepository petRepository;

	List<String> nombres(FiltroMascotas filtro) {
		return petRepository.buscarMascotas(filtro, TODO).map(FichaMascota::nombre).getContent();
	}

	@Test
	@DisplayName("Sin ningun filtro activado devuelve las 13 mascotas")
	void sinFiltros() {
		assertThat(nombres(FiltroMascotas.vacio())).hasSize(13);
	}

	@Nested
	@DisplayName("Un criterio cada vez")
	class UnCriterio {

		@Test
		void nombreContiene() {
			assertThat(nombres(FiltroMascotas.builder().nombre("LU").build())).containsExactly("Lucky", "Lucky");
		}

		@Test
		void tiposConIn() {
			assertThat(nombres(FiltroMascotas.builder().tipos(List.of("cat")).build()))
				.containsExactly("Leo", "Max", "Samantha", "Sly");
			assertThat(nombres(FiltroMascotas.builder().tipos(List.of("cat", "dog")).build())).hasSize(8);
		}

		@Test
		void nacidaDesde() {
			assertThat(nombres(FiltroMascotas.builder().nacidaDesde(LocalDate.of(2012, 1, 1)).build()))
				.containsExactly("Basil", "Max", "Samantha", "Sly");
		}

		@Test
		void apellidoDelPropietarioEmpiezaPor() {
			assertThat(nombres(FiltroMascotas.builder().apellidoPropietario("da").build()))
				.containsExactly("Basil", "Iggy");
		}

		@Test
		void ciudadesConIn() {
			assertThat(nombres(FiltroMascotas.builder().ciudades(List.of("Madison")).build()))
				.containsExactly("Freddy", "George", "Leo", "Mulligan");
		}

		@Test
		@DisplayName("minVisitas / maxVisitas: subconsulta COUNT correlacionada")
		void numeroDeVisitas() {
			assertThat(nombres(FiltroMascotas.builder().minVisitas(1).build())).containsExactly("Max", "Samantha");
			assertThat(nombres(FiltroMascotas.builder().maxVisitas(0).build())).hasSize(11);
		}

		@Test
		@DisplayName("visitadaDesde / visitadaHasta: EXISTS")
		void visitadaEntre() {
			assertThat(nombres(FiltroMascotas.builder()
				.visitadaDesde(LocalDate.of(2013, 1, 1))
				.visitadaHasta(LocalDate.of(2013, 1, 1))
				.build())).containsExactly("Samantha");
		}

		@Test
		@DisplayName("sinVisitasDesde: NOT EXISTS (revision pendiente)")
		void sinVisitasDesde() {
			List<String> pendientes = nombres(FiltroMascotas.builder().sinVisitasDesde(LocalDate.of(2013, 1, 4)).build());
			assertThat(pendientes).hasSize(12).doesNotContain("Samantha").contains("Max");
		}

		@Test
		@DisplayName("enfermedades / veterinario: EXISTS sobre el diagnostico")
		void diagnosticos() {
			assertThat(nombres(FiltroMascotas.builder().enfermedades(List.of(2)).build())).containsExactly("Samantha");
			assertThat(nombres(FiltroMascotas.builder().veterinario(1).build())).containsExactly("Max");
		}

	}

	@Nested
	@DisplayName("Combinaciones")
	class Combinaciones {

		@Test
		void losCriteriosSeCombinanConAnd() {
			assertThat(nombres(FiltroMascotas.builder()
				.tipos(List.of("cat"))
				.ciudades(List.of("Madison", "Monona"))
				.minVisitas(1)
				.build())).containsExactly("Max", "Samantha");
		}

		@Test
		@DisplayName("Enfermedad y veterinario se exigen en el MISMO diagnostico")
		void enfermedadYVeterinarioEnElMismoDiagnostico() {
			// Samantha tiene diabetes (de Leary) y Max un diagnostico de Carter,
			// pero ninguna mascota tiene una diabetes diagnosticada por Carter.
			assertThat(nombres(FiltroMascotas.builder().enfermedades(List.of(2)).veterinario(1).build())).isEmpty();
		}

		@Test
		@DisplayName("Filtrar por fechas de visita no altera el numero de visitas mostrado")
		void losFiltrosDeVisitasNoRecortanLasColumnasCalculadas() {
			FichaMascota samantha = petRepository.buscarMascotas(FiltroMascotas.builder()
				.visitadaDesde(LocalDate.of(2013, 1, 1))
				.visitadaHasta(LocalDate.of(2013, 1, 1))
				.build(), TODO).getContent().get(0);

			// Se ha filtrado por una visita del dia 1, pero la ficha cuenta las dos.
			assertThat(samantha.numeroVisitas()).isEqualTo(2);
			assertThat(samantha.ultimaVisita()).isEqualTo(LocalDate.of(2013, 1, 4));
		}

	}

	@Nested
	@DisplayName("Proyeccion, orden y paginacion")
	class ProyeccionOrdenYPaginacion {

		@Test
		void laFichaTraeLosDatosCalculadosPorLaBaseDeDatos() {
			FichaMascota max = petRepository.buscarMascotas(FiltroMascotas.builder().nombre("max").build(), TODO)
				.getContent()
				.get(0);

			assertThat(max.tipo()).isEqualTo("cat");
			assertThat(max.apellidoPropietario()).isEqualTo("Coleman");
			assertThat(max.ciudad()).isEqualTo("Monona");
			assertThat(max.numeroVisitas()).isEqualTo(2);
			assertThat(max.ultimaVisita()).isEqualTo(LocalDate.of(2013, 1, 3));

			FichaMascota leo = petRepository.buscarMascotas(FiltroMascotas.builder().nombre("leo").build(), TODO)
				.getContent()
				.get(0);
			assertThat(leo.numeroVisitas()).isZero();
			assertThat(leo.ultimaVisita()).isNull();
		}

		@Test
		@DisplayName("Se puede ordenar por un agregado (ultima visita)")
		void ordenarPorAgregado() {
			Page<FichaMascota> pagina = petRepository.buscarMascotas(FiltroMascotas.vacio(),
					PageRequest.of(0, 3, Sort.by(Sort.Order.desc("ultimaVisita"))));

			assertThat(pagina.getContent()).extracting(FichaMascota::nombre).startsWith("Samantha", "Max");
		}

		@Test
		@DisplayName("La consulta del total aplica los mismos filtros")
		void paginacion() {
			Page<FichaMascota> ultima = petRepository.buscarMascotas(FiltroMascotas.vacio(),
					PageRequest.of(2, 5, Sort.by("nombre")));
			assertThat(ultima.getContent()).hasSize(3);
			assertThat(ultima.getTotalElements()).isEqualTo(13);
			assertThat(ultima.getTotalPages()).isEqualTo(3);

			Page<FichaMascota> filtrada = petRepository.buscarMascotas(
					FiltroMascotas.builder().maxVisitas(0).build(), PageRequest.of(1, 5, Sort.by("nombre")));
			assertThat(filtrada.getTotalElements()).isEqualTo(11);
		}

		@Test
		@DisplayName("Ordenar por una columna fuera de la lista blanca falla")
		void ordenNoPermitido() {
			assertThatThrownBy(() -> petRepository.buscarMascotas(FiltroMascotas.vacio(),
					PageRequest.of(0, 5, Sort.by("owner.user.password"))))
				// Spring Data envuelve la IllegalArgumentException en InvalidDataAccessApiUsageException
				.hasMessageContaining("No se puede ordenar por 'owner.user.password'");
		}

		@Test
		void ciudadesParaElDesplegable() {
			assertThat(petRepository.ciudadesDePropietarios()).containsExactly("Madison", "McFarland", "Monona",
					"Sun Prairie", "Waunakee", "Windsor");
		}

	}

}
