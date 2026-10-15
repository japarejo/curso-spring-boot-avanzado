package org.springframework.samples.petclinic.web.api.dto;

/**
 * Vistas Jackson de {@link OwnerResponse}. Cada una hereda de la anterior, asi
 * que una vista incluye los campos de todas las que extiende:
 *
 *   Publica   -> id, firstName, lastName, city
 *   Recepcion -> lo anterior + address, telephone
 *   Admin     -> lo anterior + version, username
 *
 * Son interfaces vacias: solo sirven como "etiqueta" en @JsonView.
 */
public final class VistasPropietario {

	private VistasPropietario() {
	}

	public interface Publica {
	}

	public interface Recepcion extends Publica {
	}

	public interface Admin extends Recepcion {
	}

	public interface Custom {}

}
