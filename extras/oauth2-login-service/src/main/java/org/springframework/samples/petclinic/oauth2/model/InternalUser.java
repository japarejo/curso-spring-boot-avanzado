package org.springframework.samples.petclinic.oauth2.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "internal_users")
public class InternalUser extends BaseEntity {

	@Column(nullable = false, unique = true, length = 80)
	private String username;

	@Column(unique = true, length = 180)
	private String email;

	@Column(length = 180)
	private String displayName;

	private String password;

	private boolean enabled = true;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "user", orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<UserAuthority> authorities = new LinkedHashSet<>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "user", orphanRemoval = true)
	private Set<ExternalIdentity> externalIdentities = new LinkedHashSet<>();

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<UserAuthority> getAuthorities() {
		return authorities;
	}

	public Set<ExternalIdentity> getExternalIdentities() {
		return externalIdentities;
	}

	public void addAuthority(String authority) {
		UserAuthority userAuthority = new UserAuthority();
		userAuthority.setUser(this);
		userAuthority.setAuthority(authority);
		authorities.add(userAuthority);
	}
}
