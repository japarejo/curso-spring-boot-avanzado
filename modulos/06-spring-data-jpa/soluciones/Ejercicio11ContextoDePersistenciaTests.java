// =============================================================================
// Modulo 6 - Ejercicio 11: el contexto de persistencia, visto desde dentro.
//
// Prueba lista para copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/repository/
// con el nombre ContextoDePersistenciaTests.java. Mira la consola: cada prueba
// explica en un comentario que SQL deberia (o no) aparecer.
// =============================================================================
package org.springframework.samples.petclinic.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@DataJpaTest
@DisplayName("Contexto de persistencia y transacciones programaticas")
class ContextoDePersistenciaTests {

	@Autowired
	EntityManager em;

	@Autowired
	EntityManagerFactory emf;

	@Autowired
	PlatformTransactionManager txManager;

	@Test
	@DisplayName("1. cache de primer nivel: el mismo objeto y un solo SELECT")
	void cacheDePrimerNivel() {
		Owner a = em.find(Owner.class, 1);
		Owner b = em.find(Owner.class, 1);   // no hay segundo select en la consola

		assertThat(a).isSameAs(b);
	}

	@Test
	@DisplayName("2. dirty checking: se guarda sin llamar a save")
	void dirtyChecking() {
		Owner owner = em.find(Owner.class, 1);
		owner.setFirstName("Jorge");
		em.flush();     // aqui aparece el UPDATE owners set first_name=...
		em.clear();

		assertThat(em.find(Owner.class, 1).getFirstName()).isEqualTo("Jorge");
	}

	@Test
	@DisplayName("3. una entidad separada no se vigila; merge la vuelve a gestionar")
	void detachYMerge() {
		Owner owner = em.find(Owner.class, 1);
		em.detach(owner);
		owner.setFirstName("Desconectado");
		em.flush();     // ningun UPDATE: la entidad ya no esta gestionada
		em.clear();
		assertThat(em.find(Owner.class, 1).getFirstName()).isEqualTo("George");

		em.clear();
		Owner gestionada = em.merge(owner);   // copia el estado sobre una instancia gestionada
		em.flush();     // ahora si: UPDATE
		em.clear();

		assertThat(gestionada).isNotSameAs(owner);
		assertThat(em.find(Owner.class, 1).getFirstName()).isEqualTo("Desconectado");
	}

	/**
	 * Las pruebas 4 y 5 necesitan estar FUERA de la transaccion de @DataJpaTest
	 * para controlar ellas mismas el commit y el rollback. Como no hay rollback
	 * automatico, limpian lo que crean.
	 *
	 * El `em` inyectado es un proxy compartido: dentro de un bloque de la
	 * TransactionTemplate se une a esa transaccion; fuera, cada llamada usa un
	 * EntityManager temporal.
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@DisplayName("4. TransactionTemplate: un bloque confirma, otro se deshace")
	void transactionTemplate() {
		TransactionTemplate tx = new TransactionTemplate(txManager);

		Integer confirmado = tx.execute(status -> {
			PetType tipo = new PetType();
			tipo.setName("axolote");
			em.persist(tipo);
			return tipo.getId();
		});

		tx.executeWithoutResult(status -> {
			PetType tipo = new PetType();
			tipo.setName("dragon");
			em.persist(tipo);
			status.setRollbackOnly();   // se deshace aunque no haya excepcion
		});

		assertThat(contarTipos("axolote")).isEqualTo(1);
		assertThat(contarTipos("dragon")).isZero();

		// Limpieza: no hay rollback automatico en esta prueba.
		tx.executeWithoutResult(status -> em.remove(em.find(PetType.class, confirmado)));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@DisplayName("5. readOnly: con Hibernate, los cambios se pierden sin error")
	void soloLectura() {
		TransactionTemplate lectura = new TransactionTemplate(txManager);
		lectura.setReadOnly(true);

		// En solo lectura, Spring pone la sesion de Hibernate en FlushMode.MANUAL:
		// al confirmar no hay flush, y el cambio no llega a la base de datos.
		lectura.executeWithoutResult(status -> em.find(Owner.class, 1).setFirstName("NoSeGuarda"));

		assertThat(em.find(Owner.class, 1).getFirstName()).isEqualTo("George");
	}

	private long contarTipos(String nombre) {
		return em.createQuery("select count(t) from PetType t where t.name = :n", Long.class)
			.setParameter("n", nombre)
			.getSingleResult();
	}

}

// Nota: la diapositiva 151 muestra la cadena EntityManagerFactory -> EntityManager
// -> transaccion. En Spring no se recorre a mano: el EntityManager inyectado ya
// esta ligado a la transaccion en curso. Por eso `emf` solo se inyecta aqui para
// poder mencionarlo en clase.
