package org.springframework.samples.petclinic.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MODULO 1 - Ejemplo de aspecto aplicado con una anotacion propia.
 *
 * Marca un metodo de negocio cuya ejecucion debe quedar auditada: quien lo
 * llamo, con que argumentos, cuanto tardo y si termino bien o con excepcion.
 * La anotacion no hace nada por si sola; es {@link AuditoriaAspect} quien la
 * busca y envuelve el metodo.
 *
 * Dos detalles imprescindibles:
 * <ul>
 * <li>{@code RUNTIME}: si la retencion fuera CLASS (la de defecto), la anotacion
 * no existiria en ejecucion y el aspecto no la encontraria nunca.</li>
 * <li>Solo funciona en metodos publicos de beans, llamados desde fuera del bean
 * (pasan por el proxy). Igual que {@code @Transactional}.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditado {

	/** Nombre de la operacion de negocio, p. ej. "alta de visita". */
	String value();

	/**
	 * Si es false no se guardan los argumentos (datos personales, contrasenas,
	 * objetos enormes...).
	 */
	boolean conArgumentos() default true;

}
