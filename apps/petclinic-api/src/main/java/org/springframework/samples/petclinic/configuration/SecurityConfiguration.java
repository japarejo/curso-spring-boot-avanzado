package org.springframework.samples.petclinic.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.samples.petclinic.service.AltaGitHubService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

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

	private static final AuthenticationEntryPoint API_ENTRY_POINT = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

	private static final AuthenticationEntryPoint WEB_ENTRY_POINT = new LoginUrlAuthenticationEntryPoint("/login");

	// 1. AUTORIZACION, LOGIN, CSRF Y CABECERAS ---------------------------------
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http,
			ObjectProvider<ClientRegistrationRepository> registros,
			UserDetailsManager users, AltaGitHubService altas) throws Exception {

		// Login con GitHub (modulo 3, ejercicios 15 y 16). Solo si hay algun
		// proveedor configurado, es decir, arrancando con el perfil "github".
		// Sin este if, oauth2Login() rompe el arranque cuando no hay credenciales.
		// GitHub dice QUIEN eres; GitHubOAuth2UserService decide QUE puedes hacer
		// y, si es la primera vez y esta permitido, te crea la cuenta.
		if (registros.getIfAvailable() != null) {
			http.oauth2Login(oauth -> oauth
				.userInfoEndpoint(info -> info.userService(new GitHubOAuth2UserService(altas, users)))
				.defaultSuccessUrl("/welcome"));
		}

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
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**","/mi-documentacion**").permitAll()
				.requestMatchers("/actuator/**", "/logging").permitAll()

				// Reglas de negocio por rol
				.requestMatchers("/admin/**").hasAuthority("admin")
				.requestMatchers("/owners/**").hasAnyAuthority("owner", "admin")
				.requestMatchers("/payments/**").authenticated()

				// La API REST se securiza en el modulo 3 (ejercicio de JWT). De momento
				// queda abierta para poder explorarla con curl y con Swagger UI durante
				// los modulos 1 y 2.
				.requestMatchers(HttpMethod.DELETE,"/api/pets/*").permitAll()
				.requestMatchers("/api/**").authenticated()
				.requestMatchers("/data-api/**").permitAll()

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
				.logoutSuccessUrl("/login"))
		
			// CSRF protege formularios con sesion. Se desactiva solo donde no aplica:
			// consola H2, actuator y la API sin estado.
			.csrf(csrf -> csrf.disable())

			// Que hacer con una peticion SIN autenticar que la necesita:
			//   - la web redirige al formulario de login (lo de siempre);
			//   - la API responde 401. Sin esto, en cuanto /api/** exige autenticacion
			//     (modulo 3, ejercicio 2), una peticion anonima a la API recibe un 302
			//     hacia /login, y Swagger UI o fetch() lo siguen solos y muestran el
			//     HTML del login como si fuera la respuesta.
			// Un authenticationEntryPoint global desactiva la pagina generada de
			// login. Registrar los puntos de entrada por matcher la conserva.
			.exceptionHandling(ex -> ex
				.defaultAuthenticationEntryPointFor(API_ENTRY_POINT,
					request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/"))
				.defaultAuthenticationEntryPointFor(WEB_ENTRY_POINT, AnyRequestMatcher.INSTANCE))

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
