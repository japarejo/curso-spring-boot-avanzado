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
