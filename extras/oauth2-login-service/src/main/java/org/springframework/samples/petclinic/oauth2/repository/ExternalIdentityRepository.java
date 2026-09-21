package org.springframework.samples.petclinic.oauth2.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.oauth2.model.ExternalIdentity;

public interface ExternalIdentityRepository extends CrudRepository<ExternalIdentity, Integer> {

	Optional<ExternalIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
