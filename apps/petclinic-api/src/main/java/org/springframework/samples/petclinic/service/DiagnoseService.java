package org.springframework.samples.petclinic.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.Diagnose;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.DiagnoseRepository;
import org.springframework.stereotype.Service;

@Service
public class DiagnoseService {
	@Autowired
	private DiagnoseRepository repo;
	
	
	public Collection<Diagnose> findAll(){
		return repo.findAll();
	}
	
	public Collection<Diagnose> findByPetId(int petId){
		return repo.findByPetId(petId);
	}
	
	public Map<Visit,Diagnose> findByPets(Collection<Pet> pets){
		Map<Visit,Diagnose> diagnoses=new HashMap<Visit, Diagnose>();		
		for(Pet pet:pets) {
			for(Diagnose diagnose:findByPetId(pet.getId()))
				diagnoses.put(diagnose.getVisit(), diagnose);
		}
		return diagnoses;
	}
	
	/**
	 * La regla "una mascota solo puede tener una enfermedad prevalente en su especie"
	 * NO se comprueba aqui a mano. Se expresa como restriccion Bean Validation a nivel
	 * de clase sobre la entidad Diagnose: ver {@code service.businessrules.ValidatePossibleDisease}
	 * y su validador {@code PossibleDiseaseValidator}.
	 *
	 * Modulo 6: comparar ambos enfoques (excepcion de dominio comprobada frente a
	 * restriccion declarativa) es uno de los ejercicios de la unidad.
	 */
	public void save(@Valid Diagnose diagnose) {
		repo.save(diagnose);
	}

}
