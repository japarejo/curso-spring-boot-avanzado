package org.springframework.samples.petclinic.roles.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Casos de uso de la clinica, con los datos en memoria para no distraer.
 *
 * <p>Compara las dos formas de proteger un metodo:
 *
 * <ul>
 * <li>{@link #turnos()} pregunta por un ROL. El codigo queda atado al
 * organigrama: si manana un "becario" tambien debe ver los turnos, hay que
 * tocar esta linea o la jerarquia.</li>
 * <li>Todos los demas preguntan por un PRIVILEGIO. El codigo solo dice QUE
 * accion se intenta; QUIEN puede hacerla se decide en la base de datos.</li>
 * </ul>
 *
 * <p>Regla practica: el codigo pregunta por privilegios; los roles son para
 * administrar (asignar a personas y agrupar privilegios).
 */
@Service
public class ClinicaService {

	private final List<String> mascotas = new CopyOnWriteArrayList<>(
			List.of("Leo (gato)", "Basil (hamster)", "Rosy (perro)"));
	private final List<String> visitas = new CopyOnWriteArrayList<>();

	/** Por ROL: cualquier empleado, y por jerarquia, cualquier rol superior. */
	@PreAuthorize("hasRole('EMPLEADO')")
	public List<String> turnos() {
		return List.of("Lunes: victor (consulta), rita (mostrador)",
				"Martes: pablo (consulta y mostrador)",
				"Miercoles: eva (apoyo)");
	}

	@PreAuthorize("hasAuthority('MASCOTA_LEER')")
	public List<String> mascotas() {
		return List.copyOf(mascotas);
	}

	@PreAuthorize("hasAuthority('MASCOTA_EDITAR')")
	public String altaMascota(String nombre) {
		mascotas.add(nombre);
		return nombre;
	}

	@PreAuthorize("hasAuthority('VISITA_REGISTRAR')")
	public String registrarVisita(String mascota, String motivo) {
		String visita = mascota + ": " + motivo;
		visitas.add(visita);
		return visita;
	}

	@PreAuthorize("hasAuthority('FACTURA_VER')")
	public List<String> facturas() {
		return List.of("F-2026-001 Leo 45,00 EUR", "F-2026-002 Rosy 80,00 EUR");
	}

}
