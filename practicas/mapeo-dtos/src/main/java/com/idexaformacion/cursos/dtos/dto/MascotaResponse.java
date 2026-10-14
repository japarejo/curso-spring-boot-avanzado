package com.idexaformacion.cursos.dtos.dto;

/**
 * fechaNacimiento es texto con formato dd/MM/yyyy y edad se calcula: ninguno
 * de los dos se copia tal cual de la entidad.
 */
public record MascotaResponse(
		Integer id,
		String nombre,
		String fechaNacimiento,
		Integer edad) {
}
