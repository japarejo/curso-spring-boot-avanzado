// =============================================================================
// Modulo 1 - Ejercicio 10: dos beans del mismo tipo.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Paquete sugerido: org.springframework.samples.petclinic.service.notificaciones
// Se muestra la version FINAL (opcion 4); las otras tres van comentadas.
// =============================================================================

// ---------- NotificadorCitas.java ----------
package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.samples.petclinic.model.Visit;

public interface NotificadorCitas {

	String notificar(Visit visita);

}

// ---------- NotificadorPorEmail.java ----------
/*
package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.stereotype.Component;

// Opcion 1: @Primary          -> "si nadie dice otra cosa, esta"
// Opcion 4: @ConditionalOnProperty -> el bean solo EXISTE si la propiedad vale "email".
//           matchIfMissing = true la convierte en la opcion por defecto.
@Component("email")
@ConditionalOnProperty(name = "petclinic.notificaciones.canal", havingValue = "email", matchIfMissing = true)
class NotificadorPorEmail implements NotificadorCitas {

	@Override
	public String notificar(Visit visita) {
		return "EMAIL a " + visita.getPet().getOwner().getLastName() + ": cita el " + visita.getDate();
	}

}
*/

// ---------- NotificadorPorSms.java ----------
/*
package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.stereotype.Component;

@Component("sms")
@ConditionalOnProperty(name = "petclinic.notificaciones.canal", havingValue = "sms")
class NotificadorPorSms implements NotificadorCitas {

	@Override
	public String notificar(Visit visita) {
		return "SMS al " + visita.getPet().getOwner().getTelephone() + ": cita el " + visita.getDate();
	}

}
*/

// ---------- RecordatorioService.java ----------
/*
package org.springframework.samples.petclinic.service.notificaciones;

import org.springframework.samples.petclinic.model.Visit;
import org.springframework.stereotype.Service;

@Service
public class RecordatorioService {

	private final NotificadorCitas notificador;

	// Opcion 2: public RecordatorioService(@Qualifier("sms") NotificadorCitas notificador)
	//           El nombre del bean es el valor de @Component("sms").
	//
	// Opcion 3: public RecordatorioService(List<NotificadorCitas> notificadores)
	//           Spring inyecta TODAS las implementaciones. Util para el patron
	//           "notificar por todos los canales" o para cadenas de responsabilidad.
	public RecordatorioService(NotificadorCitas notificador) {
		this.notificador = notificador;
	}

	public String recordar(Visit visita) {
		return notificador.notificar(visita);
	}

}
*/

// ---------- application.properties ----------
//   petclinic.notificaciones.canal=email
//
// Probar sin recompilar:
//   ./mvnw -pl apps/petclinic-api spring-boot:run \
//     -Dspring-boot.run.arguments=--petclinic.notificaciones.canal=sms
//
// Y ver la decision en el informe de condiciones:
//   curl -s localhost:8080/actuator/conditions \
//     | jq '.contexts.application | .positiveMatches, .negativeMatches | keys[] | select(test("Notificador"))'
//
// El error del paso 2, para leerlo en clase:
//   Parameter 0 of constructor in ...RecordatorioService required a single bean,
//   but 2 were found:
//     - email: defined in file [.../NotificadorPorEmail.class]
//     - sms:   defined in file [.../NotificadorPorSms.class]
//   Action: Consider marking one of the beans as @Primary, updating the consumer
//   to accept multiple beans, or using @Qualifier ...
// El propio mensaje propone tres de las cuatro soluciones.
