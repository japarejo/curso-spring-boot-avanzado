package org.springframework.samples.petclinic.testingexamples.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.repository.OwnerRepository;

@DataJpaTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.defer-datasource-initialization=true",
	"spring.sql.init.mode=always",
	"spring.sql.init.platform=mysql",
	"spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("@DataJpaTest con MySQL real usando Testcontainers")
class OwnerRepositoryMySqlContainerIT extends MySqlTestContainerSupport {

	@Autowired
	private OwnerRepository ownerRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Arranca un MySQL efimero y Spring lo usa como DataSource del test")
	void shouldUseRealMySqlContainerAsTestDatabase() {
		String databaseName = this.jdbcTemplate.queryForObject("select database()", String.class);

		assertThat(databaseName).isEqualTo("petclinic");
		assertThat(mysql.isRunning()).isTrue();
	}

	@Test
	@DisplayName("Ejecuta el mismo repositorio contra MySQL, no contra H2")
	void shouldFindOwnersFromImportedSqlDataUsingRealMySql() {
		Collection<Owner> owners = this.ownerRepository.findByLastName("Davis");

		assertThat(owners).hasSize(2);
		assertThat(owners)
			.extracting(Owner::getFirstName)
			.containsExactlyInAnyOrder("Betty", "Harold");
	}

}
