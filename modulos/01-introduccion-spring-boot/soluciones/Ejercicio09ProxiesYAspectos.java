// =============================================================================
// Modulo 1 - Ejercicio 9: los objetos que Spring crea por ti (diapositiva 26).
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================

// ---------- 1. En PetclinicApplication: imprimir la clase real ----------
/*
	@Bean
	CommandLineRunner verProxies(PetService pets, VisitService visits, OwnerRepository owners) {
		return args -> {
			System.out.println("PetService      -> " + pets.getClass());
			System.out.println("VisitService    -> " + visits.getClass());
			System.out.println("OwnerRepository -> " + owners.getClass());
			System.out.println("PetService es proxy AOP: " + AopUtils.isAopProxy(pets));
		};
	}
*/
// Salida tipica:
//   PetService      -> class ...service.PetService$$SpringCGLIB$$0
//   VisitService    -> class ...service.VisitService$$SpringCGLIB$$0
//   OwnerRepository -> class jdk.proxy2.$Proxy142
//
// Por que:
//   - PetService tiene metodos @Transactional y VisitService es @Validated. Spring
//     necesita interceptar sus llamadas, asi que crea una SUBCLASE en tiempo de
//     ejecucion (CGLIB) que envuelve cada metodo publico.
//   - OwnerRepository es una interfaz sin implementacion escrita: Spring Data
//     genera un proxy dinamico de la JDK que implementa la interfaz.
//
// Consecuencias practicas (y por eso se ensena esto en el modulo 1):
//   - los metodos privados o final no se pueden interceptar;
//   - una llamada this.otroMetodo() NO pasa por el proxy (autoinvocacion);
//   - las anotaciones @Transactional, @PreAuthorize, @Cacheable, @Async y
//     @Validated dependen todas de este mecanismo.

// ---------- 2. aop/MedidorDeTiempos.java ----------
package org.springframework.samples.petclinic.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Si org.aspectj no se resuelve, anadir al pom.xml de petclinic-api:
 *
 *   <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-aop</artifactId>
 *   </dependency>
 *
 * (En Spring Boot 4.x este starter se llama spring-boot-starter-aspectj.)
 */
@Aspect
@Component
public class MedidorDeTiempos {

	private static final Logger log = LoggerFactory.getLogger(MedidorDeTiempos.class);

	// within(paquete..*) = cualquier metodo de cualquier clase de ese paquete o
	// de sus subpaquetes. Spring AOP solo intercepta beans, y solo metodos publicos.
	@Around("within(org.springframework.samples.petclinic.service..*)")
	public Object medir(ProceedingJoinPoint pjp) throws Throwable {
		long inicio = System.nanoTime();
		try {
			return pjp.proceed();
		}
		finally {
			long ms = (System.nanoTime() - inicio) / 1_000_000;
			log.info("{} tardo {} ms", pjp.getSignature().toShortString(), ms);
		}
	}

}

// ---------- 3. El experimento de la autoinvocacion ----------
// En PetService, por ejemplo:
/*
	public int numeroDeTipos() {
		return findPetTypes().size();   // llamada interna: this.findPetTypes()
	}
*/
// Al llamar a numeroDeTipos() desde un controlador, el registro muestra
//   PetService.numeroDeTipos() tardo N ms
// pero NO
//   PetService.findPetTypes() tardo N ms
// porque la segunda llamada va de `this` a `this`, sin pasar por el proxy.
// Mismo motivo que el laboratorio de transacciones avanzadas del modulo 6
// (service/transacciones/AutoinvocacionService).
