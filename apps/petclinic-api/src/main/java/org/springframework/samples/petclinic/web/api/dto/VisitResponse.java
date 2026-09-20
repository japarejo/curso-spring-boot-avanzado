package org.springframework.samples.petclinic.web.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.samples.petclinic.model.Diagnose;
import org.springframework.samples.petclinic.model.Visit;

@Schema(description = "Visit returned by the API.")
public class VisitResponse {

	@Schema(description = "Visit identifier.", example = "1")
	private Integer id;

	@Schema(description = "Visit date.", example = "2026-05-02")
	private LocalDate date;

	@Schema(description = "Visit description.", example = "Routine check with mild respiratory symptoms")
	private String description;

	@Schema(description = "Pet identifier.", example = "1")
	private Integer petId;

	@Schema(description = "Diagnosis created with the visit, when one was provided.")
	private DiagnosisResponse diagnosis;

	public static VisitResponse from(Visit visit, Diagnose diagnose) {
		VisitResponse response = new VisitResponse();
		response.setId(visit.getId());
		response.setDate(visit.getDate());
		response.setDescription(visit.getDescription());
		response.setPetId(visit.getPet().getId());
		response.setDiagnosis(DiagnosisResponse.from(diagnose));
		return response;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

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

	public DiagnosisResponse getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(DiagnosisResponse diagnosis) {
		this.diagnosis = diagnosis;
	}

}
