package com.idexaformacion.cursos.mapeos.unoamuchos;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Lado PROPIETARIO de la 1:N: la clave ajena departamento_id esta en esta tabla.
 *
 * fetch = LAZY: @ManyToOne es EAGER por defecto (diapositiva 172). Casi nunca es
 * lo que se quiere: cargar un empleado arrastraria su departamento siempre.
 */
@Entity
public class Empleado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	private double salario;

	@ManyToOne(fetch = FetchType.LAZY)
	private Departamento departamento;

	protected Empleado() {
	}

	public Empleado(String nombre, double salario) {
		this.nombre = nombre;
		this.salario = salario;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public double getSalario() {
		return salario;
	}

	public Departamento getDepartamento() {
		return departamento;
	}

	void setDepartamento(Departamento departamento) {
		this.departamento = departamento;
	}

}
