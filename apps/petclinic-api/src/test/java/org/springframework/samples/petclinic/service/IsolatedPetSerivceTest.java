package org.springframework.samples.petclinic.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;

@ExtendWith(MockitoExtension.class)
public class IsolatedPetSerivceTest {

	@Mock 
	private PetRepository petRepository;

	@Mock 
	private VisitRepository visitRepository;


	private PetService petService;

	@BeforeEach 
	public void setup() {
		// Arrange:
		petService = new PetService(petRepository, visitRepository);
	}

	@Test 
	public void testFindAllPets() {
		// Arrange:		
		Collection<Pet> oracle= new ArrayList<>();
		when(petRepository.findAll()).thenReturn(oracle);
		// Act:		
		Collection<Pet> actualResult = petService.findAllPets();
		// Assert:
		assert(actualResult.equals(oracle));
		verify(petRepository,times(1)).findAll();
	}

	@Test 
	public void testSavePet() {
		// Arrange:		
		Owner owner = mock(Owner.class);
		when(owner.getPetwithIdDifferent(anyString(),anyInt())).thenReturn(null);
		Pet pet = new Pet();
		pet.setId(1);
		pet.setName("Bufi");
		pet.setOwner(owner);		

		// Act:		
		try {
			petService.savePet(pet);
		// Assert:			
		} catch (DataAccessException | DuplicatedPetNameException e) {
			fail("No se esperaba ninguna excepcion al guardar la mascota");
		}				
		verify(petRepository,times(1)).save(pet);
		verify(owner,times(1)).getPetwithIdDifferent(anyString(),anyInt());

		// Caso negativo
		when(owner.getPetwithIdDifferent(anyString(),anyInt())).thenReturn(new Pet());
		assertThrows(DuplicatedPetNameException.class, () -> {
			petService.savePet(pet);			
		}, "Se esperaba una excepcion de tipo DuplicatedPetNameException");
	}


}
