package org.springframework.samples.petclinic.auth.security;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Esquema "bearer-jwt" para el boton Authorize de Swagger UI (http://localhost:8060/).
 *
 * Flujo en clase, sin curl:
 *   1. POST /api/v1/auth/login con un usuario de prueba -> copiar el campo "token".
 *   2. Authorize -> bearer-jwt -> pegar el token (sin el prefijo "Bearer").
 *   3. POST /api/v1/auth/validate -> Execute.
 */
@Configuration
@SecurityScheme(name = "bearer-jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfiguration {
}
