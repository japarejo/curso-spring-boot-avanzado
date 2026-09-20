package org.springframework.samples.petclinic.bills.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@Entity
public class Bill extends BaseEntity{		
	
	@Min(1)
	@Column(name="visit_id")	
	Integer visit;
	
	@Min(0)
	double amount;
	
	@NotEmpty
	String concept;

	public Integer getVisit() {
		return visit;
	}

	public void setVisit(Integer visit) {
		this.visit = visit;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getConcept() {
		return concept;
	}

	public void setConcept(String concept) {
		this.concept = concept;
	}	
	
}