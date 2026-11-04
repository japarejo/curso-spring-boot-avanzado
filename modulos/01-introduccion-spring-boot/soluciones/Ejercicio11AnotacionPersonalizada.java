// =============================================================================
// Modulo 1 - Ejercicio 11: un aspecto que se activa con una anotacion propia.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// Cada bloque va en su propio fichero, en el paquete aop/ de petclinic-api.
// Se ha probado tal cual en petclinic-api (Spring Boot 3.5).
// =============================================================================

// ---------- Parte A. Respuestas a las preguntas del ejemplo @Auditado ----------
//
// A.1 DELETE /api/pets/1 sin sesion: la seguridad de URL deja pasar (permitAll),
//     pero @PreAuthorize de PetService.deletePet lo rechaza. Aun asi aparece:
//       AUDITORIA borrado de mascota | PetService.deletePet(..) | usuario=anonimo
//                 | args=[1] | ERROR AuthorizationDeniedException | 5 ms
//     porque AuditoriaAspect tiene un orden menor (MIN_VALUE + 10) que el de
//     @PreAuthorize (200): es la capa externa y ve la excepcion al salir.
//     Con @Order(300), la seguridad cortaria antes y no habria ninguna linea.
//
// A.2 Sin @Retention(RUNTIME) la anotacion se queda en el .class pero la JVM no
//     la carga: el punto de corte no encuentra nada, no hay ningun error y las
//     pruebas de AuditoriaAspectTests fallan con "Expected size: 1 but was: 0".
//
// A.3 Con @Order(Ordered.HIGHEST_PRECEDENCE) el aspecto se coloca por delante de
//     ExposeInvocationInterceptor y, al enlazar el parametro "auditado", falla:
//       IllegalStateException: Required to bind 2 arguments, but only bound 1
//       (JoinPointMatch was NOT bound in invocation)

// ---------- B.1 aop/Cronometrado.java ----------
package org.springframework.samples.petclinic.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })   // TYPE: tambien en la clase
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cronometrado {

	/** A partir de cuantos milisegundos se considera lento. */
	long umbralMs() default 500;

}

// ---------- B.2 aop/CronometradoAspect.java ----------
/*
package org.springframework.samples.petclinic.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(AuditoriaAspect.ORDEN + 1)   // justo por dentro de la auditoria
public class CronometradoAspect {

	private static final Logger log = LoggerFactory.getLogger(CronometradoAspect.class);

	// 1) Anotacion en el METODO: @annotation mira solo el metodo ejecutado.
	@Around("@annotation(cronometrado)")
	public Object enMetodo(ProceedingJoinPoint pjp, Cronometrado cronometrado) throws Throwable {
		return medir(pjp, cronometrado.umbralMs());
	}

	// 2) Anotacion en la CLASE: @within mira la clase que declara el metodo.
	//    El "&& !@annotation(...)" evita medir dos veces cuando hay anotacion en
	//    los dos sitios: asi la del metodo gana a la de la clase.
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
*/
// Alternativa con un solo consejo: "@within(Cronometrado) || @annotation(Cronometrado)"
// sin enlazar parametros, y dentro buscar la anotacion a mano:
//   Method m = ((MethodSignature) pjp.getSignature()).getMethod();
//   Cronometrado c = AnnotatedElementUtils.findMergedAnnotation(m, Cronometrado.class);
//   if (c == null) c = AnnotatedElementUtils.findMergedAnnotation(pjp.getTarget().getClass(), Cronometrado.class);
// Funciona igual, pero es mas codigo y lee la anotacion en cada llamada.

// ---------- B.3 Donde aplicarla ----------
/*
	@Service
	@Validated
	@Cronometrado(umbralMs = 200)          // todos los metodos publicos de VisitService
	public class VisitService { ... }

	// en PetService:
	@Cronometrado(umbralMs = 50)           // solo este metodo, con su propio umbral
	@Transactional(readOnly = true)
	public List<Pet> searchPetsWithVisits(...) { ... }
*/

// ---------- B.4 test/.../aop/CronometradoAspectTests.java ----------
/*
package org.springframework.samples.petclinic.aop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

@SpringBootTest(classes = CronometradoAspectTests.App.class,
		properties = { "eureka.client.enabled=false", "spring.boot.admin.client.enabled=false" })
@ExtendWith(OutputCaptureExtension.class)
class CronometradoAspectTests {

	@Autowired
	Lento lento;

	@Test
	void laAnotacionDelMetodoGanaALaDeLaClase(CapturedOutput salida) throws Exception {
		lento.rapidoConUmbralCero();   // umbral del metodo: 0 ms -> siempre LENTO
		lento.dormir(30);              // umbral de la clase: 10 ms -> LENTO
		lento.noTanLento();            // umbral del metodo: 1000 ms -> no

		assertThat(salida).contains("LENTO Lento.rapidoConUmbralCero()")
			.contains("LENTO Lento.dormir(..)")
			.doesNotContain("LENTO Lento.noTanLento()");
		// Una sola linea por llamada: el consejo de clase no se suma al del metodo
		assertThat(salida.getOut().split("LENTO Lento.rapidoConUmbralCero").length - 1).isEqualTo(1);
	}

	@Service
	@Cronometrado(umbralMs = 10)
	static class Lento {

		@Cronometrado(umbralMs = 0)
		public void rapidoConUmbralCero() {
		}

		public void dormir(long ms) throws InterruptedException {
			Thread.sleep(ms);
		}

		@Cronometrado(umbralMs = 1000)
		public void noTanLento() throws InterruptedException {
			Thread.sleep(30);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
			SqlInitializationAutoConfiguration.class })
	@Import({ CronometradoAspect.class, Lento.class })
	static class App {

	}

}
*/

// ---------- B.5 El orden de los dos aspectos en VisitService.create ----------
// create() lleva @Auditado, @Cronometrado (heredado de la clase) y @Transactional:
//
//   AuditoriaAspect (MIN+10) -> CronometradoAspect (MIN+11) -> @Transactional (MAX) -> create()
//
// - El cronometro NO incluye el tiempo de la auditoria (esta por dentro de ella).
// - El cronometro SI incluye abrir la transaccion y el commit (esta por fuera).
// Si se le pone @Order(Ordered.LOWEST_PRECEDENCE) (el mismo que @Transactional)
// el orden entre los dos queda sin definir: hay que fijarlo siempre que importe.

// ---------- B.6 La trampa ----------
// @Cronometrado en un metodo private, final o llamado con this.metodo() no hace
// nada y Spring no avisa. Los final se ven activando:
//   logging.level.org.springframework.aop.framework.CglibAopProxy=DEBUG
// Los private y la autoinvocacion no dejan ninguna traza.
