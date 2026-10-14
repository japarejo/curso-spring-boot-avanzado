package com.idexaformacion.cursos.dtos.dto;

/**
 * Representacion corta para el listado. Otro DTO del mismo objeto, con campos
 * que NO existen en la entidad (nombreCompleto, numeroMascotas).
 *
 * Comparalo con @JsonView en petclinic-api (OwnerViewsDemoRestController):
 * las vistas solo pueden ocultar campos, un DTO aparte puede cambiar la forma.
 */
public record PropietarioResumen(
		Integer id,
		String nombreCompleto,
		String ciudad,
		int numeroMascotas) {
}
