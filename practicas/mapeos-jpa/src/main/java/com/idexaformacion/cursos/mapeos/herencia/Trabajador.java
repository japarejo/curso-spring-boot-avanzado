package com.idexaformacion.cursos.mapeos.herencia;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * Diapositiva 158: herencia en UNA SOLA TABLA.
 *
 * Ejercicio: cambia la estrategia a JOINED y despues a TABLE_PER_CLASS, ejecuta
 * las pruebas y compara target/ddl-generado.sql y las consultas de la consola.
 *
 *   SINGLE_TABLE     una tabla, columna discriminadora, columnas de las
 *                    subclases admiten NULL (no se puede poner NOT NULL a
 *                    "salario": los trabajadores por horas no lo tienen).
 *                    Consultas polimorficas sin JOIN: la mas rapida.
 *   JOINED           una tabla por clase, unidas por la clave. Esquema normalizado;
 *                    cada consulta polimorfica hace JOIN.
 *   TABLE_PER_CLASS  una tabla por clase CONCRETA, con todas las columnas.
 *                    Consultas polimorficas con UNION. No admite IDENTITY.
 *
 * Respuesta a la pregunta de la diapositiva ("¿que puede conllevar el uso de una
 * unica tabla?"): columnas que no pueden ser obligatorias y una tabla ancha que
 * crece con cada subclase.
 *
 * Y una errata: en la diapositiva las subclases no llevan @Entity. Sin ella, JPA
 * no las conoce y sus campos no se guardan.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Trabajador {

	// SEQUENCE y no IDENTITY: asi el ejercicio de TABLE_PER_CLASS funciona sin
	// cambiar nada mas (esa estrategia no admite IDENTITY).
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	private String nombre;

	protected Trabajador() {
	}

	protected Trabajador(String nombre) {
		this.nombre = nombre;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	/** Polimorfismo de verdad: cada subclase calcula su coste a su manera. */
	public abstract double costeMensual();

}
