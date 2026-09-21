package org.springframework.samples.petclinic.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.oauth2.model.ExternalIdentity;
import org.springframework.samples.petclinic.oauth2.model.InternalUser;
import org.springframework.samples.petclinic.oauth2.repository.ExternalIdentityRepository;
import org.springframework.samples.petclinic.oauth2.repository.InternalUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class ExternalIdentityServiceTests {

	private InternalUserRepository userRepository;
	private ExternalIdentityRepository identityRepository;
	private ExternalIdentityService service;

	@BeforeEach
	void setUp() {
		userRepository = org.mockito.Mockito.mock(InternalUserRepository.class);
		identityRepository = org.mockito.Mockito.mock(ExternalIdentityRepository.class);
		service = new ExternalIdentityService(userRepository, identityRepository);

		when(userRepository.save(any(InternalUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(identityRepository.save(any(ExternalIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void shouldLinkGithubIdentityToExistingInternalUserByEmail() {
		InternalUser owner = new InternalUser();
		owner.setUsername("owner1");
		owner.setEmail("owner1@petclinic.local");
		owner.addAuthority("owner");

		when(identityRepository.findByProviderAndProviderUserId("github", "12345")).thenReturn(Optional.empty());
		when(userRepository.findByEmailIgnoreCase("owner1@petclinic.local")).thenReturn(Optional.of(owner));

		InternalUser result = service.findOrCreateUser("github", oauth2User(Map.of(
				"id", "12345",
				"email", "owner1@petclinic.local",
				"login", "owner-one"), "id"));

		assertThat(result).isSameAs(owner);
		assertThat(owner.getExternalIdentities())
				.anySatisfy(identity -> {
					assertThat(identity.getProvider()).isEqualTo("github");
					assertThat(identity.getProviderUserId()).isEqualTo("12345");
				});
	}

	@Test
	void shouldCreateInternalUserForUnknownGoogleIdentity() {
		when(identityRepository.findByProviderAndProviderUserId("google", "google-subject")).thenReturn(Optional.empty());
		when(userRepository.findByEmailIgnoreCase("new.owner@example.com")).thenReturn(Optional.empty());
		when(userRepository.existsByUsername("new.owner")).thenReturn(false);

		InternalUser result = service.findOrCreateUser("google", oauth2User(Map.of(
				"sub", "google-subject",
				"email", "new.owner@example.com",
				"name", "New Owner"), "sub"));

		assertThat(result.getUsername()).isEqualTo("new.owner");
		assertThat(result.getEmail()).isEqualTo("new.owner@example.com");
		assertThat(result.getAuthorities()).anySatisfy(authority -> assertThat(authority.getAuthority()).isEqualTo("owner"));
		assertThat(result.getExternalIdentities()).anySatisfy(identity -> assertThat(identity.getProvider()).isEqualTo("google"));
	}

	private OAuth2User oauth2User(Map<String, Object> attributes, String nameAttributeKey) {
		return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, nameAttributeKey);
	}
}
