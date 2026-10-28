package org.springframework.samples.petclinic.resilience;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * MODULO 5 - Reintentos con resilience4j.
 *
 * Todos los metodos hacen lo mismo (llamar al servicio simulado); lo unico que
 * cambia es el NOMBRE de la instancia de @Retry. Toda la politica (cuantos
 * intentos, cuanto esperar, ante que errores) esta en application.properties,
 * bajo resilience4j.retry.instances.<nombre>. Cambiar la politica no exige
 * tocar ni recompilar el codigo.
 *
 *   demoEsperaFija            max-attempts + wait-duration
 *   demoExponencial           enable-exponential-backoff, multiplier, max-wait
 *   demoExponencialAleatoria  lo anterior + enable-randomized-wait (jitter)
 *   demoExcepciones           retry-exceptions / ignore-exceptions
 *   demoResultado             result-predicate + fail-after-max-attempts
 *
 * Se prueba desde Swagger UI con /api/v1/resilience-demo/retry/{escenario}.
 */
@Component
public class RetryDemoGateway {

	private final ServicioRemotoSimulado servicio;

	private final RetryRegistry retryRegistry;

	public RetryDemoGateway(ServicioRemotoSimulado servicio, RetryRegistry retryRegistry) {
		this.servicio = servicio;
		this.retryRegistry = retryRegistry;
	}

	@Retry(name = "demoEsperaFija", fallbackMethod = "agotado")
	public String esperaFija(RegistroIntentos registro) {
		return servicio.invocar(registro);
	}

	@Retry(name = "demoExponencial", fallbackMethod = "agotado")
	public String exponencial(RegistroIntentos registro) {
		return servicio.invocar(registro);
	}

	@Retry(name = "demoExponencialAleatoria", fallbackMethod = "agotado")
	public String exponencialAleatoria(RegistroIntentos registro) {
		return servicio.invocar(registro);
	}

	@Retry(name = "demoExcepciones", fallbackMethod = "agotado")
	public String excepciones(RegistroIntentos registro) {
		return servicio.invocar(registro);
	}

	@Retry(name = "demoResultado", fallbackMethod = "agotado")
	public String porResultado(RegistroIntentos registro) {
		return servicio.consultarEstado(registro);
	}

	/**
	 * Metodo de reserva comun. Se ejecuta cuando se agotan los intentos o
	 * cuando la excepcion no es reintentable. Firma: la del metodo protegido
	 * mas el Throwable final.
	 */
	String agotado(RegistroIntentos registro, Throwable causa) {
		registro.marcarFallback(causa);
		return "FALLBACK tras " + registro.intentos().size() + " intento(s)";
	}

	/**
	 * Lee de resilience4j la politica efectiva de una instancia, ya combinada
	 * con la configuracion base. Las esperas se calculan con la misma funcion
	 * que usa resilience4j; con espera aleatoria salen distintas cada vez.
	 */
	public ConfiguracionRetry configuracion(String instancia) {
		RetryConfig config = retryRegistry.retry(instancia).getRetryConfig();
		Either<Throwable, Object> falloEjemplo = Either.left(new FalloTransitorioException("ejemplo"));
		List<Long> esperas = IntStream.range(1, config.getMaxAttempts())
			.mapToObj(intento -> config.<Object>getIntervalBiFunction().apply(intento, falloEjemplo))
			.toList();
		return new ConfiguracionRetry(instancia, config.getMaxAttempts(), esperas, config.isFailAfterMaxAttempts());
	}

	/** Politica efectiva de una instancia de reintento. */
	public record ConfiguracionRetry(String instancia, int maxAttempts, List<Long> esperasEntreIntentosMs,
			boolean failAfterMaxAttempts) {
	}

}
