package org.springframework.samples.petclinic.roles.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un PRIVILEGIO es una accion concreta y de grano fino: "leer mascotas",
 * "registrar una visita". Es lo que el CODIGO comprueba:
 *
 * <pre>@PreAuthorize("hasAuthority('VISITA_REGISTRAR')")</pre>
 *
 * <p>Por convencion no lleva el prefijo {@code ROLE_}: asi, al ver una
 * autoridad en un log o en un JWT, se distingue a simple vista si es un rol o
 * un privilegio. Para Spring Security ambos son exactamente lo mismo: un
 * {@code GrantedAuthority} con un {@code String} dentro.
 */
@Entity
@Table(name = "privilegios")
@Getter
@Setter
@NoArgsConstructor
public class Privilegio {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String nombre;

	public Privilegio(String nombre) {
		this.nombre = nombre;
	}

}
