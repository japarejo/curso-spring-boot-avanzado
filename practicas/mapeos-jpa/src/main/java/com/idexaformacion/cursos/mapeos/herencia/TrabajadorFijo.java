package com.idexaformacion.cursos.mapeos.herencia;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("FIJO")
public class TrabajadorFijo extends Trabajador {

	private Integer salario;

	protected TrabajadorFijo() {
	}

	public TrabajadorFijo(String nombre, int salario) {
		super(nombre);
		this.salario = salario;
	}

	@Override
	public double costeMensual() {
		return salario;
	}

}
