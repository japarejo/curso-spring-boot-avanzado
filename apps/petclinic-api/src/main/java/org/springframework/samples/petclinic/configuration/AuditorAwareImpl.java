package org.springframework.samples.petclinic.configuration;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Puente entre Spring Security y la auditoria de Spring Data JPA.
 *
 * MODULO 6 - Es quien rellena @CreatedBy y @LastModifiedBy de
 * {@link org.springframework.samples.petclinic.model.AuditableEntity}.
 * Se activa desde {@link JpaAuditingConfiguration}.
 *
 * Devolver Optional.empty() es legitimo: significa "no hay usuario conocido"
 * y Spring Data deja los campos de auditoria a null. Es lo que ocurre, por
 * ejemplo, cuando la escritura la provoca un proceso de arranque y no una
 * peticion HTTP.
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		// El orden de estas comprobaciones importa. Comprobar primero
		// `instanceof AnonymousAuthenticationToken` y despues el null provocaba
		// NullPointerException siempre que no habia contexto de seguridad,
		// porque `null instanceof X` es false y se entraba en la primera rama.
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}
		if (authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		return Optional.ofNullable(authentication.getName());
	}

}
