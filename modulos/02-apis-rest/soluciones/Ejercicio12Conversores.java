// =============================================================================
// Modulo 2 - Ejercicio 12: conversores de texto a objetos de dominio.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================

// ---------- web/api/RangoFechas.java ----------
package org.springframework.samples.petclinic.web.api;

import java.time.LocalDate;

public record RangoFechas(LocalDate desde, LocalDate hasta) {

	public RangoFechas {
		if (desde.isAfter(hasta)) {
			throw new IllegalArgumentException("El rango empieza despues de terminar");
		}
	}

}

// ---------- web/api/RangoFechasConverter.java ----------
/*
package org.springframework.samples.petclinic.web.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// Spring Boot registra AUTOMATICAMENTE en Spring MVC cualquier bean de tipo
// Converter, GenericConverter o Formatter. No hace falta tocar WebMvcConfigurer.
@Component
public class RangoFechasConverter implements Converter<String, RangoFechas> {

	@Override
	public RangoFechas convert(String texto) {
		LocalDate hoy = LocalDate.now();
		return switch (texto.trim().toLowerCase()) {
			case "ultimos-30-dias" -> new RangoFechas(hoy.minusDays(30), hoy);
			case "este-anio" -> new RangoFechas(hoy.withDayOfYear(1), hoy);
			default -> desdeHasta(texto);
		};
	}

	private RangoFechas desdeHasta(String texto) {
		String[] partes = texto.split("\\.\\.");
		if (partes.length != 2) {
			throw new IllegalArgumentException("Formato esperado: aaaa-mm-dd..aaaa-mm-dd");
		}
		try {
			return new RangoFechas(LocalDate.parse(partes[0]), LocalDate.parse(partes[1]));
		}
		catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("Fecha no valida en el rango: " + texto, ex);
		}
	}
}
*/

// ---------- web/api/VisitRangoController.java ----------
/*
package org.springframework.samples.petclinic.web.api;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VisitRangoController {

	private final VisitService visitService;

	public VisitRangoController(VisitService visitService) {
		this.visitService = visitService;
	}

	// "/api/v1/visits/rango" convive con "/api/v1/visits/{id}": Spring elige la
	// ruta mas especifica, y un literal gana a una variable.
	@GetMapping("/api/v1/visits/rango")
	public List<VisitaResumen> porRango(@RequestParam RangoFechas rango) {
		return visitService.search(null, null, null, null, null, rango.desde(), rango.hasta(),
					PageRequest.of(0, 50, Sort.by("date")))
			.map(v -> new VisitaResumen(v.getId(), v.getDate().toString(), v.getDescription(), v.getPet().getId()))
			.getContent();
	}

	// Un record local como DTO: no se serializa la entidad (y asi no se toca
	// Visit.diagnose, que es perezosa).
	public record VisitaResumen(Integer id, String fecha, String descripcion, Integer petId) {
	}
}
*/

// ---------- Por que un rango mal escrito da 400 y no 500 ----------
// Si el conversor lanza IllegalArgumentException, Spring la envuelve en
// MethodArgumentTypeMismatchException. El GlobalExceptionHandler del proyecto la
// traduce a 400 (se anadio a handleBadRequest en la edicion de 40 horas; antes
// caia en el catch-all y daba 500).
//
// Comprobacion:
//   curl -s "localhost:8080/api/v1/visits/rango?rango=2013-01-01..2013-01-31" | jq
//   curl -s "localhost:8080/api/v1/visits/rango?rango=ultimos-30-dias" | jq
//   curl -i "localhost:8080/api/v1/visits/rango?rango=ayer"                 # 400
//
// Nota: el endpoint de demostracion del paso 2 (@PathVariable Pet pet) funciona
// por GenericIdToEntityConverter. Con un id inexistente devuelve null, de ahi
// el "no existe". Quitalo despues: no debe quedar en la API.
