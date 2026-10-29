package org.springframework.samples.petclinic.repository.criteria;

import java.time.LocalDate;

/**
 * ESCENARIO DE CRITERIA API: una fila de la tabla de resultados.
 *
 * Es una proyeccion, no la entidad Pet: la consulta la construye con
 * cb.construct(...) y nunca carga Pet, Owner ni Visit en el contexto de
 * persistencia. Ademas trae dos datos calculados por la base de datos,
 * numeroVisitas (COUNT) y ultimaVisita (MAX), que la entidad no tiene.
 *
 * El orden y los tipos de los componentes deben coincidir EXACTAMENTE con los
 * argumentos de cb.construct; si no, Hibernate falla al ejecutar la consulta
 * (no al compilar) con "Could not locate appropriate constructor".
 */
public record FichaMascota(
		Integer id,
		String nombre,
		String tipo,
		LocalDate fechaNacimiento,
		String nombrePropietario,
		String apellidoPropietario,
		String ciudad,
		Long numeroVisitas,
		LocalDate ultimaVisita) {
}
