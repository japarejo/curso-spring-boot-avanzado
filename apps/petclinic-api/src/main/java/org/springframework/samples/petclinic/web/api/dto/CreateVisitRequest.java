package org.springframework.samples.petclinic.web.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload used to create a visit and, optionally, its diagnosis.")
public class CreateVisitRequest {

	@Schema(description = "Visit date. Defaults to today when omitted.", example = "2026-05-02")
	private LocalDate date;

	@Schema(description = "Visit description.", example = "Routine check with mild respiratory symptoms",
		requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty
	private String description;

	@Schema(description = "Pet identifier.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer petId;

	@Schema(description = "Optional diagnosis. When present, the disease must be feasible for the pet type.")
	@Valid
	private CreateDiagnosisRequest diagnosis;

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getPetId() {
		return petId;
	}

	public void setPetId(Integer petId) {
		this.petId = petId;
	}

	public CreateDiagnosisRequest getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(CreateDiagnosisRequest diagnosis) {
		this.diagnosis = diagnosis;
	}

}
