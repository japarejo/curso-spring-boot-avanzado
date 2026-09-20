package org.springframework.samples.petclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Punto de entrada de la aplicacion.
 *
 * MODULO 1 - @SpringBootApplication es en realidad tres anotaciones:
 *   @Configuration        esta clase puede declarar beans con @Bean
 *   @EnableAutoConfiguration  Spring Boot configura lo que encuentra en el classpath
 *   @ComponentScan        escanea este paquete y los que cuelgan de el
 *
 * Nota para clase: @EnableDiscoveryClient ya no hace falta. Desde Spring Cloud
 * 2020 basta con tener el starter de Eureka en el classpath; el registro se
 * activa o desactiva con la propiedad eureka.client.enabled (modulo 5).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients(basePackages = { "org.springframework.samples.petclinic.apiclients" })
public class PetclinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetclinicApplication.class, args);
	}

}
