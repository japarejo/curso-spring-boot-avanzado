// =============================================================================
// Modulo 3 - Ejercicio 13: el filtro JWT escrito a mano (diapositivas 87-93),
// en Spring Security 6 y jjwt 0.12.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
//
// apps/petclinic-api/pom.xml (las versiones las fija el pom.xml raiz):
//   <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId></dependency>
//   <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><scope>runtime</scope></dependency>
//   <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><scope>runtime</scope></dependency>
//
// application.properties de petclinic-api (el MISMO secreto que auth-service):
//   jwt.secret=${JWT_SECRET:mmneWq36D763GkpMMOIBmxQ7JU5KXMu88JaZGSIRoGR0cZQux9JugEW4QQWj4UhJfjKsBNtE73LvJL3Z7yA+bQ==}
//   jwt.issuer=auth-service
// =============================================================================

// ---------- configuration/JwtAuthenticationFilter.java ----------
package org.springframework.samples.petclinic.configuration;

import java.io.IOException;
import java.util.List;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * NO se anota con @Component, a diferencia de la diapositiva 92. Si fuera un
 * bean, Spring Boot lo registraria ADEMAS como filtro de servlet normal, y se
 * ejecutaria dos veces: una dentro de la cadena de seguridad y otra fuera. Se
 * crea con `new` en la configuracion de la cadena.
 *
 * Diferencias con la diapositiva 92:
 *   - no se llama a loadUserByUsername en cada peticion: las autoridades viajan
 *     en el token, firmadas. Ir a la base de datos anula la ventaja de JWT;
 *   - la caducidad la comprueba jjwt al verificar la firma (no a mano);
 *   - se exige el emisor esperado (requireIssuer);
 *   - los fallos se registran con el logger, no con System.out.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private static final String PREFIJO = "Bearer ";

	private final JwtParser parser;

	public JwtAuthenticationFilter(String secretoBase64, String emisor) {
		SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
		this.parser = Jwts.parser().verifyWith(clave).requireIssuer(emisor).build();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (cabecera != null && cabecera.startsWith(PREFIJO)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = cabecera.substring(PREFIJO.length()).trim();
			try {
				Claims claims = parser.parseSignedClaims(token).getPayload();
				List<SimpleGrantedAuthority> autoridades = autoridades(claims);

				UsernamePasswordAuthenticationToken autenticacion =
						UsernamePasswordAuthenticationToken.authenticated(claims.getSubject(), null, autoridades);
				autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(autenticacion);
			}
			catch (JwtException | IllegalArgumentException ex) {
				// Token manipulado, caducado o de otro emisor: NO se autentica.
				// La cadena sigue y la regla authenticated() respondera 401.
				log.debug("JWT rechazado: {}", ex.getMessage());
			}
		}

		chain.doFilter(request, response);
	}

	private List<SimpleGrantedAuthority> autoridades(Claims claims) {
		Object valor = claims.get("authorities");
		if (!(valor instanceof List<?> lista)) {
			return List.of();
		}
		return lista.stream().map(String::valueOf).map(SimpleGrantedAuthority::new).toList();
	}

}

// ---------- configuration/ApiJwtSecurityConfiguration.java ----------
/*
package org.springframework.samples.petclinic.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ApiJwtSecurityConfiguration {

	// @Order(1): esta cadena se evalua ANTES que la de SecurityConfiguration, y
	// gracias a securityMatcher solo se queda con /api/**.
	@Bean
	@Order(1)
	SecurityFilterChain api(HttpSecurity http,
			@Value("${jwt.secret}") String secreto,
			@Value("${jwt.issuer:auth-service}") String emisor) throws Exception {
		return http
			.securityMatcher("/api/**")
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/**").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/**").hasAuthority("admin")
				.anyRequest().authenticated())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable())
			.addFilterBefore(new JwtAuthenticationFilter(secreto, emisor), UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				response.setContentType("application/problem+json");
				response.getWriter().write("{\"title\":\"No autenticado\",\"status\":401}");
			}))
			.build();
	}
}
*/

// Y en SecurityConfiguration, quitar la linea `.requestMatchers("/api/**").permitAll()`,
// que ya no se alcanza (esta cadena se queda antes con todo /api/**).
//
// Comparacion con el ejercicio 6 (oauth2ResourceServer + NimbusJwtDecoder):
//
//   Comprobacion                 Filtro a mano          Resource server
//   ---------------------------  ---------------------  --------------------------
//   Firma                        si (jjwt)              si (Nimbus)
//   Caducidad (exp)              si (jjwt)              si
//   No antes de (nbf)            si (jjwt)              si
//   Emisor (iss)                 si, porque se pidio    solo si se configura
//   Algoritmo esperado           implicito en la clave  configurable
//   Respuesta 401 estandar       hay que escribirla     WWW-Authenticate: Bearer ...
//   Lineas propias               ~80                    ~20
//
// Conclusion para clase: escribir el filtro una vez ensena COMO funciona; en un
// proyecto real se usa oauth2ResourceServer().
