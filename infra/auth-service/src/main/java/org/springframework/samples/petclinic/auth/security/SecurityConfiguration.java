package org.springframework.samples.petclinic.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * MODULO 3 - Seguridad del servicio de autenticacion.
 *
 * Es una API sin estado: no crea sesion, y por eso CSRF no aplica (no hay
 * cookie de sesion que un tercero pueda hacer que el navegador envie sola).
 * Este razonamiento es el que hay que repetir en clase: CSRF no se desactiva
 * "porque molesta", se desactiva cuando no hay sesion.
 *
 * El endpoint de login es publico por definicion; todo lo demas se deniega.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/auth/**").permitAll()
				.requestMatchers("/", "/doc/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
				.anyRequest().denyAll())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable())
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.build();
	}

	/**
	 * Expone el AuthenticationManager como bean para poder inyectarlo en el
	 * controlador. Se construye a partir del UserDetailsService y el
	 * PasswordEncoder, sin heredar de ninguna clase base.
	 */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider::authenticate;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		// El proyecto anterior usaba aqui NoOpPasswordEncoder, es decir,
		// contrasenas en claro en la base de datos. En un curso de seguridad
		// eso no puede aparecer ni como atajo de demostracion.
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
