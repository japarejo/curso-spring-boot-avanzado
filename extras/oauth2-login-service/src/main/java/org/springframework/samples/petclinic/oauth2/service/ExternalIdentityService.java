package org.springframework.samples.petclinic.oauth2.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.samples.petclinic.oauth2.model.ExternalIdentity;
import org.springframework.samples.petclinic.oauth2.model.InternalUser;
import org.springframework.samples.petclinic.oauth2.repository.ExternalIdentityRepository;
import org.springframework.samples.petclinic.oauth2.repository.InternalUserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExternalIdentityService {

	private static final Pattern UNSAFE_USERNAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

	private final InternalUserRepository userRepository;
	private final ExternalIdentityRepository identityRepository;

	public ExternalIdentityService(InternalUserRepository userRepository, ExternalIdentityRepository identityRepository) {
		this.userRepository = userRepository;
		this.identityRepository = identityRepository;
	}

	@Transactional
	public InternalUser findOrCreateUser(String provider, OAuth2User oauth2User) {
		ExternalProfile profile = extractProfile(provider, oauth2User.getAttributes());
		ExternalIdentity identity = identityRepository
				.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
				.orElseGet(() -> createIdentity(profile));

		identity.setEmail(profile.email());
		identity.setDisplayName(profile.displayName());
		identity.setLastLoginAt(Instant.now());
		return identityRepository.save(identity).getUser();
	}

	private ExternalIdentity createIdentity(ExternalProfile profile) {
		InternalUser user = findUserToLink(profile)
				.orElseGet(() -> createInternalUser(profile));

		ExternalIdentity identity = new ExternalIdentity();
		identity.setUser(user);
		identity.setProvider(profile.provider());
		identity.setProviderUserId(profile.providerUserId());
		user.getExternalIdentities().add(identity);
		return identity;
	}

	private Optional<InternalUser> findUserToLink(ExternalProfile profile) {
		if (StringUtils.hasText(profile.email())) {
			return userRepository.findByEmailIgnoreCase(profile.email());
		}
		return Optional.empty();
	}

	private InternalUser createInternalUser(ExternalProfile profile) {
		InternalUser user = new InternalUser();
		user.setUsername(uniqueUsername(profile));
		user.setEmail(profile.email());
		user.setDisplayName(profile.displayName());
		user.setEnabled(true);
		user.addAuthority("owner");
		return userRepository.save(user);
	}

	private String uniqueUsername(ExternalProfile profile) {
		String seed = StringUtils.hasText(profile.email())
				? profile.email().substring(0, profile.email().indexOf('@'))
				: profile.provider() + "_" + profile.providerUserId();
		String base = UNSAFE_USERNAME_CHARS.matcher(seed).replaceAll("_").toLowerCase(Locale.ROOT);
		if (!StringUtils.hasText(base)) {
			base = profile.provider() + "_user";
		}

		String candidate = base;
		int suffix = 2;
		while (userRepository.existsByUsername(candidate)) {
			candidate = base + suffix;
			suffix++;
		}
		return candidate;
	}

	private ExternalProfile extractProfile(String rawProvider, Map<String, Object> attributes) {
		String provider = rawProvider.toLowerCase(Locale.ROOT);
		String providerUserId = firstText(attributes, "sub", "id", "oid");
		if (!StringUtils.hasText(providerUserId)) {
			throw new IllegalArgumentException("OAuth2 provider did not return a stable subject identifier");
		}
		String email = firstText(attributes, "email", "preferred_username", "upn");
		String displayName = firstText(attributes, "name", "login", "displayName");
		return new ExternalProfile(provider, providerUserId, email, displayName);
	}

	private String firstText(Map<String, Object> attributes, String... keys) {
		for (String key : keys) {
			Object value = attributes.get(key);
			if (value instanceof String text && StringUtils.hasText(text)) {
				return text;
			}
			if (value != null && StringUtils.hasText(value.toString())) {
				return value.toString();
			}
		}
		return null;
	}

	private record ExternalProfile(String provider, String providerUserId, String email, String displayName) {
	}
}
