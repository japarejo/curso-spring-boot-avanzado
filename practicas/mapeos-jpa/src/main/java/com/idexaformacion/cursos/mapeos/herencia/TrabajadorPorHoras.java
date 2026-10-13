package com.idexaformacion.cursos.mapeos.herencia;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("HORAS")
public class TrabajadorPorHoras extends Trabajador {

	private Float precioHora;

	private Integer horasMes;

	protected TrabajadorPorHoras() {
	}

	public TrabajadorPorHoras(String nombre, float precioHora, int horasMes) {
		super(nombre);
		this.precioHora = precioHora;
		this.horasMes = horasMes;
	}

	@Override
	public double costeMensual() {
		return precioHora * horasMes;
	}

}
