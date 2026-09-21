package org.springframework.samples.petclinic.oauth2.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class LoginResponse implements Serializable {

	private static final long serialVersionUID = -9024853217925301432L;

	private final String tokenType = "Bearer";
	private final String token;
	private final String provider;
	private final InternalUserSummary internalUser;
	private final Map<String, Object> tokenClaims;
	private final Map<String, Object> providerAttributes;

	public LoginResponse(
			String token,
			String provider,
			InternalUserSummary internalUser,
			Map<String, Object> tokenClaims,
			Map<String, Object> providerAttributes) {
		this.token = token;
		this.provider = provider;
		this.internalUser = internalUser;
		this.tokenClaims = tokenClaims;
		this.providerAttributes = providerAttributes;
	}

	public String getTokenType() {
		return tokenType;
	}

	public String getToken() {
		return token;
	}

	public String getProvider() {
		return provider;
	}

	public InternalUserSummary getInternalUser() {
		return internalUser;
	}

	public Map<String, Object> getTokenClaims() {
		return tokenClaims;
	}

	public Map<String, Object> getProviderAttributes() {
		return providerAttributes;
	}

	public static class InternalUserSummary implements Serializable {

		private static final long serialVersionUID = 805294819200043276L;

		private final Integer id;
		private final String username;
		private final String email;
		private final String displayName;
		private final List<String> authorities;

		public InternalUserSummary(
				Integer id,
				String username,
				String email,
				String displayName,
				List<String> authorities) {
			this.id = id;
			this.username = username;
			this.email = email;
			this.displayName = displayName;
			this.authorities = authorities;
		}

		public Integer getId() {
			return id;
		}

		public String getUsername() {
			return username;
		}

		public String getEmail() {
			return email;
		}

		public String getDisplayName() {
			return displayName;
		}

		public List<String> getAuthorities() {
			return authorities;
		}
	}
}
