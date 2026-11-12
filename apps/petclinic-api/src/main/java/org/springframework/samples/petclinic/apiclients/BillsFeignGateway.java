package org.springframework.samples.petclinic.apiclients;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Bill;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * MODULO 2 - Cliente de facturas con Feign (estilo declarativo).
 *
 * Esta clase es una fina capa de adaptacion sobre la interfaz BillClient, que
 * es donde esta el "cliente" de verdad: todo el HTTP esta declarado con
 * anotaciones y el framework genera la implementacion. Aqui solo se aporta:
 *  - la traduccion al contrato comun BillsGateway,
 *  - el tratamiento del 404 (Feign lanza FeignException.NotFound),
 *  - el cortocircuito de resilience4j.
 *
 * Feign exige la cabecera Authorization como parametro; si llega en blanco se
 * pasa null, que Feign interpreta como "no enviar la cabecera".
 *
 * PATCH no esta soportado por el cliente HTTP por defecto de Feign
 * (HttpURLConnection). Ver el Javadoc de BillClient: para habilitarlo hay que
 * anadir feign-hc5, feign-okhttp o feign-java11. Es una limitacion real y
 * conocida que conviene que el alumnado conozca antes de elegir Feign.
 */
@Component
public class BillsFeignGateway implements BillsGateway {

	private static final Logger log = LoggerFactory.getLogger(BillsFeignGateway.class);

	private final BillClient billClient;

	public BillsFeignGateway(BillClient billClient) {
		this.billClient = billClient;
	}

	// --- Lecturas: con cortocircuito y valor de reserva -----------------------

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "listaVacia")
	public List<Bill> findAll(String authorization) {
		return billClient.getBills(token(authorization));
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "optionalVacio")
	public Optional<Bill> findById(Integer id, String authorization) {
		try {
			return Optional.ofNullable(billClient.getBill(id, token(authorization)));
		}
		catch (FeignException.NotFound ex) {
			return Optional.empty();
		}
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "falso")
	public boolean exists(Integer id, String authorization) {
		try {
			return billClient.headBill(id, token(authorization)).getStatusCode().is2xxSuccessful();
		}
		catch (FeignException.NotFound ex) {
			return false;
		}
	}

	// --- Escrituras: sin fallback, el error llega al llamante -----------------

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public ResponseEntity<Bill> create(Bill bill, String authorization) {
		return billClient.createBill(bill, token(authorization));
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public Bill update(Integer id, Bill bill, String authorization) {
		return billClient.updateBill(id, bill, token(authorization));
	}

	@Override
	public Bill patch(Integer id, Map<String, Object> changes, String authorization) {
		throw new UnsupportedOperationException(
				"PATCH no esta soportado por el cliente HTTP por defecto de Feign (HttpURLConnection). "
						+ "Anada feign-hc5, feign-okhttp o feign-java11 y declare el metodo en BillClient.");
	}

	@Override
	@CircuitBreaker(name = RESILIENCE_INSTANCE)
	public void delete(Integer id, String authorization) {
		billClient.deleteBill(id, token(authorization));
	}

	// --- Apoyo ----------------------------------------------------------------

	private static String token(String authorization) {
		return StringUtils.hasText(authorization) ? authorization : null;
	}

	// Metodos de reserva: misma firma que el protegido mas el Throwable final.

	List<Bill> listaVacia(String authorization, Throwable t) {
		log.warn("[Feign] bills-service no responde ({}). Se devuelve lista vacia.", t.toString());
		return List.of();
	}

	Optional<Bill> optionalVacio(Integer id, String authorization, Throwable t) {
		log.warn("[Feign] bills-service no responde ({}). Se devuelve Optional vacio.", t.toString());
		return Optional.empty();
	}

	boolean falso(Integer id, String authorization, Throwable t) {
		log.warn("[Feign] bills-service no responde ({}). Se asume que no existe.", t.toString());
		return false;
	}
}
