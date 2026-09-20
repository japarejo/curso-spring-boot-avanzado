package org.springframework.samples.petclinic.model;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "diseases")
public class Disease extends NamedEntity{
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<PetType> getPetTypeswithPrevalence() {
		return petTypeswithPrevalence;
	}

	public void setPetTypeswithPrevalence(Set<PetType> petTypeswithPrevalence) {
		this.petTypeswithPrevalence = petTypeswithPrevalence;
	}

	@Size(min = 10, max = 1024)
	@Column(length=1024)     // Needed in some environments for strings longer than 255 characters
	private String description;
	
	@NotEmpty
	@ManyToMany
	Set<PetType> petTypeswithPrevalence;
}
