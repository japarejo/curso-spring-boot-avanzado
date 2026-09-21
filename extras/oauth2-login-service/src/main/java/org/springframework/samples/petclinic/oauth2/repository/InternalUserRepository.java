package org.springframework.samples.petclinic.oauth2.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.oauth2.model.InternalUser;

public interface InternalUserRepository extends CrudRepository<InternalUser, Integer> {

	Optional<InternalUser> findByUsername(String username);

	Optional<InternalUser> findByEmailIgnoreCase(String email);

	boolean existsByUsername(String username);
}
