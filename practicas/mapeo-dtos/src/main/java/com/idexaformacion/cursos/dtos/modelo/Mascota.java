package com.idexaformacion.cursos.dtos.modelo;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Mascota {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String nombre;

	private LocalDate fechaNacimiento;

	/**
	 * El DTO de mascota no tiene este campo. Por eso no hay ciclo
	 * Propietario -> Mascota -> Propietario al serializar, que es lo que pasa
	 * cuando se devuelven las entidades tal cual.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private Propietario propietario;

}
