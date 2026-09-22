// =============================================================================
// Modulo 6 - Ejercicio 7: escritura por lotes.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
//
// Es la continuacion del ejercicio 5 (claves primarias): el resultado depende
// por completo de la estrategia de generacion de identificadores, y ese es el
// aprendizaje del ejercicio.
// =============================================================================
package org.springframework.samples.petclinic.service;

import java.time.LocalDate;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class Ejercicio07EscrituraPorLotes {

	private static final int TAMANO_DEL_LOTE = 50;

	@PersistenceContext
	private EntityManager entityManager;

	// -------------------------------------------------------------------------
	// 1. La version ingenua, que es con la que hay que empezar
	// -------------------------------------------------------------------------
	//
	// Con generate_statistics activado, esto imprime al final algo del estilo:
	//
	//     1000 entities inserted
	//     1000 JDBC statements executed
	//     0 JDBC batches
	//
	// Mil viajes a la base de datos. En H2, en memoria, casi no se nota; contra
	// un MySQL en otra maquina, con 1 ms de latencia por viaje, son 1000 ms de
	// puro ir y venir.
	@Transactional
	public void insertarSinLotes(int cuantas) {
		Owner propietario = this.entityManager.find(Owner.class, 1);
		PetType tipo = this.entityManager.find(PetType.class, 1);

		for (int i = 0; i < cuantas; i++) {
			this.entityManager.persist(nuevaMascota(i, propietario, tipo));
		}
	}

	// -------------------------------------------------------------------------
	// 2. La version por lotes
	// -------------------------------------------------------------------------
	//
	// Con estas propiedades:
	//
	//     spring.jpa.properties.hibernate.jdbc.batch_size=50
	//     spring.jpa.properties.hibernate.order_inserts=true
	//     spring.jpa.properties.hibernate.order_updates=true
	//     spring.jpa.properties.hibernate.generate_statistics=true
	//
	// Y con los dos flush/clear del bucle, que son la parte que se olvida.
	@Transactional
	public void insertarPorLotes(int cuantas) {
		Owner propietario = this.entityManager.find(Owner.class, 1);
		PetType tipo = this.entityManager.find(PetType.class, 1);

		for (int i = 0; i < cuantas; i++) {
			this.entityManager.persist(nuevaMascota(i, propietario, tipo));

			// flush() envia el lote; clear() vacia el contexto de persistencia.
			//
			// Sin el clear(), el contexto acumula las mil entidades y cada
			// comprobacion de cambios recorre todas: el coste crece de forma
			// cuadratica y el consumo de memoria tambien. Es la causa mas comun
			// de OutOfMemoryError en un proceso de carga.
			if (i > 0 && i % TAMANO_DEL_LOTE == 0) {
				this.entityManager.flush();
				this.entityManager.clear();
			}
		}

		this.entityManager.flush();
		this.entityManager.clear();
	}

	// -------------------------------------------------------------------------
	// 3. Medirlo, que es de lo que va el ejercicio
	// -------------------------------------------------------------------------
	//
	// Afirmar "esto es mas rapido" sin un numero no vale. Hibernate lleva la
	// contabilidad; solo hay que pedirsela.
	public Statistics estadisticas() {
		return this.entityManager.getEntityManagerFactory()
			.unwrap(SessionFactory.class)
			.getStatistics();
	}

	public void imprimirYReiniciarEstadisticas(String titulo) {
		Statistics estadisticas = estadisticas();
		System.out.printf("%n== %s ==%n", titulo);
		System.out.printf("  entidades insertadas : %d%n", estadisticas.getEntityInsertCount());
		System.out.printf("  sentencias JDBC      : %d%n", estadisticas.getPrepareStatementCount());
		// Este es el numero que importa. Si sale 0, el proceso por lotes NO esta
		// funcionando, aunque batch_size este configurado.
		System.out.printf("  lotes JDBC           : %d%n", estadisticas.getJdbcBatchCount());
		estadisticas.clear();
	}

	private Pet nuevaMascota(int indice, Owner propietario, PetType tipo) {
		Pet mascota = new Pet();
		mascota.setName("Lote-" + indice);
		mascota.setBirthDate(LocalDate.now().minusYears(1));
		mascota.setType(tipo);
		mascota.setOwner(propietario);
		return mascota;
	}

	// =========================================================================
	// EL AVISO, QUE ES LA PARTE IMPORTANTE DEL EJERCICIO
	// =========================================================================
	//
	// Con este proyecto tal y como esta, los "lotes JDBC" salen 0 aunque se
	// configure todo correctamente. Y no es un fallo de la configuracion.
	//
	// BaseEntity usa GenerationType.IDENTITY:
	//
	//     @Id
	//     @GeneratedValue(strategy = GenerationType.IDENTITY)
	//     protected Integer id;
	//
	// Con IDENTITY, el identificador lo genera la base de datos EN el INSERT.
	// Hibernate necesita ese valor para meter la entidad en el contexto de
	// persistencia, asi que tiene que ejecutar el INSERT de cada fila y leer la
	// clave generada, una por una. No hay nada que agrupar: agrupar significa
	// precisamente no mirar el resultado hasta el final.
	//
	// Hibernate lo dice en el log, y merece la pena buscarlo con los alumnos:
	//
	//     HHH000038: Disabling contextual LOB creation
	//     ... o, mas al grano, el hecho de que getJdbcBatchCount() sea 0.
	//
	// EL ARREGLO: pasar a SEQUENCE con reserva de bloques.
	//
	//     @Id
	//     @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen_base")
	//     @SequenceGenerator(name = "gen_base", sequenceName = "base_seq", allocationSize = 50)
	//     protected Integer id;
	//
	// Con allocationSize = 50, Hibernate pide 50 identificadores de una vez y los
	// reparte en memoria. Ya no necesita la base de datos para cada fila, y
	// entonces si puede agrupar: 1000 entidades pasan de 1000 sentencias a 20
	// lotes de 50.
	//
	// Y LA LETRA PEQUENA, que tambien hay que decir:
	//
	//   1. MySQL no tiene secuencias nativas; Hibernate las emula con una tabla.
	//      Funciona, pero deja de ser gratis.
	//   2. Con allocationSize alto se pierden identificadores en cada reinicio de
	//      la aplicacion. No es un problema, pero sorprende la primera vez que
	//      alguien ve un salto de 50 en los ids.
	//   3. Cambiar la estrategia en un esquema que ya tiene datos es una
	//      migracion de verdad, no una anotacion: hay que crear la secuencia y
	//      posicionarla por encima del maximo actual. En este proyecto seria una
	//      V3__cambio_de_estrategia_de_claves.sql.
	//
	// LA CONCLUSION PARA LLEVARSE: la estrategia de claves primarias no es un
	// detalle de mapeo, es una decision de rendimiento. IDENTITY es lo mas comodo
	// y lo que casi todos los tutoriales usan, y renuncia a la escritura por
	// lotes para siempre. Si el proyecto va a hacer cargas masivas, hay que
	// decidirlo el primer dia.
	//
	// =========================================================================
	// COMPROBACION (PowerShell)
	// =========================================================================
	//
	//   .\mvnw -pl apps/petclinic-api spring-boot:run `
	//     "-Dspring-boot.run.arguments=--spring.jpa.properties.hibernate.jdbc.batch_size=50 --spring.jpa.properties.hibernate.order_inserts=true --spring.jpa.properties.hibernate.generate_statistics=true"
	//
	// O, mas comodo, en una prueba:
	//
	//   @DataJpaTest
	//   @TestPropertySource(properties = {
	//           "spring.jpa.properties.hibernate.jdbc.batch_size=50",
	//           "spring.jpa.properties.hibernate.order_inserts=true",
	//           "spring.jpa.properties.hibernate.generate_statistics=true" })
	//   class Ejercicio07Tests { ... }

}
