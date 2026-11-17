package org.springframework.samples.petclinic.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.stereotype.Service;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;


@DataJpaTest(includeFilters = @ComponentScan.Filter(classes = {Service.class})) 
//@Sql(scripts = {"/testdata.sql"})
public class DummyRepositoryTest {
	
	@Autowired
	private PetService petService;
	@Autowired
	PetRepository petRepository;

	@Autowired 
	OwnerRepository ownerRepository;

	@Test 
	@Transactional 
	public void businessRuleTest() {
		
		// Arrange:
		Pet pet =new Pet();
		pet.setName("Bufi");
		Owner owner = ownerRepository.findById(1);
		pet.setOwner(owner);
		
		// Act:
		try {
			petService.savePet(pet);
		// Assert:
		} catch (DataAccessException | DuplicatedPetNameException e) {
			fail("No se esperaba ninguna excepcion al guardar la mascota");
		}
		List<Pet> pets = petRepository.findByOwnerFirstName(owner.getFirstName());
		assertEquals(2, pets.size(), "Se esperaba que el propietario tuviera dos mascotas");

		// Caso negativo:
		// arrange:
		Pet leonardo = new Pet();
		leonardo.setName("Leo");
		leonardo.setOwner(owner);
		// act & assert:
		assertThrows(DuplicatedPetNameException.class, () -> {
			petService.savePet(leonardo);			
		}, "Se esperaba una excepcion de tipo DuplicatedPetNameException");
	}



}
