package org.springframework.samples.petclinic.web.api;

import java.util.List;

import org.springframework.samples.petclinic.model.Bill;
import org.springframework.samples.petclinic.apiclients.BillsGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * MODULO 2 - Comparativa en vivo de clientes HTTP.
 *
 * Un unico endpoint parametrizado por el cliente que se quiere usar, para poder
 * lanzar los tres contra el mismo servicio y comparar el resultado y las trazas:
 *
 *   curl localhost:8080/api/v1/bills-demo/rest-template
 *   curl localhost:8080/api/v1/bills-demo/rest-client
 *   curl -H "Authorization: Bearer xxx" localhost:8080/api/v1/bills-demo/feign
 *
 * Requiere que bills-service este levantado. Si no lo esta, el cortocircuito de
 * resilience4j devuelve lista vacia en lugar de un error: eso tambien es parte
 * de la demostracion.
 *
 * Sustituye al BillController del proyecto anterior, que renderizaba una JSP y
 * llevaba un token JWT completo escrito a fuego en el codigo fuente.
 */
@RestController
@RequestMapping("/api/v1/bills-demo")
@Tag(name = "Bills demo", description = "Comparativa de clientes HTTP: RestTemplate, RestClient y Feign")
public class BillDemoRestController {

	private final BillsGateway billsGateway;

	public BillDemoRestController(BillsGateway billsGateway) {
		this.billsGateway = billsGateway;
	}

	@GetMapping("/rest-template")
	@Operation(summary = "Consume el servicio de facturas con RestTemplate (estilo clasico)")
	public List<Bill> conRestTemplate() {
		return billsGateway.conRestTemplate();
	}

	@GetMapping("/rest-client")
	@Operation(summary = "Consume el servicio de facturas con RestClient (recomendado)")
	public List<Bill> conRestClient() {
		return billsGateway.conRestClient();
	}

	@GetMapping("/feign")
	@Operation(summary = "Consume el servicio de facturas con un cliente declarativo Feign")
	public List<Bill> conFeign(
			@RequestHeader(name = "Authorization", required = false, defaultValue = "") String authorization) {
		return billsGateway.conFeign(authorization);
	}

}
