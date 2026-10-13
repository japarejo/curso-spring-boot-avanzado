package com.idexaformacion.cursos.mapeos.reflexivas;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * Diapositiva 169: relacion reflexiva 1:N (un arbol). Es una 1:N normal en la
 * que las dos entidades son la misma: la columna padre_id apunta a la propia
 * tabla carpeta.
 */
@Entity
public class Carpeta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	@ManyToOne
	@JoinColumn(name = "padre_id")
	private Carpeta padre;

	@OneToMany(mappedBy = "padre", cascade = CascadeType.ALL)
	private Set<Carpeta> hijas = new HashSet<>();

	protected Carpeta() {
	}

	public Carpeta(String nombre) {
		this.nombre = nombre;
	}

	public Carpeta crear(String nombreHija) {
		Carpeta hija = new Carpeta(nombreHija);
		hija.padre = this;
		hijas.add(hija);
		return hija;
	}

	/** Ruta completa recorriendo el arbol hacia arriba. */
	public String ruta() {
		return padre == null ? "/" + nombre : padre.ruta() + "/" + nombre;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Carpeta getPadre() {
		return padre;
	}

	public Set<Carpeta> getHijas() {
		return hijas;
	}

}
