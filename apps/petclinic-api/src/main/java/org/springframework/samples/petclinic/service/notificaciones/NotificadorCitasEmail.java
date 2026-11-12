package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.stereotype.Service;

// @Service y no @Component: RecordatorioService (@Service) depende de un
// NotificadorCitas, y las rebanadas @DataJpaTest(includeFilters = Service.class)
// solo cargan beans @Service. Con @Component el contexto de prueba no arranca.
@Service
@Profile("!tel")
public class NotificadorCitasEmail implements NotificadorCitas {

	@Override
	public String notificar(Visit visita) {
		// Lógica para enviar notificación por correo electrónico

		// ...
		System.out.println("Enviando notificación por correo electrónico para la cita: " + visita);

		return "Notificación enviada por correo electrónico para la cita: " + visita;
	}
	
	
}
