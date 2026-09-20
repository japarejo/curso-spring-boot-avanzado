package org.springframework.samples.petclinic.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTOs de la API de autenticacion (modulo 2 + modulo 3).
 *
 * Son `record`: inmutables, sin getters ni setters escritos a mano y con la
 * validacion declarada en los propios componentes.
 *
 * Fijarse en lo que NO viaja: la entidad User nunca sale de aqui. Si se
 * devolviera, la respuesta incluiria el hash de la contrasena y la lista de
 * autoridades con sus identificadores de base de datos. Ese es el riesgo de
 * "exposicion excesiva de datos" del OWASP API Security Top 10.
 */
public final class AuthDtos {

	private AuthDtos() {
	}

	@Schema(description = "Credenciales de acceso")
	public record LoginRequest(
			@NotBlank @Schema(example = "admin1") String username,
			@NotBlank @Schema(example = "4dm1n") String password) {
	}

	@Schema(description = "Token emitido tras una autenticacion correcta")
	public record TokenResponse(
			@Schema(description = "JWT firmado") String token,
			@Schema(description = "Algoritmo de firma en uso", example = "HS512 (simetrico)") String algorithm,
			@Schema(description = "Segundos de validez", example = "3600") long expiresIn) {
	}

	@Schema(description = "Resultado de validar un token")
	public record ValidationResponse(
			boolean valid,
			String username,
			java.util.List<String> authorities) {
	}
}
