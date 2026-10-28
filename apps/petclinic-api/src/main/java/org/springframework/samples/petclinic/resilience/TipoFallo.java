package org.springframework.samples.petclinic.resilience;

/**
 * Tipo de fallo que simula {@link ServicioRemotoSimulado}.
 *
 *   TRANSITORIO  un corte de red, un 503... Reintentar tiene sentido.
 *   PERMANENTE   datos invalidos, un 404... Reintentar solo hace perder tiempo.
 */
public enum TipoFallo {
	TRANSITORIO, PERMANENTE
}
