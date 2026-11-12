package org.springframework.samples.petclinic.apiclients;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.configuration.BillsProperties;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * MODULO 2 - Cliente de facturas con RestTemplate (estilo clasico).
 *
 * RestTemplate esta en modo mantenimiento desde Spring 5: sigue funcionando
 * pero no recibe funcionalidad nueva. Se conserva para poder compararlo.
 *
 * Rasgos propios de este cliente, utiles para comentar en clase:
 *  - Hay metodos de atajo (getForObject, postForEntity, delete...) pero NO
 *    admiten cabeceras por peticion. En cuanto hace falta Authorization hay
 *    que bajar a exchange(url, metodo, HttpEntity, tipo), que es lo que se
 *    hace aqui en casi todas las operaciones.
 *  - Los codigos 4xx/5xx se traducen en excepciones (HttpClientErrorException,
 *    HttpServerErrorException) por el ResponseErrorHandler por defecto.
 *  - PATCH solo funciona si la fabrica de peticiones lo soporta. El
 *    RestTemplateBuilder de Spring Boot elige Apache HttpClient, Jetty o el
 *    HttpClient del JDK si estan disponibles; la fabrica "simple" basada en
 *    HttpURLConnection lanza "Invalid HTTP method: PATCH".
 *  - Un 404 en findById/exists NO es un fallo del servicio y se captura DENTRO
 *    del metodo: si saliera como excepcion, el cortocircuito lo contaria como
 *    fallo y acabaria abriendo el circuito por facturas que simplemente no existen.
 */
@Component
public class BillsRestTemplateGateway implements BillsGateway {

	private static final Logger log = LoggerFactory.getLogger(BillsRestTemplateGateway.class);

	private final RestTemplate restTemplate;

	public BillsRestTemplateGateway(BillsProperties properties, RestTemplateBuilder restTemplateBuilder) {
		// El builder inyectado por Spring Boot ya trae conversores e interceptores
		// del contexto; con `new RestTemplate()` se perderian.
		this.restTemplate = restTemplateBuilder
			.rootUri(properties.baseUrl())
			.connectTimeout(properties.connectTimeout())
			.readTimeout(properties.readTimeout())
			.build();
	}

	// --- Lecturas: con cortocircuito y valor de reserva -----------------------

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "listaVacia")
	public List<Bill> findAll(String authorization) {
		ResponseEntity<Bill[]> response = restTemplate.exchange(BILLS_PATH, HttpMethod.GET,
				new HttpEntity<>(headers(authorization)), Bill[].class);
		Bill[] bills = response.getBody();
		return bills == null ? List.of() : Arrays.asList(bills);
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "optionalVacio")
	public Optional<Bill> findById(Integer id, String authorization) {
		try {
			return Optional.ofNullable(restTemplate.exchange(BILLS_PATH + "/{id}", HttpMethod.GET,
					new HttpEntity<>(headers(authorization)), Bill.class, id).getBody());
		}
		catch (HttpClientErrorException.NotFound ex) {
			return Optional.empty();
		}
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "falso")
	public boolean exists(Integer id, String authorization) {
		try {
			// HEAD: solo cabeceras, sin cuerpo.
			return restTemplate.exchange(BILLS_PATH + "/{id}", HttpMethod.HEAD,
					new HttpEntity<>(headers(authorization)), Void.class, id).getStatusCode().is2xxSuccessful();
		}
		catch (HttpClientErrorException.NotFound ex) {
			return false;
		}
	}

	// --- Escrituras: sin fallback, el error llega al llamante -----------------

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public ResponseEntity<Bill> create(Bill bill, String authorization) {
		return restTemplate.exchange(BILLS_PATH, HttpMethod.POST,
				new HttpEntity<>(bill, headers(authorization)), Bill.class);
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public Bill update(Integer id, Bill bill, String authorization) {
		return restTemplate.exchange(BILLS_PATH + "/{id}", HttpMethod.PUT,
				new HttpEntity<>(bill, headers(authorization)), Bill.class, id).getBody();
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public Bill patch(Integer id, Map<String, Object> changes, String authorization) {
		return restTemplate.exchange(BILLS_PATH + "/{id}", HttpMethod.PATCH,
				new HttpEntity<>(changes, headers(authorization)), Bill.class, id).getBody();
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public void delete(Integer id, String authorization) {
		// restTemplate.delete(...) existe, pero no deja poner cabeceras.
		restTemplate.exchange(BILLS_PATH + "/{id}", HttpMethod.DELETE,
				new HttpEntity<>(headers(authorization)), Void.class, id);
	}

	// --- Apoyo ----------------------------------------------------------------

	private static HttpHeaders headers(String authorization) {
		HttpHeaders headers = new HttpHeaders();
		if (StringUtils.hasText(authorization)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorization);
		}
		return headers;
	}

	// Metodos de reserva: misma firma que el protegido mas el Throwable final.

	List<Bill> listaVacia(String authorization, Throwable t) {
		log.warn("[RestTemplate] bills-service no responde ({}). Se devuelve lista vacia.", t.toString());
		return List.of();
	}

	Optional<Bill> optionalVacio(Integer id, String authorization, Throwable t) {
		log.warn("[RestTemplate] bills-service no responde ({}). Se devuelve Optional vacio.", t.toString());
		return Optional.empty();
	}

	boolean falso(Integer id, String authorization, Throwable t) {
		log.warn("[RestTemplate] bills-service no responde ({}). Se asume que no existe.", t.toString());
		return false;
	}
}
