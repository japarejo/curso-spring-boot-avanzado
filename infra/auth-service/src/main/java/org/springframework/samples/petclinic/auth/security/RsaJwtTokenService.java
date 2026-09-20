package org.springframework.samples.petclinic.auth.security;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/**
 * MODULO 3 - Firma ASIMETRICA con RSA (RS256).
 *
 * La clave PRIVADA firma; la PUBLICA solo verifica. Ventaja decisiva frente a
 * HMAC: los servicios de recursos pueden validar tokens sin poder emitirlos.
 * Es el modelo de OAuth 2 y OpenID Connect, donde el proveedor de identidad
 * publica su clave publica en un punto JWKS y cada servicio se la descarga.
 *
 * Las claves de este repositorio son DE DEMOSTRACION y estan versionadas a
 * proposito para que el curso funcione recien clonado. Viven en
 * application-rsa.properties, con su aviso correspondiente. En un sistema real
 * la clave privada nunca se versiona: va en un gestor de secretos.
 */
@Component
@Profile("rsa")
public class RsaJwtTokenService extends AbstractJwtTokenService {

	private final PrivateKey clavePrivada;
	private final PublicKey clavePublica;

	public RsaJwtTokenService(
			@Value("${jwt.private-key}") String privateKeyBase64,
			@Value("${jwt.public-key}") String publicKeyBase64,
			@Value("${jwt.issuer:auth-service}") String issuer,
			@Value("${jwt.validity-seconds:3600}") long validitySeconds) {
		super(issuer, validitySeconds);
		this.clavePrivada = leerClavePrivada(privateKeyBase64);
		this.clavePublica = leerClavePublica(publicKeyBase64);
	}

	@Override
	protected JwtBuilder sign(JwtBuilder builder) {
		return builder.signWith(clavePrivada, Jwts.SIG.RS256);
	}

	@Override
	protected JwtParser parser() {
		return Jwts.parser().verifyWith(clavePublica).build();
	}

	@Override
	public String algorithm() {
		return "RS256 (asimetrico)";
	}

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
