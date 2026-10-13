// =============================================================================
// Modulo 5 - Ejercicio 9: Spring Boot Admin con Actuator protegido.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
//
// La diapositiva 138 resuelve el acceso de Admin con
//     authorizeRequests.anyRequest().permitAll() ... csrf().disable()
// es decir, quitando TODA la seguridad de la aplicacion. Aqui se protege solo
// /actuator/** con una cadena propia y un usuario tecnico.
// =============================================================================

// ---------- configuration/ActuatorSecurityConfiguration.java ----------
package org.springframework.samples.petclinic.configuration;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ActuatorSecurityConfiguration {

	/**
	 * EndpointRequest es el matcher de Actuator: sigue funcionando aunque se
	 * cambie management.endpoints.web.base-path o el puerto de gestion. Mejor
	 * que escribir "/actuator/**" a mano.
	 *
	 * @Order(0): se evalua antes que la cadena general de SecurityConfiguration.
	 */
	@Bean
	@Order(0)
	SecurityFilterChain actuator(HttpSecurity http) throws Exception {
		return http
			.securityMatcher(EndpointRequest.toAnyEndpoint())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
				.anyRequest().hasAuthority("ACTUATOR"))
			.httpBasic(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Admin hace POST a /actuator/loggers sin token CSRF: en esta cadena
			// sin sesion no aplica.
			.csrf(csrf -> csrf.disable())
			.build();
	}

}

// ---------- SecurityConfiguration (la cadena general) ----------
// Quitar la linea
//     .requestMatchers("/actuator/**", "/logging").permitAll()
// y dejar solo
//     .requestMatchers("/logging").permitAll()
// Ya no se alcanza para /actuator/**: la cadena de arriba se queda antes con esas rutas.

// ---------- data.sql: el usuario tecnico ----------
// La contrasena de ejemplo es "m0n1t0r". Genera tu propio hash con la prueba
// del ejercicio 9 del modulo 3 (Ejercicio09CodificacionContrasenasTests).
//   INSERT INTO users(username,password,enabled) VALUES ('monitor','{bcrypt}<hash>',TRUE);
//   INSERT INTO authorities(id,version,username,authority) VALUES (100,1,'monitor','ACTUATOR');
// Y lo mismo en db/migration (una V3__...) si se usa el perfil mysql.

// ---------- application.properties ----------
//   spring.boot.admin.client.instance.metadata.user.name=monitor
//   spring.boot.admin.client.instance.metadata.user.password=${MONITOR_PASSWORD:m0n1t0r}
//   # Para la pestana Logfile:
//   logging.file.name=target/petclinic.log
//
// Admin lee esos metadatos y los usa como credenciales Basic al consultar los
// endpoints del cliente. Con Eureka en lugar de registro directo, el equivalente es
//   eureka.instance.metadata-map.user.name / user.password
//
// Comprobacion:
//   curl -i localhost:8080/actuator/env                        # 401
//   curl -i -u monitor:m0n1t0r localhost:8080/actuator/env     # 200
//   curl -i localhost:8080/actuator/health                     # 200
//
// Efecto colateral que conviene comentar: la prueba ReglasDeAccesoTests del
// modulo 4 (si se hizo) sigue pasando para health, pero cualquier prueba que
// leyera /actuator/metrics sin credenciales pasara a 401.
