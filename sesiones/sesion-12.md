# Sesión 12 · Repositorios, consultas y transacciones

**Módulo 6** · Diapositivas 159–164 (+ 150–153 para transacciones) · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Spring Data ofrece una escalera de formas de consultar: el nombre del
> método, JPQL, proyecciones, especificaciones y SQL nativo. Se sube un peldaño solo cuando el
> anterior no llega. Y todo ocurre dentro de una **transacción**, cuya frontera está en el
> servicio, con un contexto de persistencia que guarda cambios sin que nadie se lo pida.

## Objetivos

- Escribir consultas derivadas, JPQL, nativas, paginadas y de modificación masiva.
- Cargar solo lo necesario con proyecciones.
- Componer filtros dinámicos con `Specification`.
- Explicar la caché de primer nivel, el *dirty checking* y los estados de una entidad.
- Usar `@Transactional` con criterio: *rollback*, `readOnly`, propagación; y transacciones
  programáticas con `TransactionTemplate`.
- Reconocer las dos trampas de `@Transactional`.

## Antes de clase

- [ ] Leer el [guion de transacciones](../modulos/06-spring-data-jpa/guion-transacciones.md):
      en el último bloque se hacen sus apartados 2 y 4.1–4.3.
- [ ] `petclinic-api` con `logging.level.org.hibernate.SQL=DEBUG` a mano para descomentar.
- [ ] Repasar la solución del ejercicio 10 (los datos de `data.sql` que usan las pruebas).

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso: el laboratorio de mapeos | 🗣 | — | M6 ejercicio 8 |
| 0:10 | Repositorios: de `Repository` a `JpaRepository` | 🗣 ⌨ | 159–164 | `repository/` |
| 0:25 | **Consultas con Spring Data** | 🧑‍💻 45' | 162–163 | Ejercicio 10 |
| 1:10 | Proyecciones | ⌨ + 🧑‍💻 | — | Ejercicio 3 |
| 1:25 | **Descanso** | | | |
| 1:35 | **Consultas dinámicas con `Specification`** | 🧑‍💻 20' | 150 (Criteria) | Ejercicio 4 |
| 1:55 | **El contexto de persistencia** | 🧑‍💻 25' | 150–153 | Ejercicio 11 |
| 2:20 | **Transacciones** | ⌨ + 🧑‍💻 30' | 153 | guion de transacciones |
| 2:50 | Auditoría JPA | ⌨ | — | Ejercicio 6 |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

Corrige la relación jefe–subordinados (tarea). Pregunta: «¿qué cambio del laboratorio os
sorprendió más?». Casi siempre es la (h): la prueba que pasa leyendo de memoria.

### 0:10 – 0:25 · Repositorios

📊 Diapositivas 159–164.

⌨ Recorre `repository/` y señala la decisión de diseño: `PetRepository` y `OwnerRepository`
extienden `Repository` (solo los métodos declarados), `VetRepository` extiende
`CrudRepository` (todo el CRUD).

🗣 **Mensaje clave.** «Extender `Repository` y declarar solo lo que se usa es una forma de
**diseño**: el repositorio dice qué operaciones permite el dominio.»

Lanza la pregunta de la diapositiva 164 y deja que la respondan durante el ejercicio: ¿cada
repositorio lo usa un solo servicio? (`PetService` usa `PetRepository` y `VisitRepository`).

### 0:25 – 1:10 · Consultas con Spring Data

📊 Diapositivas 162–163.
🧑‍💻 **Ejercicio 10** (45 min): ocho métodos, ocho pruebas, ocho SQL que leer.

Puntos para recorrer el aula:

- `Page` frente a `Slice`: el `count(*)` adicional.
- Los alias de la JPQL tienen que coincidir con los *getters* de la proyección de interfaz.
- El `UPDATE` masivo se salta el contexto de persistencia: sin `clearAutomatically = true`, se
  lee el valor viejo.

### 1:10 – 1:25 · Proyecciones

⌨ (5 min) Activa el SQL y pide el listado de propietarios: todas las columnas.
🧑‍💻 (10 min) **Ejercicio 3**, pasos 2–3: proyección cerrada y su SELECT de tres columnas.

🗣 **Mensaje clave.** «La proyección condiciona la **consulta**; el DTO del módulo 2 condiciona
la **respuesta**. Se pueden usar juntas.»

### 1:25 – 1:35 · Descanso

### 1:35 – 1:55 · Consultas dinámicas con `Specification`

🧑‍💻 **Ejercicio 4** (20 min): leer `VisitSpecification` y añadir el filtro por ciudad del
propietario.

🗣 **Mensaje clave.** «Concatenar texto para construir una consulta es inyección SQL esperando
a ocurrir. La API de criterios no lo permite.»

Quien quiera más: el [laboratorio de Criteria API](../modulos/06-spring-data-jpa/laboratorio-criteria-api.md).

### 1:55 – 2:20 · El contexto de persistencia

📊 Diapositivas 150–153.
🧑‍💻 **Ejercicio 11** (25 min): caché de primer nivel, *dirty checking*, *detach*/*merge*,
`TransactionTemplate` y `readOnly`.

🗣 **Mensaje clave.** «"No he llamado a `save` y se ha guardado" y "he cambiado el objeto y no
se ha guardado" son la misma pregunta: ¿estaba la entidad **gestionada** dentro de una
transacción abierta?»

### 2:20 – 2:50 · Transacciones

📊 Diapositiva 153.

1. ⌨ (5 min) Apartado 1 del guion de transacciones: la frontera transaccional va en el
   servicio, no en el controlador ni en el repositorio.
2. 🧑‍💻 (10 min) Apartado 2: *rollback* con `RuntimeException`.
3. 🧑‍💻 (15 min) Apartados 4.1 a 4.3: `rollbackFor` con excepción comprobada, `readOnly`, y
   `REQUIRED` frente a `REQUIRES_NEW`.

🗣 **Mensaje clave.** «Dos trampas, la misma causa (sesión 1, el *proxy*): `@Transactional` no
se aplica a métodos privados ni a llamadas desde la propia clase. Y por defecto solo deshace
con excepciones **no comprobadas**.»

Las cinco trampas que no lanzan ninguna excepción están en el
[laboratorio de transacciones avanzadas](../modulos/06-spring-data-jpa/laboratorio-transacciones-avanzadas.md).

### 2:50 – 2:55 · Auditoría JPA

⌨ Demo del ejercicio 6: `AuditableEntity`, `AuditorAwareImpl` y las columnas `created_by` /
`last_modified_by` rellenas solas.

### 2:55 – 3:00 · Cierre y tarea

- **Tarea:**
  - Ejercicio 6 completo y ejercicio 3 con proyección basada en `record`.
  - Apartados 4.4 y 4.5 del guion de transacciones.
- Avisa: en la sesión 13 se presenta el **reto integrador**.

---

## Si vas con retraso

- Ejercicio 10: hacer los métodos 1, 2, 4, 6 y 8; el resto, tarea.
- Ejercicio 11: pruebas 1, 2 y 5.
- Auditoría: se puede saltar (es tarea).

## Si te sobra tiempo

- Autoinvocación del laboratorio de transacciones avanzadas (15 min): la trampa explicada con
  código que funciona y una prueba que lo demuestra.
- `@Modifying` sin `@Transactional`: provocar `TransactionRequiredException`.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `No property 'xxx' found for type 'Owner'` al arrancar | El nombre del método derivado no coincide con un campo. Spring lo valida al arrancar. |
| La proyección devuelve `null` en un campo | El alias de la JPQL no coincide con el *getter*. |
| `TransactionRequiredException` en el `UPDATE` | Falta `@Transactional` (en `@DataJpaTest` ya la hay). |
| `InvalidDataAccessApiUsageException: For queries with named parameters you need to provide names` | Falta `@Param` o el compilador sin `-parameters`. |
| La prueba con `TransactionTemplate` ve datos de otra | No tiene *rollback* automático (`NOT_SUPPORTED`): hay que limpiar a mano. |
