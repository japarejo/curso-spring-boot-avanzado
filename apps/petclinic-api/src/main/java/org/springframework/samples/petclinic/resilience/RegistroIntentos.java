package org.springframework.samples.petclinic.resilience;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuaderno de bitacora de UNA llamada protegida con @Retry.
 *
 * Se crea por peticion y se pasa como argumento al metodo anotado. Como
 * resilience4j repite la llamada con los mismos argumentos, cada intento
 * anota aqui su hora y su resultado. Asi se ve en la respuesta cuanto ha
 * esperado realmente resilience4j entre un intento y el siguiente.
 */
public class RegistroIntentos {

	/** Un intento: numero, hora, espera desde el intento anterior y resultado. */
	public record Intento(int numero, LocalTime instante, Long msDesdeAnterior, String resultado) {
	}

	private final int fallosSimulados;

	private final TipoFallo tipoFallo;

	private final Instant inicio = Instant.now();

	private final List<Intento> intentos = new ArrayList<>();

	private Instant ultimo;

	private String causaFallback;

	public RegistroIntentos(int fallosSimulados, TipoFallo tipoFallo) {
		this.fallosSimulados = fallosSimulados;
		this.tipoFallo = tipoFallo;
	}

	public synchronized int siguienteIntento() {
		return intentos.size() + 1;
	}

	public synchronized void anotar(String resultado) {
		Instant ahora = Instant.now();
		Long espera = ultimo == null ? null : Duration.between(ultimo, ahora).toMillis();
		LocalTime hora = LocalTime.ofInstant(ahora, ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS);
		intentos.add(new Intento(intentos.size() + 1, hora, espera, resultado));
		ultimo = ahora;
	}

	public synchronized void marcarFallback(Throwable causa) {
		this.causaFallback = causa.getClass().getSimpleName() + ": " + causa.getMessage();
	}

	public int fallosSimulados() {
		return fallosSimulados;
	}

	public TipoFallo tipoFallo() {
		return tipoFallo;
	}

	public synchronized List<Intento> intentos() {
		return List.copyOf(intentos);
	}

	public synchronized String causaFallback() {
		return causaFallback;
	}

	public long msDesdeInicio() {
		return Duration.between(inicio, Instant.now()).toMillis();
	}

}
