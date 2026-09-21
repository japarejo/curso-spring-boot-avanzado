package org.springframework.samples.petclinic.oauth2.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Prueba unitaria pura: ni Spring, ni base de datos, ni reflexion.
 *
 * MODULO 1 - La version anterior de esta prueba tenia que hacer
 *
 *     jwtTokenUtil = new JwtTokenUtil();
 *     ReflectionTestUtils.setField(jwtTokenUtil, "privateKey", ...);
 *     ReflectionTestUtils.setField(jwtTokenUtil, "publicKey", ...);
 *
 * porque la clase recibia sus claves con @Value sobre campos privados. Al pasar
 * a inyeccion por constructor, las claves se pasan como lo que son: argumentos.
 * La prueba se lee mejor y deja de depender de los nombres de los campos, que
 * es una dependencia invisible que se rompe en cuanto alguien renombra uno.
 */
class JwtTokenUtilTests {

	private JwtTokenUtil jwtTokenUtil;

	@BeforeEach
	void setUp() throws Exception {
		// Par de claves nuevo en cada prueba: nada que compartir ni que limpiar.
		KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
		generador.initialize(2048);
		KeyPair par = generador.generateKeyPair();

		jwtTokenUtil = new JwtTokenUtil(
			Base64.getEncoder().encodeToString(par.getPrivate().getEncoded()),
			Base64.getEncoder().encodeToString(par.getPublic().getEncoded()));
	}

	@Test
	@DisplayName("Emite un token firmado con RSA que despues valida")
	void emiteYValidaUnTokenFirmadoConRsa() {
		UserDetails usuario = new User("owner1", "0wn3r", List.of(new SimpleGrantedAuthority("owner")));

		String token = jwtTokenUtil.generateToken(usuario);

		assertThat(jwtTokenUtil.validateTokenSignatureAndExpiration(token)).isTrue();
		assertThat(jwtTokenUtil.validateToken(token, usuario)).isTrue();
		assertThat(jwtTokenUtil.getUsernameFromToken(token)).isEqualTo("owner1");
	}

	@Test
	@DisplayName("Las autoridades viajan dentro del token")
	void incluyeLasAutoridades() {
		UserDetails usuario = new User("vet1", "v3t", List.of(new SimpleGrantedAuthority("vet")));

		String token = jwtTokenUtil.generateToken(usuario);

		Object autoridades = jwtTokenUtil.getClaimFromToken(token,
			(Claims claims) -> claims.get("authorities"));
		assertThat(autoridades).asInstanceOf(
			org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
			.containsExactly("vet");
	}

	@Test
	@DisplayName("El proveedor de identidad queda registrado como claim")
	void registraElProveedorDeIdentidad() {
		UserDetails usuario = new User("ana", "x", List.of());

		String tokenLocal = jwtTokenUtil.generateToken(usuario);
		String tokenExterno = jwtTokenUtil.generateToken(usuario, Map.of("auth_provider", "google"));

		// El tipo se declara explicitamente: con una lambda que devuelve Object,
		// la inferencia de AssertJ no sabe que sobrecarga de assertThat elegir.
		Object proveedorLocal = jwtTokenUtil.getClaimFromToken(tokenLocal,
			(Claims claims) -> claims.get("auth_provider"));
		Object proveedorExterno = jwtTokenUtil.getClaimFromToken(tokenExterno,
			(Claims claims) -> claims.get("auth_provider"));

		assertThat(proveedorLocal).isEqualTo("local");
		assertThat(proveedorExterno).isEqualTo("google");
	}

	@Test
	@DisplayName("Un token manipulado se rechaza: la firma deja de cuadrar")
	void rechazaUnTokenManipulado() {
		UserDetails usuario = new User("owner1", "0wn3r", List.of(new SimpleGrantedAuthority("owner")));
		String token = jwtTokenUtil.generateToken(usuario);

		String manipulado = token.substring(0, 25) + "X" + token.substring(26);

		assertThat(jwtTokenUtil.validateTokenSignatureAndExpiration(manipulado)).isFalse();
		assertThat(jwtTokenUtil.validateToken(manipulado, usuario)).isFalse();
	}

	@Test
	@DisplayName("Un token firmado con OTRA clave se rechaza")
	void rechazaUnTokenDeOtroEmisor() throws Exception {
		UserDetails usuario = new User("owner1", "0wn3r", List.of(new SimpleGrantedAuthority("owner")));

		KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
		generador.initialize(2048);
		KeyPair otroPar = generador.generateKeyPair();
		JwtTokenUtil otroEmisor = new JwtTokenUtil(
			Base64.getEncoder().encodeToString(otroPar.getPrivate().getEncoded()),
			Base64.getEncoder().encodeToString(otroPar.getPublic().getEncoded()));

		String tokenAjeno = otroEmisor.generateToken(usuario);

		// Esta es la garantia de la firma asimetrica: sin la clave privada
		// correcta, nadie puede emitir tokens que este servicio acepte.
		assertThat(jwtTokenUtil.validateTokenSignatureAndExpiration(tokenAjeno)).isFalse();
	}
}
