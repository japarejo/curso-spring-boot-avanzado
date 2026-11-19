package org.springframework.samples.petclinic.roles.web;

import java.util.List;

import org.springframework.samples.petclinic.roles.service.GestionRolesService;
import org.springframework.samples.petclinic.roles.service.GestionRolesService.RolDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Protegido dos veces, a proposito, para comparar los dos niveles:
 * <ul>
 * <li>Por RUTA: {@code /api/admin/**} exige {@code hasRole('ADMIN')}
 * (en {@code SecurityConfiguration}).</li>
 * <li>Por METODO: conceder y retirar exigen el privilegio
 * {@code SEGURIDAD_GESTIONAR} (en {@code GestionRolesService}).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/roles")
public class AdminRolesController {

	private final GestionRolesService gestion;

	public AdminRolesController(GestionRolesService gestion) {
		this.gestion = gestion;
	}

	@GetMapping
	public List<RolDto> listar() {
		return gestion.listar();
	}

	@PutMapping("/{rol}/privilegios/{privilegio}")
	public RolDto conceder(@PathVariable String rol, @PathVariable String privilegio) {
		return gestion.conceder(rol, privilegio);
	}

	@DeleteMapping("/{rol}/privilegios/{privilegio}")
	public RolDto retirar(@PathVariable String rol, @PathVariable String privilegio) {
		return gestion.retirar(rol, privilegio);
	}

}
