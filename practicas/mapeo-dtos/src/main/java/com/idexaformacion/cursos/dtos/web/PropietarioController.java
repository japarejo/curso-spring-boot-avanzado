package com.idexaformacion.cursos.dtos.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.idexaformacion.cursos.dtos.dto.PropietarioRequest;
import com.idexaformacion.cursos.dtos.dto.PropietarioResponse;
import com.idexaformacion.cursos.dtos.dto.PropietarioResumen;
import com.idexaformacion.cursos.dtos.mapper.PropietarioMapper;
import com.idexaformacion.cursos.dtos.modelo.Propietario;
import com.idexaformacion.cursos.dtos.servicio.PropietarioService;

/**
 * Entran y salen DTOs; el servicio recibe y devuelve entidades. El mapper
 * (inyectado, es un bean) hace la traduccion en los dos sentidos.
 *
 *   curl localhost:8080/propietarios
 *   curl localhost:8080/propietarios/1
 *   curl -X POST localhost:8080/propietarios -H "Content-Type: application/json" \
 *        -d '{"nombre":"Jean","apellidos":"Coleman","ciudad":"Monona"}'
 *   curl -X PATCH localhost:8080/propietarios/1 -H "Content-Type: application/json" \
 *        -d '{"telefono":"600000000"}'
 */
@RestController
@RequestMapping("/propietarios")
public class PropietarioController {

	private final PropietarioService service;

	private final PropietarioMapper mapper;

	public PropietarioController(PropietarioService service, PropietarioMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	public List<PropietarioResumen> listado() {
		return mapper.toResumenes(service.listar());
	}

	@GetMapping("/{id}")
	public PropietarioResponse detalle(@PathVariable int id) {
		return mapper.toResponse(service.buscar(id));
	}

	@PostMapping
	public ResponseEntity<PropietarioResponse> crear(@RequestBody PropietarioRequest request) {
		Propietario creado = service.guardar(mapper.toEntity(request));
		return ResponseEntity.created(URI.create("/propietarios/" + creado.getId()))
			.body(mapper.toResponse(creado));
	}

	@PatchMapping("/{id}")
	public PropietarioResponse modificar(@PathVariable int id, @RequestBody PropietarioRequest cambios) {
		Propietario propietario = service.buscar(id);
		mapper.actualizar(cambios, propietario);
		return mapper.toResponse(service.guardar(propietario));
	}

}
