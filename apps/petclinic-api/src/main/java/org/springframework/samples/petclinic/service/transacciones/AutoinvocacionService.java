package org.springframework.samples.petclinic.service.transacciones;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * LABORATORIO DE TRANSACCIONES AVANZADAS (modulo 6, material adicional).
 *
 * La trampa numero uno de @Transactional, y la que mas tiempo cuesta depurar
 * porque no da ningun error: la AUTOINVOCACION.
 *
 * Spring implementa @Transactional con un PROXY que envuelve al bean. Quien
 * abre la transaccion es el proxy, no la clase. Por eso:
 *
 *     cliente ---> [proxy] ---> instancia real      la transaccion se abre
 *                               instancia.metodo()  NO pasa por el proxy
 *
 * Cuando un metodo llama a otro del mismo objeto con `this`, la llamada va
 * directa a la instancia real y se salta el proxy. La anotacion sigue ahi, se
 * lee perfectamente en el codigo, y no hace absolutamente nada.
 */
@Service
public class AutoinvocacionService {

	private final AutoinvocacionService self;

	private final TransactionTemplate transactionTemplate;

	/**
	 * @param self el propio bean, pero a traves del PROXY. Hace falta @Lazy: sin
	 * el, Spring intentaria construir el bean para inyectarlo dentro de su
	 * propio constructor y fallaria con una dependencia circular.
	 */
	public AutoinvocacionService(@Lazy AutoinvocacionService self, TransactionTemplate transactionTemplate) {
		this.self = self;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Lo que casi todo el mundo escribe la primera vez. NO abre transaccion.
	 */
	public boolean llamandoConThis() {
		return this.hayTransaccionActiva();
	}

	/**
	 * Arreglo 1: autoinyeccion. Funciona, pero deja a la vista que algo raro
	 * pasa; muchos equipos lo prohiben justamente por eso.
	 */
	public boolean llamandoAlProxy() {
		return this.self.hayTransaccionActiva();
	}

	/**
	 * Arreglo 2: gestion programatica. Ni proxy ni anotacion: la transaccion se
	 * abre explicitamente. Es la opcion honesta cuando el ambito transaccional
	 * no coincide con la frontera del metodo.
	 */
	public boolean usandoTransactionTemplate() {
		return Boolean.TRUE.equals(this.transactionTemplate
			.execute((estado) -> TransactionSynchronizationManager.isActualTransactionActive()));
	}

	/**
	 * Arreglo 3, y el preferible casi siempre: mover el metodo transaccional a
	 * OTRO bean. La llamada vuelve a atravesar un proxy porque ya no es del
	 * mismo objeto, y de paso la responsabilidad queda mejor repartida.
	 */
	public boolean llamandoAOtroBean(ColaboradorTransaccional colaborador) {
		return colaborador.hayTransaccionActiva();
	}

	@Transactional
	public boolean hayTransaccionActiva() {
		return TransactionSynchronizationManager.isActualTransactionActive();
	}

	/**
	 * El mismo problema, mucho mas caro: el REQUIRES_NEW que nunca se ejecuta.
	 *
	 * Este metodo pretende que la auditoria sobreviva aunque el trabajo
	 * principal falle. Como la llamada es con `this`, la auditoria acaba en la
	 * MISMA transaccion y se revierte con todo lo demas.
	 */
	@Transactional
	public String auditoriaQueNoSobrevive() {
		return this.nombreDeLaTransaccionActual();
	}

	/**
	 * La version correcta: al pasar por el proxy, REQUIRES_NEW suspende la
	 * transaccion de fuera y abre otra. El nombre de la transaccion cambia, y
	 * eso es lo que comprueba la prueba.
	 */
	@Transactional
	public String auditoriaQueSiSobrevive() {
		return this.self.nombreDeLaTransaccionActual();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String nombreDeLaTransaccionActual() {
		return TransactionSynchronizationManager.getCurrentTransactionName();
	}

	/**
	 * Colaborador en otro bean, para el arreglo 3.
	 */
	@Service
	public static class ColaboradorTransaccional {

		@Transactional
		public boolean hayTransaccionActiva() {
			return TransactionSynchronizationManager.isActualTransactionActive();
		}

	}

}
