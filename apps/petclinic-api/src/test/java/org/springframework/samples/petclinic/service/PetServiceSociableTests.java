package org.springframework.samples.petclinic.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.AfterTransaction;

/**
 * Prueba SOCIABLE de {@link PetService}: el servicio es el real y habla con el
 * {@link PetRepository} real sobre H2 (no hay mocks, a diferencia de
 * {@link IsolatedPetSerivceTest}).
 *
 * <ul>
 * <li>{@code @DataJpaTest} levanta solo la capa JPA; el filtro añade PetService.</li>
 * <li>{@code @Sql} carga datos propios de la prueba antes de cada metodo.</li>
 * <li>{@code @DataJpaTest} ya es {@code @Transactional}: cada prueba (y el script
 * de {@code @Sql}, que se ejecuta en esa misma transaccion) se deshace al terminar.
 * {@code @Rollback} se pone solo para dejarlo explicito.</li>
 * <li>{@code @AfterTransaction} comprueba, ya fuera de la transaccion, que el
 * rollback se ha producido de verdad.</li>
 * </ul>
 */
@DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PetService.class))
@Sql("/db/pet-service-sociable-data.sql")
@Rollback
class PetServiceSociableTests {

	private static final int TEST_OWNER_ID = 1000;

	private static final int TOBY_ID = 1000;

	@Autowired
	private PetService petService;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void findPetByIdReturnsPetLoadedByScript() {
		// Act:
		Pet toby = petService.findPetById(TOBY_ID);
		// Assert:
		assertNotNull(toby);
		assertEquals("Toby", toby.getName());
		assertEquals("Ana", toby.getOwner().getFirstName());
		assertEquals(2, toby.getOwner().getPets().size());
	}

	@Test
	void savePetWithNewNamePersistsIt() {
		// Arrange:
		Owner ana = petService.findPetById(TOBY_ID).getOwner();
		Pet nala = new Pet();
		nala.setName("Nala");
		nala.setOwner(ana);
		// Act:
		assertDoesNotThrow(() -> petService.savePet(nala));
		// Assert:
		assertNotNull(nala.getId(), "La mascota deberia tener id tras guardarse");
		List<Pet> anaPets = petRepository.findByOwnerFirstName("Ana");
		assertEquals(3, anaPets.size(), "Ana deberia tener ahora tres mascotas");
	}

	@Test
	void savePetWithDuplicatedNameThrows() {
		// Arrange: el nombre coincide con 'Toby' sin distinguir mayusculas
		Owner ana = petService.findPetById(TOBY_ID).getOwner();
		Pet otherToby = new Pet();
		otherToby.setName("toby");
		otherToby.setOwner(ana);
		// Act & Assert:
		assertThrows(DuplicatedPetNameException.class, () -> petService.savePet(otherToby));
		assertEquals(2, petRepository.findByOwnerFirstName("Ana").size());
	}

	@AfterTransaction
	void dataFromScriptAndTestIsRolledBack() {
		Integer owners = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM owners WHERE id = ?", Integer.class,
				TEST_OWNER_ID);
		Integer pets = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pets WHERE owner_id = ?", Integer.class,
				TEST_OWNER_ID);
		assertEquals(0, owners, "El propietario del script deberia haberse deshecho");
		assertEquals(0, pets, "Las mascotas del script y de la prueba deberian haberse deshecho");
	}

}
