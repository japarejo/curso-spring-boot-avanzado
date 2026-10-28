package org.springframework.samples.petclinic.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.samples.petclinic.repository.DiseaseRepository;
import org.springframework.samples.petclinic.repository.PetRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest 
public class EntitiesTest {

	@Autowired 
	DiseaseRepository diseaseRepository;
	@Autowired 
	PetRepository petRepository;

	@Autowired 
	private EntityManager entityManager;

	@Test 
	public void miPrueba(){
		List<Owner> owners = entityManager.
			createQuery("SELECT o FROM Owner o", Owner.class).
			getResultList();
		
		for(Owner o:owners)
			System.out.println(o);
	}


	@Test 
	public void miPrueba2(){
		Owner o=entityManager.find(Owner.class, 1);
		System.out.println(o.getFullName());
	}

	@Test 
	public void miPrueba3(){
		Pet p=new Pet();
		p.setName("Bufi");
		p.setType(entityManager.find(PetType.class,2));	// Pêrro
		p.setBirthDate(java.time.LocalDate.of(2020, 1, 1));
		p.setAge(4);
		p.setOwner(entityManager.find(Owner.class, 1));
		System.out.println("Pet: "+p +" id:"+p.getId());
		System.out.println("Voy a grabar!!!!");
		entityManager.persist(p);
		System.out.println("Grabado!!!!");
		System.out.println("Pet: "+p +" id:"+p.getId());
	}


	@Test 
	public void miPrueba4(){
		Disease d=new Disease();
		d.setName("Rabia");
		d.setDescription("Esto es muuuu maloo");
		d.setPetTypeswithPrevalence(Set.of(entityManager.find(PetType.class,2)));
		diseaseRepository.save(d);
		
		Collection<Disease> diseases = diseaseRepository.findAll();
		for(Disease di:diseases){			
			System.out.println(di +" id:"+di.getId());
			System.out.println(di.getDescription());
			for(PetType pt:di.getPetTypeswithPrevalence())
				System.out.println("  ---- Se da en: "+pt);
		}

		System.out.println("Voy a buscar enfermedades gatunas");
		List<Disease> diseasesByPetType = diseaseRepository.findByPetTypeswithPrevalence(entityManager.find(PetType.class,1));
		for(Disease di:diseasesByPetType){			
			System.out.println(di);
			System.out.println(di.getDescription());
			for(PetType pt:di.getPetTypeswithPrevalence())
				System.out.println("  ---- Se da en: "+pt);
		}

	}


	@Test 
	public void miPrueba5(){
		Pet p=petRepository.findByAge(14);
		System.out.println("Pet: "+p +" id:"+p.getId());
		System.out.println("Pet: "+p.getName() +" id:"+p.getId());
		System.out.println("Pet: "+p.getType() +" id:"+p.getId());
		System.out.println("Pet: "+p.getBirthDate() +" id:"+p.getId());
		System.out.println("Pet: "+p.getOwner() +" id:"+p.getId());


		System.out.println("Voy a buscar mascotas de Betty");
		List<Pet> petsByOwnerFirstName = petRepository.findByOwnerFirstName("Betty");
		System.out.println("Mascotas de Betty: "+petsByOwnerFirstName.size());
		for(Pet pe:petsByOwnerFirstName){
			System.out.println("Pet: "+pe +" id:"+pe.getId());
			System.out.println("Pet: "+pe.getName() +" id:"+pe.getId());
			System.out.println("Pet: "+pe.getType() +" id:"+pe.getId());
			System.out.println("Pet: "+pe.getBirthDate() +" id:"+pe.getId());
			System.out.println("Pet: "+pe.getOwner() +" id:"+pe.getId());		
		}
	}
}