package org.springframework.samples.petclinic.roles.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Que autoridades quedan GUARDADAS en el usuario: roles asignados (no los
 * heredados) y privilegios de todos los roles alcanzables.
 */
@SpringBootTest
class UsuarioDetailsServiceTests {

	@Autowired
	UserDetailsService usuarios;

	@Test
	void ritaTieneSuRolSusPrivilegiosYLosHeredadosDeEmpleado() {
		assertThat(autoridades("rita")).containsExactlyInAnyOrder(
				"ROLE_RECEPCION",
				"MASCOTA_EDITAR", "FACTURA_VER",
				"MASCOTA_LEER"); // <- heredado de ROLE_EMPLEADO
	}

	@Test
	void elRolHeredadoNoSeGuardaSoloSusPrivilegios() {
		// ROLE_EMPLEADO no aparece: lo aplica la jerarquia al decidir.
		assertThat(autoridades("rita")).doesNotContain("ROLE_EMPLEADO");
	}

	@Test
	void pabloEsLaUnionDeSusDosRoles() {
		assertThat(autoridades("pablo")).containsExactlyInAnyOrder(
				"ROLE_RECEPCION", "ROLE_VETERINARIO",
				"MASCOTA_EDITAR", "FACTURA_VER", "VISITA_REGISTRAR", "MASCOTA_LEER");
	}

	@Test
	void adminTieneTodosLosPrivilegiosPeroUnSoloRol() {
		assertThat(autoridades("admin")).containsExactlyInAnyOrder(
				"ROLE_ADMIN",
				"SEGURIDAD_GESTIONAR", "MASCOTA_EDITAR", "FACTURA_VER", "VISITA_REGISTRAR", "MASCOTA_LEER");
	}

	private List<String> autoridades(String username) {
		return usuarios.loadUserByUsername(username).getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
	}

}
