package org.springframework.samples.petclinic.auth.service;

import java.util.List;

import org.springframework.samples.petclinic.auth.model.Authorities;
import org.springframework.samples.petclinic.auth.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MODULO 3 - Origen de los usuarios.
 *
 * Implementa UserDetailsService, que es el unico contrato que Spring Security
 * exige para autenticar. De donde salgan los datos (JPA, LDAP, un proveedor
 * externo) es indiferente para el resto del framework.
 *
 * Detalle importante: en el proyecto anterior esta clase NO implementaba la
 * interfaz, solo tenia un metodo con la misma firma. Funcionaba de milagro
 * porque se inyectaba por tipo concreto; en cuanto Spring Security buscaba un
 * UserDetailsService, no lo encontraba.
 */
@Service
public class JwtUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public JwtUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		org.springframework.samples.petclinic.auth.model.User usuario = userRepository.findByUsername(username);
		if (usuario == null) {
			// Mensaje deliberadamente generico: no confirmar si el usuario
			// existe evita dar pistas a quien enumera cuentas.
			throw new UsernameNotFoundException("Credenciales no validas");
		}
		List<GrantedAuthority> authorities = usuario.getAuthorities().stream()
			.map(Authorities::getAuthority)
			.map(SimpleGrantedAuthority::new)
			.map(GrantedAuthority.class::cast)
			.toList();

		return User.withUsername(usuario.getUsername())
			.password(usuario.getPassword())
			.authorities(authorities)
			.disabled(!usuario.getEnabled())
			.build();
	}
}
