package org.springframework.samples.petclinic.roles.configuration;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso del modulo.
 *
 * <p>Fijate en que aqui casi NO aparecen privilegios: las reglas de grano fino
 * estan con {@code @PreAuthorize} en {@code ClinicaService}. En este fichero
 * solo hay dos cosas: una regla gruesa por ruta ({@code /api/admin/**} solo
 * para administradores) y la JERARQUIA DE ROLES.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain api(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers(PathRequest.toH2Console()).permitAll()
						// Regla por ROL en la cadena de filtros. Gracias al bean
						// RoleHierarchy de abajo, tambien se aplica la jerarquia
						// aqui: no hace falta escribir hasAnyRole("ADMIN", ...).
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults())
				// API sin sesion y autenticada en cada peticion con Basic: no
				// hay cookie que robar, asi que CSRF no aporta nada (modulo 3).
				// Efecto secundario muy util en clase: como el usuario se carga
				// en CADA peticion, un cambio de privilegios en BD se nota al
				// instante (ejercicio 4 del guion).
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}

	/**
	 * JERARQUIA DE ROLES. Se lee "A implica B": quien tiene A se trata como si
	 * tambien tuviera B.
	 *
	 * <pre>
	 *              ROLE_ADMIN
	 *             /          \
	 *   ROLE_RECEPCION    ROLE_VETERINARIO
	 *             \          /
	 *            ROLE_EMPLEADO
	 * </pre>
	 *
	 * <p>Es un grafo, no un arbol: ADMIN llega a EMPLEADO por dos caminos y no
	 * pasa nada. Equivale a la forma en texto que usa el articulo de Baeldung:
	 *
	 * <pre>
	 * RoleHierarchyImpl.fromHierarchy("""
	 *     ROLE_ADMIN > ROLE_RECEPCION
	 *     ROLE_ADMIN > ROLE_VETERINARIO
	 *     ROLE_RECEPCION > ROLE_EMPLEADO
	 *     ROLE_VETERINARIO > ROLE_EMPLEADO
	 *     """);
	 * </pre>
	 *
	 * <p>Es {@code static} porque la documentacion de Spring Security lo pide
	 * para los beans que usa la infraestructura de seguridad por metodo: asi se
	 * crean sin tener que instanciar antes esta clase de configuracion.
	 *
	 * <p><b>Importante:</b> la jerarquia NO cambia las autoridades guardadas en
	 * el {@code Authentication}. Se aplica en el momento de DECIDIR, cada vez
	 * que se evalua un {@code hasRole}/{@code hasAuthority}.
	 */
	@Bean
	static RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.withDefaultRolePrefix()
				.role("ADMIN").implies("RECEPCION", "VETERINARIO")
				.role("RECEPCION").implies("EMPLEADO")
				.role("VETERINARIO").implies("EMPLEADO")
				.build();
	}

	/**
	 * Para que {@code @PreAuthorize} tambien respete la jerarquia. Las reglas
	 * por URL la recogen solas al encontrar el bean {@link RoleHierarchy}; para
	 * la seguridad por metodo la documentacion recomienda declararlo asi.
	 */
	@Bean
	static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
		DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
		handler.setRoleHierarchy(roleHierarchy);
		return handler;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
