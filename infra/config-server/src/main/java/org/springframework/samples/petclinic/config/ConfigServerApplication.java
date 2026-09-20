package org.springframework.samples.petclinic.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * MODULO 5 - Servidor de configuracion (Spring Cloud Config).
 *
 * Centraliza la configuracion de todas las aplicaciones y entornos en un unico
 * sitio, de modo que el artefacto desplegado sea siempre el mismo y lo unico que
 * cambie sea de donde lee sus propiedades.
 *
 * API de consulta:
 *   /{aplicacion}/{perfil}
 *   /{aplicacion}-{perfil}.properties
 *   /{etiqueta}/{aplicacion}-{perfil}.yml
 *
 * Ejemplo con la configuracion de este repositorio:
 *   curl http://localhost:8889/bills-service/development
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}
}
