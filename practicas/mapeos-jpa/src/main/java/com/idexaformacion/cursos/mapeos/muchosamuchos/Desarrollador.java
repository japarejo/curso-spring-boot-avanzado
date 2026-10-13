package com.idexaformacion.cursos.mapeos.muchosamuchos;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * Diapositiva 157: N:M bidireccional. Este es el lado PROPIETARIO: el que no
 * lleva mappedBy y decide la tabla de union (desarrollador_tareas).
 *
 * Respuesta a la pregunta de la diapositiva ("¿por que en un caso se debe
 * especificar el mappedBy?"): sin mappedBy en Tarea, JPA veria DOS relaciones
 * N:M distintas y crearia DOS tablas de union. mappedBy dice "esta coleccion es
 * el reflejo de aquella; no la persistas por tu cuenta".
 *
 * Set y no List: con List y sin @OrderColumn, Hibernate trata la coleccion como
 * "bag" y al quitar un elemento BORRA todas las filas de la union y las vuelve
 * a insertar. Con Set borra solo la fila afectada.
 */
@Entity
public class Desarrollador {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	@ManyToMany
	private Set<Tarea> tareas = new HashSet<>();

	protected Desarrollador() {
	}

	public Desarrollador(String nombre) {
		this.nombre = nombre;
	}

	public void asignar(Tarea tarea) {
		tareas.add(tarea);
		tarea.getDesarrolladores().add(this);
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Set<Tarea> getTareas() {
		return tareas;
	}

}
