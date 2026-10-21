// =============================================================================
// Modulo 3 - Ejercicio 16: alta automatica la primera vez que se entra con GitHub.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Parte de la solucion del ejercicio 15 y sustituye dos de sus ficheros
// (GitHubProperties y GitHubOAuth2UserService).
//
// Flujo de cada login con GitHub:
//
//   GitHub -> DefaultOAuth2UserService -> GitHubOAuth2UserService
//                                               |
//                                    AltaGitHubService.usuarioPara(id, login)
//                                               |
//      1. id ya en github_accounts            -> ese usuario (y se actualiza el login)
//      2. id en petclinic.github.vinculos     -> se vincula al usuario existente
//      3. desconocido y alta-automatica=true  -> se crea gh-<login> con autoridad owner
//      4. desconocido y alta-automatica=false -> rechazo, vuelta a /login?error
//                                               |
//                         UserDetailsManager -> autoridades locales -> sesion
//
// Ficheros:
//   1. pom.xml                                        (igual que en el ejercicio 15)
//   2. resources/application-github.properties        (perfil "github")
//   3. resources/db/migration/V3__cuentas_github.sql  (solo perfil mysql, Flyway)
//   4. configuration/GitHubProperties.java            (SUSTITUYE al del 15)
//   5. model/CuentaGitHub.java                        (nuevo)
//   6. repository/CuentaGitHubRepository.java         (nuevo)
//   7. service/AltaGitHubService.java                 (nuevo, la pieza importante)
//   8. configuration/GitHubOAuth2UserService.java     (SUSTITUYE al del 15)
//   9. configuration/SecurityConfiguration.java       (cambios)
//  10. test/.../web/security/GitHubLoginSecurityTest.java   (H2, sin red)
//  11. test/.../testingexamples/spring/AltaGitHubMySqlContainerIT.java (MySQL real)
//
// Verificado con Spring Boot 3.5.16 copiando los ficheros en apps/petclinic-api:
// pasan las 10 pruebas del punto 10 y la del 11 (Docker), y el resto de la
// bateria sigue igual.
// =============================================================================


// ---------- 1. apps/petclinic-api/pom.xml ----------
/*
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-client</artifactId>
        </dependency>
*/


// ---------- 2. src/main/resources/application-github.properties ----------
/*
spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}

# Alta automatica: quien entre con GitHub por primera vez obtiene una cuenta
# gh-<login> con la autoridad indicada. Desactivada por defecto.
petclinic.github.alta-automatica=true
petclinic.github.autoridad-inicial=owner

# Opcional: vincular una cuenta de GitHub a un usuario que YA existe en vez de
# crear uno nuevo. Solo se usa la primera vez; despues manda github_accounts.
#petclinic.github.vinculos.583231=owner1
*/


// ---------- 3. src/main/resources/db/migration/V3__cuentas_github.sql ----------
/*
-- Con H2 (perfil por defecto) la tabla la crea Hibernate. Con el perfil mysql,
-- ddl-auto=validate y Flyway: sin esta migracion la aplicacion no arranca.
CREATE TABLE github_accounts (
  github_id     BIGINT      NOT NULL,
  login         VARCHAR(64),
  username      VARCHAR(64) NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  last_login_at DATETIME(6),
  PRIMARY KEY (github_id),
  CONSTRAINT fk_github_accounts_users FOREIGN KEY (username) REFERENCES users (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
*/
// Probada contra MySQL 8 con Testcontainers: ver el punto 11.


// ---------- 4. configuration/GitHubProperties.java ----------
package org.springframework.samples.petclinic.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * petclinic.github.vinculos.ID=usuario      vincula con un usuario que YA existe
 * petclinic.github.alta-automatica=true     crea la cuenta la primera vez
 * petclinic.github.autoridad-inicial=owner  autoridad de las cuentas nuevas
 */
@ConfigurationProperties(prefix = "petclinic.github")
public record GitHubProperties(
		Map<String, String> vinculos,
		@DefaultValue("false") boolean altaAutomatica,
		@DefaultValue("owner") String autoridadInicial) {

	public GitHubProperties {
		vinculos = (vinculos == null) ? Map.of() : Map.copyOf(vinculos);
	}

	public String usuarioLocalPara(String githubId) {
		return vinculos.get(githubId);
	}
}

// ---------- 5. model/CuentaGitHub.java ----------
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

// ---------- 6. repository/CuentaGitHubRepository.java ----------
package org.springframework.samples.petclinic.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.model.CuentaGitHub;

public interface CuentaGitHubRepository extends CrudRepository<CuentaGitHub, Long> {
}

// ---------- 7. service/AltaGitHubService.java ----------
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

// ---------- 8. configuration/GitHubOAuth2UserService.java ----------
package org.springframework.samples.petclinic.configuration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.samples.petclinic.service.AltaGitHubService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * GitHub dice QUIEN eres. PetClinic decide QUE puedes hacer y, si es la
 * primera vez, te crea la cuenta.
 */
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	static final String ATRIBUTO_USUARIO_LOCAL = "username";

	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegado;
	private final AltaGitHubService altas;
	private final UserDetailsService usuariosLocales;

	public GitHubOAuth2UserService(AltaGitHubService altas, UserDetailsService usuariosLocales) {
		this(new DefaultOAuth2UserService(), altas, usuariosLocales);
	}

	public GitHubOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegado,
			AltaGitHubService altas, UserDetailsService usuariosLocales) {
		this.delegado = delegado;
		this.altas = altas;
		this.usuariosLocales = usuariosLocales;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest peticion) throws OAuth2AuthenticationException {
		OAuth2User deGitHub = delegado.loadUser(peticion);
		long githubId = ((Number) deGitHub.getAttributes().get("id")).longValue();
		String login = String.valueOf(deGitHub.getAttributes().get("login"));

		// AltaGitHubService tiene su propia transaccion: cuando vuelve, el alta ya
		// esta confirmada y el UserDetailsManager (JDBC) la puede leer.
		String username = altas.usuarioPara(githubId, login)
			.orElseThrow(() -> rechazo("usuario_no_vinculado",
					"La cuenta de GitHub '" + login + "' (id " + githubId + ") no esta vinculada a PetClinic"));

		UserDetails local;
		try {
			local = usuariosLocales.loadUserByUsername(username);
		}
		catch (UsernameNotFoundException ex) {
			throw rechazo("usuario_inexistente", "El vinculo apunta a '" + username + "', que no existe");
		}
		if (!local.isEnabled()) {
			throw rechazo("usuario_deshabilitado", "El usuario '" + username + "' esta deshabilitado");
		}

		Map<String, Object> atributos = new LinkedHashMap<>(deGitHub.getAttributes());
		atributos.put(ATRIBUTO_USUARIO_LOCAL, local.getUsername());
		return new DefaultOAuth2User(local.getAuthorities(), atributos, ATRIBUTO_USUARIO_LOCAL);
	}

	private static OAuth2AuthenticationException rechazo(String codigo, String mensaje) {
		return new OAuth2AuthenticationException(new OAuth2Error(codigo, mensaje, null), mensaje);
	}
}

// ---------- 9. configuration/SecurityConfiguration.java (cambios) ----------
/*
diff --git a/apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/configuration/SecurityConfiguration.java b/apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/configuration/SecurityConfiguration.java
index 7989638..f9bb235 100644
--- a/apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/configuration/SecurityConfiguration.java
+++ b/apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/configuration/SecurityConfiguration.java
@@ -2,6 +2,7 @@ package org.springframework.samples.petclinic.configuration;
 
 import javax.sql.DataSource;
 
+import org.springframework.beans.factory.ObjectProvider;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.http.HttpMethod;
@@ -13,6 +14,8 @@ import org.springframework.security.crypto.factory.PasswordEncoderFactories;
 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.security.provisioning.JdbcUserDetailsManager;
 import org.springframework.security.provisioning.UserDetailsManager;
+import org.springframework.samples.petclinic.service.AltaGitHubService;
+import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
 import org.springframework.security.web.SecurityFilterChain;
 import org.springframework.security.web.AuthenticationEntryPoint;
 import org.springframework.security.web.authentication.HttpStatusEntryPoint;
@@ -44,7 +47,16 @@ public class SecurityConfiguration {
 
 	// 1. AUTORIZACION, LOGIN, CSRF Y CABECERAS ---------------------------------
 	@Bean
-	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
+	SecurityFilterChain filterChain(HttpSecurity http,
+			ObjectProvider<ClientRegistrationRepository> registros,
+			UserDetailsManager users, AltaGitHubService altas) throws Exception {
+
+		if (registros.getIfAvailable() != null) {
+			http.oauth2Login(oauth -> oauth
+				.userInfoEndpoint(info -> info.userService(new GitHubOAuth2UserService(altas, users)))
+				.defaultSuccessUrl("/welcome"));
+		}
+
 
 		http
 			.authorizeHttpRequests(auth -> auth
*/
// Igual que en el ejercicio 15, salvo que el servicio recibe AltaGitHubService.

// ---------- 10. src/test/java/.../web/security/GitHubLoginSecurityTest.java ----------
package org.springframework.samples.petclinic.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.configuration.GitHubOAuth2UserService;
import org.springframework.samples.petclinic.repository.CuentaGitHubRepository;
import org.springframework.samples.petclinic.service.AltaGitHubService;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Nada de esto necesita red ni credenciales reales: GitHub se sustituye por
 * una lambda que devuelve el usuario que diria GitHub.
 */
class GitHubLoginSecurityTest {

	static final String CLIENTE_DE_PRUEBA = "spring.security.oauth2.client.registration.github.client-id=id-de-prueba";
	static final String SECRETO_DE_PRUEBA = "spring.security.oauth2.client.registration.github.client-secret=secreto";

	@Nested
	@SpringBootTest(properties = { CLIENTE_DE_PRUEBA, SECRETO_DE_PRUEBA,
			"petclinic.github.alta-automatica=true",
			"petclinic.github.vinculos.583231=owner1" })
	@AutoConfigureMockMvc
	class ConAltaAutomatica {

		@Autowired AltaGitHubService altas;
		@Autowired UserDetailsManager usuarios;
		@Autowired CuentaGitHubRepository cuentas;
		@Autowired MockMvc mvc;

		OAuth2User entrarComo(long id, String login) {
			return new GitHubOAuth2UserService(req -> deGitHub(id, login), altas, usuarios).loadUser(peticion());
		}

		@Test
		void laPrimeraVezCreaLaCuentaYLaAutentica() {
			OAuth2User usuario = entrarComo(1001, "Nueva.Persona");

			assertThat(usuario.getName()).isEqualTo("gh-nueva-persona");
			assertThat(AuthorityUtils.authorityListToSet(usuario.getAuthorities())).containsExactly("owner");
			assertThat(usuarios.userExists("gh-nueva-persona")).isTrue();
			assertThat(cuentas.findById(1001L)).hasValueSatisfying(
					cuenta -> assertThat(cuenta.getUser().getUsername()).isEqualTo("gh-nueva-persona"));
		}

		@Test
		void laSegundaVezReutilizaLaCuentaAunqueCambieElLogin() {
			String primera = entrarComo(1002, "antes").getName();
			long cuentasTrasPrimera = cuentas.count();

			String segunda = entrarComo(1002, "despues").getName();

			assertThat(segunda).isEqualTo(primera).isEqualTo("gh-antes");
			assertThat(cuentas.count()).isEqualTo(cuentasTrasPrimera);
			assertThat(cuentas.findById(1002L).orElseThrow().getLogin()).isEqualTo("despues");
		}

		@Test
		void unVinculoConfiguradoApuntaAlUsuarioExistente() {
			assertThat(entrarComo(583231, "octocat").getName()).isEqualTo("owner1");
			assertThat(usuarios.userExists("gh-octocat")).isFalse();
		}

		@Test
		void dosCuentasConElMismoLoginNoChocan() {
			assertThat(entrarComo(1003, "repetido").getName()).isEqualTo("gh-repetido");
			assertThat(entrarComo(1004, "repetido").getName()).isEqualTo("gh-repetido-2");
		}

		@Test
		void unaCuentaCreadaPorGitHubNoPuedeEntrarPorFormulario() throws Exception {
			entrarComo(1005, "solo-github");
			mvc.perform(formLogin().user("gh-solo-github").password(""))
				.andExpect(unauthenticated());
		}

		@Test
		void laPaginaDeLoginOfreceGitHub() throws Exception {
			mvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/oauth2/authorization/github")));
		}

		@Test
		void elBotonRedirigeAGitHubConElCallbackCorrecto() throws Exception {
			mvc.perform(get("/oauth2/authorization/github"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", startsWith("https://github.com/login/oauth/authorize")))
				.andExpect(header().string("Location", containsString("client_id=id-de-prueba")))
				.andExpect(header().string("Location",
						containsString("redirect_uri=http://localhost/login/oauth2/code/github")));
		}

		@Test
		void unAnonimoSigueYendoAlFormularioPropio() throws Exception {
			mvc.perform(get("/owners/1"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "http://localhost/login"));
		}

		@Test
		void lasReglasDeAutorizacionSeAplicanIgual() throws Exception {
			OAuth2User owner = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("owner"),
					Map.of("username", "gh-alguien"), "username");
			mvc.perform(get("/owners/find").with(oauth2Login().oauth2User(owner)))
				.andExpect(status().isOk());
			mvc.perform(get("/admin/lo-que-sea").with(oauth2Login().oauth2User(owner)))
				.andExpect(status().isForbidden());
		}
	}

	@Nested
	@SpringBootTest(properties = { CLIENTE_DE_PRUEBA, SECRETO_DE_PRUEBA })
	class SinAltaAutomatica {

		@Autowired AltaGitHubService altas;
		@Autowired UserDetailsManager usuarios;

		@Test
		void unaCuentaDesconocidaSeRechazaYNoSeCreaNada() {
			var servicio = new GitHubOAuth2UserService(req -> deGitHub(2001, "intrusa"), altas, usuarios);

			assertThatThrownBy(() -> servicio.loadUser(peticion()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("intrusa");
			assertThat(usuarios.userExists("gh-intrusa")).isFalse();
		}
	}

	static OAuth2User deGitHub(long id, String login) {
		return new DefaultOAuth2User(AuthorityUtils.createAuthorityList("OAUTH2_USER"),
				Map.of("id", id, "login", login), "id");
	}

	static OAuth2UserRequest peticion() {
		var registro = CommonOAuth2Provider.GITHUB.getBuilder("github")
			.clientId("id").clientSecret("secreto").build();
		var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "t",
				Instant.now(), Instant.now().plusSeconds(60));
		return new OAuth2UserRequest(registro, token);
	}
}


// ---------- 11. src/test/java/.../testingexamples/spring/AltaGitHubMySqlContainerIT.java ----------
// Necesita Docker. Se ejecuta con Failsafe (mvnw verify), no con mvnw test.
package org.springframework.samples.petclinic.testingexamples.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.configuration.GitHubOAuth2UserService;
import org.springframework.samples.petclinic.service.AltaGitHubService;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.security.oauth2.client.registration.github.client-id=id",
		"spring.security.oauth2.client.registration.github.client-secret=secreto",
		"petclinic.github.alta-automatica=true",
		"petclinic.github.vinculos.583231=owner1" })
@ActiveProfiles("mysql")   // Flyway + ddl-auto=validate: falla si falta V3__cuentas_github.sql
class AltaGitHubMySqlContainerIT extends MySqlTestContainerSupport {

	@Autowired AltaGitHubService altas;
	@Autowired UserDetailsManager usuarios;
	@Autowired JdbcTemplate jdbc;

	OAuth2User entrarComo(long id, String login) {
		OAuth2User deGitHub = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("OAUTH2_USER"),
				Map.of("id", id, "login", login), "id");
		var registro = CommonOAuth2Provider.GITHUB.getBuilder("github").clientId("id").clientSecret("s").build();
		var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "t", Instant.now(),
				Instant.now().plusSeconds(60));
		return new GitHubOAuth2UserService(req -> deGitHub, altas, usuarios)
			.loadUser(new OAuth2UserRequest(registro, token));
	}

	@Test
	void flywayCreaLaTablaYElAltaFuncionaEnMySql() {
		assertThat(jdbc.queryForObject("select max(version) from flyway_schema_history", String.class))
			.isEqualTo("3");

		OAuth2User nuevo = entrarComo(9001, "persona-mysql");
		assertThat(nuevo.getName()).isEqualTo("gh-persona-mysql");
		assertThat(AuthorityUtils.authorityListToSet(nuevo.getAuthorities())).containsExactly("owner");

		assertThat(entrarComo(9001, "persona-renombrada").getName()).isEqualTo("gh-persona-mysql");
		assertThat(jdbc.queryForObject("select login from github_accounts where github_id = 9001", String.class))
			.isEqualTo("persona-renombrada");
		assertThat(jdbc.queryForObject("select count(*) from authorities where username = 'gh-persona-mysql'",
				Integer.class)).isEqualTo(1);

		assertThat(entrarComo(583231, "octocat").getName()).isEqualTo("owner1");
	}
}
