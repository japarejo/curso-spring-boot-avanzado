package org.springframework.samples.petclinic.web.api.dto;

import java.time.LocalDate;

import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.web.api.BaseEntityDeserializer;
import org.springframework.samples.petclinic.web.api.BaseEntitySerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;

@Data 
public class FacturaDTO {
	String concepto;
	LocalDate fecha;
	@Min(0)
	Double importe;
	
	@NotNull 
	@JsonSerialize(using = BaseEntitySerializer.class)
	@JsonDeserialize(using = BaseEntityDeserializer.class)
	Visit visit;
}
