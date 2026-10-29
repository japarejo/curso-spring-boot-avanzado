package org.springframework.samples.petclinic.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.NamedEntity;
import org.springframework.samples.petclinic.repository.DiseaseRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.samples.petclinic.repository.criteria.FichaMascota;
import org.springframework.samples.petclinic.repository.criteria.FiltroMascotas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ESCENARIO DE CRITERIA API: busqueda avanzada de pacientes (modulo 6).
 *
 * Servicio fino: la logica esta en la consulta. Su otra tarea es dar a la
 * pantalla los valores de los desplegables (tipos, ciudades, enfermedades y
 * veterinarios), para que el usuario elija entre valores que existen.
 */
@Service
public class BusquedaMascotasService {

	private final PetRepository petRepository;

	private final DiseaseRepository diseaseRepository;

	private final VetRepository vetRepository;

	public BusquedaMascotasService(PetRepository petRepository, DiseaseRepository diseaseRepository,
			VetRepository vetRepository) {
		this.petRepository = petRepository;
		this.diseaseRepository = diseaseRepository;
		this.vetRepository = vetRepository;
	}

	@Transactional(readOnly = true)
	public Page<FichaMascota> buscar(FiltroMascotas filtro, Pageable pagina) {
		return petRepository.buscarMascotas(filtro, pagina);
	}

	@Transactional(readOnly = true)
	public OpcionesBusqueda opciones() {
		List<String> tipos = petRepository.findPetTypes().stream().map(NamedEntity::getName).toList();
		List<Opcion> enfermedades = diseaseRepository.findAll()
			.stream()
			.map(d -> new Opcion(d.getId(), d.getName()))
			.sorted(Comparator.comparing(Opcion::nombre))
			.toList();
		List<Opcion> veterinarios = vetRepository.findAll()
			.stream()
			.map(v -> new Opcion(v.getId(), v.getFirstName() + " " + v.getLastName()))
			.sorted(Comparator.comparing(Opcion::nombre))
			.toList();
		return new OpcionesBusqueda(tipos, petRepository.ciudadesDePropietarios(), enfermedades, veterinarios);
	}

	public record Opcion(Integer id, String nombre) {
	}

	public record OpcionesBusqueda(List<String> tipos, List<String> ciudades, List<Opcion> enfermedades,
			List<Opcion> veterinarios) {
	}

}
