# Sesión 2 · Diseñar y construir una API REST

**Módulo 2** · Diapositivas 35–58 · 3 horas (85 min + 10 min de descanso + 85 min)

> **Mensaje de la sesión.** Una API REST es un **contrato**: recursos con nombre, métodos con
> semántica, códigos de estado que significan algo y errores que se entienden. Spring hace
> fácil escribir el controlador; el diseño sigue siendo cosa nuestra.

## Objetivos

- Diseñar URIs, elegir método y código de estado aplicando los principios REST.
- Recoger cualquier dato de la petición: ruta, *query string*, cabeceras, *cookies* y cuerpo.
- Controlar la respuesta completa con `ResponseEntity`: estado, cabeceras y caché HTTP.
- Devolver errores con el formato estándar `ProblemDetail` (RFC 9457).
- Validar la entrada y traducir los fallos a 400 útiles.

## Antes de clase

- [ ] `petclinic-api` arrancado y Swagger UI abierto: <http://localhost:8080/swagger-ui.html>.
- [ ] Imprimir o proyectar la tabla de 10 propuestas del **ejercicio 8** (se hace sin ordenador).
- [ ] Repasar [`docs/diapositivas-nuevas.md`](../docs/diapositivas-nuevas.md): las dos
      diapositivas sobre `ProblemDetail` que faltan en el `.pptx`.
- [ ] Swagger UI abierto en el proyector durante toda la sesión: todas las demos se hacen ahí
      (ver [`docs/swagger-ui-en-clase.md`](../docs/swagger-ui-en-clase.md)).

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso de la sesión 1 y corrección de la tarea | 🗣 | — | M1 ejercicio 5 |
| 0:10 | Principios REST y HTTP | 🗣 | 36–43 | |
| 0:30 | **Diseñar antes de programar** | 🧑‍💻 20' | 44–53 | Ejercicio 8 |
| 0:50 | Puesta en común y la API real | ⌨ | 44–53 | Swagger UI, ejercicio 1 |
| 1:05 | Controladores y *binding* | ⌨ | 54–56 | `RestfulVisitController` |
| 1:25 | **Descanso** | | | |
| 1:35 | ***Binding*: todo lo que llega en una petición** | 🧑‍💻 35' | 50–53, 56 | Ejercicio 9 |
| 2:10 | Errores: de la excepción al `ProblemDetail` | ⌨ 🗣 | 55 + nuevas | `GlobalExceptionHandler` |
| 2:25 | `ResponseEntity`, cabeceras y caché HTTP | 🧑‍💻 25' | 57 | Ejercicio 10 |
| 2:50 | Validación | ⌨ | 58 | `CreateVisitRequest` |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

- Respuesta a la pregunta con trampa de la sesión 1: `BillsGateway` es `@Component` para que
  las rebanadas `@DataJpaTest(includeFilters = ... Service.class)` del módulo 4 no lo carguen.
- Una pregunta rápida al aula: «¿qué diferencia hay entre un *starter* y un BOM?».

### 0:10 – 0:30 · Principios REST y HTTP

📊 Diapositivas 36–43.

Cuenta lo justo: recurso, URI, representación, hipermedia (HATEOAS), sin estado, *cacheable*,
interfaz uniforme. Detente en dos cosas que se usarán hoy:

- **Sin estado** (diap. 40): cada petición lleva todo lo necesario. Lo retomaremos con JWT en
  la sesión 6.
- **Cacheable** (diap. 40): la respuesta dice si se puede reutilizar. Lo practicaremos con
  `ETag` en el ejercicio 10.

⌨ Una petición de verdad, con cabeceras, para que vean la diapositiva 42 en vivo:
**Swagger UI** → *Visits* → `GET /api/v1/visits/{id}` → `id` = `1` → **Execute**. Señala en la
respuesta la orden `curl` generada (método, URL, cabecera `Accept`), el código de estado, las
*Response headers* (`Content-Type`, `Date`…) y el cuerpo.

Si el grupo quiere ver la petición **completa** tal como viaja, con todas sus cabeceras, es el
único momento de la sesión en que compensa la consola: `curl -v localhost:8080/api/v1/visits/1`.

### 0:30 – 0:50 · Diseñar antes de programar

📊 Diapositivas 44–53.
🧑‍💻 **Ejercicio 8**, en parejas y **sin ordenador** (20 min).

Reparte la tabla de las diez propuestas. Que la trabajen con las diapositivas 45–53 delante.

### 0:50 – 1:05 · Puesta en común y la API real

1. Corrige el ejercicio 8 con la [solución](../modulos/02-apis-rest/soluciones/Ejercicio08DisenoRest.md).
   Detente en la tabla de seguridad e idempotencia: **`PATCH` no es idempotente** y **`POST`
   no se puede reintentar** sin más.
2. ⌨ Abre Swagger UI y localiza las tres convenciones de ruta que conviven en el proyecto:

   ```
   /api/pets         sin versión
   /api/v1/visits    versión en la ruta
   /api/v2/pets      otra versión
   ```

🗣 **Mensaje clave.** «No es un ejemplo de buen diseño: es lo que pasa cuando una API crece
sin una convención acordada.» Proponer el **ejercicio 1** (unificar el versionado) como tarea.

### 1:05 – 1:25 · Controladores y *binding*

📊 Diapositivas 54–56.

⌨ Compara dos controladores del proyecto:

| | `PetRestController` | `RestfulVisitController` |
|---|---|---|
| Devuelve | la entidad `Pet` | `VisitResponse` |
| Acepta | la entidad `Pet` | `CreateVisitRequest` validado |
| Al crear | 201 | 201 + `Location` |

Recorre las anotaciones de la diapositiva 54 sobre `RestfulVisitController`: `@RestController`,
`@RequestMapping`, `@GetMapping`, `@PathVariable`, `@RequestBody`, `@ResponseStatus`.

> **Errata de la diapositiva 56.** `@CookieParam` y `@HeaderParam` son de JAX-RS. En Spring
> son `@CookieValue` y `@RequestHeader`. Dilo antes de que lo busquen.

### 1:25 – 1:35 · Descanso

### 1:35 – 2:10 · *Binding*: todo lo que llega en una petición

🧑‍💻 **Ejercicio 9** (35 min).

Construyen `GET /api/v1/owners` con filtros, ordenación `+campo,-campo`, selección de campos,
paginación por `offset`/`limit`, una cabecera y una *cookie*.

Puntos para recorrer el aula:

- La lista blanca de campos: que **todos** la pongan. Pregunta: «¿por qué es una medida de
  seguridad?».
- El `+` en la URL llega como espacio si no se codifica (`%2B`). Enseña cómo lo resuelve la
  solución.
- Quien acabe antes: que haga que el `limit` máximo venga de configuración.

⌨ A los 30 minutos, enseña la
[solución](../modulos/02-apis-rest/soluciones/Ejercicio09Binding.java) y señala que filtrar y
paginar **en memoria** solo vale para el ejercicio: en el módulo 6 se hará en la consulta.

### 2:10 – 2:25 · Errores: de la excepción al `ProblemDetail`

📊 Diapositiva 55 y las dos diapositivas nuevas de
[`docs/diapositivas-nuevas.md`](../docs/diapositivas-nuevas.md).

⌨ Provoca los errores en **Swagger UI** y mira el código y el cuerpo `application/problem+json`:

| Operación | Qué poner | Esperado |
|---|---|---|
| *Pets* → `GET /api/pets/{id}` | `id` = `99999` | 404 |
| *Pets* → `GET /api/pets/{id}` | `id` = `abc` (Swagger UI avisa de que no es un entero: es la validación del **cliente**; bórralo y pruébalo con `curl -i localhost:8080/api/pets/abc` para ver la del **servidor**) | 400 |
| *Visits* → `POST /api/v1/visits` | cuerpo `{}` | 400 con detalle por campo |
| *Visits* → `POST /api/v1/visits` | cuerpo `{ esto no es json` | 400 sin filtrar el mensaje de Jackson |

Abre `web/api/GlobalExceptionHandler` y señala las decisiones:

1. `basePackages`: el *advice* solo afecta a la API, no a las vistas JSP.
2. Las excepciones de dominio (`ResourceNotFoundException`…) se **traducen** aquí; el servicio
   no sabe nada de HTTP.
3. Manejador explícito para `AccessDeniedException`: sin él, los 403 serían 500.
4. El *catch-all* registra la traza y devuelve una **referencia**, nunca la traza.
5. Y respeta las excepciones de Spring que ya saben su código (`ErrorResponse`): 405, 415…

🗣 **Mensaje clave.** «`@ResponseStatus` decide el **código**. `ProblemDetail` decide el
**cuerpo**. Un cliente necesita los dos.»

### 2:25 – 2:50 · `ResponseEntity`, cabeceras y caché HTTP

📊 Diapositiva 57.
🧑‍💻 **Ejercicio 10** (25 min): `ETag` a partir de `@Version`, `304 Not Modified`, `If-Match`
obligatorio en el `PUT` (412/428) y cabecera propia en el `DELETE`.

Si no da tiempo a todo, que hagan solo los pasos 1 y 2 (GET con `ETag` y 304). El `If-Match`
se retoma en el módulo 6 al hablar de bloqueo optimista.

🗣 **Mensaje clave.** «`If-Match` es el bloqueo optimista llevado hasta el cliente.»

### 2:50 – 2:55 · Validación

📊 Diapositiva 58.

⌨ Recorre `web/api/dto/CreateVisitRequest.java`: `@NotEmpty`, `@NotNull`, `@Valid` anidado.
Enseña que **las mismas anotaciones** aparecen en el esquema de `/v3/api-docs`: la validación
documenta.

### 2:55 – 3:00 · Cierre y tarea

- **Tarea:**
  - Ejercicio 1 (unificar el versionado con 301 desde la ruta antigua).
  - Terminar el ejercicio 10 si no dio tiempo.
- Adelanta la sesión 3: «hoy hemos devuelto entidades; el jueves veremos por qué no».

---

## Si vas con retraso

- El ejercicio 8 se puede hacer en 15 minutos con solo las propuestas 1–6.
- El ejercicio 10 se reduce al GET con `ETag` (pasos 1 y 2).
- La validación (2:50) se puede fusionar con el bloque de errores.

## Si te sobra tiempo

- Ejercicio 5 (versionado por cabecera) y la nota sobre el versionado nativo de Spring Boot 4.
- Alternativa sin código del ejercicio 10: `ShallowEtagHeaderFilter`, y qué ahorra y qué no.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| El `POST` devuelve 415 | Falta `-H 'Content-Type: application/json'`. |
| `sort=+lastName` no ordena | El `+` llega como espacio: usar `%2B` o tolerarlo en el código. |
| `MissingServletRequestParameterException` | `@RequestParam` es obligatorio por defecto: `required = false` o `defaultValue`. |
| La *cookie* no llega desde Swagger UI | El navegador no deja enviar *cookies* propias: esa parte del ejercicio 9 va con `curl -b "nombre=valor"`. |
| Swagger UI no muestra la cabecera `X-Saludo` | Mirar *Response headers*; si el navegador la oculta, es que la petición fue a otro origen. |
| `curl` en PowerShell da errores raros | Es `Invoke-WebRequest`: usar `curl.exe`. |
