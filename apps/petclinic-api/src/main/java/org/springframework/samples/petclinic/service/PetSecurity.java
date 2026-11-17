package org.springframework.samples.petclinic.service;

import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de seguridad sobre mascotas para usar desde SpEL:
 * {@code @PreAuthorize("@petSecurity.isOwner(#id, authentication)")}.
 *
 * Se comprueba la propiedad ANTES de ejecutar el metodo protegido. Con
 * {@code @PostAuthorize} la comprobacion llega tarde: su interceptor (orden 500)
 * envuelve al de {@code @Transactional} (el mas interno), asi que la
 * transaccion ya ha hecho commit cuando se lanza la AccessDeniedException y el
 * borrado no se deshace.
 */
@Component("petSecurity")
public class PetSecurity {

	private final PetRepository petRepository;

	public PetSecurity(PetRepository petRepository) {
		this.petRepository = petRepository;
	}

	/**
	 * true si la mascota existe y su propietario es el usuario autenticado.
	 * Una mascota inexistente devuelve false: a quien no es admin se le
	 * responde 403 y no se le revela si el id existe o no.
	 */
	@Transactional(readOnly = true)
	public boolean isOwner(int petId, Authentication authentication) {
		if (authentication == null) {
			return false;
		}
		Pet pet = petRepository.findById(petId);
		return pet != null
				&& pet.getOwner() != null
				&& pet.getOwner().getUser() != null
				&& authentication.getName().equals(pet.getOwner().getUser().getUsername());
	}

}
