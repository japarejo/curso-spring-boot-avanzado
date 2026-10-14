# Módulo 2. Ejercicios

Se trabaja sobre `apps/petclinic-api`. Soluciones en [`soluciones/`](soluciones/).

La API se prueba con **Swagger UI** (<http://localhost:8080/swagger-ui.html>); los pocos casos en
que hace falta `curl` lo dicen y explican por qué. Guía: [`docs/swagger-ui-en-clase.md`](../../docs/swagger-ui-en-clase.md).

---

## Ejercicio 1 · Unificar el versionado

**Objetivo.** Acordar una convención y aplicarla.

La API tiene hoy tres convenciones conviviendo:

```
/api/pets           sin versión
/api/v1/visits      versión en la ruta
/api/v2/pets        otra versión
```

1. Decide una convención y justifícala.
2. Reubica `PetRestController` bajo `/api/v1/pets`.
3. Deja la ruta antigua respondiendo con `301 Moved Permanently`, para no romper clientes.
4. Comprueba que Swagger UI refleja el cambio.

**Resultado esperado.** Swagger UI muestra las operaciones bajo `/api/v1/pets`. La redirección
se comprueba en la consola, porque Swagger UI (como cualquier `fetch` del navegador) sigue el
301 sin enseñarlo: `curl -i localhost:8080/api/pets` devuelve 301 con `Location` apuntando a
`/api/v1/pets`.

**Pregunta.** ¿Qué cambios **no** exigen una versión nueva? *(Añadir campos opcionales o
recursos nuevos, si los clientes ignoran lo que no conocen.)*

---

## Ejercicio 2 · De entidad a DTO

**Objetivo.** Ver en la práctica por qué no se devuelven entidades.

1. Pide una mascota y observa la respuesta: **Swagger UI** → *Pets* → `GET /api/pets/{id}` →
   `id` = `1` → **Execute**. Mira también, al final de la página, el esquema `Pet` que publica
   la documentación: es la entidad tal cual.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -s localhost:8080/api/pets/1 | jq
   ```

   </details>

2. Quita el `@JsonIgnore` de `Pet.visits` y repite. Observa qué pasa.
3. Deshaz ese cambio.
4. Crea `web/api/dto/PetResponse` con solo `id`, `name`, `birthDate` y `type`.
5. Haz que `PetRestController` devuelva `PetResponse` en lugar de `Pet`.
6. Ajusta `PetRestControllerMockMvcTests`.

**Resultado esperado.** En el paso 2, una respuesta enorme o un fallo de serialización. Tras
el paso 5, una respuesta estable que no depende de cómo esté mapeada la entidad.

Solución: [`soluciones/Ejercicio02PetResponse.java`](soluciones/Ejercicio02PetResponse.java)

---

## Ejercicio 3 · Dos vistas del mismo objeto con `@JsonView`

**Objetivo.** Practicar `@JsonView`, que el proyecto todavía no usa.

1. Crea las interfaces de vista:

   ```java
   public class Vistas {
       public interface Resumen {}
       public interface Detalle extends Resumen {}
   }
   ```

2. Anota los campos de `PetResponse`: `id` y `name` en `Resumen`; el resto en `Detalle`.
3. Expón dos endpoints sobre el mismo objeto:
   - `GET /api/v1/pets` con `@JsonView(Vistas.Resumen.class)`
   - `GET /api/v1/pets/{id}` con `@JsonView(Vistas.Detalle.class)`

**Resultado esperado.** El listado devuelve dos campos por mascota; el detalle, todos.

**Pregunta.** ¿Cuándo usarías `@JsonView` y cuándo dos DTOs distintos? *(Pista: `@JsonView`
mantiene una sola clase; dos DTOs permiten que las dos representaciones evolucionen por
separado. El criterio es si son la misma cosa vista de dos maneras o dos contratos.)*

---

## Ejercicio 4 · Migrar un cliente HTTP a RestClient

**Objetivo.** Sustituir `RestTemplate`, que está en modo mantenimiento.

1. Abre `apiclients/BillsGateway` y compara `conRestTemplate()` con `conRestClient()`.
2. Levanta el servicio de facturas y prueba los tres estilos:

   ```bash
   ./mvnw -pl infra/bills-service spring-boot:run      # otra terminal
   ```

   y en **Swagger UI** → *Bills demo* ejecuta los tres `GET` (`/rest-template`,
   `/rest-client`, `/feign`). La API que se consume tiene su propia Swagger UI:
   <http://localhost:8040/>.

3. Añade a `conRestClient()` el manejo del caso 404 con `onStatus`.
4. Para el servicio de facturas y comprueba que el cortocircuito devuelve lista vacía.

**Resultado esperado.** Con el servicio caído, la respuesta es `[]` y en el registro aparece
el aviso del cortocircuito. La aplicación no devuelve un 500.

---

## Ejercicio 5 · Versionado por cabecera

**Objetivo.** Ver la alternativa a versionar en la ruta.

1. Añade a `PetRestController` un endpoint que responda solo si llega
   `X-API-Version: 2`, usando `headers` en la anotación de mapeo:

   ```java
   @GetMapping(path = "/{id}", headers = "X-API-Version=2")
   ```

2. Prueba las dos variantes. Aquí **hace falta `curl`**, y es parte de la lección: OpenAPI no
   admite dos operaciones con la misma ruta y el mismo método, así que Swagger UI solo muestra
   una de las dos versiones. El versionado por cabecera es difícil de documentar; es un
   argumento más a favor del versionado en la ruta.

   ```bash
   curl -s localhost:8080/api/v1/pets/1
   curl -s -H "X-API-Version: 2" localhost:8080/api/v1/pets/1
   ```

**Resultado esperado.** La misma URI devuelve dos representaciones según la cabecera.

**Nota para 4.x.** Spring Boot 4 trae versionado nativo: `@GetMapping(path = "...",
version = "1.1")` más `spring.mvc.apiversion.use.header=X-API-Version`. Este ejercicio es la
forma de hacerlo en 3.5.

---

## Ejercicio 6 · CORS

**Objetivo.** Configurar CORS en un solo sitio y entender qué es.

1. Comprueba que hoy no hay configuración de CORS:

   ```bash
   grep -rn "CorsConfiguration\|@CrossOrigin" apps/ --include=*.java
   ```

2. Añade un bean `CorsConfigurationSource` que permita el origen
   `http://localhost:3000` sobre `/api/**`, con los métodos GET, POST, PUT y DELETE.
3. Actívalo en `SecurityConfiguration` con `.cors(Customizer.withDefaults())`.
4. Comprueba la petición de verificación previa. Con `curl`, porque esta petición la lanza el
   **navegador** por su cuenta cuando una página de **otro origen** llama a la API; desde Swagger
   UI (mismo origen) no se produce nunca:

   ```bash
   curl -i -X OPTIONS localhost:8080/api/v1/visits \
     -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET"
   ```

**Resultado esperado.** 200 con la cabecera `Access-Control-Allow-Origin`.

**Pregunta.** ¿Por qué `allowedOrigins("*")` junto a `allowCredentials(true)` es una
combinación que el navegador rechaza?

> **Mensaje clave.** CORS no es una medida de seguridad del servidor: es una relajación
> controlada de la política del navegador. No protege tu API de un cliente que no sea un
> navegador.

---

## Ejercicio 7 · Spring Data REST

**Objetivo.** Ver cuánto se puede automatizar, y decidir si compensa.

1. Crea un módulo o un perfil con `spring-boot-starter-data-rest`.
2. Configura `spring.data.rest.base-path=/data-api` y
   `spring.data.rest.detection-strategy=ANNOTATED`.
3. Anota `VetRepository` con `@RepositoryRestResource`.
4. Explora lo que aparece solo. Spring Data REST no se documenta en Swagger UI (no son
   controladores tuyos), pero tiene su propio explorador interactivo: añade la dependencia
   `org.springframework.data:spring-data-rest-hal-explorer` y abre <http://localhost:8080/data-api>.
   Navega por los enlaces `_links` y prueba `vets`.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -s localhost:8080/data-api/vets | jq
   ```

   </details>

5. Define una proyección con `@Projection` y pruébala con `?projection=...`.

**Pregunta de cierre.** ¿En qué casos usarías Spring Data REST en un proyecto real? ¿Qué
pierdes frente a controladores escritos a mano? *(Control del contrato, DTOs, validación
específica, versionado.)*

---

# Ejercicios añadidos para la edición de 40 horas

Cubren lo que las transparencias explican y los siete ejercicios anteriores no practicaban:
diseño de URIs y semántica de los métodos (diapositivas 36–53), *binding* de parámetros
(54–58), cabeceras y caché HTTP (40, 57), serialización propia (59–61), conversores (módulo 2,
«convertidores avanzados y condicionales»), Spring Data REST a fondo (64–67) y OpenAPI a
fondo (68–70).

---

## Ejercicio 8 · Diseñar antes de programar

**Objetivo.** Aplicar las buenas prácticas de nombrado y la semántica de HTTP a una API real.
**Diapositivas:** 36–53. **Tiempo:** 20 min, en parejas y **sin ordenador**.

Un compañero ha propuesto estos endpoints para la clínica:

| # | Propuesta |
|---|---|
| 1 | `GET /getAllOwners` |
| 2 | `POST /owners/delete?id=3` |
| 3 | `GET /owners/3/pets/7/visits/` |
| 4 | `GET /api/Owner_Pets?ownerId=3` |
| 5 | `POST /visits/create` |
| 6 | `PUT /pets/7/rename?name=Leo` |
| 7 | `GET /vets/listarConEspecialidades` |
| 8 | `DELETE /owners` |
| 9 | `GET /api/v1.1/pets` |
| 10 | `GET /visits?desde=2026-01-01&hasta=2026-01-31&ordenar=fecha&pagina=2` |

1. Para cada uno: ¿qué regla incumple? Propón la versión correcta (método + URI).
2. Completa la tabla de la diapositiva 49, ampliada:

   | | GET | HEAD | OPTIONS | POST | PUT | PATCH | DELETE |
   |---|---|---|---|---|---|---|---|
   | Seguro | | | | | | | |
   | Idempotente | | | | | | | |

3. Elige el código de estado para cada situación:
   - se crea una visita;
   - se borra una mascota que existía;
   - se pide una mascota que no existe;
   - el JSON de entrada no valida;
   - un `owner` intenta ver los datos de otro `owner`;
   - nadie se ha autenticado;
   - dos personas editan a la vez la misma mascota y la segunda llega tarde;
   - el cliente pide `application/xml` y la API solo habla JSON.

4. Comprueba tus respuestas contra la API real con Swagger UI. ¿Qué endpoints actuales del
   proyecto incumplen alguna de tus reglas?

**Pregunta.** ¿Es un problema la propuesta 10? *(No: los filtros, la ordenación y la
paginación van en la *query string*. Lo discutible es mezclar idiomas en los nombres de
parámetros con el resto de la API.)*

Solución: [`soluciones/Ejercicio08DisenoRest.md`](soluciones/Ejercicio08DisenoRest.md)

---

## Ejercicio 9 · *Binding*: todo lo que puede llegar en una petición

**Objetivo.** Recoger parámetros de ruta, de consulta, cabeceras y *cookies*, y aplicar las
convenciones de filtrado, ordenación, selección de campos y paginación.
**Diapositivas:** 44, 50–53, 56. **Tiempo:** 40 min.

Crea `web/api/OwnerQueryController` con `GET /api/v1/owners` que acepte:

| Parámetro | Ejemplo | Significado |
|---|---|---|
| `city` | `?city=Madison` | filtro exacto |
| `lastName` | `?lastName=Da` | empieza por |
| `sort` | `?sort=+lastName,-city` | `+` ascendente, `-` descendente (diapositiva 51) |
| `fields` | `?fields=firstName,lastName` | solo esos campos (diapositiva 52) |
| `offset`, `limit` | `?offset=0&limit=3` | paginación (diapositiva 53) |

y además:

- la cabecera `Accept-Language`, con `@RequestHeader`, para devolver un saludo en
  español o inglés en la cabecera de respuesta `X-Saludo`;
- una *cookie* `ultimaCiudad` con `@CookieValue(required = false)`, que se usa como
  valor por defecto de `city` si no llega el parámetro.

> **Ojo con la diapositiva 56.** `@CookieParam` y `@HeaderParam` son anotaciones de
> **JAX-RS** (Jakarta REST), no de Spring. En Spring MVC son `@CookieValue` y
> `@RequestHeader`.

Requisitos de calidad:

1. `sort` y `fields` solo admiten una **lista blanca** de propiedades. Cualquier otra da
   400, no 500 (mira cómo lo hace `RestfulVisitSearchController`).
2. `limit` tiene un máximo de 50, aunque el cliente pida más.
3. Pruébalo en **Swagger UI** → *owner-query-controller* → `GET /api/v1/owners`. Swagger UI
   ha deducido de las anotaciones qué va en la *query string*, qué en la cabecera y qué en la
   *cookie*; míralo antes de rellenar nada.
   - `sort` = `-lastName`, `fields` = `firstName` y `lastName` (dos elementos), `limit` = `3`.
   - `limit` = `1` y `Accept-Language` = `en`: busca `x-saludo` en *Response headers*.
   - `sort` = `password` → 400 con `ProblemDetail`.
   - `sort` = `+lastName`: funciona, porque Swagger UI codifica el `+` como `%2B`. En la consola
     no es así (ver la pregunta del final).

   La *cookie* es lo único que **no** se puede probar desde Swagger UI: el navegador no deja que
   una página fije la cabecera `Cookie`. Para eso, la consola:

   ```bash
   curl -s -b "ultimaCiudad=Madison" "localhost:8080/api/v1/owners" | jq length
   curl -s "localhost:8080/api/v1/owners?sort=+lastName" | jq        # ¿qué pasa con el +?
   ```

**Resultado esperado.** Cada petición devuelve solo lo pedido, ordenado como se pidió, y
`sort=password` responde 400 con un `ProblemDetail`.

**Pregunta.** ¿Por qué la lista blanca es una medida de **seguridad** y no solo de
robustez? *(Sin ella, ordenar por un campo oculto permite deducir su valor fila a fila.)*

Solución: [`soluciones/Ejercicio09Binding.java`](soluciones/Ejercicio09Binding.java)

---

## Ejercicio 10 · `ResponseEntity`, cabeceras y caché HTTP

**Objetivo.** Controlar la respuesta completa: estado, cabeceras y peticiones condicionales.
**Diapositivas:** 40 (*cacheable*), 57. **Tiempo:** 35 min.

Las entidades del proyecto tienen `@Version` (en `BaseEntity`). Es justo lo que hace falta
para una `ETag`.

1. Haz que `GET /api/v1/pets/{id}` devuelva un `ResponseEntity` con:
   - `ETag: "<version>"`;
   - `Cache-Control: max-age=60, private`.
2. Si la petición trae `If-None-Match` con la misma `ETag`, responde **304 Not Modified**
   sin cuerpo. (Pista: `WebRequest.checkNotModified(String etag)`.)
3. Haz que `PUT /api/v1/pets/{id}` exija `If-Match`. Si la versión no coincide, responde
   **412 Precondition Failed**. Sin la cabecera, **428 Precondition Required**.
4. Haz que `DELETE` devuelva la cabecera propia de la diapositiva 57.
5. Documenta las dos cabeceras para poder probarlas en Swagger UI: el `If-Match` del `PUT` ya
   aparece solo (es un `@RequestHeader`); el `If-None-Match` del `GET` no, porque lo lee
   `WebRequest` por dentro. Decláralo con
   `@Parameter(in = ParameterIn.HEADER, name = "If-None-Match", schema = @Schema(type = "string"))`.
6. Prueba la secuencia completa en **Swagger UI** → *Pets*:
   1. `GET /api/v1/pets/{id}` con `id` = `1` → 200 y `etag: "1"` en *Response headers*.
   2. Lo mismo con `If-None-Match` = `"1"` (con las comillas) → 304, sin cuerpo.
   3. `PUT /api/v1/pets/{id}` con `If-Match` = `"7"` y el cuerpo de abajo → 412.
   4. Repite el `PUT` sin `If-Match` → 428, y con `"1"` → 204 y una `etag` nueva.

   ```json
   {"id": 1, "name": "Leo", "birthDate": "2010/09/07", "type": "cat", "owner": 1}
   ```

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -i localhost:8080/api/v1/pets/1                              # 200 + ETag: "1"
   curl -i -H 'If-None-Match: "1"' localhost:8080/api/v1/pets/1      # 304
   curl -i -X PUT -H 'If-Match: "7"' -H 'Content-Type: application/json' \
        -d '{"id":1,"name":"Leo","birthDate":"2010/09/07","type":"cat","owner":1}' \
        localhost:8080/api/v1/pets/1                                 # 412
   ```

   </details>

**Resultado esperado.** La segunda petición no transfiere cuerpo. El PUT con una versión
antigua se rechaza **antes** de tocar la base de datos.

**Pregunta.** ¿Qué problema de los vistos en el módulo 6 resuelve el `If-Match`? *(La
actualización perdida: es bloqueo optimista llevado hasta el cliente HTTP.)*

**Alternativa sin código.** Registra un `ShallowEtagHeaderFilter` y compara. ¿Qué ahorra y
qué no? *(Ahorra red, pero el servidor sigue generando la respuesta entera para calcular el
hash.)*

Solución: [`soluciones/Ejercicio10CacheHttp.java`](soluciones/Ejercicio10CacheHttp.java)

---

## Ejercicio 11 · Tu propio serializador

**Objetivo.** Escribir y registrar un par serializador/deserializador Jackson, y conocer la
configuración global.
**Diapositivas:** 59–61. **Tiempo:** 35 min.

1. Expón `GET /api/v1/vets` devolviendo `vetService.findVets()`. Observa cómo salen las
   especialidades: objetos con `id`, `name` y `version`.
2. Escribe `SpecialtyJson` con dos clases internas, `Serializer` y `Deserializer`, que
   representen una especialidad **por su nombre**: `"specialties": ["radiology", "surgery"]`.
   Regístralas con `@JsonComponent` en lugar de anotar el campo con `@JsonSerialize`.
3. Compara con `PetTypeSerializer`/`PetTypeDeserializer`, que el proyecto registra campo a
   campo. ¿Qué cambia? ¿Qué riesgo tiene registrar globalmente?
4. Ahora experimenta con la configuración **global** de Jackson, una propiedad cada vez, y
   mira qué le pasa a **toda** la API:

   ```properties
   spring.jackson.property-naming-strategy=SNAKE_CASE
   spring.jackson.default-property-inclusion=non_null
   spring.jackson.serialization.indent-output=true
   ```

**Resultado esperado.** Con `@JsonComponent`, todas las especialidades del sistema salen como
texto en cualquier respuesta. Con `SNAKE_CASE`, `birthDate` pasa a `birth_date`… y todos los
clientes existentes se rompen.

> **Mensaje clave.** La configuración global de Jackson cambia el contrato de **toda** la
> API de golpe. Úsala al principio del proyecto o no la uses.

**Nota para 4.x.** Spring Boot 4 usa Jackson 3: el paquete pasa de `com.fasterxml.jackson` a
`tools.jackson`, y `@JsonComponent` pasa a llamarse `@JacksonComponent`.

Solución: [`soluciones/Ejercicio11SerializadorPropio.java`](soluciones/Ejercicio11SerializadorPropio.java)

---

## Ejercicio 12 · Conversores: de texto a objetos de dominio

**Objetivo.** Entender el `ConditionalGenericConverter` del proyecto y escribir un conversor
propio.
**Temario:** «Convertidores avanzados y condicionales». **Tiempo:** 30 min.

1. Lee `configuration/GenericIdToEntityConverter`. Responde: ¿para qué pares de tipos se
   ofrece? ¿Qué decide `matches()`? ¿De dónde saca la entidad?
2. Compruébalo en vivo. Añade temporalmente este endpoint:

   ```java
   @GetMapping("/api/v1/demo/pets/{pet}")
   public String demo(@PathVariable("pet") Pet pet) {
       return pet == null ? "no existe" : pet.getName();
   }
   ```

   y pide `/api/v1/demo/pets/1`. Ningún código ha llamado al repositorio: lo hizo el
   conversor.
3. Escribe un conversor propio `Converter<String, RangoFechas>`, donde
   `record RangoFechas(LocalDate desde, LocalDate hasta)`, que acepte:
   - `2013-01-01..2013-01-31`
   - `ultimos-30-dias`
   - `este-anio`
4. Úsalo en `GET /api/v1/visits/rango?rango=...` delegando en `VisitService.search(...)`.
5. Un rango mal escrito debe dar **400**, no 500.

**Resultado esperado.** El controlador recibe directamente un `RangoFechas`, sin una línea de
análisis de texto.

**Preguntas.**
- ¿Qué diferencia hay entre un `Converter` y un `Formatter`? *(El `Formatter` es
  bidireccional y sensible a la configuración regional: sirve también para pintar el valor
  en una vista. `PetTypeFormatter` es un ejemplo.)*
- ¿Qué riesgo tiene `GenericIdToEntityConverter`? *(Cualquier entidad se carga a partir de
  un número que manda el cliente: combinado con una autorización descuidada, es el primer
  riesgo del OWASP API Top 10.)*

Solución: [`soluciones/Ejercicio12Conversores.java`](soluciones/Ejercicio12Conversores.java)

---

## Ejercicio 13 · Spring Data REST a fondo

**Objetivo.** Ir más allá del ejercicio 7: rutas, proyecciones por defecto, búsquedas,
ocultación de operaciones y eventos.
**Diapositivas:** 64–67. **Tiempo:** 40 min. **Requisito:** ejercicio 7.

1. Añade `spring-boot-starter-data-rest` (la diapositiva 65 muestra por error
   `spring-data-jpa`) y, opcionalmente, `spring-data-rest-hal-explorer`.
2. Configura:

   ```properties
   spring.data.rest.base-path=/data-api
   spring.data.rest.detection-strategy=annotated
   ```

   y abre `/data-api/**` en `SecurityConfiguration` (la regla final es `denyAll()`).
3. Anota `VetRepository` con
   `@RepositoryRestResource(path = "veterinarios", collectionResourceRel = "veterinarios", excerptProjection = VetResumen.class)`.
4. Crea la proyección `VetResumen` con nombre, apellido y número de especialidades
   (`@Value("#{target.nrOfSpecialties}")`).
5. Añade un método de búsqueda y localízalo en `/data-api/veterinarios/search`:

   ```java
   List<Vet> findByLastNameStartingWithIgnoreCase(@Param("apellido") String apellido);
   ```

6. Impide que se pueda borrar por la API: redeclara `deleteById` con
   `@RestResource(exported = false)`. Comprueba que `DELETE` da 405.
7. Añade un `@RepositoryEventHandler` que registre en el log cada alta (`@HandleAfterCreate`).

Pruébalo todo con el **explorador HAL** (<http://localhost:8080/data-api>): navega a
`veterinarios`, sigue el enlace `search`, rellena `apellido` = `or`, y prueba el botón de
borrado sobre un veterinario (debe dar 405). El explorador es a Spring Data REST lo que
Swagger UI a los controladores.

<details><summary>Con <code>curl</code></summary>

```bash
curl -s localhost:8080/data-api | jq
curl -s localhost:8080/data-api/veterinarios | jq '._embedded.veterinarios[0]'
curl -s "localhost:8080/data-api/veterinarios/search/findByLastNameStartingWithIgnoreCase?apellido=or" | jq
curl -i -X DELETE localhost:8080/data-api/veterinarios/1          # 405
```

</details>

**Resultado esperado.** El listado usa la proyección resumida; el detalle, la entidad
completa; el `DELETE` no está disponible.

**Pregunta de cierre.** Compara el esfuerzo con el `OwnerQueryController` del ejercicio 9.
¿Qué controlas en uno y no en otro?

Solución: [`soluciones/Ejercicio13SpringDataRest.java`](soluciones/Ejercicio13SpringDataRest.java)

---

## Ejercicio 14 · OpenAPI a fondo

**Objetivo.** Que la documentación generada sea un contrato utilizable, no un listado de
rutas.
**Diapositivas:** 68–70. **Tiempo:** 40 min.

1. Mira el estado actual: <http://localhost:8080/swagger-ui.html> y `/v3/api-docs`.
2. Completa la definición global en `OpenApi30Config`: descripción, contacto, licencia y
   servidores (el equivalente programático de `@OpenAPIDefinition`, diapositiva 69).
3. Los esquemas de seguridad `basic` y `bearer-jwt` ya están declarados en `OpenApi30Config`
   (por eso existe el botón **Authorize** desde el primer día). Léelo y compáralo con la forma
   declarativa (`@SecurityScheme`) de la diapositiva 70. Después exige
   `@SecurityRequirement(name = "bearer-jwt")` en las operaciones de **escritura**: aparece un
   candado solo en ellas. Pega un token de `auth-service` (módulo 3) en *Authorize*.
4. Documenta los errores con su esquema real: `@ApiResponse(responseCode = "404", content =
   @Content(schema = @Schema(implementation = ProblemDetail.class)))`.
5. Separa la documentación en dos grupos con `GroupedOpenApi`: `v1` (`/api/v1/**`) y
   `legado` (`/api/pets/**`, `/api/v2/**`).
6. Oculta el endpoint de demostración del ejercicio 12 con `@Hidden`.
7. Lee las propiedades `springdoc.*` que ya trae `application.properties` (las que preparan
   Swagger UI para clase: formularios editables, duración de la petición, Actuator incluido,
   solo `/api/**` y `/actuator/**`). Después mueve las rutas:

   ```properties
   springdoc.api-docs.path=/api-docs
   springdoc.swagger-ui.path=/docs
   ```

   No olvides actualizar `SecurityConfiguration`: la regla de Swagger apunta a las rutas
   antiguas.
8. Exporta el contrato y valídalo. Abre en el navegador <http://localhost:8080/api-docs.yaml/v1>
   (un documento por grupo), guárdalo como `openapi-v1.yaml` y pasa el *linter*:

   ```bash
   npx @redocly/cli lint openapi-v1.yaml
   ```

**Resultado esperado.** Swagger UI muestra dos grupos, el botón *Authorize* y los errores con
su esquema `ProblemDetail`. El *linter* da menos avisos que con la configuración inicial.

**Pregunta.** ¿Documentación generada desde el código (*code first*) o código generado desde
el contrato (*contract first*)? ¿Cuándo cada una?

Solución: [`soluciones/Ejercicio14OpenApi.java`](soluciones/Ejercicio14OpenApi.java)

---

## Ejercicio 15 · Vistas por rol: `@JsonView` fijo y `MappingJacksonValue`

**Objetivo.** Ver las dos formas de elegir una vista Jackson: fija en el método o decidida
en cada petición.
**Continúa:** ejercicio 3. **Tiempo:** 25 min.

El proyecto ya trae un ejemplo completo y funcionando, sobre propietarios, para que no
choque con el `PetResponse` del ejercicio 3:

| Fichero | Qué contiene |
|---|---|
| `web/api/dto/VistasPropietario` | Tres vistas encadenadas: `Publica` ⊂ `Recepcion` ⊂ `Admin` |
| `web/api/dto/OwnerResponse` | Un único DTO; cada campo lleva su `@JsonView` |
| `web/api/OwnerViewsDemoRestController` | Los tres endpoints de abajo |

| Vista | Campos |
|---|---|
| `Publica` | `id`, `firstName`, `lastName`, `city` |
| `Recepcion` | lo anterior + `address`, `telephone` |
| `Admin` | lo anterior + `version`, `username` |

1. **Vista fija.** En **Swagger UI** → *Views demo*:
   - `GET /api/v1/views-demo/owners` → cada propietario con cuatro campos (`Publica`).
   - `GET /api/v1/views-demo/owners/{id}` con `id` = `1` → seis campos (`Recepcion`).

   Lee el controlador: la vista va en `@JsonView` sobre el método. Se decide **al
   compilar**; para otra vista hace falta otro método.
2. **Vista dinámica.** `GET /api/v1/views-demo/owners/{id}/por-rol` con `id` = `1` y el
   desplegable `rol` en `PUBLICO`, `RECEPCION` y `ADMIN`. Mismo método, tres respuestas.
   Lee cómo se hace:

   ```java
   MappingJacksonValue respuesta = new MappingJacksonValue(OwnerResponse.from(owner));
   respuesta.setSerializationView(rol.getVista());
   return respuesta;
   ```

   `MappingJacksonValue` envuelve el objeto y le adjunta la vista; el conversor Jackson de
   Spring la aplica al serializar.
3. Pide `rol=admin` (en minúsculas) y `rol=jefe`. ¿Qué código devuelve y por qué? *(400: Spring
   convierte el texto al enum `Rol` por su nombre exacto.)*
4. Quita el `@JsonView` del campo `id` de `OwnerResponse` y repite el paso 1. ¿Dónde está el
   `id`? *(Ha desaparecido de todas las vistas: Spring Boot desactiva
   `DEFAULT_VIEW_INCLUSION`, así que con una vista activa un campo sin `@JsonView` no se
   serializa nunca.)* Deshaz el cambio.
5. **El problema de seguridad.** Haz que la vista salga del usuario autenticado y no de un
   parámetro. Añade un endpoint `GET /api/v1/views-demo/owners/{id}/mia` que reciba
   `Authentication` y elija `Admin` si tiene la autoridad `admin`, `Recepcion` si tiene `vet`
   y `Publica` en otro caso (o si no hay usuario: `/api/**` sigue abierta y `Authentication`
   llega a `null`). Para probarlo, entra en <http://localhost:8080/login> con `admin1`/`4dm1n`,
   `vet1`/`v3t` u `owner1`/`0wn3r` y, en el mismo navegador, llama al endpoint desde Swagger UI:
   la cookie de sesión viaja sola.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -s localhost:8080/api/v1/views-demo/owners | jq '.[0]'
   curl -s localhost:8080/api/v1/views-demo/owners/1 | jq
   curl -s "localhost:8080/api/v1/views-demo/owners/1/por-rol?rol=ADMIN" | jq
   curl -i "localhost:8080/api/v1/views-demo/owners/1/por-rol?rol=jefe"        # 400
   ```

   </details>

**Resultado esperado.** El mismo objeto sale con cuatro, seis u ocho campos. En el paso 5,
cada usuario ve su vista sin poder elegirla.

**Pregunta.** En el paso 2, ¿qué impide que cualquiera pida `rol=ADMIN`? *(Nada. Una vista
elegida por el cliente es una preferencia de presentación, no un control de acceso. Ocultar
campos a quien no debe verlos exige sacar el rol de la autenticación, como en el paso 5.)*

**Nota para 4.x.** Spring Framework 7 marca `MappingJacksonValue` como obsoleta: la vista se
pasa como *hint* de serialización al conversor de Jackson 3. `@JsonView` en el método sigue
funcionando igual.

Pruebas: `OwnerViewsDemoRestControllerMockMvcTests`.

Solución del paso 5: [`soluciones/Ejercicio15VistaPorUsuario.java`](soluciones/Ejercicio15VistaPorUsuario.java)
