package org.springframework.samples.petclinic.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@EqualsAndHashCode (callSuper = false, of = {"id"})
public class Booking extends BaseEntity{
	@NotNull 
	LocalDateTime startDate;
	@NotNull 
	LocalDateTime endDate;
	
	@ManyToOne
	@NotNull
	Pet pet;

	@ManyToOne 
	@NotNull
	Room room;
}
