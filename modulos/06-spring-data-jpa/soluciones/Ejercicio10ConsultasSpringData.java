// =============================================================================
// Modulo 6 - Ejercicio 10: consultas con Spring Data.
//
// SOLUCION DE REFERENCIA. Dos partes:
//   1. metodos a anadir a repository/OwnerRepository.java (y la proyeccion);
//   2. la prueba, lista para copiar a
//      apps/petclinic-api/src/test/java/.../repository/OwnerRepositoryConsultasTests.java
// =============================================================================

// ---------- 1a. repository/PropietariosPorCiudad.java ----------
/*
package org.springframework.samples.petclinic.repository;

// Proyeccion de interfaz para una consulta de agregacion: los alias de la JPQL
// ("as ciudad", "as total") tienen que coincidir con los getters.
public interface PropietariosPorCiudad {
	String getCiudad();
	Long getTotal();
}
*/

// ---------- 1b. Metodos para OwnerRepository ----------
/*
	List<Owner> findByCityOrderByLastNameAsc(String city);

	long countByCity(String city);

	boolean existsByTelephone(String telephone);

	Page<Owner> findByLastNameStartingWith(String prefijo, Pageable pageable);

	Slice<Owner> findByCity(String city, Pageable pageable);

	@Query("select o.city as ciudad, count(o) as total from Owner o group by o.city order by count(o) desc, o.city")
	List<PropietariosPorCiudad> contarPorCiudad();

	// SQL nativo: nombres de TABLA y COLUMNA, no de entidad y campo.
	@Query(value = "select * from owners where telephone like :prefijo%", nativeQuery = true)
	List<Owner> buscarPorPrefijoDeTelefono(@Param("prefijo") String prefijo);

	// clearAutomatically: vacia el contexto de persistencia despues del UPDATE.
	// Sin el, las entidades ya cargadas siguen con el valor viejo en memoria:
	// el UPDATE masivo va directo a la BD y se salta el contexto.
	@Modifying(clearAutomatically = true)
	@Query("update Owner o set o.city = :nueva where o.city = :vieja")
	int renombrarCiudad(@Param("vieja") String vieja, @Param("nueva") String nueva);
*/
// Imports necesarios en OwnerRepository:
//   org.springframework.data.domain.Page, Pageable, Slice
//   org.springframework.data.jpa.repository.Modifying

// ---------- 2. La prueba ----------
package org.springframework.samples.petclinic.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.model.Owner;

import jakarta.persistence.EntityManager;

/**
 * Datos de data.sql: 10 propietarios, 4 de ellos en Madison
 * (Escobito, Franklin, McTavish, Schroeder), 2 en Monona y 1 en cada una de
 * las otras cuatro ciudades.
 *
 * @DataJpaTest es transaccional: la prueba del UPDATE masivo se deshace sola.
 */
@DataJpaTest
@DisplayName("Consultas de Spring Data sobre OwnerRepository")
class OwnerRepositoryConsultasTests {

	@Autowired
	OwnerRepository repo;

	@Autowired
	EntityManager em;

	@Test
	@DisplayName("1. derivado con orden")
	void derivadoConOrden() {
		assertThat(repo.findByCityOrderByLastNameAsc("Madison"))
			.extracting(Owner::getLastName)
			.containsExactly("Escobito", "Franklin", "McTavish", "Schroeder");
	}

	@Test
	@DisplayName("2 y 3. recuento y existencia: select count(...) sin cargar entidades")
	void recuentoYExistencia() {
		assertThat(repo.countByCity("Monona")).isEqualTo(2);
		assertThat(repo.existsByTelephone("6085551023")).isTrue();
		assertThat(repo.existsByTelephone("000")).isFalse();
	}

	@Test
	@DisplayName("4. Page: trae la pagina Y lanza un count(*) para el total")
	void paginado() {
		Page<Owner> pagina = repo.findByLastNameStartingWith("", PageRequest.of(0, 3, Sort.by("lastName")));

		assertThat(pagina.getContent()).hasSize(3);
		assertThat(pagina.getTotalElements()).isEqualTo(10);
		assertThat(pagina.getTotalPages()).isEqualTo(4);
	}

	@Test
	@DisplayName("5. Slice: sin count(*); pide un elemento de mas para saber si hay siguiente")
	void slice() {
		Slice<Owner> trozo = repo.findByCity("Madison", PageRequest.of(0, 3));

		assertThat(trozo.getContent()).hasSize(3);
		assertThat(trozo.hasNext()).isTrue();
		// Slice no tiene getTotalElements(): no lo sabe.
	}

	@Test
	@DisplayName("6. JPQL con agregacion y proyeccion de interfaz")
	void agregacion() {
		List<PropietariosPorCiudad> filas = repo.contarPorCiudad();

		assertThat(filas).hasSize(6);
		assertThat(filas.get(0).getCiudad()).isEqualTo("Madison");
		assertThat(filas.get(0).getTotal()).isEqualTo(4L);
	}

	@Test
	@DisplayName("7. SQL nativo")
	void nativo() {
		assertThat(repo.buscarPorPrefijoDeTelefono("608555")).hasSize(10);
		assertThat(repo.buscarPorPrefijoDeTelefono("6085551")).hasSize(2);
	}

	@Test
	@DisplayName("8. UPDATE masivo y el contexto de persistencia")
	void modificacionMasiva() {
		Owner franklin = repo.findById(1);
		assertThat(franklin.getCity()).isEqualTo("Madison");

		int cambiadas = repo.renombrarCiudad("Madison", "Madison WI");

		assertThat(cambiadas).isEqualTo(4);
		// Con clearAutomatically = true, esta lectura va a la BD y ve el cambio.
		// Quitalo y veras "Madison": el objeto viejo seguia en el contexto.
		assertThat(repo.findById(1).getCity()).isEqualTo("Madison WI");
		// El objeto leido antes del UPDATE quedo separado (detached) y conserva el valor viejo:
		assertThat(franklin.getCity()).isEqualTo("Madison");
		assertThat(em.contains(franklin)).isFalse();
	}

}
