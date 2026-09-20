package org.springframework.samples.petclinic.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MODULO 3 - Servicio de autenticacion basado en JWT.
 *
 * Emite y valida JSON Web Tokens. Es el resultado de fusionar los dos modulos
 * del proyecto anterior (security-microservice y security-rsa-microservice),
 * que eran identicos byte a byte salvo la clase que firmaba, y que ademas
 * compartian el mismo paquete Java.
 *
 * Ahora la diferencia es un perfil:
 *   ./mvnw -pl infra/auth-service spring-boot:run                              (HS512)
 *   ./mvnw -pl infra/auth-service spring-boot:run -Dspring-boot.run.profiles=rsa
 */
@SpringBootApplication
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
