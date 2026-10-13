package com.idexaformacion.cursos.tareas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * La prueba de la diapositiva 31, actualizada:
 *   - findOne(id) ya no existe en Spring Data 2+: es findById(id), que devuelve Optional;
 *   - @DataJpaTest levanta solo JPA + H2 y deshace cada prueba al terminar.
 *
 * Ojo: data.sql TAMBIEN se ejecuta en @DataJpaTest, por eso hay 3 tareas de partida.
 */
@DataJpaTest
class TareaRepositoryTests {

	@Autowired
	private TareaRepository repository;

	@Test
	void guardaYRecuperaUnaTarea() {
		Tarea guardada = repository.save(new Tarea("Probar el repositorio", LocalDate.of(2026, 2, 1)));

		assertThat(repository.findById(guardada.getId()))
			.get()
			.extracting(Tarea::getDescripcion)
			.isEqualTo("Probar el repositorio");
	}

	@Test
	void lasPendientesSalenOrdenadasPorFechaLimite() {
		List<Tarea> pendientes = repository.findByHechoFalseOrderByFechaLimiteAsc();

		assertThat(pendientes).extracting(Tarea::isHecho).containsOnly(false);
		assertThat(pendientes).extracting(Tarea::getFechaLimite).isSorted();
	}

}
