package org.springframework.samples.petclinic.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Vinculo persistente entre una cuenta de GitHub y un usuario de PetClinic.
 *
 * La clave es el id NUMERICO de GitHub, que no cambia nunca. El login se
 * guarda solo para mostrarlo: se puede cambiar en GitHub en cualquier momento.
 */
@Entity
@Table(name = "github_accounts")
public class CuentaGitHub {

	@Id
	@Column(name = "github_id")
	private Long githubId;

	@Column(name = "login", length = 64)
	private String login;

	@ManyToOne(optional = false)
	@JoinColumn(name = "username", nullable = false)
	private User user;

	@Column(name = "created_at", nullable = false)
	private Instant creada;

	@Column(name = "last_login_at")
	private Instant ultimoAcceso;

	protected CuentaGitHub() {
	}

	public CuentaGitHub(Long githubId, String login, User user) {
		this.githubId = githubId;
		this.login = login;
		this.user = user;
		this.creada = Instant.now();
		this.ultimoAcceso = this.creada;
	}

	public void registrarAcceso(String loginActual) {
		this.login = loginActual;
		this.ultimoAcceso = Instant.now();
	}

	public Long getGithubId() {
		return githubId;
	}

	public String getLogin() {
		return login;
	}

	public User getUser() {
		return user;
	}

	public Instant getCreada() {
		return creada;
	}

	public Instant getUltimoAcceso() {
		return ultimoAcceso;
	}
}
