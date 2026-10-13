package com.idexaformacion.cursos.tareas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de la diapositiva 31. Basta con declarar la interfaz: Spring Data
 * genera la implementacion al arrancar.
 *
 * Los dos metodos derivados son el anticipo del modulo 6: el nombre del metodo
 * ES la consulta.
 */
public interface TareaRepository extends JpaRepository<Tarea, Integer> {

	List<Tarea> findByHechoFalseOrderByFechaLimiteAsc();

	List<Tarea> findByFechaLimiteBeforeAndHechoFalse(LocalDate fecha);

}
