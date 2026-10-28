package org.springframework.samples.petclinic.resilience;

import org.springframework.stereotype.Component;

/**
 * Hace el papel de un servicio remoto poco fiable, sin necesidad de levantar
 * nada: falla las primeras N llamadas y despues responde bien.
 *
 * Es @Component y no @Service por lo mismo que BillsGateway: las rebanadas
 * @DataJpaTest incluyen los @Service y no deben cargar esto.
 */
@Component
public class ServicioRemotoSimulado {

	public static final String PENDIENTE = "PENDIENTE";

	public static final String COMPLETADO = "COMPLETADO";

	/** Lanza una excepcion en los primeros N intentos; despues responde. */
	public String invocar(RegistroIntentos registro) {
		int intento = registro.siguienteIntento();
		if (intento <= registro.fallosSimulados()) {
			RuntimeException fallo = registro.tipoFallo() == TipoFallo.PERMANENTE
					? new FalloPermanenteException("Peticion invalida (intento " + intento + ")")
					: new FalloTransitorioException("Servicio no disponible (intento " + intento + ")");
			registro.anotar("FALLO " + fallo.getClass().getSimpleName());
			throw fallo;
		}
		registro.anotar("OK");
		return "Respuesta correcta en el intento " + intento;
	}

	/**
	 * No falla nunca: responde PENDIENTE en los primeros N intentos y
	 * COMPLETADO despues. Sirve para reintentar segun el resultado.
	 */
	public String consultarEstado(RegistroIntentos registro) {
		String estado = registro.siguienteIntento() <= registro.fallosSimulados() ? PENDIENTE : COMPLETADO;
		registro.anotar(estado);
		return estado;
	}

}
