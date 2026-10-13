// =============================================================================
// Modulo 6 - Ejercicio 9: la conexion y el pool.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// ENDPOINT DIDACTICO: quitarlo despues. Una API que retiene conexiones a
// peticion del cliente es una invitacion a una denegacion de servicio.
// =============================================================================

// ---------- web/api/DemoPoolController.java ----------
package org.springframework.samples.petclinic.web.api;

import java.util.Map;

import org.springframework.samples.petclinic.service.DemoPoolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoPoolController {

	private final DemoPoolService servicio;

	public DemoPoolController(DemoPoolService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/api/v1/demo/pool/ocupar")
	public Map<String, Object> ocupar(@RequestParam(defaultValue = "3000") long ms) throws InterruptedException {
		long inicio = System.currentTimeMillis();
		int tipos = servicio.retenerConexion(Math.min(ms, 10_000));
		return Map.of("tiposDeMascota", tipos, "ms", System.currentTimeMillis() - inicio,
				"hilo", Thread.currentThread().getName());
	}

}

// ---------- service/DemoPoolService.java ----------
/*
package org.springframework.samples.petclinic.service;

import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoPoolService {

	private final PetRepository petRepository;

	public DemoPoolService(PetRepository petRepository) {
		this.petRepository = petRepository;
	}

	// La transaccion toma una conexion del pool al empezar y no la devuelve
	// hasta terminar. El sleep simula lo que NO hay que hacer nunca dentro de
	// una transaccion: esperar a otro sistema (una llamada HTTP, un fichero...).
	@Transactional(readOnly = true)
	public int retenerConexion(long ms) throws InterruptedException {
		int tipos = petRepository.findPetTypes().size();
		Thread.sleep(ms);
		return tipos;
	}
}
*/

// ---------- application.properties ----------
//   spring.datasource.hikari.maximum-pool-size=2
//   spring.datasource.hikari.connection-timeout=1000
//   spring.datasource.hikari.leak-detection-threshold=2000
//
// ---------- SecurityConfiguration ----------
// /api/** ya es publico; si se protegio en el modulo 3, anadir credenciales al curl.
//
// ---------- Lo que se ve en la consola ----------
//   HikariPool-1 - Connection is not available, request timed out after 1000ms
//       (total=2, active=2, idle=0, waiting=...)
//   ...
//   Connection leak detection triggered for conn0: url=jdbc:h2:mem:... on thread http-nio-8080-exec-1,
//       stack trace follows
//
// El aviso de "leak" no significa que la conexion se haya perdido: significa que
// alguien la ha retenido mas de 2 s. En produccion es la pista para encontrar
// transacciones demasiado largas.
//
// Metricas para vigilarlo en vivo (o en Spring Boot Admin):
//   curl -s localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements'
//   curl -s localhost:8080/actuator/metrics/hikaricp.connections.pending | jq '.measurements'
//   curl -s localhost:8080/actuator/metrics/hikaricp.connections.timeout | jq '.measurements'
