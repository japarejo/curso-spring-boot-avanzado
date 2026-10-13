package org.springframework.samples.petclinic.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * MODULO 2 - Documentacion OpenAPI.
 *
 * Swagger UI (http://localhost:8080/swagger-ui.html) es la herramienta con la que
 * se prueba la API en clase, en lugar de curl. Por eso se declaran desde el
 * principio los dos esquemas de autenticacion que aparecen a lo largo del curso:
 *
 *   basic       usuario y contrasena (Actuator protegido, modulo 5 ejercicio 9)
 *   bearer-jwt  token emitido por infra/auth-service (modulo 3)
 *
 * Declararlos aqui NO protege nada: solo hace que Swagger UI muestre el boton
 * "Authorize" y sepa como enviar las credenciales. Que operaciones las EXIGEN
 * lo deciden SecurityConfiguration y, en la documentacion, @SecurityRequirement
 * (modulo 2, ejercicio 14).
 */
@Configuration
public class OpenApi30Config {

  public static final String BASIC = "basic";

  public static final String BEARER_JWT = "bearer-jwt";

  private final String moduleName;
  private final String apiVersion;

  public OpenApi30Config(
      @Value("${spring.application.name:monolith}") String moduleName,
      @Value("${api.version:1.0.0}") String apiVersion) {
    this.moduleName = moduleName;
    this.apiVersion = apiVersion;
  }

  @Bean
  public OpenAPI customOpenAPI() {
    final String apiTitle = String.format("%s API", StringUtils.capitalize(moduleName));
    return new OpenAPI()
        .info(new Info().title(apiTitle).version(apiVersion)
            .description("API del curso Spring Boot avanzado. Usuarios de prueba: "
                + "admin1/4dm1n, owner1/0wn3r, vet1/v3t. Para un JWT: POST /api/v1/auth/login "
                + "en http://localhost:8060 (auth-service)."))
        .components(new Components()
            .addSecuritySchemes(BASIC, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("basic"))
            .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
  }
}
