package com.idexaformacion.cursos.mapeos.reflexivas;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * Diapositiva 170: relacion reflexiva N:M (directores y doctorandos). Se mapea
 * como cualquier N:M bidireccional; la unica diferencia es que los dos lados son
 * la misma clase.
 *
 * @JoinTable da nombres que se entienden. Sin el, las columnas se llamarian
 * investigador_id y directores_id, y no se sabria cual es cual.
 *
 * Tambien se usa como "Researcher" de las diapositivas 167-168 (paquete jointable).
 */
@Entity
public class Investigador {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	@ManyToMany
	@JoinTable(name = "direccion_tesis",
			joinColumns = @JoinColumn(name = "doctorando_id"),
			inverseJoinColumns = @JoinColumn(name = "director_id"))
	private Set<Investigador> directores = new HashSet<>();

	@ManyToMany(mappedBy = "directores")
	private Set<Investigador> doctorandos = new HashSet<>();

	protected Investigador() {
	}

	public Investigador(String nombre) {
		this.nombre = nombre;
	}

	public void dirigidoPor(Investigador director) {
		directores.add(director);
		director.doctorandos.add(this);
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Set<Investigador> getDirectores() {
		return directores;
	}

	public Set<Investigador> getDoctorandos() {
		return doctorandos;
	}

}
