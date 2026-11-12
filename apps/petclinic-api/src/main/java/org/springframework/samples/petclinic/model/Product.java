package org.springframework.samples.petclinic.model;

import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
public class Product {
	@NotBlank 
	String name;
	@Size(min=1, max=200)
	String description;
	@DecimalMin(value = "0.0", message = "Nuestros precios son baratos... pero no pueden ser negativos")
	Double price;
	@NotEmpty 
	List<PetType> petTypes;
}
