package org.springframework.samples.petclinic.configuration;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.DispatcherType;

/**
 * MODULO 3 - Configuracion de seguridad.
 *
 * Estilo Spring Security 6: no se hereda de WebSecurityConfigurerAdapter
 * (desaparecio en la version 6), se publican beans. Todo el DSL es de lambdas,
 * que es lo unico que quedara en Spring Security 7.
 *
 * Tres piezas independientes:
 *   1. SecurityFilterChain  -> que se protege y como se entra
 *   2. UserDetailsManager   -> de donde salen los usuarios
 *   3. PasswordEncoder      -> como se comparan las contrasenas
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	// 1. AUTORIZACION, LOGIN, CSRF Y CABECERAS ---------------------------------
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
			.authorizeHttpRequests(auth -> auth
				// FORWARD y ERROR son despachos internos del contenedor (por ejemplo,
				// al renderizar una JSP). Sin esto, cada forward se evaluaria como si
				// fuera una peticion nueva y acabaria en denyAll.
				.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

				// Recursos publicos
				.requestMatchers("/", "/welcome", "/error", "/resources/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/oups").permitAll()
				.requestMatchers("/h2-console/**").permitAll()

				// Documentacion y diagnostico
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
				.requestMatchers("/actuator/**", "/logging").permitAll()

				// Reglas de negocio por rol
				.requestMatchers("/admin/**").hasAuthority("admin")
				.requestMatchers("/owners/**").hasAnyAuthority("owner", "admin")
				.requestMatchers("/payments/**").authenticated()

				// La API REST se securiza en el modulo 3 (ejercicio de JWT). De momento
				// queda abierta para poder explorarla con curl y con Swagger UI durante
				// los modulos 1 y 2.
				.requestMatchers("/api/**").permitAll()

				// Regla final restrictiva: lo que no se ha permitido explicitamente,
				// se deniega. Preferible a anyRequest().authenticated(), que deja pasar
				// cualquier ruta nueva a cualquier usuario logueado.
				.anyRequest().denyAll())

			// Spring genera la pagina de login. Es deliberado: el temario no evalua
			// la vista de login, y asi no hay una JSP mas que mantener.
			.formLogin(form -> form
				.defaultSuccessUrl("/welcome")
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/"))

			// CSRF protege formularios con sesion. Se desactiva solo donde no aplica:
			// consola H2, actuator y la API sin estado.
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**", "/actuator/**", "/api/**"))

			// La consola de H2 se pinta dentro de un frame del mismo origen.
			.headers(headers -> headers
				.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}

	// 2. ORIGEN DE LOS USUARIOS ------------------------------------------------
	@Bean
	UserDetailsManager users(DataSource dataSource) {
		JdbcUserDetailsManager mgr = new JdbcUserDetailsManager(dataSource);
		mgr.setUsersByUsernameQuery(
			"select username, password, enabled from users where username = ?");
		mgr.setAuthoritiesByUsernameQuery(
			"select username, authority from authorities where username = ?");
		return mgr;
	}

	// 3. CIFRADO DE CONTRASENAS ------------------------------------------------
	@Bean
	PasswordEncoder passwordEncoder() {
		// Codificador delegado: elige el algoritmo segun el prefijo almacenado
		// en la propia contrasena ({bcrypt}..., {noop}..., {pbkdf2}...).
		// Ventaja sobre `new BCryptPasswordEncoder()`: permite migrar de algoritmo
		// sin invalidar las contrasenas ya guardadas.
		// Ver los INSERT de src/main/resources/data.sql.
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
