package org.springframework.samples.petclinic.acl.model;

import jakarta.validation.constraints.NotBlank;

public class RecordForm {

	@NotBlank
	private String petName;

	@NotBlank
	private String diagnosis;

	private String treatmentPlan;

	public String getPetName() {
		return petName;
	}

	public void setPetName(String petName) {
		this.petName = petName;
	}

	public String getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(String diagnosis) {
		this.diagnosis = diagnosis;
	}

	public String getTreatmentPlan() {
		return treatmentPlan;
	}

	public void setTreatmentPlan(String treatmentPlan) {
		this.treatmentPlan = treatmentPlan;
	}

}
