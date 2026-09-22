package org.springframework.samples.petclinic.service.transacciones;

import org.springframework.context.annotation.Lazy;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/**
 * LABORATORIO DE TRANSACCIONES AVANZADAS (modulo 6, material adicional).
 *
 * Escritura concurrente sobre la misma fila: actualizacion perdida, bloqueo
 * optimista y bloqueo pesimista.
 *
 * TRUCO DIDACTICO IMPORTANTE. Reproducir concurrencia con dos hilos y esperas
 * da pruebas que fallan un dia de cada veinte, que es lo peor que le puede
 * pasar a un curso. Aqui el "segundo usuario" se simula con REQUIRES_NEW: abre
 * una transaccion independiente, con su propio contexto de persistencia, que
 * confirma ANTES de que termine la primera. Para la base de datos el efecto es
 * exactamente el mismo que el de otro usuario, y el orden esta garantizado.
 *
 *     transaccion A: lee la visita (version 1)
 *       transaccion B (REQUIRES_NEW): lee, escribe y CONFIRMA (version 2)
 *     transaccion A: escribe... y aqui es donde se decide todo
 */
@Service
public class EdicionConcurrenteService {

	private final VisitRepository visitRepository;

	private final EdicionConcurrenteService self;

	@PersistenceContext
	private EntityManager entityManager;

	public EdicionConcurrenteService(VisitRepository visitRepository, @Lazy EdicionConcurrenteService self) {
		this.visitRepository = visitRepository;
		this.self = self;
	}

	// ------------------------------------------------------------------
	// 1. Actualizacion perdida, silenciosa
	// ------------------------------------------------------------------

	/**
	 * Las dos transacciones escriben con UPDATE masivo. El UPDATE no toca la
	 * columna `version`, asi que no hay nada que detecte el conflicto: gana
	 * quien escribe el ultimo y el otro cambio desaparece sin dejar rastro.
	 *
	 * No hay excepcion, no hay aviso y no hay log. Solo un dato que ya no esta.
	 */
	@Transactional
	public void editarConUpdateMasivo(int idVisita, String textoUsuarioA, String textoUsuarioB) {
		this.visitRepository.findById(idVisita).orElseThrow();

		this.self.escribirEnOtraTransaccionConUpdateMasivo(idVisita, textoUsuarioB);

		this.visitRepository.actualizarDescripcionEnMasa(idVisita, textoUsuarioA);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void escribirEnOtraTransaccionConUpdateMasivo(int idVisita, String texto) {
		this.visitRepository.actualizarDescripcionEnMasa(idVisita, texto);
	}

	// ------------------------------------------------------------------
	// 2. Bloqueo optimista: el conflicto se detecta al confirmar
	// ------------------------------------------------------------------

	/**
	 * Lo mismo, pero escribiendo a traves de la ENTIDAD. Hibernate genera
	 *
	 *     update visits set description=?, version=2 where id=? and version=1
	 *
	 * La transaccion B ya dejo la fila en version 2, asi que el update de A
	 * afecta a CERO filas e Hibernate lanza OptimisticLockException, que Spring
	 * traduce a ObjectOptimisticLockingFailureException.
	 *
	 * Detalle que conviene senalar en clase: la excepcion NO sale de save(),
	 * sale del `flush` que ocurre al confirmar. El fallo aparece lejos de la
	 * linea que lo provoca.
	 */
	@Transactional
	public void editarConEntidadGestionada(int idVisita, String textoUsuarioA, String textoUsuarioB) {
		Visit visita = this.visitRepository.findById(idVisita).orElseThrow();

		this.self.escribirEnOtraTransaccionConEntidad(idVisita, textoUsuarioB);

		visita.setDescription(textoUsuarioA);
		this.visitRepository.save(visita);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void escribirEnOtraTransaccionConEntidad(int idVisita, String texto) {
		Visit visita = this.visitRepository.findById(idVisita).orElseThrow();
		visita.setDescription(texto);
		this.visitRepository.save(visita);
	}

	// ------------------------------------------------------------------
	// 3. Bloqueo pesimista: el conflicto se evita antes de que ocurra
	// ------------------------------------------------------------------

	/**
	 * Optimista: escribe y ya veremos. Pesimista: bloquea la fila desde que la
	 * lee, y el segundo espera.
	 *
	 * Devuelve el modo de bloqueo que el contexto de persistencia tiene sobre
	 * la entidad, que es la forma de comprobarlo sin montar dos hilos.
	 */
	@Transactional
	public LockModeType editarConBloqueoPesimista(int idVisita, String texto) {
		Visit visita = this.visitRepository.findByIdBloqueando(idVisita).orElseThrow();
		visita.setDescription(texto);
		this.visitRepository.save(visita);
		return this.entityManager.getLockMode(visita);
	}

	/**
	 * Para contrastar: una lectura normal no bloquea nada.
	 */
	@Transactional
	public LockModeType leerSinBloqueo(int idVisita) {
		Visit visita = this.visitRepository.findById(idVisita).orElseThrow();
		return this.entityManager.getLockMode(visita);
	}

	// ------------------------------------------------------------------
	// 4. NESTED: la propagacion que no funciona donde todo el mundo cree
	// ------------------------------------------------------------------

	/**
	 * NESTED promete un punto de retorno intermedio: si falla la parte anidada
	 * se deshace solo esa parte y la transaccion de fuera continua. Se apoya en
	 * SAVEPOINTs de JDBC.
	 *
	 * Con JpaTransactionManager, que es el que configura Spring Boot cuando hay
	 * JPA, esto lanza NestedTransactionNotSupportedException. No es un fallo de
	 * configuracion del proyecto: el gestor viene con los savepoints desactivados
	 * porque Hibernate mantiene su propio contexto de persistencia, y un
	 * savepoint deshace la base de datos pero NO deshace lo que Hibernate tiene
	 * en memoria. Los dos quedarian descuadrados.
	 *
	 * Se puede activar con nestedTransactionAllowed=true, pero entonces hay que
	 * saber muy bien lo que se hace. En la practica: para "esto puede fallar sin
	 * tirar el resto" se usa REQUIRES_NEW, que es lo que ya se vio en clase.
	 */
	@Transactional
	public void intentarNested(int idVisita, String texto) {
		this.self.parteAnidada(idVisita, texto);
	}

	@Transactional(propagation = Propagation.NESTED)
	public void parteAnidada(int idVisita, String texto) {
		this.visitRepository.actualizarDescripcionEnMasa(idVisita, texto);
	}

	// ------------------------------------------------------------------
	// 5. El otro precio del UPDATE masivo: el contexto se queda obsoleto
	// ------------------------------------------------------------------

	/**
	 * Un UPDATE masivo no pasa por el contexto de persistencia, asi que las
	 * entidades ya cargadas siguen teniendo el valor viejo. Dentro de la misma
	 * transaccion se puede acabar leyendo un dato que ya no existe en la base de
	 * datos, y peor: si esa entidad se modifica y se confirma, el valor viejo
	 * vuelve a escribirse encima.
	 *
	 * Los arreglos, por orden de preferencia:
	 *
	 *   1. no mezclar en la misma transaccion UPDATE masivo y entidades vivas;
	 *   2. {@code @Modifying(clearAutomatically = true, flushAutomatically = true)};
	 *   3. {@code entityManager.refresh(entidad)}, que es lo que hace este metodo
	 *      para dejarlo a la vista.
	 */
	@Transactional
	public EstadoDelContexto desincronizacionTrasUpdateMasivo(int idVisita, String nuevoTexto) {
		Visit visita = this.visitRepository.findById(idVisita).orElseThrow();
		String antes = visita.getDescription();

		this.visitRepository.actualizarDescripcionEnMasa(idVisita, nuevoTexto);
		String enMemoria = visita.getDescription();

		this.entityManager.refresh(visita);
		String trasRefrescar = visita.getDescription();

		return new EstadoDelContexto(antes, enMemoria, trasRefrescar);
	}

	/**
	 * Tres fotos del mismo campo: antes del UPDATE masivo, justo despues (en
	 * memoria) y despues de refrescar desde la base de datos.
	 */
	public record EstadoDelContexto(String antes, String enMemoria, String trasRefrescar) {
	}

	// ------------------------------------------------------------------
	// Utilidad para las pruebas
	// ------------------------------------------------------------------

	@Transactional(readOnly = true)
	public String descripcionActual(int idVisita) {
		return this.visitRepository.findById(idVisita).orElseThrow().getDescription();
	}

	@Transactional
	public void restaurarDescripcion(int idVisita, String texto) {
		this.visitRepository.actualizarDescripcionEnMasa(idVisita, texto);
	}

}
