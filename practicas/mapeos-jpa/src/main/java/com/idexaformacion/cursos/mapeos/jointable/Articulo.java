package com.idexaformacion.cursos.mapeos.jointable;

import java.util.HashSet;
import java.util.Set;

import com.idexaformacion.cursos.mapeos.reflexivas.Investigador;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * Diapositivas 167-168: DOS relaciones N:M entre las mismas dos entidades.
 *
 * Segun la especificacion JPA, el nombre por defecto de la tabla de union es
 * <tabla propietaria>_<tabla destino>: articulo_investigador para LAS DOS. Es
 * la colision que muestra la diapositiva 167.
 *
 * Matiz importante en Spring Boot: su estrategia de nombres implicitos
 * (SpringImplicitNamingStrategy) usa <tabla propietaria>_<nombre del campo>, es
 * decir articulo_autores y articulo_revisores. Con Spring Boot la colision NO
 * se produce aunque se quite @JoinTable. Comprobarlo es parte del ejercicio.
 * Aun asi, @JoinTable explicito es lo recomendable: el esquema no depende de una
 * configuracion que puede cambiar.
 *
 * Errata de la diapositiva 168: joinColumns describe la columna que apunta al
 * PROPIETARIO (el articulo), e inverseJoinColumns la que apunta al otro lado
 * (el investigador). La diapositiva los tiene al reves (aurhor_id en joinColumns).
 */
@Entity
public class Articulo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String titulo;

	@ManyToMany
	@JoinTable(name = "articulo_autores",
			joinColumns = @JoinColumn(name = "articulo_id"),
			inverseJoinColumns = @JoinColumn(name = "autor_id"))
	private Set<Investigador> autores = new HashSet<>();

	@ManyToMany
	@JoinTable(name = "articulo_revisores",
			joinColumns = @JoinColumn(name = "articulo_id"),
			inverseJoinColumns = @JoinColumn(name = "revisor_id"))
	private Set<Investigador> revisores = new HashSet<>();

	protected Articulo() {
	}

	public Articulo(String titulo) {
		this.titulo = titulo;
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public Set<Investigador> getAutores() {
		return autores;
	}

	public Set<Investigador> getRevisores() {
		return revisores;
	}

}
