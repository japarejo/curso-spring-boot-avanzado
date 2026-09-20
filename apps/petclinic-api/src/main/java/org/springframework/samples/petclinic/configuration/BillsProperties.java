package org.springframework.samples.petclinic.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * MODULO 5 - Configuracion tipada.
 *
 * Agrupa las propiedades `petclinic.bills.*` en un objeto inmutable y validado,
 * en lugar de repartir @Value("${...}") por el codigo.
 *
 * Tres ventajas que conviene senalar en clase:
 *   1. Validacion al arrancar: si falta la URL, la aplicacion no levanta y dice
 *      exactamente que propiedad falta. Con @Value el fallo aparece mas tarde.
 *   2. Autocompletado en el IDE, porque el tipo se conoce.
 *   3. Conversion automatica: "5s" se convierte en Duration sin escribir nada.
 *
 * En el proyecto anterior esta misma URL estaba escrita a fuego en tres sitios
 * distintos, con tres valores diferentes, y el token JWT iba en el codigo fuente.
 */
@Validated
@ConfigurationProperties(prefix = "petclinic.bills")
public record BillsProperties(

		@NotBlank String baseUrl,

		@DefaultValue("2s") Duration connectTimeout,

		@DefaultValue("5s") Duration readTimeout) {
}
