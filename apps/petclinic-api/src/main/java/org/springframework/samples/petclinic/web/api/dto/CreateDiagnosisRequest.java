package org.springframework.samples.petclinic.web.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Diagnosis data associated with a new visit.")
public class CreateDiagnosisRequest {

	@Schema(description = "Diagnosis description.", example = "Persistent coughing and respiratory inflammation")
	@Size(min = 10, max = 1024)
	private String description;

	@Schema(description = "Disease identifier.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer diseaseId;

	@Schema(description = "Veterinarian identifier.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer vetId;

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
