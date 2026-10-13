// =============================================================================
// Modulo 6 - Ejercicio 12: medir el N+1 con las estadisticas de Hibernate.
//
// Prueba lista para copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/repository/
// con el nombre NMasUnoMedidoTests.java.
// =============================================================================
package org.springframework.samples.petclinic.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;

import jakarta.persistence.EntityManager;

/**
 * Paso 4 del ejercicio: anadir a la lista de properties
 *     "spring.jpa.properties.hibernate.default_batch_fetch_size=16"
 * y volver a ejecutar. La consulta simple baja de 1 + N a 1 + N/16 (redondeando).
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@DisplayName("N+1 medido")
class NMasUnoMedidoTests {

	@Autowired
	EntityManager em;

	@Autowired
	OwnerRepository ownerRepository;

	Statistics estadisticas;

	@BeforeEach
	void preparar() {
		estadisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
	}

	/** Ejecuta la carga, recorre TODAS las mascotas y devuelve cuantas sentencias SQL hubo. */
	private long medir(String etiqueta, Supplier<Collection<Owner>> carga) {
		em.clear();                 // nada en memoria: todo sale de la BD
		estadisticas.clear();

		Collection<Owner> owners = carga.get();
		int mascotas = 0;
		for (Owner o : owners) {
			for (Pet p : o.getPets()) {
				mascotas++;
				p.getName();
			}
		}

		long sentencias = estadisticas.getPrepareStatementCount();
		System.out.printf("%-22s -> %2d propietarios, %2d mascotas, %3d sentencias SQL%n",
				etiqueta, owners.size(), mascotas, sentencias);
		return sentencias;
	}

	@Test
	void compararEstrategias() {
		long simple = medir("JPQL simple", () -> em
			.createQuery("select o from Owner o", Owner.class)
			.getResultList());

		long fetchJoin = medir("LEFT JOIN FETCH", () -> em
			.createQuery("select distinct o from Owner o left join fetch o.pets", Owner.class)
			.getResultList());

		long grafo = medir("@EntityGraph", () -> ownerRepository.findByLastName(""));

		// Lo unico que se puede afirmar sin depender de los datos: las dos
		// soluciones hacen menos consultas que la version ingenua, y lo mismo entre si.
		assertThat(fetchJoin).isLessThan(simple);
		assertThat(grafo).isEqualTo(fetchJoin);
	}

	@Test
	void unaSolaConsultaSiNoSeTocanLasMascotas() {
		em.clear();
		estadisticas.clear();

		List<Owner> owners = em.createQuery("select o from Owner o", Owner.class).getResultList();

		// Sin recorrer o.getPets(), la coleccion perezosa no se carga: 1 sentencia.
		// El N+1 no lo causa la consulta, sino el BUCLE que viene despues.
		assertThat(owners).isNotEmpty();
		assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
	}

}

// Salida tipica con Pet.visits EAGER (el estado inicial del proyecto):
//   JPQL simple            -> 10 propietarios, 13 mascotas,  24 sentencias SQL
//   LEFT JOIN FETCH        -> 10 propietarios, 13 mascotas,  14 sentencias SQL
//   @EntityGraph           -> 10 propietarios, 13 mascotas,  14 sentencias SQL
// Los 13 "de mas" del fetch join son las visitas: EAGER obliga a cargarlas mascota
// a mascota aunque nadie las use. Con Pet.visits LAZY, el fetch join baja a 1.
// (Los numeros exactos dependen de data.sql y de la version de Hibernate: lo que
// importa es la proporcion.)
