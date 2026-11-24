package org.springframework.samples.petclinic.apiclients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * MODULO 2 - Consumo de APIs: cliente declarativo con OpenFeign.
 *
 * Se declara la interfaz y el framework genera la implementacion. El codigo de
 * negocio deja de ver detalles de HTTP. Se reutilizan las anotaciones de Spring
 * MVC (@GetMapping, @PathVariable, @RequestBody...) con el significado inverso:
 * en un controlador describen lo que SE RECIBE; aqui, lo que SE ENVIA.
 *
 * Sobre la URL: si `petclinic.bills.base-url` esta vacia, Feign resuelve el
 * destino por nombre de servicio contra Eureka ("bills-service"). Con la
 * propiedad puesta, va directo a esa URL. Esto permite ejecutar la aplicacion
 * con y sin infraestructura de descubrimiento.
 *
 * Detalle importante sobre PATCH: el cliente HTTP por defecto de Feign usa
 * HttpURLConnection, que NO admite PATCH ("Invalid HTTP method: PATCH").
 * Para usarlo hay que anadir un cliente real (feign-hc5 -Apache HttpClient 5-,
 * feign-okhttp o feign-java11). Por eso BillsFeignGateway no lo expone y esta
 * declarado aqui, comentado, como ejercicio.
 *
 * Nota para Spring Boot 4.x: el equivalente nativo son las interfaces con
 * @HttpExchange registradas con @ImportHttpServices, sin dependencia de Feign.
 */
@FeignClient(value = "bills-service")
public interface BillClient {

	@GetMapping("/api/v1/bills")
	List<Bill> getBills(@RequestHeader("Authorization") String token);

	@GetMapping("/api/v1/bills/{id}")
	Bill getBill(@PathVariable("id") Integer id, @RequestHeader("Authorization") String token);

	/** ResponseEntity permite ver el estado (404 incluido) sin que Feign lance excepcion con HEAD. */
	@RequestMapping(method = RequestMethod.HEAD, value = "/api/v1/bills/{id}")
	ResponseEntity<Void> headBill(@PathVariable("id") Integer id, @RequestHeader("Authorization") String token);

	@PostMapping("/api/v1/bills")
	ResponseEntity<Bill> createBill(@RequestBody Bill bill, @RequestHeader("Authorization") String token);

	@PutMapping("/api/v1/bills/{id}")
	Bill updateBill(@PathVariable("id") Integer id, @RequestBody Bill bill,
			@RequestHeader("Authorization") String token);

	@DeleteMapping("/api/v1/bills/{id}")
	void deleteBill(@PathVariable("id") Integer id, @RequestHeader("Authorization") String token);

	// Requiere un cliente Feign distinto del de por defecto (ver Javadoc):
	// @PatchMapping("/api/v1/bills/{id}")
	// Bill patchBill(@PathVariable("id") Integer id, @RequestBody Map<String, Object> changes,
	//         @RequestHeader("Authorization") String token);   // import java.util.Map

}
