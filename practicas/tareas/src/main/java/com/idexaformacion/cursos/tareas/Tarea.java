package com.idexaformacion.cursos.tareas;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La entidad de la diapositiva 154, actualizada:
 *   - jakarta.persistence en lugar de javax.persistence (Spring Boot 3);
 *   - LocalDate en lugar de java.util.Date;
 *   - Integer en lugar de int para el id, para poder distinguir "nueva" (null).
 *
 * MODULO 6 - Para ver el DDL que genera Hibernate, arranca y busca en la consola
 * "create table tarea" (spring.jpa.show-sql=true en application.properties).
 */
@Entity
@Table(name = "tarea")
public class Tarea {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@NotBlank
	@Size(max = 255)
	@Column(name = "descripcion")
	private String descripcion;

	@Column(name = "fecha_limite")
	private LocalDate fechaLimite;

	@Column(name = "esta_hecho")
	private boolean hecho;

	protected Tarea() {
		// Constructor vacio exigido por JPA (y por Jackson al deserializar).
	}

	public Tarea(String descripcion, LocalDate fechaLimite) {
		this.descripcion = descripcion;
		this.fechaLimite = fechaLimite;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public LocalDate getFechaLimite() {
		return fechaLimite;
	}

	public void setFechaLimite(LocalDate fechaLimite) {
		this.fechaLimite = fechaLimite;
	}

	public boolean isHecho() {
		return hecho;
	}

	public void setHecho(boolean hecho) {
		this.hecho = hecho;
	}

}
