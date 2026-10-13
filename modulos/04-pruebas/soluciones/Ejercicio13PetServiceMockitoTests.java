// =============================================================================
// Modulo 4 - Ejercicio 13: dobles de prueba con Mockito.
//
// Prueba lista para ejecutar. Copiar a
//   apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/service/
// con el nombre PetServiceMockitoTests.java (el de la clase), y lanzar:
//   .\mvnw.cmd -pl apps/petclinic-api test "-Dtest=PetServiceMockitoTests"
// =============================================================================
package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;

/**
 * MockitoExtension crea los @Mock e inyecta en @InjectMocks POR CONSTRUCTOR,
 * que es como lo haria Spring. Ademas es ESTRICTA: un stub que no se usa hace
 * fallar la prueba (UnnecessaryStubbingException). Eso evita pruebas que
 * preparan cosas que no comprueban.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PetService aislado con Mockito")
class PetServiceMockitoTests {

	@Mock
	PetRepository petRepository;

	@Mock
	VisitRepository visitRepository;

	@InjectMocks
	PetService petService;

	@Test
	@DisplayName("1. stub: devuelve lo que da el repositorio")
	void stub() {
		Pet leo = mascota(7, "Leo");
		when(petRepository.findById(7)).thenReturn(leo);

		assertThat(petService.findPetById(7)).isSameAs(leo);
	}

	@Test
	@DisplayName("2. consultar tipos no toca el repositorio de visitas")
	void sinInteracciones() {
		when(petRepository.findPetTypes()).thenReturn(List.of());

		petService.findPetTypes();

		verifyNoInteractions(visitRepository);
	}

	@Test
	@DisplayName("3. borrar llama una vez a deleteById y a nada mas")
	void verificacionExacta() {
		petService.deletePet(7);

		verify(petRepository, times(1)).deleteById(7);
		verifyNoMoreInteractions(petRepository, visitRepository);
	}

	@Test
	@DisplayName("4. guardar una visita entrega EXACTAMENTE la recibida")
	void captor() {
		Visit visita = new Visit();
		visita.setDescription("Revision anual");

		petService.saveVisit(visita);

		ArgumentCaptor<Visit> captor = ArgumentCaptor.forClass(Visit.class);
		verify(visitRepository).save(captor.capture());
		assertThat(captor.getValue()).isSameAs(visita);
		assertThat(captor.getValue().getDescription()).isEqualTo("Revision anual");
	}

	@Test
	@DisplayName("5. saboteador: el fallo del repositorio se propaga")
	void saboteador() {
		when(petRepository.findById(7)).thenThrow(new DataRetrievalFailureException("base de datos caida"));

		assertThrows(DataRetrievalFailureException.class, () -> petService.findPetById(7));
	}

	@Test
	@DisplayName("6. saboteador sobre un metodo void: doThrow")
	void saboteadorVoid() throws Exception {
		// when(petRepository.save(x)) NO compila: save devuelve void y no se puede
		// pasar void como argumento de when(). Para eso existe la familia doXxx().
		doThrow(new DataIntegrityViolationException("restriccion violada")).when(petRepository).save(any(Pet.class));
		Pet nueva = mascotaDeUnDueno(null, "Nuevo");

		assertThrows(DataIntegrityViolationException.class, () -> petService.savePet(nueva));
	}

	@Test
	@DisplayName("7. nombre repetido en el mismo dueno: excepcion y NO se guarda")
	void reglaDeNegocio() {
		Owner dueno = new Owner();
		Pet existente = mascota(1, "Leo");
		dueno.addPet(existente);
		Pet nueva = mascota(null, "leo");   // la comparacion ignora mayusculas
		dueno.addPet(nueva);

		DuplicatedPetNameException ex = assertThrows(DuplicatedPetNameException.class,
				() -> petService.savePet(nueva));

		assertThat(ex).isNotNull();
		verify(petRepository, never()).save(any());
	}

	@Test
	@DisplayName("8. spy: objeto real con un metodo sobrescrito")
	void spyDeUnaLista() {
		List<String> lista = spy(new ArrayList<>());
		lista.add("a");                    // metodo real
		doReturn(100).when(lista).size();  // con spy, siempre doReturn: when(lista.size()) llamaria al real

		assertThat(lista.get(0)).isEqualTo("a");
		assertThat(lista.size()).isEqualTo(100);
		verify(lista).add("a");
	}

	// --- Ayudas para construir objetos reales del dominio ------------------------

	private static Pet mascota(Integer id, String nombre) {
		Pet pet = new Pet();
		pet.setId(id);
		pet.setName(nombre);
		return pet;
	}

	private static Pet mascotaDeUnDueno(Integer id, String nombre) {
		Owner dueno = new Owner();
		Pet pet = mascota(id, nombre);
		dueno.addPet(pet);
		return pet;
	}

}
