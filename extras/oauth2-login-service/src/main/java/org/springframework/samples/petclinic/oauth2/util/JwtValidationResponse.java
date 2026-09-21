package org.springframework.samples.petclinic.oauth2.util;

public class JwtValidationResponse {

	private final boolean valid;

	public JwtValidationResponse(boolean valid) {
		this.valid = valid;
	}

	public boolean isValid() {
		return valid;
	}
}
