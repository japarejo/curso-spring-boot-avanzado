package org.springframework.samples.petclinic.auth.security;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * MODULO 3 - Firma SIMETRICA con HMAC-SHA512.
 *
 * Una unica clave secreta firma y verifica. Es la opcion mas sencilla y sirve
 * perfectamente cuando quien emite el token es tambien quien lo valida.
 *
 * Su limite conviene decirlo en voz alta: cualquier servicio que necesite
 * VALIDAR tokens necesita el secreto, y con el secreto tambien puede EMITIRLOS.
 * En cuanto hay mas de un servicio implicado, la firma asimetrica (perfil `rsa`)
 * es la respuesta correcta.
 *
 * HS512 exige una clave de al menos 512 bits. jjwt lo comprueba y falla al
 * arrancar si el secreto es mas corto, en lugar de firmar con algo debil.
 */
@Component
@Profile("!rsa")
public class HmacJwtTokenService extends AbstractJwtTokenService {

	private final SecretKey clave;

	public HmacJwtTokenService(
			@Value("${jwt.secret}") String secretoBase64,
			@Value("${jwt.issuer:auth-service}") String issuer,
			@Value("${jwt.validity-seconds:3600}") long validitySeconds) {
		super(issuer, validitySeconds);
		this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
	}

	@Override
	protected JwtBuilder sign(JwtBuilder builder) {
		return builder.signWith(clave, Jwts.SIG.HS512);
	}

	@Override
	protected JwtParser parser() {
		return Jwts.parser().verifyWith(clave).build();
	}

	@Override
	public String algorithm() {
		return "HS512 (simetrico)";
	}
}
