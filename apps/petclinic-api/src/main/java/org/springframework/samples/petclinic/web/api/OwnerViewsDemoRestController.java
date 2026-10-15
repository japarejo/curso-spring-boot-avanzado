package org.springframework.samples.petclinic.web.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.boot.actuate.web.exchanges.HttpExchange.Principal;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.service.OwnerService;
import org.springframework.samples.petclinic.web.api.dto.OwnerResponse;
import org.springframework.samples.petclinic.web.api.dto.VistasPropietario;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * MODULO 2 - Varias representaciones del mismo objeto con @JsonView.
 *
 * El mismo OwnerResponse sale con mas o menos campos segun la vista:
 *
 *   1. Vista FIJA, decidida al compilar con @JsonView en el metodo:
 *        curl localhost:8080/api/v1/views-demo/owners          # Publica
 *        curl localhost:8080/api/v1/views-demo/owners/1        # Recepcion
 *
 *   2. Vista DINAMICA, decidida en cada peticion con MappingJacksonValue:
 *        curl "localhost:8080/api/v1/views-demo/owners/1/por-rol?rol=PUBLICO"
 *        curl "localhost:8080/api/v1/views-demo/owners/1/por-rol?rol=ADMIN"
 *
 * AVISO: en el caso 2 el rol lo dice el propio cliente, asi que cualquiera puede
 * pedir la vista ADMIN. Sirve para ver el mecanismo, NO es seguridad. En una API
 * real el rol se saca del usuario autenticado (Authentication/SecurityContext,
 * modulo 3), nunca de un parametro.
 */
@RestController
@RequestMapping("/api/v1/views-demo/owners")
@Tag(name = "Views demo", description = "Mismo DTO, distintas representaciones: @JsonView y MappingJacksonValue")
public class OwnerViewsDemoRestController {

	/**
	 * Cada rol apunta a la vista que le corresponde. Spring convierte el texto del
	 * parametro al enum por su nombre exacto: "ADMIN" vale, "admin" o "jefe" dan 400.
	 */
	public enum Rol {

		PUBLICO(VistasPropietario.Publica.class),
		RECEPCION(VistasPropietario.Recepcion.class),
		ADMIN(VistasPropietario.Admin.class),
		CUSTOM(VistasPropietario.Custom.class	);

		private final Class<?> vista;

		Rol(Class<?> vista) {
			this.vista = vista;
		}

		public Class<?> getVista() {
			return vista;
		}

	}

	private final OwnerService ownerService;

	public OwnerViewsDemoRestController(OwnerService ownerService) {
		this.ownerService = ownerService;
	}

	// --- 1. Vista fija con @JsonView ------------------------------------------------

	@GetMapping
	@JsonView(VistasPropietario.Publica.class)
	@Operation(summary = "Listado con la vista Publica (id, nombre, apellidos, ciudad)")
	public List<OwnerResponse> listado() {
		// "" porque la consulta es "lastName LIKE :lastName%": devuelve todos.
		return ownerService.findOwnerByLastName("").stream().map(OwnerResponse::from).toList();
	}

	@GetMapping("/{id}")
	@JsonView(VistasPropietario.Recepcion.class)
	@Operation(summary = "Detalle con la vista Recepcion (anade direccion y telefono)")
	public OwnerResponse detalle(@PathVariable("id") int id) {
		return OwnerResponse.from(buscar(id));
	}

	// --- 2. Vista dinamica con MappingJacksonValue --------------------------------

	@GetMapping("/{id}/por-rol")
	@Operation(summary = "Detalle con la vista que corresponde al rol recibido",
		description = "PUBLICO, RECEPCION o ADMIN. Solo demostrativo: el rol NO debe venir del cliente.")
	// El tipo de retorno MappingJacksonValue no dice nada a springdoc: se documenta a mano.
	@ApiResponse(responseCode = "200", description = "Owner con los campos de la vista elegida",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = OwnerResponse.class)))
	public MappingJacksonValue porRol(@PathVariable("id") int id,
			@Parameter(description = "Rol que decide la vista")
			@RequestParam(name = "rol", defaultValue = "PUBLICO") Rol rol, Principal principal) {
		// Envuelve el objeto y le adjunta la vista. El conversor Jackson de Spring
		// la lee al serializar: es lo mismo que hace @JsonView, pero elegido en
		// tiempo de ejecucion.
		MappingJacksonValue respuesta = new MappingJacksonValue(OwnerResponse.from(buscar(id)));
		respuesta.setSerializationView(VistasPropietario.Custom.class);
		return respuesta;
	}

	private Owner buscar(int id) {
		Owner owner = ownerService.findOwnerById(id);
		if (owner == null) {
			throw new ResourceNotFoundException("No existe el propietario " + id);
		}
		return owner;
	}

}
