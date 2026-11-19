package org.springframework.samples.petclinic.roles.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.roles.model.Rol;
import org.springframework.samples.petclinic.roles.model.Usuario;
import org.springframework.samples.petclinic.roles.repository.RolRepository;
import org.springframework.samples.petclinic.roles.repository.UsuarioRepository;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El punto donde roles y privilegios se convierten en {@link GrantedAuthority}.
 *
 * <p>Spring Security no sabe nada de "roles" ni de "privilegios": solo ve una
 * lista plana de cadenas. Este servicio decide que cadenas meter. Para
 * {@code rita} (ROLE_RECEPCION) el resultado es:
 *
 * <pre>
 *   ROLE_RECEPCION                    &lt;- el rol asignado   (para hasRole)
 *   MASCOTA_EDITAR, FACTURA_VER       &lt;- sus privilegios  (para hasAuthority)
 *   MASCOTA_LEER                      &lt;- heredado de ROLE_EMPLEADO, SOLO si
 *                                        demo.roles.heredar-privilegios=true
 * </pre>
 *
 * <h2>Por que hace falta {@code heredarPrivilegios}</h2>
 *
 * El {@link RoleHierarchy} trabaja con NOMBRES de autoridad: sabe que
 * {@code ROLE_RECEPCION} implica {@code ROLE_EMPLEADO}, pero no tiene ni idea
 * de que {@code ROLE_EMPLEADO} agrupa a {@code MASCOTA_LEER}, porque esa
 * relacion esta en la tabla {@code roles_privilegios}. Si al cargar el usuario
 * solo expandimos los privilegios de los roles ASIGNADOS (como hace el
 * articulo de Baeldung), entonces:
 *
 * <ul>
 * <li>{@code hasRole('EMPLEADO')} para rita &rarr; SI (lo resuelve la jerarquia).</li>
 * <li>{@code hasAuthority('MASCOTA_LEER')} para rita &rarr; NO (nadie le dio ese privilegio).</li>
 * </ul>
 *
 * La solucion es preguntar a la jerarquia por los roles ALCANZABLES antes de
 * recoger privilegios.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarios;
	private final RolRepository roles;
	private final RoleHierarchy jerarquia;
	private final boolean heredarPrivilegios;

	public UsuarioDetailsService(UsuarioRepository usuarios, RolRepository roles, RoleHierarchy jerarquia,
			@Value("${demo.roles.heredar-privilegios:true}") boolean heredarPrivilegios) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.jerarquia = jerarquia;
		this.heredarPrivilegios = heredarPrivilegios;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		Usuario usuario = usuarios.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException(username));
		return User.withUsername(usuario.getUsername())
				.password(usuario.getPassword())
				.disabled(!usuario.isActivo())
				.authorities(autoridades(usuario.getRoles()))
				.build();
	}

	Collection<GrantedAuthority> autoridades(Collection<Rol> asignados) {
		Set<GrantedAuthority> autoridades = new LinkedHashSet<>();

		// 1. Los roles ASIGNADOS, tal cual. Los heredados NO se anaden: de eso
		//    se encarga la jerarquia al decidir. Asi el Authentication (y un
		//    JWT, si lo emitieramos) refleja lo que el usuario tiene de verdad.
		asignados.forEach(rol -> autoridades.add(new SimpleGrantedAuthority(rol.getNombre())));

		// 2. Los privilegios de los roles efectivos.
		Collection<Rol> efectivos = heredarPrivilegios ? alcanzables(asignados) : asignados;
		efectivos.forEach(rol -> rol.getPrivilegios()
				.forEach(p -> autoridades.add(new SimpleGrantedAuthority(p.getNombre()))));

		return autoridades;
	}

	/** Roles asignados mas todos los que implican segun la jerarquia. */
	private Collection<Rol> alcanzables(Collection<Rol> asignados) {
		List<SimpleGrantedAuthority> comoAutoridades = asignados.stream()
				.map(rol -> new SimpleGrantedAuthority(rol.getNombre()))
				.toList();
		List<String> nombres = jerarquia.getReachableGrantedAuthorities(comoAutoridades).stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
		return roles.findByNombreIn(nombres);
	}

}
