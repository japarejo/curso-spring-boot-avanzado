package org.springframework.samples.petclinic.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MODULO 1 - Marca un metodo (o todos los metodos publicos de una clase) para
 * que {@link CronometradoAspect} mida cuanto tarda y avise si es lento.
 * <p>
 * Si la clase y el metodo llevan la anotacion, manda la del metodo.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cronometrado {

	/** A partir de cuantos milisegundos se considera lento. */
	long umbralMs() default 500;

}
