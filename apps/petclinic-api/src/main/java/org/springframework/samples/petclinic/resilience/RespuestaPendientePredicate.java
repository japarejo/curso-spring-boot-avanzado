package org.springframework.samples.petclinic.resilience;

import java.util.function.Predicate;

/**
 * Predicado para la propiedad result-predicate de la instancia demoResultado.
 *
 * Reintentar no es solo cosa de excepciones: si el servicio responde bien pero
 * con un estado "todavia no" (PENDIENTE), tambien se puede volver a preguntar.
 * Devolver true significa "este resultado no vale, reintenta".
 *
 * resilience4j lo instancia por reflexion: necesita constructor publico sin
 * argumentos y no es un bean de Spring.
 */
public class RespuestaPendientePredicate implements Predicate<Object> {

	@Override
	public boolean test(Object resultado) {
		return ServicioRemotoSimulado.PENDIENTE.equals(resultado);
	}

}
