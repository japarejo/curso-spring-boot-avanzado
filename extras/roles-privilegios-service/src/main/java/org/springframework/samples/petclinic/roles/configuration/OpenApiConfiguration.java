package org.springframework.samples.petclinic.roles.configuration;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Boton "Authorize" de Swagger UI con autenticacion Basic: permite cambiar de
 * usuario (eva, rita, victor, pablo, admin) sin salir del navegador.
 */
@Configuration
@OpenAPIDefinition(
		info = @Info(title = "Roles y privilegios",
				description = "Usuarios: eva, rita, victor, pablo, admin. Contrasena de todos: demo"),
		security = @SecurityRequirement(name = "basic"))
@SecurityScheme(name = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfiguration {
}
