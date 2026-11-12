package org.springframework.samples.petclinic.apiclients;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Bill;

/**
 * MODULO 2 - Consumo de APIs REST: contrato comun de la pasarela de facturas.
 *
 * Antes esta clase mezclaba los tres clientes HTTP y solo sabia hacer GET.
 * Ahora es un CONTRATO (el que ve el codigo de negocio) y hay una
 * implementacion por cada tecnologia, para poder compararlas lado a lado
 * haciendo exactamente las mismas operaciones:
 *
 *   BillsRestTemplateGateway  estilo clasico (modo mantenimiento desde Spring 5).
 *   BillsRestClientGateway    recomendado desde Spring Framework 6.1 (sincrono).
 *   BillsFeignGateway         cliente declarativo sobre la interfaz BillClient.
 *
 * Operaciones (verbo HTTP -> metodo):
 *   GET    /api/v1/bills          -> findAll
 *   GET    /api/v1/bills/{id}     -> findById
 *   HEAD   /api/v1/bills/{id}     -> exists
 *   POST   /api/v1/bills          -> create   (devuelve ResponseEntity: estado + Location)
 *   PUT    /api/v1/bills/{id}     -> update   (reemplazo completo)
 *   PATCH  /api/v1/bills/{id}     -> patch    (modificacion parcial)
 *   DELETE /api/v1/bills/{id}     -> delete
 *
 * Todas las operaciones reciben la cabecera Authorization (puede ser vacia).
 * Feign la necesita explicita; en los otros dos se reenvia solo si no esta
 * en blanco, para que el contrato sea el mismo en los tres.
 *
 * Es una pasarela hacia un servicio externo, no un servicio de dominio: las
 * implementaciones son @Component y NO @Service. Las rebanadas @DataJpaTest del
 * modulo 4 incluyen los beans @Service con
 *     @DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
 * y, si fueran @Service, cada prueba de persistencia intentaria levantar
 * tambien los clientes HTTP. Una rebanada debe cargar solo lo suyo.
 *
 * Resiliencia: las lecturas llevan cortocircuito con fallback (lista vacia /
 * Optional vacio / false). Las escrituras NO tienen fallback a proposito:
 * devolver "todo bien" ante un POST o DELETE que no se ha ejecutado seria
 * mentir al llamante. Un fallo de escritura se propaga como excepcion.
 * Tampoco se reintentan: POST y PATCH no son idempotentes y repetirlos podria
 * duplicar o aplicar dos veces el cambio. El reintento solo se demuestra sobre
 * la lectura (BillsRestClientGateway.findAllConReintentos).
 */
public interface BillsGateway {

	/** Nombre de la instancia de resilience4j (application.properties). */
	String RESILIENCE_INSTANCE = "bills";

	/** Ruta base del recurso en bills-service. */
	String BILLS_PATH = "/api/v1/bills";

	List<Bill> findAll(String authorization);

	/** Vacio si la factura no existe (404) o el servicio no responde. */
	java.util.Optional<Bill> findById(Integer id, String authorization);

	/** HEAD: comprueba la existencia sin descargar el cuerpo. */
	boolean exists(Integer id, String authorization);

	/** POST: 201 Created con la factura creada y su cabecera Location. */
	ResponseEntity<Bill> create(Bill bill, String authorization);

	/** PUT: reemplaza la factura completa. Idempotente. */
	Bill update(Integer id, Bill bill, String authorization);

	/** PATCH: cambia solo los campos enviados, p. ej. Map.of("amount", 12.5). */
	Bill patch(Integer id, Map<String, Object> changes, String authorization);

	/** DELETE: elimina la factura. Idempotente. */
	void delete(Integer id, String authorization);

}
