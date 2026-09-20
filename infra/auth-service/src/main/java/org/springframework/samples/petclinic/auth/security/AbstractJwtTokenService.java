package org.springframework.samples.petclinic.auth.security;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/**
 * Parte comun a las dos estrategias de firma. Lo unico que cambia entre HS512 y
 * RS256 es con que clave se firma y con cual se verifica; la construccion del
 * token y la lectura de los claims son identicas.
 *
 * API de jjwt 0.12, que es la que aparece en el material del curso. Ojo al
 * migrar desde 0.9.x: alli se escribia
 *     Jwts.parser().setSigningKey(secreto).parseClaimsJws(token)
 *     .signWith(SignatureAlgorithm.HS512, secreto)
 * y ese API ya no existe.
 */
public abstract class AbstractJwtTokenService implements JwtTokenService {

	private static final Logger log = LoggerFactory.getLogger(AbstractJwtTokenService.class);

	/** Nombre del claim donde viajan las autoridades del usuario. */
	protected static final String CLAIM_AUTHORITIES = "authorities";

	private final String issuer;
	private final long validitySeconds;

	protected AbstractJwtTokenService(String issuer, long validitySeconds) {
		this.issuer = issuer;
		this.validitySeconds = validitySeconds;
	}

	/**
	 * Aplica la firma al constructor de tokens. Cada estrategia sabe que clave
	 * y que algoritmo usar.
	 */
	protected abstract JwtBuilder sign(JwtBuilder builder);

	/** Parser configurado con la clave de verificacion correspondiente. */
	protected abstract JwtParser parser();

	@Override
	public String generateToken(UserDetails userDetails) {
		Instant ahora = Instant.now();
		List<String> authorities = userDetails.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.toList();

		JwtBuilder builder = Jwts.builder()
			.claims(Map.of(CLAIM_AUTHORITIES, authorities))
			.subject(userDetails.getUsername())
			.issuer(issuer)
			.issuedAt(Date.from(ahora))
			.expiration(Date.from(ahora.plusSeconds(validitySeconds)));

		return sign(builder).compact();
	}

	@Override
	public String getUsernameFromToken(String token) {
		return claims(token).getSubject();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> getAuthoritiesFromToken(String token) {
		Object valor = claims(token).get(CLAIM_AUTHORITIES);
		return valor instanceof List<?> lista ? (List<String>) lista : List.of();
	}

	@Override
	public boolean validateToken(String token, UserDetails userDetails) {
		try {
			// Comprobar el sujeto NO es redundante con verificar la firma: un
			// token valido emitido para OTRO usuario tiene la firma correcta,
			// pero no autoriza a este.
			return getUsernameFromToken(token).equals(userDetails.getUsername());
		}
		catch (JwtException | IllegalArgumentException ex) {
			log.debug("Token rechazado: {}", ex.getMessage());
			return false;
		}
	}

	@Override
	public boolean isTokenValid(String token) {
		try {
			claims(token);
			return true;
		}
		catch (JwtException | IllegalArgumentException ex) {
			log.debug("Token rechazado: {}", ex.getMessage());
			return false;
		}
	}

	/**
	 * Lee los claims verificando la firma. Si el token esta manipulado o ha
	 * caducado, jjwt lanza excepcion aqui mismo: no hay que comprobar la
	 * caducidad a mano como hacia el codigo anterior.
	 */
	protected Claims claims(String token) {
		return parser().parseSignedClaims(token).getPayload();
	}
}
