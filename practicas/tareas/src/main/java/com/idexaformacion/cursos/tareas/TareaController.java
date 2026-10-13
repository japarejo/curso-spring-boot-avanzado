package com.idexaformacion.cursos.tareas;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

/**
 * El PersonController de la diapositiva 29, reescrito con las practicas que se
 * ven en el modulo 2:
 *   - @GetMapping/@PostMapping... en lugar de @RequestMapping(method = ...);
 *   - @RequestBody en el POST (en la diapositiva falta, y sin el los datos
 *     llegan vacios);
 *   - 201 + cabecera Location al crear;
 *   - 404 cuando no existe, en lugar del .get() de un Optional vacio (que
 *     acabaria en un 500).
 *
 * Inyeccion por constructor: sin @Autowired, porque hay un unico constructor.
 *
 * Para no adelantar el modulo 2, aqui se devuelve la entidad. En el modulo 2 se
 * discute por que en una API real se devolveria un DTO.
 */
@RestController
@RequestMapping("/api/v1/tareas")
public class TareaController {

	private final TareaRepository repository;

	public TareaController(TareaRepository repository) {
		this.repository = repository;
	}

	@GetMapping
	public List<Tarea> listar(@RequestParam(defaultValue = "false") boolean soloPendientes) {
		return soloPendientes ? repository.findByHechoFalseOrderByFechaLimiteAsc() : repository.findAll();
	}

	@GetMapping("/vencidas")
	public List<Tarea> vencidas() {
		return repository.findByFechaLimiteBeforeAndHechoFalse(LocalDate.now());
	}

	@GetMapping("/{id}")
	public Tarea buscar(@PathVariable Integer id) {
		return repository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la tarea " + id));
	}

	@PostMapping
	public ResponseEntity<Tarea> crear(@Valid @RequestBody Tarea tarea) {
		tarea.setId(null); // el cliente no decide el identificador
		Tarea guardada = repository.save(tarea);
		return ResponseEntity.created(URI.create("/api/v1/tareas/" + guardada.getId())).body(guardada);
	}

	@PutMapping("/{id}")
	public Tarea actualizar(@PathVariable Integer id, @Valid @RequestBody Tarea cambios) {
		Tarea tarea = buscar(id);
		tarea.setDescripcion(cambios.getDescripcion());
		tarea.setFechaLimite(cambios.getFechaLimite());
		tarea.setHecho(cambios.isHecho());
		return repository.save(tarea);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void borrar(@PathVariable Integer id) {
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la tarea " + id);
		}
		repository.deleteById(id);
	}

}
