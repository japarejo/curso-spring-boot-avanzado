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
