package org.springframework.samples.petclinic.web.api;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

import org.springframework.samples.petclinic.resilience.RegistroIntentos;
import org.springframework.samples.petclinic.resilience.RetryDemoGateway;
import org.springframework.samples.petclinic.resilience.RetryDemoGateway.ConfiguracionRetry;
import org.springframework.samples.petclinic.resilience.TipoFallo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * MODULO 5 - Demostracion de las propiedades de reintento de resilience4j.
 *
 * No necesita ningun otro servicio levantado: el "servicio remoto" es un bean
 * que falla las primeras N llamadas. Cada respuesta incluye la politica leida
 * de application.properties y, para cada intento, la hora y los milisegundos
 * transcurridos desde el anterior. Asi se comprueba que la espera real
 * coincide con la configurada.
 *
 *   GET /api/v1/resilience-demo/retry                          politicas configuradas
 *   GET /api/v1/resilience-demo/retry/espera-fija?fallos=2     1000 ms entre intentos
 *   GET /api/v1/resilience-demo/retry/exponencial?fallos=4     200, 400, 800, 1000 (tope)
 *   GET /api/v1/resilience-demo/retry/exponencial-aleatoria?fallos=4
 *   GET /api/v1/resilience-demo/retry/excepciones?fallos=2&tipo=permanente
 *   GET /api/v1/resilience-demo/retry/resultado?fallos=2
 *
 * Con fallos mayor o igual que max-attempts se agotan los intentos y responde
 * el metodo de reserva (fallback).
 */
@RestController
@RequestMapping("/api/v1/resilience-demo/retry")
@Tag(name = "Resilience demo", description = "Propiedades de reintento de resilience4j: intentos, esperas y excepciones")
public class RetryDemoRestController {

	/** Escenario de la URL, instancia de @Retry que usa y metodo que la invoca. */
	private enum Escenario {

		ESPERA_FIJA("espera-fija", "demoEsperaFija", RetryDemoGateway::esperaFija),
		EXPONENCIAL("exponencial", "demoExponencial", RetryDemoGateway::exponencial),
		EXPONENCIAL_ALEATORIA("exponencial-aleatoria", "demoExponencialAleatoria", RetryDemoGateway::exponencialAleatoria),
		EXCEPCIONES("excepciones", "demoExcepciones", RetryDemoGateway::excepciones),
		RESULTADO("resultado", "demoResultado", RetryDemoGateway::porResultado);

		final String ruta;

		final String instancia;

		final BiFunction<RetryDemoGateway, RegistroIntentos, String> llamada;

		Escenario(String ruta, String instancia,
				BiFunction<RetryDemoGateway, RegistroIntentos, String> llamada) {
			this.ruta = ruta;
			this.instancia = instancia;
			this.llamada = llamada;
		}

		static Escenario deRuta(String ruta) {
			return Arrays.stream(values())
				.filter(e -> e.ruta.equals(ruta))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Escenario desconocido: " + ruta
						+ ". Disponibles: " + Arrays.stream(values()).map(e -> e.ruta).toList()));
		}

	}

	/** Lo que devuelve cada demostracion. */
	public record InformeReintentos(String escenario, ConfiguracionRetry configuracion,
			List<RegistroIntentos.Intento> intentos, String resultado, String causaFallback, long duracionTotalMs) {
	}

	private final RetryDemoGateway gateway;

	public RetryDemoRestController(RetryDemoGateway gateway) {
		this.gateway = gateway;
	}

	@GetMapping
	@Operation(summary = "Politica de reintento efectiva de cada escenario",
			description = "Leida de resilience4j, es decir, despues de aplicar configs.default y las propiedades de cada instancia.")
	public List<ConfiguracionRetry> configuraciones() {
		return Arrays.stream(Escenario.values()).map(e -> e.instancia).map(gateway::configuracion).toList();
	}

	@GetMapping("/{escenario}")
	@Operation(summary = "Ejecuta una llamada que falla las primeras N veces y muestra cada intento",
			description = "Escenarios: espera-fija, exponencial, exponencial-aleatoria, excepciones, resultado.")
	public InformeReintentos ejecutar(
			@Parameter(description = "espera-fija | exponencial | exponencial-aleatoria | excepciones | resultado",
					example = "espera-fija") @PathVariable String escenario,
			@Parameter(description = "Numero de intentos que fallan antes de responder bien") @RequestParam(defaultValue = "2") int fallos,
			@Parameter(description = "transitorio (se reintenta) o permanente (solo lo ignora demoExcepciones)") @RequestParam(defaultValue = "transitorio") String tipo) {
		Escenario e = Escenario.deRuta(escenario);
		RegistroIntentos registro = new RegistroIntentos(fallos, TipoFallo.valueOf(tipo.toUpperCase(Locale.ROOT)));
		String resultado = e.llamada.apply(gateway, registro);
		return new InformeReintentos(e.ruta, gateway.configuracion(e.instancia), registro.intentos(), resultado,
				registro.causaFallback(), registro.msDesdeInicio());
	}

}
