package org.springframework.samples.petclinic.roles.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.roles.model.Privilegio;
import org.springframework.samples.petclinic.roles.model.Rol;
import org.springframework.samples.petclinic.roles.repository.PrivilegioRepository;
import org.springframework.samples.petclinic.roles.repository.RolRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Administracion de la tabla {@code roles_privilegios}. Es la gran ventaja del
 * modelo rol/privilegio: cambiar QUIEN puede hacer algo es un dato, no un
 * despliegue.
 */
@Service
public class GestionRolesService {

	public record RolDto(String nombre, List<String> privilegios) {
	}

	private final RolRepository roles;
	private final PrivilegioRepository privilegios;

	public GestionRolesService(RolRepository roles, PrivilegioRepository privilegios) {
		this.roles = roles;
		this.privilegios = privilegios;
	}

	@Transactional(readOnly = true)
	public List<RolDto> listar() {
		return roles.findAll().stream()
				.sorted(Comparator.comparing(Rol::getNombre))
				.map(GestionRolesService::aDto)
				.toList();
	}

	@PreAuthorize("hasAuthority('SEGURIDAD_GESTIONAR')")
	@Transactional
	public RolDto conceder(String rol, String privilegio) {
		Rol r = buscarRol(rol);
		r.getPrivilegios().add(buscarPrivilegio(privilegio));
		return aDto(r);
	}

	@PreAuthorize("hasAuthority('SEGURIDAD_GESTIONAR')")
	@Transactional
	public RolDto retirar(String rol, String privilegio) {
		Rol r = buscarRol(rol);
		r.getPrivilegios().removeIf(p -> p.getNombre().equals(privilegio));
		return aDto(r);
	}

	/** Admite "RECEPCION" o "ROLE_RECEPCION". */
	private Rol buscarRol(String nombre) {
		String conPrefijo = nombre.startsWith(Rol.PREFIJO) ? nombre : Rol.PREFIJO + nombre;
		return roles.findByNombre(conPrefijo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el rol " + conPrefijo));
	}

	private Privilegio buscarPrivilegio(String nombre) {
		return privilegios.findByNombre(nombre)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el privilegio " + nombre));
	}

	private static RolDto aDto(Rol rol) {
		return new RolDto(rol.getNombre(),
				rol.getPrivilegios().stream().map(Privilegio::getNombre).sorted().toList());
	}

}
