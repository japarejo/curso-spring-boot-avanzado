package org.springframework.samples.petclinic.oauth2.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "internal_authorities",
		uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "authority" }))
public class UserAuthority extends BaseEntity {

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id")
	private InternalUser user;

	@Size(min = 3, max = 50)
	@Column(nullable = false, length = 50)
	private String authority;

	public InternalUser getUser() {
		return user;
	}

	public void setUser(InternalUser user) {
		this.user = user;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}
}
