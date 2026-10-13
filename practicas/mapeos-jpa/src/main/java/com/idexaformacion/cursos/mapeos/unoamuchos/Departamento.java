package com.idexaformacion.cursos.mapeos.unoamuchos;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Diapositiva 156, corregida.
 *
 * La diapositiva pone @OneToMany(targetEntity = Employee.class) SIN mappedBy y
 * ademas @ManyToOne en Employee. Eso no es una relacion bidireccional: son DOS
 * relaciones independientes. Hibernate crea una columna departamento_id en
 * empleado Y una tabla de union departamento_employeelist. Ejercicio: quitar
 * mappedBy y mirar target/ddl-generado.sql.
 *
 * Lista con tipo (List<Empleado>), no List sin parametrizar: asi no hace falta
 * targetEntity.
 */
@Entity
public class Departamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	/**
	 * orphanRemoval: un empleado que sale de la lista se BORRA. Tiene sentido si
	 * un empleado no puede existir sin departamento; si no, se quita.
	 */
	@OneToMany(mappedBy = "departamento", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Empleado> empleados = new ArrayList<>();

	protected Departamento() {
	}

	public Departamento(String nombre) {
		this.nombre = nombre;
	}

	public void contratar(Empleado empleado) {
		empleados.add(empleado);
		empleado.setDepartamento(this);
	}

	public void despedir(Empleado empleado) {
		empleados.remove(empleado);
		empleado.setDepartamento(null);
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public List<Empleado> getEmpleados() {
		return empleados;
	}

}
