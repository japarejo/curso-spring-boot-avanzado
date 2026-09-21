package org.springframework.samples.petclinic.oauth2.util;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.samples.petclinic.oauth2.model.InternalUser;
import org.springframework.samples.petclinic.oauth2.service.ExternalIdentityService;
import org.springframework.samples.petclinic.oauth2.service.InternalUserDetailsService;
import org.springframework.samples.petclinic.oauth2.util.LoginResponse.InternalUserSummary;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2JwtSuccessHandler implements AuthenticationSuccessHandler {

	private final ExternalIdentityService externalIdentityService;
	private final InternalUserDetailsService userDetailsService;
	private final JwtTokenUtil jwtTokenUtil;
	private final ObjectMapper objectMapper;

	public OAuth2JwtSuccessHandler(
			ExternalIdentityService externalIdentityService,
			InternalUserDetailsService userDetailsService,
			JwtTokenUtil jwtTokenUtil,
			ObjectMapper objectMapper) {
		this.externalIdentityService = externalIdentityService;
		this.userDetailsService = userDetailsService;
		this.jwtTokenUtil = jwtTokenUtil;
		this.objectMapper = objectMapper;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			org.springframework.security.core.Authentication authentication) throws IOException, ServletException {
		OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
		String provider = oauthToken.getAuthorizedClientRegistrationId();
		InternalUser internalUser = externalIdentityService.findOrCreateUser(
				provider,
				oauthToken.getPrincipal());
		UserDetails userDetails = userDetailsService.toUserDetails(internalUser);
		Map<String, Object> providerAttributes = new LinkedHashMap<>(oauthToken.getPrincipal().getAttributes());
		Map<String, Object> tokenClaims = tokenClaims(provider, oauthToken, internalUser, userDetails);
		String jwt = jwtTokenUtil.generateToken(userDetails, tokenClaims);

		LoginResponse body = new LoginResponse(
				jwt,
				provider,
				internalUserSummary(internalUser),
				tokenClaims,
				providerAttributes);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), body);
	}

	private Map<String, Object> tokenClaims(
			String provider,
			OAuth2AuthenticationToken oauthToken,
			InternalUser internalUser,
			UserDetails userDetails) {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("auth_provider", provider);
		claims.put("provider_user_id", oauthToken.getPrincipal().getName());
		putIfPresent(claims, "email", internalUser.getEmail());
		putIfPresent(claims, "display_name", internalUser.getDisplayName());
		claims.put("authorities",
				userDetails.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
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

	private void putIfPresent(Map<String, Object> claims, String key, String value) {
		if (value != null && !value.isBlank()) {
			claims.put(key, value);
		}
	}
}
