package org.springframework.samples.petclinic.roles.configuration;

import java.util.Arrays;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.samples.petclinic.roles.model.Privilegio;
import org.springframework.samples.petclinic.roles.model.Rol;
import org.springframework.samples.petclinic.roles.model.Usuario;
import org.springframework.samples.petclinic.roles.repository.PrivilegioRepository;
import org.springframework.samples.petclinic.roles.repository.RolRepository;
import org.springframework.samples.petclinic.roles.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equivalente al {@code SetupDataLoader} del articulo de Baeldung: crea
 * privilegios, roles que los agrupan y usuarios que tienen roles. Es
 * idempotente ("crear si no existe"), asi que serviria igual con una base de
 * datos persistente.
 *
 * <pre>
 *  Rol               | Privilegios PROPIOS (los de sus filas en roles_privilegios)
 *  ------------------+-----------------------------------------------------------
 *  ROLE_EMPLEADO     | MASCOTA_LEER
 *  ROLE_RECEPCION    | MASCOTA_EDITAR, FACTURA_VER
 *  ROLE_VETERINARIO  | VISITA_REGISTRAR
 *  ROLE_ADMIN        | SEGURIDAD_GESTIONAR
 * </pre>
 *
 * <p>Ningun rol repite los privilegios de los roles que "hereda": eso se lo
 * dejamos a la jerarquia. Por eso, si la herencia de privilegios esta
 * desactivada, se nota enseguida (ejercicio 2 del guion).
 */
@Component
public class DatosIniciales implements ApplicationRunner {

	private final PrivilegioRepository privilegios;
	private final RolRepository roles;
	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;

	public DatosIniciales(PrivilegioRepository privilegios, RolRepository roles, UsuarioRepository usuarios,
			PasswordEncoder passwordEncoder) {
		this.privilegios = privilegios;
		this.roles = roles;
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Privilegio mascotaLeer = privilegio("MASCOTA_LEER");
		Privilegio mascotaEditar = privilegio("MASCOTA_EDITAR");
		Privilegio visitaRegistrar = privilegio("VISITA_REGISTRAR");
		Privilegio facturaVer = privilegio("FACTURA_VER");
		Privilegio seguridadGestionar = privilegio("SEGURIDAD_GESTIONAR");

		Rol empleado = rol("ROLE_EMPLEADO", mascotaLeer);
		Rol recepcion = rol("ROLE_RECEPCION", mascotaEditar, facturaVer);
		Rol veterinario = rol("ROLE_VETERINARIO", visitaRegistrar);
		Rol admin = rol("ROLE_ADMIN", seguridadGestionar);

		usuario("eva", empleado);
		usuario("rita", recepcion);
		usuario("victor", veterinario);
		// pablo tiene DOS roles: composicion de roles, que no es lo mismo que
		// jerarquia. Sus privilegios son la union de los de ambos.
		usuario("pablo", recepcion, veterinario);
		usuario("admin", admin);
	}

	private Privilegio privilegio(String nombre) {
		return privilegios.findByNombre(nombre)
				.orElseGet(() -> privilegios.save(new Privilegio(nombre)));
	}

	private Rol rol(String nombre, Privilegio... suyos) {
		Rol rol = roles.findByNombre(nombre).orElseGet(() -> new Rol(nombre));
		rol.getPrivilegios().addAll(Arrays.asList(suyos));
		return roles.save(rol);
	}

	private void usuario(String username, Rol... suyos) {
		if (usuarios.findByUsername(username).isPresent()) {
			return;
		}
		Usuario usuario = new Usuario(username, passwordEncoder.encode("demo"));
		usuario.getRoles().addAll(Arrays.asList(suyos));
		usuarios.save(usuario);
	}

}
