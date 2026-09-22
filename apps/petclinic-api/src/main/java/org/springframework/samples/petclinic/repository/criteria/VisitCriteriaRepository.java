package org.springframework.samples.petclinic.repository.criteria;

import java.util.List;

import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;

/**
 * LABORATORIO DE CRITERIA API (modulo 6, material adicional).
 *
 * Fragmento de repositorio implementado a mano. Spring Data lo compone con la
 * interfaz {@code VisitRepository}: el alumno llama a los metodos derivados y a
 * estos por el mismo objeto, sin saber cual es cual.
 *
 * La regla de nombrado importa: la implementacion DEBE llamarse
 * {@code VisitCriteriaRepositoryImpl}, es decir, el nombre de esta interfaz mas
 * el sufijo {@code Impl}. Si no, Spring Data no la encuentra y falla al arrancar
 * con "No property found for type Visit".
 *
 * Por que Criteria API y no JPQL con @Query:
 *
 *   - Los criterios se COMPONEN en tiempo de ejecucion. Con JPQL habria que
 *     concatenar cadenas, que es ilegible y abre la puerta a inyeccion.
 *   - Es tipada: con el metamodelo estatico, renombrar un campo de la entidad
 *     rompe la COMPILACION en lugar de romper la consulta en produccion.
 *
 * Y su precio, que hay que decir en clase: es mucho mas verbosa. Para una
 * consulta fija, @Query se lee infinitamente mejor.
 */
public interface VisitCriteriaRepository {

	/**
	 * Busqueda con criterios opcionales combinados con AND.
	 * Los campos nulos del filtro no generan predicado.
	 */
	List<Visit> buscar(FiltroVisitas filtro);

	/**
	 * Agregacion: visitas y mascotas distintas por tipo de mascota.
	 * Cuenta la base de datos, no Java.
	 */
	List<ResumenPorTipo> resumirPorTipoDeMascota();

	/**
	 * Subconsulta correlacionada: mascotas que NO tienen ninguna visita.
	 * Es el caso que peor se expresa con consultas derivadas.
	 */
	List<Pet> mascotasSinVisitas();

	/**
	 * Agrupacion con HAVING: propietarios cuyas mascotas acumulan al menos
	 * {@code minimo} visitas.
	 */
	List<String> propietariosConAlMenos(int minimo);
}
