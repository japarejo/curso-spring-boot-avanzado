// =============================================================================
// Modulo 2 - Ejercicio 15, paso 5: la vista sale del usuario autenticado.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Es un metodo mas para web/api/OwnerViewsDemoRestController, junto a porRol.
// =============================================================================
package org.springframework.samples.petclinic.web.api;

import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.samples.petclinic.web.api.dto.OwnerResponse;
import org.springframework.samples.petclinic.web.api.dto.VistasPropietario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

class OwnerViewsDemoRestControllerConUsuario {

	@GetMapping("/{id}/mia")
	public MappingJacksonValue mia(@PathVariable("id") int id, Authentication authentication) {
		MappingJacksonValue respuesta = new MappingJacksonValue(OwnerResponse.from(buscar(id)));
		respuesta.setSerializationView(vistaPara(authentication));
		return respuesta;
	}

	/**
	 * Las autoridades del proyecto son "admin", "vet" y "owner", sin prefijo ROLE_
	 * (ver V2__datos_iniciales.sql). Mientras /api/** sea permitAll, una peticion
	 * anonima llega con authentication == null: se le da la vista mas pobre.
	 */
	private static Class<?> vistaPara(Authentication authentication) {
		if (authentication == null) {
			return VistasPropietario.Publica.class;
		}
		if (tiene(authentication, "admin")) {
			return VistasPropietario.Admin.class;
		}
		if (tiene(authentication, "vet")) {
			return VistasPropietario.Recepcion.class;
		}
		return VistasPropietario.Publica.class;
	}

	private static boolean tiene(Authentication authentication, String autoridad) {
		return authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(autoridad::equals);
	}

	// buscar(id) es el metodo privado que ya tiene el controlador.
	private org.springframework.samples.petclinic.model.Owner buscar(int id) {
		throw new UnsupportedOperationException("ver OwnerViewsDemoRestController");
	}

}
