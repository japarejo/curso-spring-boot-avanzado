package org.springframework.samples.petclinic.resilience;

/**
 * Fallo que no se arregla reintentando. La instancia demoExcepciones lo
 * declara en ignore-exceptions: se propaga al primer intento.
 */
public class FalloPermanenteException extends RuntimeException {

	public FalloPermanenteException(String message) {
		super(message);
	}

}
