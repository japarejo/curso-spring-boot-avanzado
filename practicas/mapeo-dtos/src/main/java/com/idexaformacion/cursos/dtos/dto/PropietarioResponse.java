package com.idexaformacion.cursos.dtos.dto;

import java.util.List;

/**
 * Detalle de un propietario. Es el OwnerResponse de petclinic-api escrito como
 * record: sin getters, setters ni metodo from(). MapStruct rellena un record
 * llamando a su constructor.
 *
 * username sale de propietario.usuario.username (ver @Mapping en el mapper).
 * El password del usuario no esta aqui, asi que nunca se copia.
 */
public record PropietarioResponse(
		Integer id,
		String nombre,
		String apellidos,
		String ciudad,
		String direccion,
		String telefono,
		String username,
		List<MascotaResponse> mascotas) {
}
