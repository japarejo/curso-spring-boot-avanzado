package org.springframework.samples.petclinic.model;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Entity 
@Getter 
@Setter
@EqualsAndHashCode(callSuper = false, of = {"id"}) 
public class Room extends NamedEntity {

	@Min(1)
	private int capacidad;
	
	
}
