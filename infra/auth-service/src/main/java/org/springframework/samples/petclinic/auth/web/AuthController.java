package org.springframework.samples.petclinic.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.samples.petclinic.auth.security.JwtTokenService;
import org.springframework.samples.petclinic.auth.web.AuthDtos.LoginRequest;
import org.springframework.samples.petclinic.auth.web.AuthDtos.TokenResponse;
import org.springframework.samples.petclinic.auth.web.AuthDtos.ValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * MODULO 3 - API de autenticacion.
 *
 * Flujo completo que se demuestra en clase:
 *
 *   1. POST /api/v1/auth/login     usuario y contrasena  -> JWT
 *   2. El cliente guarda el token y lo envia en cada peticion:
 *        Authorization: Bearer eyJhbGciOi...
 *   3. POST /api/v1/auth/validate  comprueba firma y caducidad
 *
 * Probar con:
 *   TOKEN=$(curl -s -X POST localhost:8060/api/v1/auth/login \
 *     -H 'Content-Type: application/json' \
 *     -d '{"username":"admin1","password":"4dm1n"}' | jq -r .token)
 *   curl -s -X POST localhost:8060/api/v1/auth/validate -H "Authorization: Bearer $TOKEN"
 *
 * Y despues pegar el token en https://jwt.io para ver las tres partes:
 * cabecera, cuerpo y firma.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Emision y validacion de JSON Web Tokens")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtTokenService tokenService;
	private final long validitySeconds;

	public AuthController(AuthenticationManager authenticationManager,
			UserDetailsService userDetailsService,
			JwtTokenService tokenService,
			@Value("${jwt.validity-seconds:3600}") long validitySeconds) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.tokenService = tokenService;
		this.validitySeconds = validitySeconds;
	}

	@PostMapping("/login")
	@Operation(summary = "Valida las credenciales y devuelve un JWT firmado")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Autenticacion correcta"),
		@ApiResponse(responseCode = "401", description = "Credenciales no validas")
	})
	public TokenResponse login(@Valid @RequestBody LoginRequest peticion) {
		// authenticate() lanza AuthenticationException si las credenciales no
		// son correctas. No hay que comprobar la contrasena a mano en ningun
		// momento: de eso se encarga el PasswordEncoder configurado.
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(peticion.username(), peticion.password()));

		UserDetails userDetails = userDetailsService.loadUserByUsername(peticion.username());
		return new TokenResponse(
			tokenService.generateToken(userDetails),
			tokenService.algorithm(),
			validitySeconds);
	}

	@PostMapping("/validate")
	@Operation(summary = "Comprueba la firma y la caducidad de un token")
	public ValidationResponse validate(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		String token = extraerToken(authorization);
		if (token == null || !tokenService.isTokenValid(token)) {
			return new ValidationResponse(false, null, java.util.List.of());
		}
		return new ValidationResponse(true,
			tokenService.getUsernameFromToken(token),
			tokenService.getAuthoritiesFromToken(token));
	}

	/**
	 * La cabecera llega como "Bearer <token>". Se quita el prefijo y se
	 * devuelve solo el token.
	 */
	private String extraerToken(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return null;
		}
		return authorization.substring("Bearer ".length()).trim();
	}

	/**
	 * Unifica la respuesta ante credenciales incorrectas. Se responde 401 con
	 * un mensaje generico: distinguir "usuario inexistente" de "contrasena
	 * incorrecta" facilita enumerar cuentas validas.
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail credencialesNoValidas(AuthenticationException ex) {
		ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problema.setTitle("Credenciales no validas");
		problema.setDetail("El usuario o la contrasena no son correctos");
		return problema;
	}
}
