package org.springframework.samples.petclinic.oauth2.service;

import java.util.List;

import org.springframework.samples.petclinic.oauth2.model.InternalUser;
import org.springframework.samples.petclinic.oauth2.repository.InternalUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class InternalUserDetailsService implements UserDetailsService {

	private final InternalUserRepository userRepository;

	public InternalUserDetailsService(InternalUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		InternalUser user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
		return toUserDetails(user);
	}

	public UserDetails toUserDetails(InternalUser user) {
		List<GrantedAuthority> authorities = user.getAuthorities().stream()
				.map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
				.map(GrantedAuthority.class::cast)
				.toList();
		return User.withUsername(user.getUsername())
				.password(user.getPassword() == null ? "" : user.getPassword())
				.disabled(!user.isEnabled())
				.authorities(authorities)
				.build();
	}
}
