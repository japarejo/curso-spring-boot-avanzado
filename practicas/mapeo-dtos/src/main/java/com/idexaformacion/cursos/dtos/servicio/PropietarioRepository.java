package com.idexaformacion.cursos.dtos.servicio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.idexaformacion.cursos.dtos.modelo.Propietario;

/**
 * Los @EntityGraph cargan en la misma consulta lo que el mapper va a leer. Sin
 * ellos, con open-in-view=false, el mapper falla con LazyInitializationException
 * al recorrer las mascotas fuera de la transaccion.
 *
 * Ejercicio: quita el @EntityGraph de findConMascotasById y ejecuta las pruebas.
 */
public interface PropietarioRepository extends JpaRepository<Propietario, Integer> {

	@EntityGraph(attributePaths = { "usuario", "mascotas" })
	Optional<Propietario> findConMascotasById(Integer id);

	@EntityGraph(attributePaths = "mascotas")
	List<Propietario> findAllByOrderByApellidos();

}
