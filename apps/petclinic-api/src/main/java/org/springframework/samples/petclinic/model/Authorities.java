package org.springframework.samples.petclinic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "authorities")
public class Authorities extends BaseEntity{
	
	@ManyToOne
	@JoinColumn(name = "username")
	User user;
	
	@Size(min = 3, max = 50)
	String authority;
	
	 public String getAuthority() {
		return authority;
	}
	 
	 public void setAuthority(String authority) {
		this.authority = authority;
	}
	 
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
}
