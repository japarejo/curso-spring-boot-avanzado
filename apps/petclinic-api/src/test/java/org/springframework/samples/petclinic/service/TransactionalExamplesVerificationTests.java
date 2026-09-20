package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
@Import({ ValidationAutoConfiguration.class, TransactionalExamplesVerificationTests.ExampleWriteService.class,
		TransactionalExamplesVerificationTests.ExampleAuditService.class,
		TransactionalExamplesVerificationTests.ExampleUseCaseService.class })
@TestPropertySource(properties = {
		"spring.jpa.show-sql=true",
		"spring.jpa.properties.hibernate.format_sql=true",
		"logging.level.org.hibernate.SQL=DEBUG",
		"logging.level.org.hibernate.orm.jdbc.bind=TRACE",
		"logging.level.org.springframework.transaction.interceptor=TRACE",
		"logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG"
})
@ExtendWith(OutputCaptureExtension.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransactionalExamplesVerificationTests {

	@Autowired
	private ExampleWriteService writeService;

	@Autowired
	private ExampleUseCaseService useCaseService;

	@Autowired
	private OwnerRepository ownerRepository;

	@Autowired
	private PetRepository petRepository;

	@Test
	void rollbackForCheckedExceptionRollsBackAndLeavesRollbackInLog(CapturedOutput output) {
		assertThatThrownBy(() -> this.writeService.savePetAndThrowCheckedWithRollbackFor("ROLLBACK_FOR_OK"))
			.isInstanceOf(DuplicatedPetNameException.class);

		assertThat(petNamesForOwner(1)).doesNotContain("ROLLBACK_FOR_OK");
		assertThat(output).contains("insert").contains("pets").contains("Initiating transaction rollback");
	}

	@Test
	void checkedExceptionWithoutRollbackForCommitsAndLeavesCommitInLog(CapturedOutput output) {
		assertThatThrownBy(() -> this.writeService.savePetAndThrowCheckedWithoutRollbackFor("ROLLBACK_FOR_OFF"))
			.isInstanceOf(DuplicatedPetNameException.class);

		assertThat(petNamesForOwner(1)).contains("ROLLBACK_FOR_OFF");
		assertThat(output).contains("insert").contains("pets").contains("Initiating transaction commit");
	}

	@Test
	void dirtyCheckingUpdatesManagedEntityUnlessTransactionIsReadOnly(CapturedOutput output) {
		String originalCity = this.ownerRepository.findById(1).getCity();

		this.writeService.changeOwnerCity(1, "Dirty Checking City");
		assertThat(this.ownerRepository.findById(1).getCity()).isEqualTo("Dirty Checking City");
		assertThat(output).contains("update").contains("owners");

		this.writeService.changeOwnerCityReadOnly(1, "Read Only City");
		assertThat(this.ownerRepository.findById(1).getCity()).isEqualTo("Dirty Checking City");

		this.writeService.changeOwnerCity(1, originalCity);
	}

	@Test
	void requiresNewCommitsAuditWhenParentRollsBack(CapturedOutput output) {
		assertThatThrownBy(() -> this.useCaseService.parentFailsAfterRequiresNewAudit())
			.isInstanceOf(IllegalStateException.class);

		assertThat(this.ownerRepository.findByLastName("PARENT_ROLLBACK")).isEmpty();
		assertThat(this.ownerRepository.findByLastName("AUDIT_REQUIRES_NEW")).hasSize(1);
		assertThat(output).contains("Suspending current transaction")
			.contains("Creating new transaction")
			.contains("Initiating transaction rollback");
	}

	@Test
	void requiredAuditRollsBackWithParent(CapturedOutput output) {
		assertThatThrownBy(() -> this.useCaseService.parentFailsAfterRequiredAudit())
			.isInstanceOf(IllegalStateException.class);

		assertThat(this.ownerRepository.findByLastName("AUDIT_REQUIRED")).isEmpty();
		assertThat(output).contains("Participating in existing transaction")
			.contains("Initiating transaction rollback");
	}

	@Test
	void isolationLevelIsVisibleInTransactionLog(CapturedOutput output) {
		this.writeService.findOwnerWithReadCommitted(1);
		this.writeService.findOwnerWithSerializable(1);

		assertThat(output).contains("ISOLATION_READ_COMMITTED").contains("ISOLATION_SERIALIZABLE");
	}

	@Test
	void timeoutRollsBackWhenAnotherDatabaseOperationHappensAfterTheDeadline(CapturedOutput output) {
		String originalCity = this.ownerRepository.findById(1).getCity();

		assertThatThrownBy(() -> this.writeService.slowOwnerChange(1))
			.isInstanceOf(TransactionTimedOutException.class);

		assertThat(this.ownerRepository.findById(1).getCity()).isEqualTo(originalCity);
		assertThat(output).contains("timeout_1").contains("Initiating transaction rollback");
	}

	private Collection<String> petNamesForOwner(int ownerId) {
		return this.petRepository.findAll().stream()
			.filter((pet) -> pet.getOwner() != null && pet.getOwner().getId() == ownerId)
			.map(Pet::getName)
			.toList();
	}

	private static Owner owner(String lastName, String username) {
		Owner owner = new Owner();
		owner.setFirstName("Tx");
		owner.setLastName(lastName);
		owner.setAddress("Test Street");
		owner.setCity("Test City");
		owner.setTelephone("9999999999");
		org.springframework.samples.petclinic.model.User user = new org.springframework.samples.petclinic.model.User();
		user.setUsername(username);
		user.setPassword("password");
		user.setEnabled(true);
		owner.setUser(user);
		return owner;
	}

	@Service
	static class ExampleWriteService {

		private final OwnerRepository ownerRepository;

		private final PetRepository petRepository;

		ExampleWriteService(OwnerRepository ownerRepository, PetRepository petRepository) {
			this.ownerRepository = ownerRepository;
			this.petRepository = petRepository;
		}

		@Transactional(rollbackFor = DuplicatedPetNameException.class)
		public void savePetAndThrowCheckedWithRollbackFor(String name)
				throws DataAccessException, DuplicatedPetNameException {
			savePetForOwnerOne(name);
			throw new DuplicatedPetNameException();
		}

		@Transactional
		public void savePetAndThrowCheckedWithoutRollbackFor(String name)
				throws DataAccessException, DuplicatedPetNameException {
			savePetForOwnerOne(name);
			throw new DuplicatedPetNameException();
		}

		@Transactional
		public void changeOwnerCity(int ownerId, String city) {
			Owner owner = this.ownerRepository.findById(ownerId);
			owner.setCity(city);
		}

		@Transactional(readOnly = true)
		public void changeOwnerCityReadOnly(int ownerId, String city) {
			Owner owner = this.ownerRepository.findById(ownerId);
			owner.setCity(city);
		}

		@Transactional(isolation = Isolation.READ_COMMITTED)
		public Owner findOwnerWithReadCommitted(int ownerId) {
			return this.ownerRepository.findById(ownerId);
		}

		@Transactional(isolation = Isolation.SERIALIZABLE)
		public Owner findOwnerWithSerializable(int ownerId) {
			return this.ownerRepository.findById(ownerId);
		}

		@Transactional(timeout = 1)
		public void slowOwnerChange(int ownerId) throws InterruptedException {
			Owner owner = this.ownerRepository.findById(ownerId);
			Thread.sleep(1_500);
			owner.setCity("Timeout City");
			this.ownerRepository.findByLastName("timeout_1");
		}

		private void savePetForOwnerOne(String name) {
			Owner owner = this.ownerRepository.findById(1);
			Pet pet = new Pet();
			pet.setName(name);
			pet.setBirthDate(LocalDate.now());
			PetType type = this.petRepository.findPetTypes().get(0);
			pet.setType(type);
			owner.addPet(pet);
			this.petRepository.save(pet);
		}

	}

	@Service
	static class ExampleUseCaseService {

		private final OwnerRepository ownerRepository;

		private final ExampleAuditService auditService;

		ExampleUseCaseService(OwnerRepository ownerRepository, ExampleAuditService auditService) {
			this.ownerRepository = ownerRepository;
			this.auditService = auditService;
		}

		@Transactional
		public void parentFailsAfterRequiresNewAudit() {
			this.ownerRepository.save(owner("PARENT_ROLLBACK", "parent_rollback"));
			this.auditService.registerAuditRequiresNew();
			throw new IllegalStateException("Parent fails after REQUIRES_NEW audit");
		}

		@Transactional
		public void parentFailsAfterRequiredAudit() {
			this.auditService.registerAuditRequired();
			throw new IllegalStateException("Parent fails after REQUIRED audit");
		}

	}

	@Service
	static class ExampleAuditService {

		private final OwnerRepository ownerRepository;

		ExampleAuditService(OwnerRepository ownerRepository) {
			this.ownerRepository = ownerRepository;
		}

		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void registerAuditRequiresNew() {
			this.ownerRepository.save(owner("AUDIT_REQUIRES_NEW", "audit_requires_new"));
		}

		@Transactional(propagation = Propagation.REQUIRED)
		public void registerAuditRequired() {
			this.ownerRepository.save(owner("AUDIT_REQUIRED", "audit_required"));
		}

	}

}
