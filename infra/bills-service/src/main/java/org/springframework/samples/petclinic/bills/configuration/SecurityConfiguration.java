package org.springframework.samples.petclinic.bills.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * MODULO 3 y 5 - Seguridad de un servicio de apoyo.
 *
 * Este servicio existe para demostrar configuracion distribuida, no seguridad,
 * asi que su API es publica A PROPOSITO y esta dicho aqui de forma explicita.
 *
 * Lo que SI conviene ensenar de esta clase:
 *   - Es una API sin estado: no hay sesion, luego no hay cookie de sesion que
 *     robar, luego CSRF no aplica. Desactivar CSRF solo es correcto cuando se
 *     cumplen esas tres cosas. En una aplicacion con formularios y sesion,
 *     desactivarlo es un fallo de seguridad.
 *   - El proyecto anterior declaraba aqui un NoOpPasswordEncoder (contrasenas
 *     en claro) y un filtro JWT comentado. Un codificador que no codifica no
 *     debe existir ni en una demo: si hace falta autenticar, se usa el
 *     auth-service, que emite tokens de verdad.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable())
			.build();
	}
}
