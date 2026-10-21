package org.springframework.samples.petclinic.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.samples.petclinic.configuration.GitHubProperties;
import org.springframework.samples.petclinic.model.Authorities;
import org.springframework.samples.petclinic.model.CuentaGitHub;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.repository.AuthoritiesRepository;
import org.springframework.samples.petclinic.repository.CuentaGitHubRepository;
import org.springframework.samples.petclinic.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decide que usuario de PetClinic corresponde a una cuenta de GitHub y, si
 * esta permitido, lo da de alta la primera vez.
 *
 * @EnableConfigurationProperties: el servicio declara la configuracion que
 * necesita. Asi tambien arranca en las pruebas @DataJpaTest que cargan todos
 * los @Service (OwnerServiceTests...), donde no hay @ConfigurationPropertiesScan.
 */
@Service
@EnableConfigurationProperties(GitHubProperties.class)
public class AltaGitHubService {

	private final CuentaGitHubRepository cuentas;
	private final UserRepository usuarios;
	private final AuthoritiesRepository autoridades;
	private final GitHubProperties github;

	public AltaGitHubService(CuentaGitHubRepository cuentas, UserRepository usuarios,
			AuthoritiesRepository autoridades, GitHubProperties github) {
		this.cuentas = cuentas;
		this.usuarios = usuarios;
		this.autoridades = autoridades;
		this.github = github;
	}

	/**
	 * @return el username local, o vacio si esta cuenta de GitHub no puede entrar.
	 */
	@Transactional
	public Optional<String> usuarioPara(long githubId, String login) {
		// 1. Ya entro antes: el vinculo esta en la base de datos.
		Optional<CuentaGitHub> existente = cuentas.findById(githubId);
		if (existente.isPresent()) {
			existente.get().registrarAcceso(login);
			return Optional.of(existente.get().getUser().getUsername());
		}

		// 2. Primera vez, pero alguien la vinculo a un usuario que ya existe.
		String vinculado = github.usuarioLocalPara(String.valueOf(githubId));
		if (vinculado != null) {
			return usuarios.findById(vinculado).map(user -> vincular(githubId, login, user));
		}

		// 3. Primera vez y desconocida: alta solo si esta permitida.
		if (!github.altaAutomatica()) {
			return Optional.empty();
		}
		return Optional.of(vincular(githubId, login, crearUsuario(login)));
	}

	private String vincular(long githubId, String login, User user) {
		cuentas.save(new CuentaGitHub(githubId, login, user));
		return user.getUsername();
	}

	private User crearUsuario(String login) {
		User user = new User();
		user.setUsername(usernameLibre(login));
		// Contrasena aleatoria que nadie conoce: la cuenta solo puede entrar por GitHub.
		// Nunca vacia ni con el prefijo noop: el formulario aceptaria esa contrasena.
		// BCrypt directo y no el bean PasswordEncoder: las pruebas @DataJpaTest que
		// cargan todos los @Service no tienen la configuracion de seguridad.
		user.setPassword("{bcrypt}" + new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
		user.setEnabled(true);
		usuarios.save(user);

		Authorities autoridad = new Authorities();
		autoridad.setUser(user);
		autoridad.setAuthority(github.autoridadInicial());
		autoridades.save(autoridad);
		return user;
	}

	// El prefijo "gh-" evita choques con los usuarios de siempre: un login de
	// GitHub "admin1" no debe convertirse en el admin1 de PetClinic.
	private String usernameLibre(String login) {
		String base = "gh-" + login.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
		String candidato = base;
		for (int i = 2; usuarios.existsById(candidato); i++) {
			candidato = base + "-" + i;
		}
		return candidato;
	}
}
