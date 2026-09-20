package org.springframework.samples.petclinic.web.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.samples.petclinic.model.Diagnose;

@Schema(description = "Diagnosis returned with a visit.")
public class DiagnosisResponse {

	@Schema(description = "Diagnosis identifier.", example = "1")
	private Integer id;

	@Schema(description = "Diagnosis description.", example = "Persistent coughing and respiratory inflammation")
	private String description;

	@Schema(description = "Disease identifier.", example = "1")
	private Integer diseaseId;

	@Schema(description = "Veterinarian identifier.", example = "1")
	private Integer vetId;

	public static DiagnosisResponse from(Diagnose diagnose) {
		if (diagnose == null) {
			return null;
		}
		DiagnosisResponse response = new DiagnosisResponse();
		response.setId(diagnose.getId());
		response.setDescription(diagnose.getDescription());
		response.setDiseaseId(diagnose.getDisease().getId());
		response.setVetId(diagnose.getVet().getId());
		return response;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getDiseaseId() {
		return diseaseId;
	}

	public void setDiseaseId(Integer diseaseId) {
		this.diseaseId = diseaseId;
	}

	public Integer getVetId() {
		return vetId;
	}

	public void setVetId(Integer vetId) {
		this.vetId = vetId;
	}

}
