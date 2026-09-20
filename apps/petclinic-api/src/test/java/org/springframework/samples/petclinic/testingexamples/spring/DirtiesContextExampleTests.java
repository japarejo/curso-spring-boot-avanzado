package org.springframework.samples.petclinic.testingexamples.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = DirtiesContextExampleTests.WaitingRoomConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("@DirtiesContext: cuando un test ensucia el contexto de Spring")
class DirtiesContextExampleTests {

	private static int dirtyContextId;

	@Autowired
	private WaitingRoom waitingRoom;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@Order(1)
	@DisplayName("Un test modifica un bean singleton compartido")
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void shouldMarkContextAsDirtyAfterMutatingSingletonBean() {
		this.waitingRoom.callNextPatient();
		this.waitingRoom.callNextPatient();
		dirtyContextId = System.identityHashCode(this.applicationContext);

		assertThat(this.waitingRoom.calledPatients()).isEqualTo(2);
	}

	@Test
	@Order(2)
	@DisplayName("Spring crea un contexto nuevo para que el siguiente test empiece limpio")
	void shouldReceiveFreshContextAfterDirtiesContext() {
		int freshContextId = System.identityHashCode(this.applicationContext);

		assertThat(freshContextId).isNotEqualTo(dirtyContextId);
		assertThat(this.waitingRoom.calledPatients()).isZero();
	}

	@Configuration
	static class WaitingRoomConfiguration {

		@Bean
		WaitingRoom waitingRoom() {
			return new WaitingRoom();
		}

	}

	static class WaitingRoom {

		private int calledPatients;

		void callNextPatient() {
			this.calledPatients++;
		}

		int calledPatients() {
			return this.calledPatients;
		}

	}

}
