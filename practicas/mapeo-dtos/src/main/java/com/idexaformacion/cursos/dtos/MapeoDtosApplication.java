package com.idexaformacion.cursos.dtos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mapeo entidad <-> DTO con MapStruct.
 *
 *   modelo     Entidades JPA (Propietario, Mascota, Usuario). Nunca salen por la API.
 *   dto        Records de entrada y salida.
 *   mapper     PropietarioMapper: solo la interfaz. La implementacion la escribe
 *              MapStruct al compilar (target/generated-sources/annotations/).
 *   servicio   Trabaja con entidades, no sabe nada de DTOs.
 *   web        El controlador traduce entre DTOs y entidades con el mapper.
 *
 * Arranque: ./mvnw spring-boot:run y luego GET localhost:8080/propietarios
 */
@SpringBootApplication
public class MapeoDtosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapeoDtosApplication.class, args);
	}

}
