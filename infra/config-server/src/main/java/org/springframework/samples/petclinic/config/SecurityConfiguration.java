package org.springframework.samples.petclinic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * MODULO 5 - Ejercicio 14: consumo seguro del servidor de configuracion.
 *
 * Hay dos cadenas de filtros y solo se activa una, segun el perfil:
 *
 *   - Sin el perfil `secure` (lo normal en clase): todo abierto, igual que
 *     antes de anadir Spring Security. Los ejercicios 1 a 13 no cambian.
 *
 *   - Con el perfil `secure`: Basic obligatorio y dos papeles distintos.
 *       CONFIG_CLIENT -> leer configuracion (/{aplicacion}/{perfil}...)
 *       CONFIG_ADMIN  -> /encrypt, /decrypt y Actuator, salvo health e info
 *     La separacion importa: si un cliente pudiera llamar a /decrypt, cualquier
 *     servicio comprometido descifraria los secretos de todos los demas.
 *
 * CSRF se desactiva en los dos casos, y es correcto: es una API sin estado,
 * sin sesion ni formularios, que se consume con Basic desde otros servicios.
 */
@Configuration
public class SecurityConfiguration {

	@Bean
	@Profile("!secure")
	SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.csrf(csrf -> csrf.disable())
			.build();
	}

	@Bean
	@Profile("secure")
	SecurityFilterChain secureFilterChain(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(auth -> auth
				// Docker y Eureka comprueban la salud sin credenciales
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers("/encrypt/**", "/decrypt/**", "/actuator/**").hasRole("CONFIG_ADMIN")
				.anyRequest().hasRole("CONFIG_CLIENT"))
			.httpBasic(Customizer.withDefaults())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable())
			.build();
	}

	/**
	 * Las claves llegan por variables de entorno (ver application-secure.properties)
	 * y se guardan ya codificadas con BCrypt: en memoria no queda ninguna en claro.
	 */
	@Bean
	@Profile("secure")
	UserDetailsService configUsers(
			@Value("${config.security.client.username}") String clientUser,
			@Value("${config.security.client.password}") String clientPassword,
			@Value("${config.security.admin.username}") String adminUser,
			@Value("${config.security.admin.password}") String adminPassword) {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		return new InMemoryUserDetailsManager(
			User.withUsername(clientUser).password(encoder.encode(clientPassword))
				.roles("CONFIG_CLIENT").build(),
			// El administrador tambien puede leer configuracion: le hace falta
			// para comprobar lo que se sirve despues de cifrar un valor.
			User.withUsername(adminUser).password(encoder.encode(adminPassword))
				.roles("CONFIG_ADMIN", "CONFIG_CLIENT").build());
	}
}
