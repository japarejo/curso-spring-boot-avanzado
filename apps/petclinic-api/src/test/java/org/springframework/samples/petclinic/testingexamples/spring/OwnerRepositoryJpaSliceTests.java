package org.springframework.samples.petclinic.testingexamples.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DisplayName("@DataJpaTest: slice de repositorios JPA")
class OwnerRepositoryJpaSliceTests {

	@Autowired
	private OwnerRepository ownerRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Carga repositorios, entidades, H2 y datos SQL, pero no controladores ni servicios")
	void shouldFindOwnersFromImportedSqlData() {
		Collection<Owner> owners = this.ownerRepository.findByLastName("Davis");

		assertThat(owners).hasSize(2);
		assertThat(owners).extracting(Owner::getFirstName).containsExactlyInAnyOrder("Betty", "Harold");
	}

	@Test
	@Transactional
	@DisplayName("Cada test es transaccional y se revierte al terminar")
	void shouldSaveOwnerOnlyInsideThisTestTransaction() {
		Owner owner = new Owner();
		owner.setFirstName("Clara");
		owner.setLastName("Montero");
		owner.setAddress("Calle de la Luna 7");
		owner.setCity("Sevilla");
		owner.setTelephone("600123123");

		User user = new User();
		user.setUsername("clara");
		user.setPassword("testing");
		user.setEnabled(true);
		owner.setUser(user);

		this.ownerRepository.save(owner);
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.ownerRepository.findByLastName("Montero")).singleElement()
			.extracting(Owner::getFirstName)
			.isEqualTo("Clara");
	}

}
