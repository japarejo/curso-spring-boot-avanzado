package org.springframework.samples.petclinic.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.model.Disease;
import org.springframework.samples.petclinic.model.PetType;


public interface DiseaseRepository extends  CrudRepository<Disease, Integer>{
	Collection<Disease> findAll();
	
	
	@Query("SELECT d FROM Disease d JOIN d.petTypeswithPrevalence p WHERE p = :petType")
	List<Disease> findByPetTypeswithPrevalence(PetType petType);
}
