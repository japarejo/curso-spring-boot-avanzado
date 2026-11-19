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
 * Un usuario tiene ROLES, nunca privilegios sueltos. Los privilegios le llegan
 * siempre a traves de sus roles:
 *
 * <pre>
 *   usuarios --&lt; usuarios_roles &gt;-- roles --&lt; roles_privilegios &gt;-- privilegios
 * </pre>
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String password;

	private boolean activo = true;

	@ManyToMany
	@JoinTable(name = "usuarios_roles",
			joinColumns = @JoinColumn(name = "usuario_id"),
			inverseJoinColumns = @JoinColumn(name = "rol_id"))
	private Set<Rol> roles = new HashSet<>();

	public Usuario(String username, String password) {
		this.username = username;
		this.password = password;
	}

}
