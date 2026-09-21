package org.springframework.samples.petclinic.oauth2.util;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.oauth2.model.InternalUser;
import org.springframework.samples.petclinic.oauth2.repository.InternalUserRepository;
import org.springframework.samples.petclinic.oauth2.util.LoginResponse.InternalUserSummary;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LocalJwtSuccessHandler implements AuthenticationSuccessHandler {

	private final InternalUserRepository userRepository;
	private final JwtTokenUtil jwtTokenUtil;
	private final ObjectMapper objectMapper;

	public LocalJwtSuccessHandler(
			InternalUserRepository userRepository,
			JwtTokenUtil jwtTokenUtil,
			ObjectMapper objectMapper) {
		this.userRepository = userRepository;
		this.jwtTokenUtil = jwtTokenUtil;
		this.objectMapper = objectMapper;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		InternalUser internalUser = userRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + authentication.getName()));
		Map<String, Object> tokenClaims = tokenClaims(authentication, internalUser);
		String jwt = jwtTokenUtil.generateToken(authentication, tokenClaims);

		LoginResponse body = new LoginResponse(
				jwt,
				"local",
				internalUserSummary(internalUser),
				tokenClaims,
				providerAttributes(request));

		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), body);
	}

	private Map<String, Object> tokenClaims(Authentication authentication, InternalUser internalUser) {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("auth_provider", "local");
		claims.put("login_method", "form");
		putIfPresent(claims, "email", internalUser.getEmail());
		putIfPresent(claims, "display_name", internalUser.getDisplayName());
		claims.put("authorities",
				authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
		return claims;
	}

	private InternalUserSummary internalUserSummary(InternalUser internalUser) {
		List<String> authorities = internalUser.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.toList();
		return new InternalUserSummary(
				internalUser.getId(),
				internalUser.getUsername(),
				internalUser.getEmail(),
				internalUser.getDisplayName(),
				authorities);
	}

	private Map<String, Object> providerAttributes(HttpServletRequest request) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("username", request.getParameter("username"));
		attributes.put("password", "[PROTECTED]");
		return attributes;
	}

	private void putIfPresent(Map<String, Object> claims, String key, String value) {
		if (value != null && !value.isBlank()) {
			claims.put(key, value);
		}
	}
}
