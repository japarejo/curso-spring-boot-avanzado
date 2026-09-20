package org.springframework.samples.petclinic.apiclients;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.samples.petclinic.configuration.BillsProperties;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * MODULO 2 - Consumo de APIs REST: las tres opciones, una al lado de otra.
 *
 * Esta clase es una PASARELA hacia un servicio externo, no un servicio de
 * dominio, y por eso esta anotada con @Component y vive junto a los clientes.
 * No es un detalle cosmetico: las rebanadas @DataJpaTest del modulo 4 incluyen
 * los beans @Service con
 *     @DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
 * y si esta clase fuera @Service, cada prueba de persistencia intentaria
 * levantar tambien los clientes HTTP. Una rebanada debe cargar solo lo suyo.
 *
 *   conRestTemplate  estilo clasico. RestTemplate esta en modo mantenimiento
 *                    desde Spring 5: no recibe funcionalidad nueva.
 *   conRestClient    opcion recomendada desde Spring Framework 6.1 en
 *                    aplicaciones sincronas. Misma API fluida que WebClient
 *                    pero sin arrastrar el modelo reactivo.
 *   conFeign         cliente declarativo: se declara la interfaz y el
 *                    framework genera la implementacion.
 *
 * Los tres atacan el mismo endpoint. En clase se ejecutan contra
 * /api/v1/bills-demo y se comparan legibilidad y trazas.
 *
 * Todos los metodos llevan cortocircuito: si el servicio de facturas esta
 * caido, se devuelve lista vacia en lugar de propagar el fallo.
 */
@Component
public class BillsGateway {

	private static final Logger log = LoggerFactory.getLogger(BillsGateway.class);

	private final BillsProperties properties;
	private final BillClient billClient;
	private final RestClient restClient;
	private final RestTemplate restTemplate;

	public BillsGateway(BillsProperties properties, BillClient billClient,
			RestClient.Builder restClientBuilder, RestTemplateBuilder restTemplateBuilder) {
		this.properties = properties;
		this.billClient = billClient;
		// Los builders que inyecta Spring Boot ya traen aplicados los
		// conversores de mensajes y los interceptores registrados en el contexto.
		// Crear `new RestTemplate()` a mano, como hacia el codigo anterior,
		// se salta toda esa configuracion.
		this.restClient = restClientBuilder
			.baseUrl(properties.baseUrl())
			.build();
		this.restTemplate = restTemplateBuilder
			.connectTimeout(properties.connectTimeout())
			.readTimeout(properties.readTimeout())
			.build();
	}

	/** Estilo clasico, mantenido aqui solo para poder compararlo. */
	@CircuitBreaker(name = "bills", fallbackMethod = "sinFacturas")
	public List<Bill> conRestTemplate() {
		Bill[] bills = restTemplate.getForObject(properties.baseUrl() + "/api/v1/bills", Bill[].class);
		return bills == null ? List.of() : Arrays.asList(bills);
	}

	/** Estilo recomendado hoy en aplicaciones sincronas. */
	@CircuitBreaker(name = "bills", fallbackMethod = "sinFacturas")
	public List<Bill> conRestClient() {
		return restClient.get()
			.uri("/api/v1/bills")
			.retrieve()
			.body(new ParameterizedTypeReference<List<Bill>>() {
			});
	}

	/** Estilo declarativo: el codigo de negocio no ve HTTP. */
	@CircuitBreaker(name = "bills", fallbackMethod = "sinFacturasConToken")
	public List<Bill> conFeign(String authorizationHeader) {
		return billClient.getBills(authorizationHeader);
	}

	// --- Metodos de reserva del cortocircuito --------------------------------
	// La firma debe coincidir con la del metodo protegido mas el Throwable final.

	List<Bill> sinFacturas(Throwable t) {
		log.warn("El servicio de facturas no responde ({}). Se devuelve lista vacia.", t.toString());
		return List.of();
	}

	List<Bill> sinFacturasConToken(String authorizationHeader, Throwable t) {
		return sinFacturas(t);
	}
}
