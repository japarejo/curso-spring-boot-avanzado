package org.springframework.samples.petclinic.roles.web;

import java.util.Collection;
import java.util.List;

import org.springframework.samples.petclinic.roles.model.Rol;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Quien soy y que puedo hacer". La pieza mas util para entender el modulo:
 * ensena lado a lado lo que hay GUARDADO en el {@link Authentication} y lo que
 * resulta de aplicarle la jerarquia, que es lo que usan las decisiones.
 */
@RestController
public class YoController {

	public record Yo(String usuario,
			List<String> rolesAsignados,
			List<String> privilegios,
			List<String> autoridadesEfectivas) {
	}

	private final RoleHierarchy jerarquia;

	public YoController(RoleHierarchy jerarquia) {
		this.jerarquia = jerarquia;
	}

	@GetMapping("/api/yo")
	public Yo yo(Authentication autenticacion) {
		List<String> guardadas = nombres(autenticacion.getAuthorities());
		return new Yo(autenticacion.getName(),
				guardadas.stream().filter(a -> a.startsWith(Rol.PREFIJO)).toList(),
				guardadas.stream().filter(a -> !a.startsWith(Rol.PREFIJO)).toList(),
				nombres(jerarquia.getReachableGrantedAuthorities(autenticacion.getAuthorities())));
	}

	private static List<String> nombres(Collection<? extends GrantedAuthority> autoridades) {
		return autoridades.stream().map(GrantedAuthority::getAuthority).sorted().toList();
	}

}
