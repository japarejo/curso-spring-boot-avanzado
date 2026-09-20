package org.springframework.samples.petclinic.testingexamples.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DataJpaTest
@TestPropertySource(properties = {
		"spring.jpa.show-sql=false",
		"spring.jpa.properties.hibernate.show_sql=false",
		"logging.level.org.hibernate.SQL=INFO",
		"logging.level.org.hibernate.orm.jdbc.bind=INFO",
		"logging.level.org.springframework.boot=INFO",
		"logging.level.org.springframework.jdbc.core=INFO",
		"logging.level.org.springframework.orm.jpa=INFO",
		"logging.level.org.springframework.samples.petclinic=INFO",
		"logging.level.org.springframework.security=INFO",
		"logging.level.org.springframework.test.context.transaction=INFO"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("@DataJpaTest: @BeforeAll, @BeforeEach y transacciones")
class DataJpaBeforeEachBeforeAllTransactionExampleTests {

	private static final String BEFORE_ALL_USERNAME = "tx-demo-before-all";

	private static final String BEFORE_ALL_LAST_NAME = "TxDemoBeforeAll";

	private static final String BEFORE_EACH_USERNAME_PREFIX = "tx-demo-before-each-";

	private static final String BEFORE_EACH_LAST_NAME = "TxDemoBeforeEach";

	private static final String BEFORE_ALL_ORIGINAL_CITY = "Madrid";

	private static final String BEFORE_ALL_CITY_CHANGED_IN_BEFORE_EACH = "Ciudad cambiada en BeforeEach";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private int beforeEachExecutions;

	@BeforeAll
	void beforeAll() {
		printStep("@BeforeAll", "se ejecuta una sola vez antes de todos los tests");
		printTransactionState("@BeforeAll");

		this.jdbcTemplate.update("insert into users(username, password, enabled) values (?, ?, true)",
			BEFORE_ALL_USERNAME, "testing");
		this.jdbcTemplate.update("""
				insert into owners(first_name, last_name, address, city, telephone, username)
				values (?, ?, ?, ?, ?, ?)
				""", "Ana", BEFORE_ALL_LAST_NAME, "Calle BeforeAll 1", BEFORE_ALL_ORIGINAL_CITY, "600000001",
			BEFORE_ALL_USERNAME);

		printDatabaseState("@BeforeAll despues de insertar");
	}

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		this.beforeEachExecutions++;

		printStep("@BeforeEach #" + this.beforeEachExecutions,
			"se ejecuta antes de " + testInfo.getDisplayName());
		printTransactionState("@BeforeEach #" + this.beforeEachExecutions);
		printDatabaseState("@BeforeEach #" + this.beforeEachExecutions + " al entrar, antes de preparar datos");

		assertThat(countOwnersByLastName(BEFORE_ALL_LAST_NAME)).as("La fila creada en @BeforeAll ya esta confirmada")
			.isEqualTo(1);
		assertThat(cityOfOwner(BEFORE_ALL_LAST_NAME))
			.as("La modificacion hecha por el @BeforeEach anterior se revirtio al terminar su @Test")
			.contains(BEFORE_ALL_ORIGINAL_CITY);
		assertThat(countOwnersByLastName(BEFORE_EACH_LAST_NAME))
			.as("La fila creada por el @BeforeEach anterior no sobrevivio al rollback del @Test anterior")
			.isZero();

		this.jdbcTemplate.update("update owners set city = ? where last_name = ?",
			BEFORE_ALL_CITY_CHANGED_IN_BEFORE_EACH, BEFORE_ALL_LAST_NAME);
		this.jdbcTemplate.update("insert into users(username, password, enabled) values (?, ?, true)",
			BEFORE_EACH_USERNAME_PREFIX + this.beforeEachExecutions, "testing");
		this.jdbcTemplate.update("""
				insert into owners(first_name, last_name, address, city, telephone, username)
				values (?, ?, ?, ?, ?, ?)
				""", "Bea " + this.beforeEachExecutions, BEFORE_EACH_LAST_NAME, "Calle BeforeEach 1",
			"Sevilla", "600000002", BEFORE_EACH_USERNAME_PREFIX + this.beforeEachExecutions);

		printDatabaseState("@BeforeEach #" + this.beforeEachExecutions + " despues de preparar datos");
	}

	@Test
	@Order(2)
	@DisplayName("test 1: ve los datos de @BeforeAll y los cambios de su propio @BeforeEach")
	void firstTestSeesBeforeAllDataAndCurrentBeforeEachChanges() {
		printStep("@Test 1", "comienza el primer test");
		printTransactionState("@Test 1");
		printDatabaseState("@Test 1");

		assertThat(countOwnersByLastName(BEFORE_ALL_LAST_NAME)).isEqualTo(1);
		assertThat(countOwnersByLastName(BEFORE_EACH_LAST_NAME)).isEqualTo(1);
		assertThat(cityOfOwner(BEFORE_ALL_LAST_NAME)).contains(BEFORE_ALL_CITY_CHANGED_IN_BEFORE_EACH);

	}

	@Test
	@Order(1)
	@DisplayName("test 2: demuestra que lo del @BeforeEach del test 1 fue revertido")
	void secondTestStartsWithOnlyBeforeAllDataCommitted() {
		printStep("@Test 2", "comienza el segundo test");
		printTransactionState("@Test 2");
		printDatabaseState("@Test 2");

		assertThat(countOwnersByLastName(BEFORE_ALL_LAST_NAME)).isEqualTo(1);
		assertThat(countOwnersByLastName(BEFORE_EACH_LAST_NAME)).isEqualTo(1);
		assertThat(cityOfOwner(BEFORE_ALL_LAST_NAME)).contains(BEFORE_ALL_CITY_CHANGED_IN_BEFORE_EACH);

	}

	@AfterAll
	void afterAll() {
		printStep("@AfterAll", "limpia solo el dato confirmado por @BeforeAll para no afectar a otros tests");
		printTransactionState("@AfterAll");

		this.jdbcTemplate.update("delete from owners where last_name = ?", BEFORE_ALL_LAST_NAME);
		this.jdbcTemplate.update("delete from users where username = ?", BEFORE_ALL_USERNAME);

		printDatabaseState("@AfterAll despues de limpiar");
	}

	private long countOwnersByLastName(String lastName) {
		return this.jdbcTemplate.queryForObject("select count(*) from owners where last_name = ?", Long.class, lastName);
	}

	private Optional<String> cityOfOwner(String lastName) {
		return this.jdbcTemplate.query("select city from owners where last_name = ?", (rs, rowNum) -> rs.getString(1),
			lastName).stream().findFirst();
	}

	private void printDatabaseState(String point) {
		System.out.printf("""
				%s
				  beforeAll rows  = %d, city = %s
				  beforeEach rows = %d
				%n""", point, countOwnersByLastName(BEFORE_ALL_LAST_NAME),
			cityOfOwner(BEFORE_ALL_LAST_NAME).orElse("<no existe>"), countOwnersByLastName(BEFORE_EACH_LAST_NAME));
	}

	private void printTransactionState(String point) {
		System.out.printf("%s -> transaccion activa: %s%n", point,
			TransactionSynchronizationManager.isActualTransactionActive());
	}

	private void printStep(String point, String message) {
		System.out.printf("%n--- %s: %s ---%n", point, message);
	}

}
