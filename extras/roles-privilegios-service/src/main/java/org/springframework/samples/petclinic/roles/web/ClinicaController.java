package org.springframework.samples.petclinic.roles.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.roles.service.ClinicaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador sin una sola linea de seguridad: la autorizacion esta en
 * {@link ClinicaService}, que es donde vive el caso de uso.
 */
@RestController
@RequestMapping("/api")
public class ClinicaController {

	public record NuevaMascota(String nombre) {
	}

	public record NuevaVisita(String mascota, String motivo) {
	}

	private final ClinicaService clinica;

	public ClinicaController(ClinicaService clinica) {
		this.clinica = clinica;
	}

	@GetMapping("/turnos")
	public List<String> turnos() {
		return clinica.turnos();
	}

	@GetMapping("/mascotas")
	public List<String> mascotas() {
		return clinica.mascotas();
	}

	@PostMapping("/mascotas")
	@ResponseStatus(HttpStatus.CREATED)
	public String altaMascota(@RequestBody NuevaMascota mascota) {
		return clinica.altaMascota(mascota.nombre());
	}

	@PostMapping("/visitas")
	@ResponseStatus(HttpStatus.CREATED)
	public String registrarVisita(@RequestBody NuevaVisita visita) {
		return clinica.registrarVisita(visita.mascota(), visita.motivo());
	}

	@GetMapping("/facturas")
	public List<String> facturas() {
		return clinica.facturas();
	}

}
