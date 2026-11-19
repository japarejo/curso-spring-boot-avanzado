package org.springframework.samples.petclinic.roles.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un ROL es un PUESTO o perfil de la organizacion ("recepcionista",
 * "veterinario"), es decir, un nombre para un CONJUNTO de privilegios.
 *
 * <p>Lo decide el negocio y cambia con el organigrama; por eso vive en la base
 * de datos y no en el codigo. Si manana recepcion tambien debe poder registrar
 * visitas, se anade una fila en {@code roles_privilegios} y no se toca ni se
 * redespliega nada.
 *
 * <p>Por convencion SI lleva el prefijo {@code ROLE_}: es lo que espera
 * {@code hasRole('RECEPCION')}, que por dentro comprueba la autoridad
 * {@code ROLE_RECEPCION}.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

	public static final String PREFIJO = "ROLE_";

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String nombre;

	@ManyToMany
	@JoinTable(name = "roles_privilegios",
			joinColumns = @JoinColumn(name = "rol_id"),
			inverseJoinColumns = @JoinColumn(name = "privilegio_id"))
	private Set<Privilegio> privilegios = new HashSet<>();

	public Rol(String nombre) {
		this.nombre = nombre;
	}

}
