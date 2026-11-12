package org.springframework.samples.petclinic.web.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.apiclients.BillsFeignGateway;
import org.springframework.samples.petclinic.apiclients.BillsGateway;
import org.springframework.samples.petclinic.apiclients.BillsRestClientGateway;
import org.springframework.samples.petclinic.apiclients.BillsRestTemplateGateway;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * MODULO 2 - Comparativa en vivo de clientes HTTP, con todas las operaciones.
 *
 * La ruta incluye el cliente que se quiere usar ({cliente} = rest-template,
 * rest-client o feign), de modo que la MISMA operacion se lanza con las tres
 * tecnologias contra el mismo servicio y se comparan resultado y trazas:
 *
 *   GET    /api/v1/bills-demo/{cliente}                listar
 *   GET    /api/v1/bills-demo/{cliente}/{id}           consultar una
 *   HEAD   /api/v1/bills-demo/{cliente}/{id}           comprobar existencia
 *   POST   /api/v1/bills-demo/{cliente}                crear
 *   PUT    /api/v1/bills-demo/{cliente}/{id}           reemplazar
 *   PATCH  /api/v1/bills-demo/{cliente}/{id}           modificar parcialmente
 *   DELETE /api/v1/bills-demo/{cliente}/{id}           borrar
 *
 *   curl localhost:8080/api/v1/bills-demo/rest-client
 *   curl -X POST -H "Content-Type: application/json" \
 *        -d '{"concept":"Vacuna","amount":30}' localhost:8080/api/v1/bills-demo/rest-template
 *   curl -X DELETE -H "Authorization: Bearer xxx" localhost:8080/api/v1/bills-demo/feign/3
 *
 * Requiere bills-service levantado. En las lecturas, si no lo esta, el
 * cortocircuito de resilience4j devuelve un valor vacio en lugar de un error;
 * en las escrituras el fallo se propaga: no se puede fingir un POST o un
 * DELETE que no se ha hecho. Ambas cosas son parte de la demostracion.
 *
 * La cabecera Authorization se reenvia a bills-service con los tres clientes.
 *
 * Sustituye al BillController del proyecto anterior, que renderizaba una JSP y
 * llevaba un token JWT completo escrito a fuego en el codigo fuente.
 */
@RestController
@RequestMapping("/api/v1/bills-demo")
@Tag(name = "Bills demo", description = "Comparativa de clientes HTTP (RestTemplate, RestClient y Feign) con todos los verbos")
@SecurityRequirement(name = "bearer-jwt")
public class BillDemoRestController {

	private final BillsRestTemplateGateway restTemplateGateway;
	private final BillsRestClientGateway restClientGateway;
	private final BillsFeignGateway feignGateway;

	public BillDemoRestController(BillsRestTemplateGateway restTemplateGateway,
			BillsRestClientGateway restClientGateway, BillsFeignGateway feignGateway) {
		this.restTemplateGateway = restTemplateGateway;
		this.restClientGateway = restClientGateway;
		this.feignGateway = feignGateway;
	}

	// Literal: tiene prioridad sobre /{cliente} y se declara aparte porque es
	// una variante del cliente rest-client (modulo 5), no un cliente nuevo.
	@GetMapping("/rest-client-reintentos")
	@Operation(summary = "RestClient protegido con @Retry y @CircuitBreaker (modulo 5)",
		description = "Con bills-service parado se ve en el log cada reintento y la espera aplicada antes de devolver lista vacia.")
	public List<Bill> conRestClientYReintentos(@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		return restClientGateway.findAllConReintentos(authorization);
	}

	@GetMapping("/{cliente}")
	@Operation(summary = "GET: lista las facturas con el cliente elegido")
	public List<Bill> findAll(@PathVariable String cliente,
			@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		return gateway(cliente).findAll(authorization);
	}

	@GetMapping("/{cliente}/{id}")
	@Operation(summary = "GET: consulta una factura. 404 si no existe")
	public Bill findById(@PathVariable String cliente, @PathVariable Integer id,
			@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		return gateway(cliente).findById(id, authorization)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura " + id + " no encontrada"));
	}

	// Spring MVC atiende HEAD con el handler del GET, pero aqui se declara un
	// manejador propio para que llegue a exists() y se use un HEAD real hacia bills-service.
	@RequestMapping(method = org.springframework.web.bind.annotation.RequestMethod.HEAD, path = "/{cliente}/{id}")
	@Operation(summary = "HEAD: 200 si la factura existe, 404 si no (sin cuerpo)")
	public ResponseEntity<Void> exists(@PathVariable String cliente, @PathVariable Integer id,
			@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		return gateway(cliente).exists(id, authorization)
				? ResponseEntity.ok().build()
				: ResponseEntity.notFound().build();
	}

	@PostMapping("/{cliente}")
	@Operation(summary = "POST: crea una factura y devuelve 201 con la cabecera Location de bills-service")
	public ResponseEntity<Bill> create(@PathVariable String cliente, @Valid @RequestBody Bill bill,
			@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		ResponseEntity<Bill> remote = gateway(cliente).create(bill, authorization);
		ResponseEntity.BodyBuilder response = ResponseEntity.status(remote.getStatusCode());
		if (remote.getHeaders().getLocation() != null) {
			response.location(remote.getHeaders().getLocation());
		}
		return response.body(remote.getBody());
	}

	@PutMapping("/{cliente}/{id}")
	@Operation(summary = "PUT: reemplaza la factura completa")
	public Bill update(@PathVariable String cliente, @PathVariable Integer id, @Valid @RequestBody Bill bill,
			@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		return gateway(cliente).update(id, bill, authorization);
	}

	@PatchMapping("/{cliente}/{id}")
	@Operation(summary = "PATCH: modifica solo los campos enviados",
		description = "Ejemplo de cuerpo: {\"amount\": 12.5}. Con el cliente feign responde 501: el cliente HTTP por defecto de Feign no admite PATCH.")
	public Bill patch(@PathVariable String cliente, @PathVariable Integer id, @RequestBody Map<String, Object> changes,
			@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		try {
			return gateway(cliente).patch(id, changes, authorization);
		}
		catch (UnsupportedOperationException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, ex.getMessage(), ex);
		}
	}

	@DeleteMapping("/{cliente}/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "DELETE: elimina la factura")
	public void delete(@PathVariable String cliente, @PathVariable Integer id,
			@Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
		gateway(cliente).delete(id, authorization);
	}

	/** Elige la implementacion segun el segmento de la ruta. */
	private BillsGateway gateway(String cliente) {
		return switch (cliente) {
			case "rest-template" -> restTemplateGateway;
			case "rest-client" -> restClientGateway;
			case "feign" -> feignGateway;
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Cliente desconocido '" + cliente + "'. Use rest-template, rest-client o feign.");
		};
	}

}
