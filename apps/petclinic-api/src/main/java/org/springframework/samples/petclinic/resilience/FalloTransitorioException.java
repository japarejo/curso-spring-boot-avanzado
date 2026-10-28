package org.springframework.samples.petclinic.resilience;

/** Fallo que se arregla solo si se espera un poco: se reintenta. */
public class FalloTransitorioException extends RuntimeException {

	public FalloTransitorioException(String message) {
		super(message);
	}

}
