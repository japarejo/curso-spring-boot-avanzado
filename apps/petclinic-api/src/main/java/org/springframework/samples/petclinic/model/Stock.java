package org.springframework.samples.petclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter 
@Setter  
@EqualsAndHashCode (callSuper = false, of = {"id"})
public class Stock {
	
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY) 
	Integer id;

	@OneToOne(optional = false)	
	Product product;
	
	@Column(nullable = false, name = "howmuch")
	Integer quantity;
}
