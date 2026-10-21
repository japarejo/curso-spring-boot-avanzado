// =============================================================================
// Modulo 3 - Ejercicio 15: entrar en petclinic-api con GitHub (OAuth 2).
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
//
// Idea central:   GitHub dice QUIEN eres  (autenticacion)
//                 PetClinic decide QUE puedes hacer  (autorizacion)
//
// Ficheros:
//   1. pom.xml                                       (una dependencia)
//   2. resources/application-github.properties       (perfil "github")
//   3. configuration/GitHubProperties.java           (vinculos GitHub -> usuario local)
//   4. configuration/GitHubOAuth2UserService.java    (la pieza importante)
//   5. configuration/SecurityConfiguration.java      (activar oauth2Login)
//   6. test/.../web/security/GitHubLoginSecurityTest.java
//
// Verificado con Spring Boot 3.5.16: las 8 pruebas del punto 6 pasan.
// =============================================================================


// ---------- 1. apps/petclinic-api/pom.xml (junto al resource-server) ----------
/*
        <!-- Cliente OAuth2: la aplicacion delega el login en GitHub -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-client</artifactId>
        </dependency>
*/
// resource-server  -> la aplicacion ACEPTA tokens emitidos por otro (ejercicio 6)
// oauth2-client    -> la aplicacion es CLIENTE de un proveedor de identidad
// Son independientes y pueden convivir en el mismo proyecto.


// ---------- 2. src/main/resources/application-github.properties ----------
/*
# Credenciales: SIEMPRE de variables de entorno, nunca escritas aqui.
# Sin valor por defecto a proposito: si se activa el perfil y faltan,
# la aplicacion NO arranca y dice exactamente que variable falta.
spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}

# Vinculos: id NUMERICO de GitHub -> usuario de PetClinic.
# Tu id: https://api.github.com/users/<tu-login>  (campo "id").
# 583231 es el de "octocat", el usuario de ejemplo de GitHub. Cambialo por el tuyo.
petclinic.github.vinculos.583231=owner1
*/
// No hace falta escribir authorization-uri, token-uri ni user-info-uri:
// Spring Security los trae de serie para GitHub, Google, Facebook y Okta
// (enum CommonOAuth2Provider). La URL de callback por defecto es
//   {baseUrl}/login/oauth2/code/{registrationId}
//   -> http://localhost:8000/login/oauth2/code/github
// y tiene que coincidir CARACTER A CARACTER con la registrada en GitHub.


// ---------- 3. configuration/GitHubProperties.java ----------
package org.springframework.samples.petclinic.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Vinculos entre cuentas de GitHub y usuarios de PetClinic.
 *
 * Clave: el id NUMERICO de GitHub. NO el login (se puede cambiar en
 * cualquier momento y otra persona podria quedarse el antiguo) ni el email
 * (puede ser privado o no estar verificado).
 *
 * La recoge @ConfigurationPropertiesScan de PetclinicApplication (modulo 5).
 * En un sistema real seria una tabla: ver ExternalIdentity en
 * extras/oauth2-login-service.
 */
@ConfigurationProperties(prefix = "petclinic.github")
public record GitHubProperties(Map<String, String> vinculos) {

	public GitHubProperties {
		vinculos = (vinculos == null) ? Map.of() : Map.copyOf(vinculos);
	}

	public String usuarioLocalPara(String githubId) {
		return vinculos.get(githubId);
	}
}


// ---------- 4. configuration/GitHubOAuth2UserService.java ----------
package org.springframework.samples.petclinic.configuration;

import java.util.LinkedHashMap;
import java.util.Map;

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
 * Se ejecuta UNA vez por login, justo despues de que Spring haya cambiado el
 * "code" de GitHub por un access token. Es el equivalente a
 * UserDetailsService.loadUserByUsername(...) para el login con proveedor.
 *
 * Sin esta clase, cualquier persona con cuenta en GitHub entraria con las
 * autoridades OAUTH2_USER y SCOPE_read:user, que no significan nada para
 * PetClinic. Y su nombre (Principal.getName()) seria el id numerico de
 * GitHub, de modo que la comprobacion de propiedad de
 * OwnerService.findOwnerById(int, Principal) nunca encontraria su ficha.
 */
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	static final String ATRIBUTO_USUARIO_LOCAL = "username";

	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegado;
	private final UserDetailsService usuariosLocales;
	private final GitHubProperties github;

	public GitHubOAuth2UserService(UserDetailsService usuariosLocales, GitHubProperties github) {
		// DefaultOAuth2UserService llama a https://api.github.com/user con el token.
		this(new DefaultOAuth2UserService(), usuariosLocales, github);
	}

	// Para las pruebas: el delegado se sustituye por una lambda y no hay red.
	public GitHubOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegado,
			UserDetailsService usuariosLocales, GitHubProperties github) {
		this.delegado = delegado;
		this.usuariosLocales = usuariosLocales;
		this.github = github;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest peticion) throws OAuth2AuthenticationException {
		// 1. Quien eres segun GitHub
		OAuth2User deGitHub = delegado.loadUser(peticion);
		String githubId = String.valueOf(deGitHub.getAttributes().get("id"));
		String login = String.valueOf(deGitHub.getAttributes().get("login"));

		// 2. Quien eres en PetClinic. Lista blanca: sin vinculo, no se entra.
		String username = github.usuarioLocalPara(githubId);
		if (username == null) {
			throw rechazo("usuario_no_vinculado",
					"La cuenta de GitHub '" + login + "' (id " + githubId + ") no esta vinculada a PetClinic");
		}

		UserDetails local;
		try {
			local = usuariosLocales.loadUserByUsername(username);
		}
		catch (UsernameNotFoundException ex) {
			throw rechazo("usuario_inexistente", "El vinculo apunta a '" + username + "', que no existe");
		}
		// Deshabilitar al usuario local tambien le corta el acceso por GitHub.
		if (!local.isEnabled()) {
			throw rechazo("usuario_deshabilitado", "El usuario '" + username + "' esta deshabilitado");
		}

		// 3. Que puedes hacer: las autoridades de la tabla authorities, no las de GitHub.
		//    El nombre del Principal pasa a ser el username local.
		Map<String, Object> atributos = new LinkedHashMap<>(deGitHub.getAttributes());
		atributos.put(ATRIBUTO_USUARIO_LOCAL, local.getUsername());
		return new DefaultOAuth2User(local.getAuthorities(), atributos, ATRIBUTO_USUARIO_LOCAL);
	}

	// OAuth2AuthenticationException -> failureHandler -> /login?error, y el
	// mensaje aparece en la pagina de login generada por Spring.
	private static OAuth2AuthenticationException rechazo(String codigo, String mensaje) {
		return new OAuth2AuthenticationException(new OAuth2Error(codigo, mensaje, null), mensaje);
	}
}


// ---------- 5. configuration/SecurityConfiguration.java (cambios) ----------
/*
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http,
			ObjectProvider<ClientRegistrationRepository> registros,
			UserDetailsManager users, GitHubProperties github) throws Exception {

		// Solo si hay algun proveedor configurado (perfil "github"). Sin esto,
		// oauth2Login() falla al arrancar en cuanto no hay credenciales:
		// "No qualifying bean of type ClientRegistrationRepository".
		if (registros.getIfAvailable() != null) {
			http.oauth2Login(oauth -> oauth
				.userInfoEndpoint(info -> info.userService(new GitHubOAuth2UserService(users, github)))
				.defaultSuccessUrl("/welcome"));
		}

		http
			.authorizeHttpRequests(auth -> auth
				... // SIN CAMBIOS
*/
// Lo que NO hace falta tocar, y conviene preguntar en clase por que:
//   - Ninguna regla de autorizacion nueva. /oauth2/authorization/github y
//     /login/oauth2/code/github los atienden filtros que van ANTES del de
//     autorizacion, asi que denyAll() no llega a verlos.
//   - Ninguna vista. Con formLogin() y oauth2Login() sin loginPage(), la pagina
//     generada muestra el formulario Y un enlace "GitHub".
//   - Ningun controlador de callback. Lo atiende OAuth2LoginAuthenticationFilter.
//   - Ninguna regla de negocio. /owners/** sigue pidiendo hasAnyAuthority("owner","admin")
//     y OwnerService sigue comparando con Principal.getName(): los dos funcionan
//     igual para quien entra por formulario y para quien entra por GitHub.


// ---------- 6. src/test/java/.../web/security/GitHubLoginSecurityTest.java ----------
package org.springframework.samples.petclinic.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
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
import org.springframework.samples.petclinic.configuration.GitHubProperties;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Nada de esto necesita red ni credenciales reales de GitHub.
 */
class GitHubLoginSecurityTest {

	/** Prueba unitaria del servicio: GitHub se sustituye por una lambda. */
	@Nested
	class ServicioDeUsuarios {

		private final OAuth2User deGitHub = new DefaultOAuth2User(
				AuthorityUtils.createAuthorityList("OAUTH2_USER"),
				Map.of("id", 583231, "login", "octocat"), "id");

		private final InMemoryUserDetailsManager locales = new InMemoryUserDetailsManager(
				User.withUsername("owner1").password("x").authorities("owner").build(),
				User.withUsername("baja").password("x").authorities("owner").disabled(true).build());

		private OAuth2User cargar(Map<String, String> vinculos) {
			return new GitHubOAuth2UserService(req -> deGitHub, locales, new GitHubProperties(vinculos))
				.loadUser(peticion());
		}

		@Test
		void cuentaVinculadaRecibeNombreYAutoridadesLocales() {
			OAuth2User usuario = cargar(Map.of("583231", "owner1"));
			assertThat(usuario.getName()).isEqualTo("owner1");
			assertThat(AuthorityUtils.authorityListToSet(usuario.getAuthorities())).containsExactly("owner");
			assertThat((String) usuario.getAttribute("login")).isEqualTo("octocat");
		}

		@Test
		void cuentaSinVincularSeRechaza() {
			assertThatThrownBy(() -> cargar(Map.of()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("octocat");
		}

		@Test
		void usuarioLocalDeshabilitadoSeRechaza() {
			assertThatThrownBy(() -> cargar(Map.of("583231", "baja")))
				.isInstanceOf(OAuth2AuthenticationException.class);
		}

		private OAuth2UserRequest peticion() {
			var registro = CommonOAuth2Provider.GITHUB.getBuilder("github")
				.clientId("id").clientSecret("secreto").build();
			var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "t",
					Instant.now(), Instant.now().plusSeconds(60));
			return new OAuth2UserRequest(registro, token);
		}
	}

	/** La cadena de filtros completa, con un registro de GitHub de mentira. */
	@Nested
	@SpringBootTest(properties = {
			"spring.security.oauth2.client.registration.github.client-id=id-de-prueba",
			"spring.security.oauth2.client.registration.github.client-secret=secreto-de-prueba" })
	@AutoConfigureMockMvc
	class FlujoWeb {

		@Autowired
		MockMvc mvc;

		@Test
		void laPaginaDeLoginOfreceGitHub() throws Exception {
			mvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/oauth2/authorization/github")));
		}

		// Primer salto del flujo: la redireccion a GitHub con client_id, state y callback.
		@Test
		void elBotonRedirigeAGitHubConElCallbackCorrecto() throws Exception {
			mvc.perform(get("/oauth2/authorization/github"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", startsWith("https://github.com/login/oauth/authorize")))
				.andExpect(header().string("Location", containsString("client_id=id-de-prueba")))
				.andExpect(header().string("Location",
						containsString("redirect_uri=http://localhost/login/oauth2/code/github")));
		}

		// Con un solo proveedor, oauth2Login() querria mandar a GitHub directamente a
		// todo anonimo. Nuestro entry point web se registra antes y gana.
		@Test
		void anonimoSigueYendoAlFormularioPropio() throws Exception {
			mvc.perform(get("/owners/1"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "http://localhost/login"));
		}

		// oauth2Login() de spring-security-test: como @WithMockUser, pero dejando en
		// el contexto un OAuth2AuthenticationToken, que es lo que deja el login real.
		@Test
		void usuarioDeGitHubConAutoridadOwnerEntraEnOwners() throws Exception {
			OAuth2User owner1 = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("owner"),
					Map.of("username", "owner1", "id", 583231), "username");
			mvc.perform(get("/owners/find").with(oauth2Login().oauth2User(owner1)))
				.andExpect(status().isOk());
		}

		@Test
		void usuarioDeGitHubSinAutoridadAdminNoEntraEnAdmin() throws Exception {
			OAuth2User owner1 = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("owner"),
					Map.of("username", "owner1"), "username");
			mvc.perform(get("/admin/anything").with(oauth2Login().oauth2User(owner1)))
				.andExpect(status().isForbidden());
		}
	}
}
