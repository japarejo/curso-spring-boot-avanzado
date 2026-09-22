package org.springframework.samples.petclinic.service.transacciones;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LABORATORIO DE TRANSACCIONES AVANZADAS (modulo 6, material adicional).
 *
 * El problema: hay efectos que NO se pueden deshacer con un rollback. Enviar un
 * correo, cobrar una tarjeta, publicar un mensaje en una cola, invalidar una
 * cache compartida. Si se lanzan dentro de la transaccion y esta se revierte, el
 * efecto ya ha salido y no hay vuelta atras.
 *
 * El sintoma clasico: el cliente recibe el correo de "cita confirmada" de una
 * cita que no existe en la base de datos.
 *
 * La solucion de Spring: publicar un evento dentro de la transaccion y
 * escucharlo con @TransactionalEventListener, que lo entrega DESPUES de
 * confirmar. Ver NotificacionesDeVisita.
 */
@Service
public class RegistroDeVisitasService {

	private final VisitRepository visitRepository;

	private final ApplicationEventPublisher publicador;

	public RegistroDeVisitasService(VisitRepository visitRepository, ApplicationEventPublisher publicador) {
		this.visitRepository = visitRepository;
		this.publicador = publicador;
	}

	/**
	 * Camino feliz: se escribe, se publica y la transaccion confirma.
	 */
	@Transactional
	public void registrar(int idVisita, String descripcion) {
		this.visitRepository.actualizarDescripcionEnMasa(idVisita, descripcion);
		this.publicador.publishEvent(new VisitaRegistrada(idVisita, descripcion));
	}

	/**
	 * Camino que importa: se escribe, se publica y DESPUES falla.
	 *
	 * La escritura se revierte. La pregunta del laboratorio es que pasa con cada
	 * uno de los dos escuchadores.
	 */
	@Transactional
	public void registrarYFallar(int idVisita, String descripcion) {
		this.visitRepository.actualizarDescripcionEnMasa(idVisita, descripcion);
		this.publicador.publishEvent(new VisitaRegistrada(idVisita, descripcion));
		throw new IllegalStateException("La visita no se ha podido registrar");
	}

	/**
	 * Tercera trampa, mas sutil que las otras dos: publicar SIN transaccion.
	 *
	 * @TransactionalEventListener no tiene ningun commit al que engancharse, asi
	 * que descarta el evento en silencio. Ni excepcion ni aviso: el escuchador
	 * simplemente no se ejecuta. Se arregla con fallbackExecution=true, que lo
	 * entrega igualmente cuando no hay transaccion.
	 */
	public void registrarSinTransaccion(int idVisita, String descripcion) {
		this.publicador.publishEvent(new VisitaRegistrada(idVisita, descripcion));
	}

}
