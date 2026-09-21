package org.springframework.samples.petclinic.oauth2.util;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/**
 * AMPLIACION del modulo 3 - Emision de JWT propios tras un inicio de sesion
 * externo (Google, GitHub, Entra ID...).
 *
 * Firma con RSA porque este servicio actua como proveedor de identidad: emite
 * tokens que consumiran otros servicios, y esos solo deben poder VERIFICAR.
 *
 * Reescrito con la API de jjwt 0.12, que es la del material del curso. La
 * version anterior usaba la 0.9.1:
 *     Jwts.parser().setSigningKey(clave).parseClaimsJws(token).getBody()
 *     .signWith(SignatureAlgorithm.RS256, clave)
 * Ese API desapareci0 en 0.10 y el artefacto monolitico `jjwt` ya no se publica:
 * ahora son tres (jjwt-api, jjwt-impl y jjwt-jackson).
 *
 * Comparar con infra/auth-service, que resuelve lo mismo con una interfaz y dos
 * implementaciones seleccionables por perfil, es un buen ejercicio de diseno.
 */
@Component
public class JwtTokenUtil {

	private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

	/** Cinco horas. Para un token de acceso es generoso; ver el modulo 3. */
	public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;

	private static final String CLAIM_AUTHORITIES = "authorities";

	private final PrivateKey clavePrivada;
	private final PublicKey clavePublica;

	public JwtTokenUtil(
			@Value("${jwt.private-key}") String privateKeyBase64,
			@Value("${jwt.public-key}") String publicKeyBase64) {
		this.clavePrivada = leerClavePrivada(privateKeyBase64);
		this.clavePublica = leerClavePublica(publicKeyBase64);
	}

	// --- Lectura --------------------------------------------------------------

	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public <T> T getClaimFromToken(String token, Function<Claims, T> resolutor) {
		return resolutor.apply(claims(token));
	}

	// --- Emision --------------------------------------------------------------

	public String generateToken(UserDetails userDetails) {
		return generateToken(userDetails, Map.of("auth_provider", "local"));
	}

	public String generateToken(UserDetails userDetails, Map<String, Object> claimsAdicionales) {
		Map<String, Object> claims = new HashMap<>(claimsAdicionales);
		claims.put(CLAIM_AUTHORITIES, userDetails.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.toList());
		return construir(claims, userDetails.getUsername());
	}

	public String generateToken(Authentication authentication) {
		return generateToken(authentication, Map.of("auth_provider", "local"));
	}

	public String generateToken(Authentication authentication, Map<String, Object> claimsAdicionales) {
		Map<String, Object> claims = new HashMap<>(claimsAdicionales);
		claims.put(CLAIM_AUTHORITIES, authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.toList());
		return construir(claims, authentication.getName());
	}

	private String construir(Map<String, Object> claims, String sujeto) {
		Instant ahora = Instant.now();
		return Jwts.builder()
			.claims(claims)
			.subject(sujeto)
			.issuedAt(Date.from(ahora))
			.expiration(Date.from(ahora.plusSeconds(JWT_TOKEN_VALIDITY)))
			.signWith(clavePrivada, Jwts.SIG.RS256)
			.compact();
	}

	// --- Validacion -----------------------------------------------------------

	public Boolean validateToken(String token, UserDetails userDetails) {
		try {
			// Verificar la firma no basta: un token valido emitido para otro
			// usuario esta correctamente firmado y no autoriza a este.
			return getUsernameFromToken(token).equals(userDetails.getUsername());
		}
		catch (JwtException | IllegalArgumentException ex) {
			log.debug("Token rechazado: {}", ex.getMessage());
			return false;
		}
	}

	public boolean validateTokenSignatureAndExpiration(String token) {
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
	 * caducado, jjwt lanza excepcion aqui: no hay que comprobar la caducidad
	 * a mano, como hacia la version anterior.
	 */
	private Claims claims(String token) {
		JwtParser parser = Jwts.parser().verifyWith(clavePublica).build();
		return parser.parseSignedClaims(token).getPayload();
	}

	// --- Carga de claves ------------------------------------------------------

	private static PrivateKey leerClavePrivada(String base64) {
		try {
			byte[] bytes = Base64.getDecoder().decode(limpiar(base64));
			return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
		}
		catch (Exception ex) {
			throw new IllegalStateException("La clave privada RSA (jwt.private-key) no es valida", ex);
		}
	}

	private static PublicKey leerClavePublica(String base64) {
		try {
			byte[] bytes = Base64.getDecoder().decode(limpiar(base64));
			return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
		}
		catch (Exception ex) {
			throw new IllegalStateException("La clave publica RSA (jwt.public-key) no es valida", ex);
		}
	}

	/** Tolera que la clave venga en formato PEM, con cabeceras y saltos de linea. */
	private static String limpiar(String clave) {
		return clave
			.replaceAll("-----BEGIN (.*)-----", "")
			.replaceAll("-----END (.*)-----", "")
			.replaceAll("\\s", "");
	}
}
