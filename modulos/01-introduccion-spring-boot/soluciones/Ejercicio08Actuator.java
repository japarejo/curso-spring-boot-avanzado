// =============================================================================
// Modulo 1 - Ejercicio 8: Actuator con indicadores e informacion propios.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Copia cada clase a apps/petclinic-api/src/main/java/.../configuration/
// =============================================================================

// ---------- configuration/FacturasHealthIndicator.java ----------
package org.springframework.samples.petclinic.configuration;

import java.time.Duration;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * El nombre del bean decide el nombre del indicador: "facturasHealthIndicator"
 * aparece en /actuator/health como "facturas" (Spring quita el sufijo).
 *
 * Dos detalles que importan en un indicador de verdad:
 *   - timeout corto: /actuator/health lo consultan balanceadores y orquestadores
 *     cada pocos segundos; si el indicador tarda 30 s, el health tarda 30 s;
 *   - nunca lanzar la excepcion hacia fuera: se traduce a DOWN con detalle.
 */
@Component
public class FacturasHealthIndicator implements HealthIndicator {

	private final RestClient restClient;

	private final String baseUrl;

	public FacturasHealthIndicator(RestClient.Builder builder, BillsProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(1));
		factory.setReadTimeout(Duration.ofSeconds(1));
		this.baseUrl = properties.baseUrl();
		this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
	}

	@Override
	public Health health() {
		try {
			restClient.get().uri("/actuator/health").retrieve().toBodilessEntity();
			return Health.up().withDetail("url", baseUrl).build();
		}
		catch (RuntimeException ex) {
			return Health.down().withDetail("url", baseUrl).withDetail("error", ex.getClass().getSimpleName()).build();
		}
	}

}

// ---------- configuration/ClinicaInfoContributor.java ----------
/*
package org.springframework.samples.petclinic.configuration;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.stereotype.Component;

@Component
public class ClinicaInfoContributor implements InfoContributor {

	private final PetService petService;

	public ClinicaInfoContributor(PetService petService) {
		this.petService = petService;
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("clinica", Map.of("mascotas", petService.findAllPets().size()));
	}

}
*/

// ---------- application.properties ----------
//   management.endpoint.health.probes.enabled=true
//   management.endpoint.health.group.externos.include=facturas
//   management.endpoint.health.group.externos.show-details=always
//   management.info.build.enabled=true      (activo por defecto si existe build-info.properties)
//
// ---------- apps/petclinic-api/pom.xml: informacion de compilacion ----------
//   <plugin>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-maven-plugin</artifactId>
//     <executions>
//       <execution>
//         <goals><goal>build-info</goal></goals>
//       </execution>
//     </executions>
//   </plugin>
//
// Genera META-INF/build-info.properties al compilar, y Actuator lo publica solo.
//
// Comprobacion:
//   curl -s localhost:8080/actuator/info | jq
//   curl -s localhost:8080/actuator/health/externos | jq
//
// Nota: findAllPets().size() carga todas las mascotas para contarlas. Sirve
// para el ejercicio; en un sistema real se anadiria `long count();` al
// repositorio (ver la nota de Ejercicio04Estadisticas.java).
