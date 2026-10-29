package org.springframework.samples.petclinic.repository.criteria;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ESCENARIO DE CRITERIA API: busqueda avanzada de pacientes (modulo 6).
 *
 * Fragmento de repositorio que PetRepository incorpora extendiendo esta
 * interfaz. La implementacion se llama igual mas el sufijo Impl.
 *
 * Criterios de ordenacion admitidos en el Pageable (cualquier otro es un 400):
 * nombre, tipo, fechaNacimiento, propietario, ciudad, numeroVisitas, ultimaVisita.
 */
public interface BusquedaMascotasRepository {

	/** Propiedades por las que se puede ordenar. Es una lista blanca: el usuario no elige columnas libres. */
	Set<String> CRITERIOS_DE_ORDEN = Set.of("nombre", "tipo", "fechaNacimiento", "propietario", "ciudad",
			"numeroVisitas", "ultimaVisita");

	/**
	 * Busqueda paginada con criterios opcionales combinados con AND.
	 * Lanza dos consultas: la de la pagina y, si hace falta, la del total.
	 */
	Page<FichaMascota> buscarMascotas(FiltroMascotas filtro, Pageable pagina);

	/** Ciudades distintas de los propietarios, para el desplegable de la pantalla. */
	List<String> ciudadesDePropietarios();

}
