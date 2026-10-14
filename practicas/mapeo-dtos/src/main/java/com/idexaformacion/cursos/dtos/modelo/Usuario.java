package com.idexaformacion.cursos.dtos.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.Setter;

/**
 * Cuenta de acceso de un propietario. El password esta aqui para que se vea
 * que el DTO NO lo expone: el mapper solo copia lo que el DTO declara.
 */
@Entity
@Getter
@Setter
public class Usuario {

	@Id
	private String username;

	private String password;

}
