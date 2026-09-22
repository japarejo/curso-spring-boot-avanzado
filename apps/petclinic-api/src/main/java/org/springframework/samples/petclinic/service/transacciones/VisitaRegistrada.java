package org.springframework.samples.petclinic.service.transacciones;

/**
 * LABORATORIO DE TRANSACCIONES AVANZADAS (modulo 6, material adicional).
 *
 * Evento de dominio. Desde Spring 4.2 no hace falta heredar de
 * ApplicationEvent: cualquier objeto vale, y un record es la eleccion natural
 * porque un evento es un hecho ya ocurrido y por tanto inmutable.
 *
 * El nombre va en pasado a proposito: describe algo que YA paso, no una orden.
 */
public record VisitaRegistrada(int idVisita, String descripcion) {
}
