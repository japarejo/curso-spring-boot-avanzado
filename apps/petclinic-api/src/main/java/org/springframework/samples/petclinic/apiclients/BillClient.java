package org.springframework.samples.petclinic.apiclients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * MODULO 2 - Consumo de APIs: cliente declarativo con OpenFeign.
 *
 * Se declara la interfaz y el framework genera la implementacion. El codigo de
 * negocio deja de ver detalles de HTTP.
 *
 * Sobre la URL: si `petclinic.bills.base-url` esta vacia, Feign resuelve el
 * destino por nombre de servicio contra Eureka ("bills-service"). Con la
 * propiedad puesta, va directo a esa URL. Esto permite ejecutar la aplicacion
 * con y sin infraestructura de descubrimiento.
 *
 * Nota para Spring Boot 4.x: el equivalente nativo son las interfaces con
 * @HttpExchange registradas con @ImportHttpServices, sin dependencia de Feign.
 */
@FeignClient(value = "bills-service", url = "${petclinic.bills.base-url:}")
public interface BillClient {

	@GetMapping("/api/v1/bills")
	List<Bill> getBills(@RequestHeader("Authorization") String token);
}
