package org.springframework.samples.petclinic.aop;

import java.time.Instant;

/**
 * Una linea del registro de auditoria.
 *
 * @param resultado "OK" o "ERROR NombreDeLaExcepcion"
 */
public record EntradaAuditoria(Instant instante, String accion, String metodo, String usuario, String argumentos,
		String resultado, long duracionMs) {

	public boolean correcta() {
		return "OK".equals(resultado);
	}

}
