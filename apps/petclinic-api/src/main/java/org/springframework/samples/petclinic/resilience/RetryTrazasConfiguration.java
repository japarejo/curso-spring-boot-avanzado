package org.springframework.samples.petclinic.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;

/**
 * Escribe en el log cada evento de reintento de TODAS las instancias de @Retry.
 *
 * resilience4j recoge los beans RegistryEventConsumer<Retry> y los avisa cada
 * vez que se crea una instancia; aqui se aprovecha para suscribirse a sus
 * eventos. En la consola se ve la espera que se ha aplicado antes de cada
 * reintento, que es justo lo que controlan las propiedades wait-duration,
 * enable-exponential-backoff y enable-randomized-wait.
 *
 * Los mismos eventos se consultan en /actuator/retryevents.
 */
@Configuration(proxyBeanMethods = false)
public class RetryTrazasConfiguration {

	private static final Logger log = LoggerFactory.getLogger(RetryTrazasConfiguration.class);

	@Bean
	RegistryEventConsumer<Retry> trazasDeReintentos() {
		return new RegistryEventConsumer<>() {

			@Override
			public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
				entryAddedEvent.getAddedEntry()
					.getEventPublisher()
					.onRetry(e -> log.info("[{}] intento {} fallido ({}). Siguiente intento dentro de {} ms",
							e.getName(), e.getNumberOfRetryAttempts(), describir(e.getLastThrowable()),
							e.getWaitInterval().toMillis()))
					.onSuccess(e -> log.info("[{}] exito tras {} reintento(s)", e.getName(),
							e.getNumberOfRetryAttempts()))
					.onError(e -> log.warn("[{}] se agotan los intentos tras {} reintento(s): {}", e.getName(),
							e.getNumberOfRetryAttempts(), describir(e.getLastThrowable())))
					.onIgnoredError(e -> log.warn("[{}] excepcion no reintentable, se propaga sin reintentar: {}",
							e.getName(), describir(e.getLastThrowable())));
			}

			@Override
			public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
			}

			@Override
			public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
			}

		};
	}

	private static String describir(Throwable t) {
		return t == null ? "resultado no valido" : t.getClass().getSimpleName() + ": " + t.getMessage();
	}

}
