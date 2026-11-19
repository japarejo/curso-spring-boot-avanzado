package org.springframework.samples.petclinic.roles.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * La jerarquia aislada, sin arrancar Spring: es un objeto que, dadas unas
 * autoridades, devuelve las "alcanzables".
 */
class JerarquiaDeRolesTest {

	private final RoleHierarchy jerarquia = SecurityConfiguration.roleHierarchy();

	@Test
	void adminAlcanzaTodosLosRolesPorAmbasRamas() {
		assertThat(alcanzables("ROLE_ADMIN")).containsExactlyInAnyOrder(
				"ROLE_ADMIN", "ROLE_RECEPCION", "ROLE_VETERINARIO", "ROLE_EMPLEADO");
	}

	@Test
	void laJerarquiaSoloBajaNuncaSube() {
		assertThat(alcanzables("ROLE_EMPLEADO")).containsExactly("ROLE_EMPLEADO");
		assertThat(alcanzables("ROLE_RECEPCION"))
				.containsExactlyInAnyOrder("ROLE_RECEPCION", "ROLE_EMPLEADO")
				.doesNotContain("ROLE_VETERINARIO");
	}

	@Test
	void losPrivilegiosPasanSinCambios() {
		// La jerarquia no sabe que ROLE_EMPLEADO "contiene" MASCOTA_LEER:
		// esa relacion esta en la base de datos, no aqui.
		assertThat(alcanzables("ROLE_RECEPCION", "FACTURA_VER"))
				.containsExactlyInAnyOrder("ROLE_RECEPCION", "ROLE_EMPLEADO", "FACTURA_VER")
				.doesNotContain("MASCOTA_LEER");
	}

	private List<String> alcanzables(String... autoridades) {
		return jerarquia.getReachableGrantedAuthorities(AuthorityUtils.createAuthorityList(autoridades)).stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
	}

}
