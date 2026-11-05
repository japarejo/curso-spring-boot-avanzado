package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.samples.petclinic.model.Visit;

public interface NotificadorCitas {
	String notificar(Visit visita);
}
