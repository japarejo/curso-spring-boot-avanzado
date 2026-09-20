package org.springframework.samples.petclinic.testingexamples.spring;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
abstract class MySqlTestContainerSupport {

	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.36");

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
		.withDatabaseName("petclinic")
		.withUsername("petclinic")
		.withPassword("petclinic");

}
