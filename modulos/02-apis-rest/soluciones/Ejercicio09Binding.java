// =============================================================================
// Modulo 2 - Ejercicio 9: binding de parametros, cabeceras y cookies.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Copiar a apps/petclinic-api/src/main/java/.../web/api/OwnerQueryController.java
//
// Para no adelantar el modulo 6, el filtrado, la ordenacion y la paginacion se
// hacen EN MEMORIA sobre lo que devuelve OwnerService. Con muchos propietarios
// esto seria inaceptable: se haria en la consulta con Pageable y Specification
// (modulo 6, ejercicio 4). Merece la pena decirlo en clase.
// =============================================================================
package org.springframework.samples.petclinic.web.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.service.OwnerService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owners")
public class OwnerQueryController {

	private static final int LIMITE_MAXIMO = 50;

	/**
	 * LISTA BLANCA. Unica fuente de verdad para `sort` y `fields`: solo lo que
	 * aparece aqui se puede pedir. Ni `telephone` ni `user` estan, a proposito.
	 */
	private static final Map<String, Function<Owner, Object>> CAMPOS = new LinkedHashMap<>();
	static {
		CAMPOS.put("id", Owner::getId);
		CAMPOS.put("firstName", Owner::getFirstName);
		CAMPOS.put("lastName", Owner::getLastName);
		CAMPOS.put("city", Owner::getCity);
		CAMPOS.put("address", Owner::getAddress);
	}

	private final OwnerService ownerService;

	public OwnerQueryController(OwnerService ownerService) {
		this.ownerService = ownerService;
	}

	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> buscar(
			@RequestParam(required = false) String city,
			@RequestParam(defaultValue = "") String lastName,
			@RequestParam(defaultValue = "+lastName") String sort,
			@RequestParam(required = false) List<String> fields,
			@RequestParam(defaultValue = "0") int offset,
			@RequestParam(defaultValue = "10") int limit,
			@RequestHeader(name = "Accept-Language", defaultValue = "es") String idioma,
			@CookieValue(name = "ultimaCiudad", required = false) String ultimaCiudad) {

		String ciudad = city != null ? city : ultimaCiudad;
		List<String> columnas = fields == null ? new ArrayList<>(CAMPOS.keySet()) : validar(fields);
		int tope = Math.min(Math.max(limit, 1), LIMITE_MAXIMO);

		List<Map<String, Object>> resultado = ownerService.findOwnerByLastName(lastName).stream()
			.filter(o -> ciudad == null || ciudad.equalsIgnoreCase(o.getCity()))
			.sorted(ordenacion(sort))
			.skip(Math.max(offset, 0))
			.limit(tope)
			.map(o -> seleccionar(o, columnas))
			.toList();

		String saludo = idioma.toLowerCase().startsWith("en") ? "Hello" : "Hola";
		return ResponseEntity.ok().header("X-Saludo", saludo).body(resultado);
	}

	/** "+lastName,-city" -> comparador compuesto. */
	private Comparator<Owner> ordenacion(String sort) {
		Comparator<Owner> comparador = null;
		for (String criterio : sort.split(",")) {
			String limpio = criterio.trim();
			// Un '+' en la URL llega como espacio si no se codifica (%2B). Se tolera.
			boolean descendente = limpio.startsWith("-");
			String campo = limpio.replaceFirst("^[+\\- ]", "");
			Function<Owner, Object> extractor = extractor(campo);
			Comparator<Owner> este = Comparator.comparing(o -> String.valueOf(extractor.apply(o)),
					String.CASE_INSENSITIVE_ORDER);
			if (descendente) {
				este = este.reversed();
			}
			comparador = comparador == null ? este : comparador.thenComparing(este);
		}
		return comparador;
	}

	private List<String> validar(List<String> campos) {
		// `fields=a,b` llega como lista de un elemento o de dos segun el cliente.
		List<String> limpios = campos.stream()
			.flatMap(c -> Arrays.stream(c.split(",")))
			.map(String::trim)
			.toList();
		limpios.forEach(this::extractor);
		return limpios;
	}

	private Function<Owner, Object> extractor(String campo) {
		Function<Owner, Object> f = CAMPOS.get(campo);
		if (f == null) {
			// GlobalExceptionHandler lo traduce a 400 con ProblemDetail.
			throw new BadRequestException("Campo no permitido: " + campo + ". Permitidos: " + CAMPOS.keySet());
		}
		return f;
	}

	private Map<String, Object> seleccionar(Owner owner, List<String> columnas) {
		Map<String, Object> fila = new LinkedHashMap<>();
		columnas.forEach(c -> fila.put(c, CAMPOS.get(c).apply(owner)));
		return fila;
	}

}

// Comprobacion rapida de que '+' necesita codificarse en la URL:
//   curl -s "localhost:8080/api/v1/owners?sort=%2BlastName,-city" | jq
//   curl -s "localhost:8080/api/v1/owners?sort=+lastName"         # llega " lastName"
