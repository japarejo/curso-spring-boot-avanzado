package org.springframework.samples.petclinic.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MODULO 1 - Mide los metodos anotados con {@link Cronometrado}: escribe un
 * WARN si la llamada llega al umbral y una linea DEBUG si no.
 *
 * <h2>Dos consejos, un solo cronometro</h2>
 * {@code @annotation(x)} solo mira el metodo ejecutado y {@code @within(x)} la
 * clase que lo declara. El segundo consejo excluye los metodos anotados, asi
 * la anotacion del metodo gana a la de la clase y no se mide dos veces.
 *
 * <h2>El orden</h2>
 * Justo por dentro de {@link AuditoriaAspect}: el tiempo medido no incluye la
 * auditoria, pero si abrir la transaccion y el commit (@Transactional es el
 * aspecto mas interno).
 */
@Aspect
@Component
@Order(AuditoriaAspect.ORDEN + 1)
public class CronometradoAspect {

	private static final Logger log = LoggerFactory.getLogger(CronometradoAspect.class);

	@Around("@annotation(cronometrado)")
	public Object enMetodo(ProceedingJoinPoint pjp, Cronometrado cronometrado) throws Throwable {
		return medir(pjp, cronometrado.umbralMs());
	}

	@Around("@within(cronometrado) && !@annotation(org.springframework.samples.petclinic.aop.Cronometrado)")
	public Object enClase(ProceedingJoinPoint pjp, Cronometrado cronometrado) throws Throwable {
		return medir(pjp, cronometrado.umbralMs());
	}

	private Object medir(ProceedingJoinPoint pjp, long umbralMs) throws Throwable {
		long inicio = System.nanoTime();
		try {
			return pjp.proceed();
		}
		finally {
			long ms = (System.nanoTime() - inicio) / 1_000_000;
			if (ms >= umbralMs) {
				log.warn("LENTO {} tardo {} ms (umbral {} ms)", pjp.getSignature().toShortString(), ms, umbralMs);
			}
			else {
				log.debug("{} tardo {} ms", pjp.getSignature().toShortString(), ms);
			}
		}
	}

}
