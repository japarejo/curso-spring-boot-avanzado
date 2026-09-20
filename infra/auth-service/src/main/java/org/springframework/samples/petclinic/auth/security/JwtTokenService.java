package org.springframework.samples.petclinic.auth.security;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * MODULO 3 - Emision y validacion de JSON Web Tokens.
 *
 * Se declara como interfaz con DOS implementaciones seleccionables por perfil:
 *
 *   perfil `hs512` (por defecto) -> {@link HmacJwtTokenService}
 *        Firma simetrica: el mismo secreto firma y verifica. Sencillo, pero
 *        obliga a compartir el secreto con todos los servicios que validen.
 *
 *   perfil `rsa`                 -> {@link RsaJwtTokenService}
 *        Firma asimetrica: la clave privada firma, la publica verifica. Los
 *        servicios de recursos solo necesitan la publica, asi que nadie mas
 *        puede emitir tokens. Es lo que usan los proveedores de identidad.
 *
 * En el proyecto anterior esto eran dos modulos Maven completos, duplicados
 * byte a byte salvo esta clase, y ademas compartiendo el mismo paquete Java.
 */
public interface JwtTokenService {

	/** Emite un token firmado para el usuario indicado. */
	String generateToken(UserDetails userDetails);

	/** Extrae el sujeto (nombre de usuario) del token. Lanza excepcion si no es valido. */
	String getUsernameFromToken(String token);

	/** Extrae las autoridades incluidas como claim. */
	List<String> getAuthoritiesFromToken(String token);

	/** true si la firma es correcta, el token no ha caducado y el sujeto coincide. */
	boolean validateToken(String token, UserDetails userDetails);

	/** true si la firma es correcta y el token no ha caducado. */
	boolean isTokenValid(String token);

	/** Algoritmo en uso, para poder mostrarlo en clase. */
	String algorithm();
}
