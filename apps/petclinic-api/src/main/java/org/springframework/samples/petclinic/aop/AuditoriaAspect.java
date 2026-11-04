package org.springframework.samples.petclinic.aop;

import java.time.Instant;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * MODULO 1 - Aspecto que se aplica solo a los metodos anotados con
 * {@link Auditado}.
 *
 * <h2>El punto de corte</h2>
 * {@code @annotation(auditado)} significa "cualquier metodo que lleve la
 * anotacion del tipo del parametro {@code auditado}". Al escribir el nombre del
 * parametro (en minuscula) en vez del nombre de la clase, Spring ademas nos
 * pasa la anotacion ya leida, con sus atributos: {@code auditado.value()}.
 * Es la diferencia con {@code @annotation(org...Auditado)}, que solo filtra.
 *
 * <h2>El orden</h2>
 * {@code @Transactional} y {@code @PreAuthorize} tambien son aspectos. Con un
 * orden muy bajo la auditoria es la capa mas externa, de modo que tambien
 * registra los accesos denegados y los fallos al hacer commit:
 * <pre>
 *   MIN_VALUE + 1    ExposeInvocationInterceptor (de Spring, siempre primero)
 *   MIN_VALUE + 10   AuditoriaAspect             (este)
 *   200              @PreAuthorize               (AuthorizationInterceptorsOrder)
 *   MAX_VALUE        @Transactional              (por defecto, el mas interno)
 * </pre>
 * Ojo: con {@code HIGHEST_PRECEDENCE} a secas el aspecto quedaria por delante
 * de ExposeInvocationInterceptor y fallaria el enlace del parametro con
 * "Required to bind 2 arguments, but only bound 1 (JoinPointMatch was NOT
 * bound in invocation)".
 *
 * <h2>Lo que no hace</h2>
 * No intercepta metodos privados, finales ni llamadas this.metodo(): el aspecto
 * vive en el proxy y esas llamadas no pasan por el.
 */
@Aspect
@Component
@Order(AuditoriaAspect.ORDEN)
public class AuditoriaAspect {

	/** Despues de ExposeInvocationInterceptor y antes de la seguridad de metodos. */
	public static final int ORDEN = Ordered.HIGHEST_PRECEDENCE + 10;

	private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

	private final RegistroAuditoria registro;

	public AuditoriaAspect(RegistroAuditoria registro) {
		this.registro = registro;
	}

	@Around("@annotation(auditado)")
	public Object auditar(ProceedingJoinPoint pjp, Auditado auditado) throws Throwable {
		long inicio = System.nanoTime();
		String resultado = "OK";
		try {
			return pjp.proceed();
		}
		catch (Throwable ex) {
			resultado = "ERROR " + ex.getClass().getSimpleName();
			throw ex; // el aspecto observa; no se traga la excepcion
		}
		finally {
			EntradaAuditoria entrada = new EntradaAuditoria(Instant.now(), auditado.value(),
					pjp.getSignature().toShortString(), usuarioActual(),
					auditado.conArgumentos() ? Arrays.toString(pjp.getArgs()) : "[ocultos]", resultado,
					(System.nanoTime() - inicio) / 1_000_000);
			registro.registrar(entrada);
			log.info("AUDITORIA {} | {} | usuario={} | args={} | {} | {} ms", entrada.accion(), entrada.metodo(),
					entrada.usuario(), entrada.argumentos(), entrada.resultado(), entrada.duracionMs());
		}
	}

	private static String usuarioActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
			return "anonimo";
		}
		return auth.getName();
	}

}
