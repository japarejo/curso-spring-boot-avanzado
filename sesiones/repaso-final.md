# Repaso final · Diez preguntas que cruzan módulos

Para la sesión 14. Cada pregunta mezcla al menos dos módulos: no se responde recordando una
diapositiva, sino relacionando cosas. Las respuestas están al final, para el docente.

---

1. `@Transactional` y `@PreAuthorize` dejan de funcionar **en el mismo caso**. ¿Cuál, y por qué
   la explicación está en la sesión 1?

2. Un controlador devuelve una entidad `Owner` y la respuesta tarda cada vez más según crece la
   base de datos. Da **dos** causas posibles, de módulos distintos.

3. La prueba de un controlador con `@WebMvcTest` pasa, pero en producción la petición responde
   403. ¿Qué no estaba probando la prueba, y con qué anotaciones se arregla?

4. Cambias `jwt.validity-seconds` en el servidor de configuración y llamas a
   `/actuator/refresh`. Los tokens se siguen emitiendo con la caducidad antigua. ¿Por qué?

5. Una petición `PUT` llega con un `If-Match` de una versión antigua. ¿Qué código devuelve la
   API del ejercicio 10 del módulo 2, y qué concepto del módulo 6 es el mismo en la base de
   datos?

6. `/api/v1/owners?sort=password` no debe funcionar. ¿Qué mecanismo lo impide en el módulo 2?
   ¿Por qué es también una medida del módulo 3?

7. Una prueba con `@DataJpaTest` comprueba que al guardar un departamento se guardan sus
   empleados, y pasa. En producción, los empleados quedan sin departamento. ¿Qué le falta a la
   prueba?

8. Quieres que el *health* de tu aplicación incluya un servicio externo, pero que Kubernetes no
   la reinicie cuando ese servicio se caiga. ¿Cómo?

9. Spring Boot Admin muestra tu aplicación en rojo justo después de proteger Actuator. ¿Qué
   falta?

10. Tienes un listado de propietarios con sus mascotas que hace 1 + N consultas. Propón tres
    soluciones y di cuál elegirías si el listado es paginado de 20 en 20.

---

## Respuestas (para el docente)

1. En las **llamadas internas** (`this.metodo()`) y en los métodos privados. Las dos
   anotaciones funcionan con un *proxy* que envuelve el bean (módulo 1, ejercicio 9); una
   llamada desde dentro del objeto no pasa por el *proxy*.

2. (a) Módulo 6: relaciones `EAGER` o N+1 al serializar las asociaciones. (b) Módulo 2: no hay
   paginación ni DTO, así que se serializa todo; y con `open-in-view` activo, las consultas
   ocurren durante la serialización, fuera del servicio.

3. `@WebMvcTest` sin usuario o con `@WithMockUser` genérico no prueba las **reglas de
   autorización** reales. Se arregla con pruebas de seguridad: `@WithUserDetails`,
   `with(user(...).authorities(...))` o `.with(jwt())` (módulo 4, ejercicio 15).

4. Porque se lee con `@Value` en el constructor de un bean que **no** es `@RefreshScope`. Solo
   los beans `@RefreshScope` y las `@ConfigurationProperties` recogen el valor nuevo (módulo 5,
   ejercicio 10).

5. **412 Precondition Failed**. Es el **bloqueo optimista** (`@Version`): la misma comprobación
   que hace Hibernate con `where version = ?` al actualizar.

6. La **lista blanca** de campos de ordenación (`SORT_PROPERTIES` en
   `RestfulVisitSearchController`, ejercicio 9). Sin ella, ordenar por un campo oculto permite
   deducir su valor fila a fila: es una fuga de información.

7. Vaciar el contexto de persistencia (`flush()` + `clear()`) antes de comprobar. Sin eso, la
   prueba lee los objetos de **memoria**. Y lo que falla en producción es que no se fijó el
   **lado propietario** (`empleado.setDepartamento(...)`) (módulo 6, ejercicio 8).

8. Un `HealthIndicator` propio en un **grupo** aparte, y las sondas de Kubernetes apuntando a
   `/actuator/health/liveness` y `/readiness` (módulo 1, ejercicio 8).

9. Las credenciales del usuario técnico en los **metadatos** del cliente
   (`spring.boot.admin.client.instance.metadata.user.name` / `.password`) (módulo 5,
   ejercicio 9).

10. `LEFT JOIN FETCH`, `@EntityGraph` y `default_batch_fetch_size` (o `@BatchSize`). Con
    paginación, **no** el *fetch join* de una colección: Hibernate pagina en memoria
    (`HHH90003004: firstResult/maxResults specified with collection fetch`). Lo correcto es
    paginar los propietarios y traer sus mascotas con *batch fetching* o con una segunda
    consulta `where owner.id in (...)`.
