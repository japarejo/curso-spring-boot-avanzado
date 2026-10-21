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
