package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.context.annotation.Profile;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.stereotype.Component;

@Component
@Profile("tel")
public class NotificadorCitasSMS implements NotificadorCitas {

	@Override
	public String notificar(Visit visita) {
		// Lógica para enviar notificación por SMS

		// ...
		System.out.println("Enviando notificación por SMS para la cita: " + visita);
		return "Notificación enviada por SMS para la cita: " + visita;
	}
}
