package org.springframework.samples.petclinic.roles.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.samples.petclinic.roles.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {

	Optional<Rol> findByNombre(String nombre);

	List<Rol> findByNombreIn(Collection<String> nombres);

}
