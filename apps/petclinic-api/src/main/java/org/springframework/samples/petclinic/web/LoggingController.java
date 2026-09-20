package org.springframework.samples.petclinic.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MODULO 5 - Niveles de registro en caliente.
 *
 * Demostracion en clase:
 *   1. GET /logging               -> en la consola solo se ven INFO, WARN y ERROR
 *   2. GET  /actuator/loggers/org.springframework.samples.petclinic
 *   3. POST /actuator/loggers/org.springframework.samples.petclinic
 *      Content-Type: application/json
 *      {"configuredLevel":"TRACE"}
 *   4. GET /logging               -> ahora aparecen tambien TRACE y DEBUG
 *
 * El nivel cambia sin reiniciar la aplicacion. Para volver al valor original
 * se envia {"configuredLevel":null}.
 */
@RestController
public class LoggingController {

	private static final Logger log = LoggerFactory.getLogger(LoggingController.class);

	@GetMapping("/logging")
	public String logExamples() {
		log.trace("Mensaje de nivel TRACE");
		log.debug("Mensaje de nivel DEBUG");
		log.info("Mensaje de nivel INFO");
		log.warn("Mensaje de nivel WARN");
		log.error("Mensaje de nivel ERROR");
		return "Revisa la consola: se han emitido cinco mensajes, uno por nivel.";
	}
}
