package com.idexaformacion.cursos.tareas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal tal cual la genera Spring Initializr.
 *
 * MODULO 1 - @SpringBootApplication = @Configuration + @EnableAutoConfiguration
 * + @ComponentScan. El escaneo empieza en ESTE paquete: cualquier clase fuera
 * de com.idexaformacion.cursos.tareas (o sus subpaquetes) no se detecta.
 *
 * No hace falta @EnableJpaRepositories (la diapositiva 165 lo menciona): la
 * autoconfiguracion de Spring Data lo activa sola al encontrar el starter.
 */
@SpringBootApplication
public class TareasApplication {

	public static void main(String[] args) {
		SpringApplication.run(TareasApplication.class, args);
	}

}
