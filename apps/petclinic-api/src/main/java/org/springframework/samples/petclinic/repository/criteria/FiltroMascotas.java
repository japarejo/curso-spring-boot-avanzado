package org.springframework.samples.petclinic.repository.criteria;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import lombok.Builder;

/**
 * ESCENARIO DE CRITERIA API: busqueda avanzada de pacientes (modulo 6).
 *
 * Un campo por cada control de la pantalla de busqueda
 * (resources/busqueda-mascotas.html). TODOS son opcionales: null, o una lista
 * vacia, significa "el usuario no ha activado este filtro".
 *
 * Con 13 criterios opcionales hay 2^13 = 8192 combinaciones posibles. Escribir
 * un metodo derivado o una @Query por combinacion es imposible; concatenar JPQL
 * a mano es ilegible e inseguro. Es el caso de libro de la Criteria API.
 *
 * Los criterios se agrupan como en la pantalla:
 *
 *   Mascota       nombre, tipos, nacidaDesde, nacidaHasta
 *   Propietario   apellidoPropietario, ciudades
 *   Historial     minVisitas, maxVisitas, visitadaDesde, visitadaHasta,
 *                 sinVisitasDesde
 *   Diagnosticos  enfermedades, veterinario
 *
 * Spring MVC lo rellena directamente desde la query string (los record se
 * enlazan por constructor) y las listas aceptan el parametro repetido:
 * ?tipos=cat&tipos=dog. @Builder es solo para que las pruebas se lean bien.
 */
@Builder
public record FiltroMascotas(
		// --- Mascota --------------------------------------------------------
		/** El nombre contiene este texto (sin distinguir mayusculas). */
		String nombre,
		/** El tipo es alguno de estos (IN). */
		List<String> tipos,
		@DateTimeFormat(iso = ISO.DATE) LocalDate nacidaDesde,
		@DateTimeFormat(iso = ISO.DATE) LocalDate nacidaHasta,

		// --- Propietario ----------------------------------------------------
		/** El apellido del propietario empieza por este texto. */
		String apellidoPropietario,
		/** El propietario vive en alguna de estas ciudades (IN). */
		List<String> ciudades,

		// --- Historial de visitas -------------------------------------------
		/** Numero total de visitas de la mascota, minimo y maximo (subconsulta COUNT). */
		Integer minVisitas,
		Integer maxVisitas,
		/** Tiene al menos una visita en este intervalo (EXISTS). */
		@DateTimeFormat(iso = ISO.DATE) LocalDate visitadaDesde,
		@DateTimeFormat(iso = ISO.DATE) LocalDate visitadaHasta,
		/** No ha venido a consulta desde esta fecha: "revision pendiente" (NOT EXISTS). */
		@DateTimeFormat(iso = ISO.DATE) LocalDate sinVisitasDesde,

		// --- Diagnosticos ---------------------------------------------------
		/** Le han diagnosticado alguna de estas enfermedades (ids). */
		List<Integer> enfermedades,
		/** Le ha diagnosticado este veterinario (id). */
		Integer veterinario) {

	public static FiltroMascotas vacio() {
		return builder().build();
	}

}
