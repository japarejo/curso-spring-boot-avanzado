package org.springframework.samples.petclinic.repository.criteria;

/**
 * LABORATORIO DE CRITERIA API (modulo 6, material adicional).
 *
 * Resultado de una consulta de agregacion. No es una entidad: es una
 * PROYECCION construida por la propia consulta con `cb.construct(...)`.
 *
 * Fijarse en lo que esto evita: sin proyeccion habria que traer todas las
 * visitas con sus mascotas y contar en Java. Aqui cuenta la base de datos y
 * viajan tres valores por fila.
 */
public record ResumenPorTipo(String tipoMascota, long numeroDeVisitas, long mascotasDistintas) {
}
