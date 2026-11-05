package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.stereotype.Service;



@Service 
public class RecordatorioService {
	private NotificadorCitas notificador;

	
	public RecordatorioService(NotificadorCitas notificador) {
		this.notificador = notificador;
	}

	public String enviarRecordatorio(Visit visita) {
		// Lógica para enviar recordatorio de cita
		return notificador.notificar(visita);
	}
}
