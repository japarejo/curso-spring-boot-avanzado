package com.idexaformacion.cursos.dtos.dto;

/**
 * Entrada para crear (POST) y para modificar parcialmente (PATCH).
 *
 * No lleva id, version, usuario ni mascotas: el cliente no puede fijarlos.
 * Si se recibiera la entidad directamente, un JSON con "id" o "version"
 * los sobrescribiria (mass assignment).
 *
 * En un PATCH los campos que llegan a null se dejan como estaban.
 */
public record PropietarioRequest(
		String nombre,
		String apellidos,
		String direccion,
		String ciudad,
		String telefono) {
}
