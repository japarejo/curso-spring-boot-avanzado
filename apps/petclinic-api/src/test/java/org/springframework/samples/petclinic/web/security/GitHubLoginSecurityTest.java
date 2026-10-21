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
