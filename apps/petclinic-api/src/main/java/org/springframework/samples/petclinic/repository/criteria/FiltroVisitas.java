package org.springframework.samples.petclinic.repository.criteria;

import java.time.LocalDate;

/**
 * LABORATORIO DE CRITERIA API (modulo 6, material adicional).
 *
 * Criterios de busqueda de visitas. Todos los campos son opcionales: un null
 * significa "no filtrar por esto".
 *
 * Es un `record`, asi que es inmutable y no hace falta escribir getters. El
 * metodo `vacio()` evita tener que pasar siete nulls cuando no se filtra nada.
 */
public record FiltroVisitas(
		String descripcion,
		String nombreMascota,
		String tipoMascota,
		String ciudadPropietario,
		LocalDate desde,
		LocalDate hasta,
		Integer minimoVisitasDelPropietario) {

	public static FiltroVisitas vacio() {
		return new FiltroVisitas(null, null, null, null, null, null, null);
	}

	public FiltroVisitas conDescripcion(String valor) {
		return new FiltroVisitas(valor, nombreMascota, tipoMascota, ciudadPropietario, desde, hasta,
				minimoVisitasDelPropietario);
	}

	public FiltroVisitas conNombreMascota(String valor) {
		return new FiltroVisitas(descripcion, valor, tipoMascota, ciudadPropietario, desde, hasta,
				minimoVisitasDelPropietario);
	}

	public FiltroVisitas conTipoMascota(String valor) {
		return new FiltroVisitas(descripcion, nombreMascota, valor, ciudadPropietario, desde, hasta,
				minimoVisitasDelPropietario);
	}

	public FiltroVisitas conCiudadPropietario(String valor) {
		return new FiltroVisitas(descripcion, nombreMascota, tipoMascota, valor, desde, hasta,
				minimoVisitasDelPropietario);
	}

	public FiltroVisitas entre(LocalDate inicio, LocalDate fin) {
		return new FiltroVisitas(descripcion, nombreMascota, tipoMascota, ciudadPropietario, inicio, fin,
				minimoVisitasDelPropietario);
	}

	public FiltroVisitas conMinimoVisitasDelPropietario(Integer valor) {
		return new FiltroVisitas(descripcion, nombreMascota, tipoMascota, ciudadPropietario, desde, hasta, valor);
	}
}
