package org.springframework.samples.petclinic.web.api;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.samples.petclinic.repository.criteria.BusquedaMascotasRepository;
import org.springframework.samples.petclinic.repository.criteria.FichaMascota;
import org.springframework.samples.petclinic.repository.criteria.FiltroMascotas;
import org.springframework.samples.petclinic.service.BusquedaMascotasService;
import org.springframework.samples.petclinic.service.BusquedaMascotasService.OpcionesBusqueda;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ESCENARIO DE CRITERIA API: busqueda avanzada de pacientes (modulo 6).
 *
 * Lo consume la pantalla /resources/busqueda-mascotas.html. Cada filtro activado
 * en la pantalla se convierte en un parametro; los desactivados no se envian.
 *
 *   GET /api/v1/busqueda-mascotas?tipos=cat&tipos=dog&ciudades=Madison&minVisitas=1
 *       &page=0&size=10&sort=ultimaVisita,desc
 */
@RestController
@RequestMapping("/api/v1/busqueda-mascotas")
@Tag(name = "Busqueda avanzada de mascotas", description = "Escenario de la Criteria API (modulo 6)")
public class BusquedaMascotasRestController {

	private static final int TAMANO_MAXIMO = 50;

	private final BusquedaMascotasService servicio;

	public BusquedaMascotasRestController(BusquedaMascotasService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Busca mascotas combinando cualquier subconjunto de los 13 criterios",
			description = "Todos los criterios son opcionales. Orden admitido: nombre, tipo, fechaNacimiento, "
					+ "propietario, ciudad, numeroVisitas, ultimaVisita. Mira el SQL generado en la consola.")
	public ResultadoBusqueda buscar(@ParameterObject FiltroMascotas filtro,
			@ParameterObject @PageableDefault(size = 10, sort = "nombre") Pageable pagina) {
		for (Sort.Order orden : pagina.getSort()) {
			if (!BusquedaMascotasRepository.CRITERIOS_DE_ORDEN.contains(orden.getProperty())) {
				throw new BadRequestException("No se puede ordenar por '" + orden.getProperty()
						+ "'. Valores admitidos: " + BusquedaMascotasRepository.CRITERIOS_DE_ORDEN);
			}
		}
		Pageable acotada = PageRequest.of(pagina.getPageNumber(), Math.min(pagina.getPageSize(), TAMANO_MAXIMO),
				pagina.getSort());
		return ResultadoBusqueda.de(servicio.buscar(filtro, acotada));
	}

	@GetMapping("/opciones")
	@Operation(summary = "Valores posibles de los desplegables de la pantalla de busqueda")
	public OpcionesBusqueda opciones() {
		return servicio.opciones();
	}

	/**
	 * Respuesta propia en lugar de serializar Page: el JSON de PageImpl no es
	 * estable entre versiones de Spring Data, y el propio Spring Data lo avisa.
	 */
	public record ResultadoBusqueda(List<FichaMascota> contenido, int pagina, int tamano, long totalElementos,
			int totalPaginas) {

		static ResultadoBusqueda de(Page<FichaMascota> page) {
			return new ResultadoBusqueda(page.getContent(), page.getNumber(), page.getSize(),
					page.getTotalElements(), page.getTotalPages());
		}
	}

}
