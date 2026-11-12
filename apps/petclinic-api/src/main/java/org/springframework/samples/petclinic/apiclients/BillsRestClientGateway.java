package org.springframework.samples.petclinic.apiclients;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.configuration.BillsProperties;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * MODULO 2 - Cliente de facturas con RestClient (recomendado en apps sincronas).
 *
 * Disponible desde Spring Framework 6.1. Tiene la misma API fluida que
 * WebClient pero sin arrastrar el modelo reactivo, y cada verbo HTTP tiene su
 * metodo (get, head, post, put, patch, delete), con cabeceras por peticion.
 *
 * Dos formas de tratar la respuesta, ambas usadas aqui:
 *  - retrieve(): camino corto. Los 4xx/5xx se convierten en excepciones.
 *  - exchange(): control total sobre la respuesta (estado, cabeceras, cuerpo).
 *    Se usa en findById y exists para tratar el 404 como un resultado normal
 *    y no como una excepcion (que el cortocircuito contaria como fallo).
 *
 * El builder inyectado por Spring Boot ya trae conversores, interceptores y la
 * fabrica de peticiones configurada (con soporte de PATCH).
 */
@Component
public class BillsRestClientGateway implements BillsGateway {

	private static final Logger log = LoggerFactory.getLogger(BillsRestClientGateway.class);

	private final RestClient restClient;

	public BillsRestClientGateway(BillsProperties properties, RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder
			.baseUrl(properties.baseUrl())
			.build();
	}

	// --- Lecturas: con cortocircuito y valor de reserva -----------------------

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "listaVacia")
	public List<Bill> findAll(String authorization) {
		return pedirFacturas(authorization);
	}

	/**
	 * MODULO 5 - Reintento + cortocircuito sobre una llamada HTTP real.
	 *
	 * Orden por defecto de los aspectos de resilience4j (de fuera a dentro):
	 *     Retry( CircuitBreaker( RateLimiter( TimeLimiter( Bulkhead( metodo )))))
	 * Cada reintento pasa por el cortocircuito y cuenta en sus estadisticas.
	 *
	 * Por eso el fallback va en @Retry (el exterior) y NO en @CircuitBreaker:
	 * si el cortocircuito tuviera fallback, se tragaria la excepcion y el
	 * reintento nunca llegaria a verla. Y por eso la instancia "bills" de retry
	 * ignora CallNotPermittedException: con el circuito abierto no tiene
	 * sentido insistir.
	 *
	 * Politica en application.properties: resilience4j.retry.instances.bills.*
	 */
	@Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "listaVacia")
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public List<Bill> findAllConReintentos(String authorization) {
		// Se llama a un metodo privado y no a findAll(): una llamada interna
		// (this.findAll) se salta el proxy y con ella los aspectos.
		return pedirFacturas(authorization);
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "optionalVacio")
	public Optional<Bill> findById(Integer id, String authorization) {
		return Optional.ofNullable(restClient.get()
			.uri(BILLS_PATH + "/{id}", id)
			.headers(h -> autorizar(h, authorization))
			.exchange((request, response) -> response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
					? null
					: response.bodyTo(Bill.class)));
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "falso")
	public boolean exists(Integer id, String authorization) {
		return Boolean.TRUE.equals(restClient.head()
			.uri(BILLS_PATH + "/{id}", id)
			.headers(h -> autorizar(h, authorization))
			.exchange((request, response) -> response.getStatusCode().is2xxSuccessful()));
	}

	// --- Escrituras: sin fallback, el error llega al llamante -----------------

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public ResponseEntity<Bill> create(Bill bill, String authorization) {
		return restClient.post()
			.uri(BILLS_PATH)
			.headers(h -> autorizar(h, authorization))
			.body(bill)
			.retrieve()
			.toEntity(Bill.class);
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public Bill update(Integer id, Bill bill, String authorization) {
		return restClient.put()
			.uri(BILLS_PATH + "/{id}", id)
			.headers(h -> autorizar(h, authorization))
			.body(bill)
			.retrieve()
			.body(Bill.class);
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public Bill patch(Integer id, Map<String, Object> changes, String authorization) {
		return restClient.patch()
			.uri(BILLS_PATH + "/{id}", id)
			.headers(h -> autorizar(h, authorization))
			.body(changes)
			.retrieve()
			.body(Bill.class);
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public void delete(Integer id, String authorization) {
		restClient.delete()
			.uri(BILLS_PATH + "/{id}", id)
			.headers(h -> autorizar(h, authorization))
			.retrieve()
			.toBodilessEntity();
	}

	// --- Apoyo ----------------------------------------------------------------

	private List<Bill> pedirFacturas(String authorization) {
		return restClient.get()
			.uri(BILLS_PATH)
			.headers(h -> autorizar(h, authorization))
			.retrieve()
			.body(new ParameterizedTypeReference<List<Bill>>() {});
	}

	private static void autorizar(HttpHeaders headers, String authorization) {
		if (StringUtils.hasText(authorization)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorization);
		}
	}

	// Metodos de reserva: misma firma que el protegido mas el Throwable final.

	List<Bill> listaVacia(String authorization, Throwable t) {
		log.warn("[RestClient] bills-service no responde ({}). Se devuelve lista vacia.", t.toString());
		return List.of();
	}

	Optional<Bill> optionalVacio(Integer id, String authorization, Throwable t) {
		log.warn("[RestClient] bills-service no responde ({}). Se devuelve Optional vacio.", t.toString());
		return Optional.empty();
	}

	boolean falso(Integer id, String authorization, Throwable t) {
		log.warn("[RestClient] bills-service no responde ({}). Se asume que no existe.", t.toString());
		return false;
	}
}
