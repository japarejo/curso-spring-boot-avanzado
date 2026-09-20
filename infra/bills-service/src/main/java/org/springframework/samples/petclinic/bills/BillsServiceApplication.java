package org.springframework.samples.petclinic.bills;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MODULO 5 - Aplicacion cliente de la infraestructura del curso.
 *
 * Es el nodo sobre el que se demuestra todo el modulo 5:
 *   - se registra en Eureka (registry, 8761)
 *   - lee su configuracion del Config Server (8889)
 *   - aparece en el panel de Spring Boot Admin (9090)
 *   - recarga propiedades en caliente con @RefreshScope y /actuator/refresh
 *
 * Nota: @EnableDiscoveryClient ya no es necesario desde Spring Cloud 2020.
 * Basta con tener el starter de Eureka en el classpath.
 */
@SpringBootApplication
public class BillsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillsServiceApplication.class, args);
	}
}
