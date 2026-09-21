package org.springframework.samples.petclinic.oauth2.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "external_identities",
		uniqueConstraints = @UniqueConstraint(columnNames = { "provider", "provider_user_id" }))
public class ExternalIdentity extends BaseEntity {

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id")
	private InternalUser user;

	@Column(nullable = false, length = 40)
	private String provider;

	@Column(name = "provider_user_id", nullable = false, length = 180)
	private String providerUserId;

	@Column(length = 180)
	private String email;

	@Column(length = 180)
	private String displayName;

	private Instant lastLoginAt;

	public InternalUser getUser() {
		return user;
	}

	public void setUser(InternalUser user) {
		this.user = user;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Instant lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}
}
