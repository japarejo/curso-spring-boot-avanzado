# Sesión 13 · Rendimiento con JPA y cierre del temario

**Módulo 6** · Diapositivas 171–175 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** El rendimiento de una aplicación JPA no se adivina: **se mide**
> contando consultas. El problema N+1 no lo causa la consulta, sino el bucle que viene después;
> y `EAGER` no lo resuelve, solo lo cambia de sitio. `LAZY` por defecto, y traer lo necesario
> cuando se sabe que se necesita.

## Objetivos

- Explicar la carga adelantada y la perezosa, y sus valores por defecto.
- Detectar el N+1 en el registro de SQL y corregirlo con `JOIN FETCH` y `@EntityGraph`.
- Medir el número de consultas con las estadísticas de Hibernate y comparar estrategias,
  incluido el *batch fetching*.
- Insertar en lotes y saber por qué `IDENTITY` lo impide.
- Entender qué hace `open-in-view` y por qué el proyecto lo desactiva.
- Repasar los nueve consejos del curso y presentar el reto integrador.

## Antes de clase

- [ ] Leer el [guion de N+1](../modulos/06-spring-data-jpa/guion-consultas-n-mas-1.md): los
      dos primeros bloques lo siguen.
- [ ] Probar el perfil `nplus1`:
      `./mvnw -pl apps/petclinic-api spring-boot:run -Dspring-boot.run.profiles=nplus1`.
- [ ] Tener a mano [`modulos/reto-integrador.md`](../modulos/reto-integrador.md) para el final.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso: transacciones | 🗣 | — | |
| 0:10 | Carga adelantada y perezosa; `open-in-view` | 🗣 ⌨ | 171–172 | `application-nplus1.properties` |
| 0:25 | **Detectar y corregir el N+1** | 🧑‍💻 45' | 173–174 | guion N+1, ejercicios 1–3 |
| 1:10 | **Por qué `EAGER` no es la solución** | 🧑‍💻 15' | 172 | guion N+1, ejercicios 4–5 |
| 1:25 | **Descanso** | | | |
| 1:35 | **Medir el N+1** | 🧑‍💻 30' | 171–174 | M6 ejercicio 12 |
| 2:05 | **Escritura por lotes** | 🧑‍💻 25' | 166 | Ejercicio 7 |
| 2:30 | Los nueve consejos del curso | 🗣 | 175 | |
| 2:45 | Presentación del reto integrador | 🗣 | — | `reto-integrador.md` |
| 2:55 | Cierre | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

Una pregunta por trampa: «¿por qué no se deshizo la transacción?» (excepción comprobada sin
`rollbackFor`) y «¿por qué no se aplicó `@Transactional`?» (llamada interna).

### 0:10 – 0:25 · Carga adelantada y perezosa

📊 Diapositivas 171–172.

🗣 La pregunta de la diapositiva 171: «¿cuántos atributos deberíamos cargar en una consulta?».
Si se carga todo por las relaciones, acaba la base de datos entera en memoria.

| Relación | Por defecto | Recomendación |
|---|---|---|
| `@ManyToOne`, `@OneToOne` | `EAGER` | cambiar a `LAZY` |
| `@OneToMany`, `@ManyToMany` | `LAZY` | dejar `LAZY` |

⌨ `open-in-view`: enseña la línea de `application.properties` y su comentario. Con OSIV
activado, las relaciones perezosas se cargan **al renderizar la vista**, fuera del servicio y
sin que nadie lo vea. El proyecto lo desactiva para que el problema dé la cara.

> Las notas del ponente de la diapositiva 172 lo explican; la del 173 habla de
> `ExampleServiceTests`, que no existe en este repositorio: el ejemplo equivalente es el guion
> de N+1 con el perfil `nplus1`.

### 0:25 – 1:10 · Detectar y corregir el N+1

📊 Diapositivas 173–174.
🧑‍💻 **Guion de N+1, ejercicios 1–3** (45 min): comentar el `@EntityGraph`, arrancar con
`nplus1`, contar las consultas de `GET /owners`; corregir con `LEFT JOIN FETCH`; comparar con
`@EntityGraph`.

🗣 **Mensaje clave.** «Sin el perfil `nplus1` la aplicación **falla** con
`LazyInitializationException`. Con él, **funciona** y lanza una consulta por propietario además de la inicial (y más, por
las visitas `EAGER`). El defecto es el
mismo: OSIV solo lo esconde.»

### 1:10 – 1:25 · Por qué `EAGER` no es la solución

🧑‍💻 **Guion de N+1, ejercicios 4–5** (15 min): `EAGER` global y el caso de `Pet.visits`.

### 1:25 – 1:35 · Descanso

### 1:35 – 2:05 · Medir el N+1

🧑‍💻 **Ejercicio 12** (30 min): las estadísticas de Hibernate en una prueba; tabla con el
número de sentencias de cada estrategia; `Pet.visits` a `LAZY`; y `default_batch_fetch_size`.

Puesta en común: rellena en la pizarra la tabla de cuatro filas con los números del grupo.

🗣 **Mensaje clave.** «El *fetch join* trae todo en una consulta y multiplica filas. El
*batch fetching* trae por bloques sin multiplicar. No hay una respuesta: hay una medida.»

### 2:05 – 2:30 · Escritura por lotes

📊 Diapositiva 166 (recuerda la sesión 11).
🧑‍💻 **Ejercicio 7** (25 min): insertar 1000 mascotas, activar `batch_size` y comparar
estadísticas. Descubrir que con `IDENTITY` no agrupa.

### 2:30 – 2:45 · Los nueve consejos del curso

📊 Diapositiva 175.

Recorre cada consejo y pide al grupo **dónde lo vimos** en el curso:

| Consejo | Dónde |
|---|---|
| Convención sobre configuración | Sesión 1: aplicación desde cero, autoconfiguración |
| Inyección por constructor, siempre | Sesión 1 (ejercicio 3) y sesión 7 (Mockito sin reflexión) |
| DTO entre capas, nunca entidades | Sesión 3 (ejercicio 2) |
| Documenta tu API desde el principio | Sesión 4 (ejercicio 14) |
| Seguridad desde el diseño, denegación por defecto | Sesión 5 (ejercicio 8 y el `denyAll()` final) |
| Prueba cada capa por separado | Sesiones 7 y 8 (rebanadas) |
| Perfiles para cada entorno | Sesión 9 (ejercicio 2) |
| Actuator para monitorizar, incluso en producción | Sesiones 1 y 9 (y protegido, ejercicio 9) |
| `LAZY` por defecto, `EAGER` con criterio | Hoy |

Haz la pregunta de la diapositiva: «¿cuál ha sido el más importante para vosotros?».

### 2:45 – 2:55 · Presentación del reto integrador

🗣 Presenta [`modulos/reto-integrador.md`](../modulos/reto-integrador.md): una funcionalidad
pequeña que recorre los seis módulos. Se trabaja por libre; en la sesión 14 se revisan dos o
tres soluciones y se comentan los errores comunes.

### 2:55 – 3:00 · Cierre

- **Tarea:** empezar el reto integrador. Traer dudas concretas a la sesión 14.

---

## Si vas con retraso

- Guion de N+1: ejercicios 1 y 2 (el 3 como demo).
- Ejercicio 7 como demo con la solución.

## Si te sobra tiempo

- El [laboratorio de transacciones avanzadas](../modulos/06-spring-data-jpa/laboratorio-transacciones-avanzadas.md):
  bloqueo optimista frente a pesimista (15 min).
- Caché de segundo nivel: cuándo y cuándo no.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `LazyInitializationException` al quitar el `@EntityGraph` | Es el defecto que se quiere ver; con `nplus1` se convierte en N+1. |
| El *fetch join* devuelve propietarios repetidos | Falta `DISTINCT` (o se usa una versión de Hibernate anterior a la 6). |
| `MultipleBagFetchException` | Dos colecciones `List` con *fetch join* a la vez. Usar `Set` o dos consultas. |
| El lote no agrupa inserciones | `GenerationType.IDENTITY`. |
| Las estadísticas dan 0 | Falta `hibernate.generate_statistics=true`. |
