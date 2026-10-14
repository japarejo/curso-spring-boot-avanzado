package com.idexaformacion.cursos.dtos.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PropietarioNoEncontradoException extends RuntimeException {

	public PropietarioNoEncontradoException(int id) {
		super("No existe el propietario " + id);
	}

}
