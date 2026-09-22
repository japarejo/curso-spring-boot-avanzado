package org.springframework.samples.petclinic.repository.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.VisitRepository;

/**
 * LABORATORIO DE CRITERIA API (modulo 6, material adicional).
 *
 * Comprueba las cuatro consultas del fragmento contra los datos de `data.sql`.
 *
 * Datos relevantes, que conviene tener a la vista en clase:
 *
 *   - 13 mascotas, de las que SOLO 2 tienen visitas: Samantha (7) y Max (8).
 *   - Las dos son gatos y las dos son de Jean Coleman (owner 6, ciudad Monona).
 *   - 4 visitas en total, del 2013-01-01 al 2013-01-04.
 *
 * Es un conjunto de datos comodo para este laboratorio precisamente porque esta
 * desequilibrado: un tipo con todas las visitas y once mascotas sin ninguna.
 */
@DataJpaTest
@DisplayName("Criteria API: fragmento de repositorio implementado a mano")
class VisitCriteriaRepositoryTests {

	@Autowired
	private VisitRepository visitRepository;

	@Nested
	@DisplayName("1. Predicados dinamicos")
	class PredicadosDinamicos {

		@Test
		@DisplayName("El filtro vacio no anade ningun predicado y devuelve todo")
		void filtroVacioDevuelveTodasLasVisitas() {
			List<Visit> visitas = visitRepository.buscar(FiltroVisitas.vacio());

			// cb.and() sobre un array vacio es una conjuncion cierta: ni un `if`
			// extra ni una consulta aparte para el caso "sin filtros".
			assertThat(visitas).hasSize(4);
		}

		@Test
		@DisplayName("Ordena por fecha descendente")
		void ordenaPorFechaDescendente() {
			List<Visit> visitas = visitRepository.buscar(FiltroVisitas.vacio());

			assertThat(visitas).extracting(Visit::getDate).isSortedAccordingTo(java.util.Comparator.reverseOrder());
			assertThat(visitas.get(0).getDescription()).isEqualTo("spayed");
		}

		@Test
		@DisplayName("La descripcion es un LIKE que ignora mayusculas")
		void filtraPorDescripcionIgnorandoMayusculas() {
			List<Visit> visitas = visitRepository.buscar(FiltroVisitas.vacio().conDescripcion("RABIES"));

			assertThat(visitas).hasSize(2);
			assertThat(visitas).allSatisfy(v -> assertThat(v.getDescription()).isEqualTo("rabies shot"));
		}

		@Test
		@DisplayName("Filtra por un atributo heredado de NamedEntity (el nombre de la mascota)")
		void filtraPorNombreDeMascota() {
			List<Visit> visitas = visitRepository.buscar(FiltroVisitas.vacio().conNombreMascota("max"));

			// Max es la mascota 8, con dos visitas.
			assertThat(visitas).hasSize(2);
		}

		@Test
		@DisplayName("Filtra atravesando dos joins: visita -> mascota -> tipo")
		void filtraPorTipoDeMascota() {
			assertThat(visitRepository.buscar(FiltroVisitas.vacio().conTipoMascota("cat"))).hasSize(4);
			assertThat(visitRepository.buscar(FiltroVisitas.vacio().conTipoMascota("dog"))).isEmpty();
		}

		@Test
		@DisplayName("Filtra atravesando visita -> mascota -> propietario")
		void filtraPorCiudadDelPropietario() {
			assertThat(visitRepository.buscar(FiltroVisitas.vacio().conCiudadPropietario("Monona"))).hasSize(4);
			assertThat(visitRepository.buscar(FiltroVisitas.vacio().conCiudadPropietario("Madison"))).isEmpty();
		}

		@Test
		@DisplayName("El rango de fechas es cerrado por los dos extremos")
		void filtraPorRangoDeFechas() {
			List<Visit> visitas = visitRepository
				.buscar(FiltroVisitas.vacio().entre(LocalDate.of(2013, 1, 2), LocalDate.of(2013, 1, 3)));

			assertThat(visitas).hasSize(2);
			assertThat(visitas).extracting(Visit::getDescription).containsExactly("neutered", "rabies shot");
		}

		@Test
		@DisplayName("Los criterios se combinan con AND")
		void combinaVariosCriterios() {
			FiltroVisitas filtro = FiltroVisitas.vacio()
				.conTipoMascota("cat")
				.conCiudadPropietario("Monona")
				.conNombreMascota("Samantha")
				.entre(LocalDate.of(2013, 1, 1), LocalDate.of(2013, 1, 1));

			assertThat(visitRepository.buscar(filtro)).hasSize(1);
		}

		@Test
		@DisplayName("Un criterio que usa una subconsulta correlacionada")
		void filtraPorMinimoDeVisitasDelPropietario() {
			// Las 4 visitas son de Coleman, que tiene exactamente 4.
			assertThat(visitRepository.buscar(FiltroVisitas.vacio().conMinimoVisitasDelPropietario(4))).hasSize(4);
			assertThat(visitRepository.buscar(FiltroVisitas.vacio().conMinimoVisitasDelPropietario(5))).isEmpty();
		}
	}

	@Nested
	@DisplayName("2. Agregacion con proyeccion")
	class Agregacion {

		@Test
		@DisplayName("Cuenta la base de datos, no Java, y devuelve el record directamente")
		void resumePorTipoDeMascota() {
			List<ResumenPorTipo> resumen = visitRepository.resumirPorTipoDeMascota();

			// Solo aparece 'cat': los tipos sin visitas no salen porque el join
			// desde Visit es interno. Es la diferencia entre agrupar partiendo de
			// Visit y partiendo de PetType, y merece una pregunta en clase.
			assertThat(resumen).hasSize(1);
			assertThat(resumen.get(0).tipoMascota()).isEqualTo("cat");
			assertThat(resumen.get(0).numeroDeVisitas()).isEqualTo(4);
			assertThat(resumen.get(0).mascotasDistintas()).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("3. Subconsulta correlacionada")
	class Subconsulta {

		@Test
		@DisplayName("NOT EXISTS: las mascotas sin ninguna visita")
		void encuentraLasMascotasSinVisitas() {
			List<Pet> mascotas = visitRepository.mascotasSinVisitas();

			// 13 mascotas menos las 2 que tienen visitas.
			assertThat(mascotas).hasSize(11);
			assertThat(mascotas).extracting(Pet::getName).doesNotContain("Samantha", "Max");
		}

		@Test
		@DisplayName("Sin correlate() el resultado seria la lista vacia")
		void laCorrelacionEsLaQueHaceElTrabajo() {
			// Este test no prueba codigo nuevo: documenta el fallo que se comete
			// al escribirlo. Si se borra el subconsulta.correlate(mascota) del
			// Impl, el EXISTS pasa a ser "existe alguna visita en la tabla",
			// que es cierto, el NOT EXISTS es falso para todas las filas y este
			// assert cae. Es la forma de dejar constancia de una trampa.
			assertThat(visitRepository.mascotasSinVisitas()).isNotEmpty();
		}
	}

	@Nested
	@DisplayName("4. Agrupacion con HAVING")
	class AgrupacionConHaving {

		@Test
		@DisplayName("HAVING filtra grupos, no filas")
		void encuentraPropietariosConUnMinimoDeVisitas() {
			assertThat(visitRepository.propietariosConAlMenos(1)).containsExactly("Coleman");
			assertThat(visitRepository.propietariosConAlMenos(4)).containsExactly("Coleman");
			assertThat(visitRepository.propietariosConAlMenos(5)).isEmpty();
		}
	}
}
