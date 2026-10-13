package com.idexaformacion.cursos.mapeos.muchosamuchos;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/** Lado INVERSO de la N:M (mappedBy). Modificar solo este lado NO se guarda. */
@Entity
public class Tarea {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String titulo;

	@ManyToMany(mappedBy = "tareas")
	private Set<Desarrollador> desarrolladores = new HashSet<>();

	protected Tarea() {
	}

	public Tarea(String titulo) {
		this.titulo = titulo;
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public Set<Desarrollador> getDesarrolladores() {
		return desarrolladores;
	}

}
