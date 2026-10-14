package com.idexaformacion.cursos.dtos.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idexaformacion.cursos.dtos.modelo.Propietario;

/**
 * Trabaja solo con entidades: no conoce los DTOs ni el mapper. La traduccion
 * es cosa de la capa web.
 */
@Service
public class PropietarioService {

	private final PropietarioRepository repository;

	public PropietarioService(PropietarioRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<Propietario> listar() {
		return repository.findAllByOrderByApellidos();
	}

	@Transactional(readOnly = true)
	public Propietario buscar(int id) {
		return repository.findConMascotasById(id)
			.orElseThrow(() -> new PropietarioNoEncontradoException(id));
	}

	@Transactional
	public Propietario guardar(Propietario propietario) {
		return repository.save(propietario);
	}

}
