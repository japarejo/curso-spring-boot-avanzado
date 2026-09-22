package org.springframework.samples.petclinic.service.transacciones;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * LABORATORIO DE TRANSACCIONES AVANZADAS (modulo 6, material adicional).
 *
 * Los tres escuchadores del mismo evento, para poder compararlos.
 *
 * En lugar de enviar correos de verdad, cada uno apunta lo que haria en una
 * lista. Asi la prueba puede afirmar quien se ejecuto y quien no, que es
 * exactamente lo que se quiere ensenar.
 */
@Component
public class NotificacionesDeVisita {

	/** Lo que haria un @EventListener normal: se entrega al publicar. */
	private final List<String> entregadasAlPublicar = new CopyOnWriteArrayList<>();

	/** Lo que haria un @TransactionalEventListener: se entrega al confirmar. */
	private final List<String> entregadasAlConfirmar = new CopyOnWriteArrayList<>();

	/** Util para compensar: solo se entrega cuando la transaccion se revierte. */
	private final List<String> entregadasAlRevertir = new CopyOnWriteArrayList<>();

	/**
	 * Se ejecuta en el momento del publishEvent, dentro de la transaccion y
	 * antes de saber si va a confirmar. Es el que provoca el correo fantasma.
	 */
	@EventListener
	public void alPublicar(VisitaRegistrada evento) {
		this.entregadasAlPublicar.add(evento.descripcion());
	}

	/**
	 * AFTER_COMMIT es la fase por defecto; se escribe aqui para que se vea.
	 *
	 * AVISO para cuando este escuchador tenga que tocar la base de datos: en
	 * este punto la transaccion YA esta confirmada, asi que cualquier escritura
	 * necesita @Transactional(propagation = REQUIRES_NEW). Sin eso, con
	 * JpaTransactionManager el cambio se descarta sin avisar.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alConfirmar(VisitaRegistrada evento) {
		this.entregadasAlConfirmar.add(evento.descripcion());
	}

	/**
	 * La cara complementaria: notificar precisamente cuando algo se ha ido al
	 * traste. Sirve para avisar a un operador o para registrar el intento.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
	public void alRevertir(VisitaRegistrada evento) {
		this.entregadasAlRevertir.add(evento.descripcion());
	}

	public List<String> getEntregadasAlPublicar() {
		return this.entregadasAlPublicar;
	}

	public List<String> getEntregadasAlConfirmar() {
		return this.entregadasAlConfirmar;
	}

	public List<String> getEntregadasAlRevertir() {
		return this.entregadasAlRevertir;
	}

	public void limpiar() {
		this.entregadasAlPublicar.clear();
		this.entregadasAlConfirmar.clear();
		this.entregadasAlRevertir.clear();
	}

}
