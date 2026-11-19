package org.springframework.samples.petclinic.roles.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.samples.petclinic.roles.model.Privilegio;

public interface PrivilegioRepository extends JpaRepository<Privilegio, Long> {

	Optional<Privilegio> findByNombre(String nombre);

}
