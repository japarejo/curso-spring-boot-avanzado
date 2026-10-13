// =============================================================================
// Modulo 3 - Ejercicios 8 y 10: seguridad de la practica "tareas".
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Copiar a practicas/tareas/src/main/java/com/idexaformacion/cursos/tareas/
// SeguridadConfig.java, con spring-boot-starter-security ya descomentado.
//
// EJERCICIO 8 no lleva codigo: se trata de NO escribir esta clase y observar.
// Respuestas de los curl del paso 3, sin ninguna configuracion:
//   GET  sin credenciales               -> 401 + WWW-Authenticate: Basic
//   GET  con -u user:<generada>         -> 200
//   POST con -u user:<generada>         -> 403 (CSRF activo por defecto)
//   GET  /actuator/health sin credenc.  -> 401 (Actuator tambien queda protegido)
//
// En cuanto existe un bean SecurityFilterChain propio (esta clase), la
// configuracion por defecto de Spring Boot se retira ENTERA: ya no hay usuario
// "user" ni contrasena generada, ni reglas implicitas. Por eso hay que declarar
// todo lo que se quiera conservar.
// =============================================================================

// ---------- application.properties (ejercicio 10) ----------
//   spring.sql.init.schema-locations=classpath:org/springframework/security/core/userdetails/jdbc/users.ddl
//
// El DDL oficial crea:
//   users(username varchar_ignorecase(50) PK, password varchar_ignorecase(500), enabled boolean)
//   authorities(username, authority) + indice unico (username, authority)
// Ojo: varchar_ignorecase es de H2. Para MySQL o PostgreSQL hay que adaptarlo.

package com.idexaformacion.cursos.tareas;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// Las dos anotaciones de OpenAPI hacen que Swagger UI muestre el boton Authorize con
// usuario y contrasena (esquema "basic") y lo aplique a todas las operaciones. Asi se
// prueba con ana y con jefe sin salir del navegador.
@Configuration
@SecurityScheme(name = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
@OpenAPIDefinition(security = @SecurityRequirement(name = "basic"))
public class SeguridadConfig {

	@Bean
	SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health").permitAll()
				// Swagger UI y el documento OpenAPI: publicos, para poder probar el resto.
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/tareas/**").hasRole("USER")
				.requestMatchers("/api/v1/tareas/**").hasRole("ADMIN")
				.anyRequest().denyAll())
			// API sin estado y probada con curl: Basic y sin CSRF. Si la usara un
			// navegador con sesion, el CSRF tendria que volver.
			.httpBasic(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/**
	 * Con el esquema canonico, JdbcUserDetailsManager funciona SIN declarar
	 * consultas: las que trae por defecto son exactamente para esas tablas.
	 * petclinic-api tiene que declararlas porque su tabla authorities tiene
	 * columnas de mas (id, version) y la crea Hibernate, no el DDL oficial.
	 */
	@Bean
	JdbcUserDetailsManager usuarios(DataSource dataSource) {
		return new JdbcUserDetailsManager(dataSource);
	}

	@Bean
	CommandLineRunner crearUsuarios(JdbcUserDetailsManager usuarios, PasswordEncoder encoder) {
		return args -> {
			if (!usuarios.userExists("ana")) {
				usuarios.createUser(User.withUsername("ana")
					.password(encoder.encode("ana"))
					.roles("USER")               // se guarda como ROLE_USER
					.build());
			}
			if (!usuarios.userExists("jefe")) {
				usuarios.createUser(User.withUsername("jefe")
					.password(encoder.encode("jefe"))
					.roles("USER", "ADMIN")
					.build());
			}
		};
	}

}

// Comprobacion en Swagger UI (http://localhost:8080/swagger-ui.html):
//   Authorize -> basic -> ana / ana    GET /api/v1/tareas -> 200 ; DELETE -> 403
//   Logout, Authorize -> jefe / jefe   DELETE /api/v1/tareas/{id} -> 204
//
// Con curl:
//   curl -i -u ana:ana   localhost:8080/api/v1/tareas                         # 200
//   curl -i -u ana:ana   -X DELETE localhost:8080/api/v1/tareas/1             # 403
//   curl -i -u jefe:jefe -X DELETE localhost:8080/api/v1/tareas/1             # 204
//   curl -i               localhost:8080/actuator/health                      # 200
//   curl -i -u jefe:jefe  localhost:8080/actuator/metrics                     # 403 (denyAll)
//
// Para ver las tablas en /h2-console (paso 5) hay que abrirla en la cadena,
// igual que hace petclinic-api. Anadir temporalmente, antes del denyAll():
//   .requestMatchers("/h2-console/**").permitAll()
// y despues de httpBasic(...):
//   .headers(h -> h.frameOptions(f -> f.sameOrigin()))
// (el CSRF ya esta desactivado en esta cadena; si no, habria que ignorar
// /h2-console/** como hace petclinic-api).
