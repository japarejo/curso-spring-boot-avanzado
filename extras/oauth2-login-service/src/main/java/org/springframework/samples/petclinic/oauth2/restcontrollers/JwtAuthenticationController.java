package org.springframework.samples.petclinic.oauth2.restcontrollers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.oauth2.service.InternalUserDetailsService;
import org.springframework.samples.petclinic.oauth2.util.JwtRequest;
import org.springframework.samples.petclinic.oauth2.util.JwtResponse;
import org.springframework.samples.petclinic.oauth2.util.JwtTokenRequest;
import org.springframework.samples.petclinic.oauth2.util.JwtTokenUtil;
import org.springframework.samples.petclinic.oauth2.util.JwtValidationResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@Tag(name = "Authentication", description = "Local and OAuth2 authentication endpoints")
public class JwtAuthenticationController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenUtil jwtTokenUtil;
	private final InternalUserDetailsService userDetailsService;

	public JwtAuthenticationController(
			AuthenticationManager authenticationManager,
			JwtTokenUtil jwtTokenUtil,
			InternalUserDetailsService userDetailsService) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenUtil = jwtTokenUtil;
		this.userDetailsService = userDetailsService;
	}

	@Operation(summary = "Authenticate with local credentials", description = "Validates local credentials and returns a JWT token on success.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Authentication succeeded",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))),
			@ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
	})
	@RequestMapping(value = "/authenticate", method = RequestMethod.POST)
	public ResponseEntity<JwtResponse> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest)
			throws Exception {
		Authentication authentication = authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
		return ResponseEntity.ok(new JwtResponse(jwtTokenUtil.generateToken(authentication)));
	}

	@Operation(summary = "OAuth2 login entry points", description = "Start OAuth2 login with /oauth2/authorization/google, /oauth2/authorization/github or /oauth2/authorization/azure.")
	@GetMapping("/oauth2/providers")
	public String oauth2Providers() {
		return "OAuth2 login providers: /oauth2/authorization/google, /oauth2/authorization/github, /oauth2/authorization/azure";
	}

	@Operation(summary = "OAuth2 login success", description = "Receives the JWT generated after a successful OAuth2 login.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OAuth2 authentication succeeded",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class)))
	})
	@GetMapping("/oauth2/success")
	public ResponseEntity<JwtResponse> oauth2Success(@RequestParam String token) {
		return ResponseEntity.ok(new JwtResponse(token));
	}

	@Operation(summary = "Validate JWT token", description = "Validates token signature, expiration and internal user existence.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Token validation result",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtValidationResponse.class)))
	})
	@RequestMapping(value = "/validate", method = RequestMethod.POST)
	public ResponseEntity<JwtValidationResponse> validateToken(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody(required = false) JwtTokenRequest tokenRequest) {
		String token = resolveToken(authorizationHeader, tokenRequest);
		return ResponseEntity.ok(new JwtValidationResponse(isValidToken(token)));
	}

	@Operation(summary = "Refresh JWT token", description = "Validates an existing JWT token and returns a renewed one.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Token renewed",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))),
			@ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
	})
	@RequestMapping(value = "/refresh", method = RequestMethod.POST)
	public ResponseEntity<JwtResponse> refreshToken(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody(required = false) JwtTokenRequest tokenRequest) {
		String token = resolveToken(authorizationHeader, tokenRequest);
		if (!isValidToken(token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		UserDetails userDetails = userDetailsService.loadUserByUsername(jwtTokenUtil.getUsernameFromToken(token));
		return ResponseEntity.ok(new JwtResponse(jwtTokenUtil.generateToken(userDetails)));
	}

	private Authentication authenticate(String username, String password) throws Exception {
		try {
			return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (DisabledException e) {
			throw new Exception("USER_DISABLED", e);
		} catch (BadCredentialsException e) {
			throw new Exception("INVALID_CREDENTIALS", e);
		}
	}

	private boolean isValidToken(String token) {
		if (token == null || token.isBlank() || !jwtTokenUtil.validateTokenSignatureAndExpiration(token)) {
			return false;
		}

		try {
			UserDetails userDetails = userDetailsService.loadUserByUsername(jwtTokenUtil.getUsernameFromToken(token));
			return jwtTokenUtil.validateToken(token, userDetails);
		} catch (UsernameNotFoundException | IllegalArgumentException e) {
			return false;
		}
	}

	private String resolveToken(String authorizationHeader, JwtTokenRequest tokenRequest) {
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			return authorizationHeader.substring(7);
		}
		return tokenRequest != null ? tokenRequest.getToken() : null;
	}
}
