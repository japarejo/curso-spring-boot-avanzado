# Sesión 4 · APIs generadas y APIs documentadas

**Módulo 2** · Diapositivas 64–70 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Hay dos maneras de ahorrar trabajo en una API: **generarla**
> (Spring Data REST) o **documentarla sola** (OpenAPI). La primera ahorra código a cambio de
> control; la segunda ahorra documentación sin perder nada. Saber cuándo usar cada una es lo
> que distingue a quien diseña APIs.

## Objetivos

- Exponer repositorios como API REST con Spring Data REST y personalizarla: rutas,
  proyecciones, búsquedas, operaciones ocultas y eventos.
- Decidir cuándo compensa Spring Data REST frente a controladores escritos a mano.
- Convertir la documentación generada por springdoc en un contrato útil: metadatos, errores,
  seguridad, grupos.
- Configurar CORS en un único sitio y entender qué protege y qué no.

## Antes de clase

- [ ] Añadir `spring-boot-starter-data-rest` a un **clon aparte** o a una rama, y comprobar que
      arranca (la descarga de la dependencia tarda; mejor tenerla en caché).
- [ ] `infra/auth-service` compilado: se usa al final para el botón *Authorize* de Swagger UI.
- [ ] Node.js instalado si se quiere enseñar el *linter* de OpenAPI (`npx @redocly/cli`).

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso: tarea del versionado por cabecera | ⌨ | — | Ejercicio 5 |
| 0:10 | Spring Data REST: qué es y qué expone | 🗣 ⌨ | 64–65 | |
| 0:25 | **Primera API generada** | 🧑‍💻 20' | 65–66 | Ejercicio 7 |
| 0:45 | **Spring Data REST a fondo** | 🧑‍💻 40' | 66–67 | Ejercicio 13 |
| 1:25 | **Descanso** | | | |
| 1:35 | Debate: ¿generar la API? | 🗣 | — | Ejercicios 9 y 13 |
| 1:45 | OpenAPI con springdoc | ⌨ | 68–70 | `OpenApi30Config`, `PetVisitSearchController` |
| 2:00 | **OpenAPI a fondo** | 🧑‍💻 35' | 68–70 | Ejercicio 14 |
| 2:35 | CORS | ⌨ + 🧑‍💻 | — | Ejercicio 6 |
| 2:55 | Cierre del módulo 2 y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

⌨ Corrige el ejercicio 5 (versionado por cabecera) y enseña cómo será en Spring Boot 4:
`@GetMapping(version = "1.1")` con `spring.mvc.apiversion.use.header`.

### 0:10 – 0:25 · Spring Data REST

📊 Diapositivas 64–65.

1. 🗣 Qué hace: colecciones, elementos, asociaciones, paginación con enlaces, búsquedas,
   proyecciones (diap. 64). Todo sin controladores.
2. Las estrategias de detección (diap. 65). Detente en esta:

🗣 **Mensaje clave.** «Con la estrategia por defecto, añadir el *starter* publica **todos** los
repositorios públicos. En este proyecto eso incluye `UserRepository`, con los *hashes* de las
contraseñas. La primera línea de configuración siempre es `detection-strategy=annotated`.»

> **Errata de la diapositiva 65.** La dependencia que muestra (`spring-data-jpa`) no es la de
> Spring Data REST. Es `spring-boot-starter-data-rest`.

### 0:25 – 0:45 · Primera API generada

🧑‍💻 **Ejercicio 7** (20 min). Base `/data-api`, estrategia `annotated`, `VetRepository`
anotado, proyección con `?projection=`.

Avisa de que la regla final de `SecurityConfiguration` es `denyAll()`: hay que abrir
`/data-api/**`. Es un buen recordatorio de la sesión 5.

### 0:45 – 1:25 · Spring Data REST a fondo

🧑‍💻 **Ejercicio 13** (40 min): rutas propias, `excerptProjection`, búsqueda con `@Param`,
`exported = false` en el borrado, `@RepositoryEventHandler`.

Puntos para recorrer el aula:

- La proyección por defecto (`excerptProjection`) solo se aplica a **listados**; el detalle
  devuelve la entidad completa salvo que se pida `?projection=`.
- Los eventos de Spring Data REST solo saltan cuando la operación entra **por la API
  generada**, no cuando el código llama al repositorio.
- Quien acabe: añadir `spring-data-rest-hal-explorer` y navegar la API desde el navegador.

### 1:25 – 1:35 · Descanso

### 1:35 – 1:45 · Debate: ¿generar la API?

🗣 Compara en la pizarra el `OwnerQueryController` del ejercicio 9 (sesión 2) con el
`VetRepository` anotado del ejercicio 13:

| | Controlador a mano | Spring Data REST |
|---|---|---|
| Líneas de código | muchas | casi ninguna |
| Contrato (nombres, formato) | decides tú | HAL, decidido por el *framework* |
| DTO / exposición de datos | explícito | proyecciones |
| Validación y reglas de negocio | en el servicio | eventos y validadores |
| Versionado | como quieras | difícil |

🗣 **Mensaje clave.** «Spring Data REST es estupendo para APIs **internas** de datos y
prototipos. Para una API pública con contrato estable, controlador a mano.»

### 1:45 – 2:00 · OpenAPI con springdoc

📊 Diapositivas 68–70.

⌨ Recorre:

1. <http://localhost:8080/v3/api-docs> y <http://localhost:8080/swagger-ui.html>.
2. `configuration/OpenApi30Config`: el bean `OpenAPI` (equivalente programático a
   `@OpenAPIDefinition` de la diap. 69).
3. `web/api/PetVisitSearchController`: descripción en Markdown con el SQL resultante.
4. Las anotaciones de validación del DTO aparecen en el esquema: `required`, `minLength`…

> **Errata de la diapositiva 68.** `springdoc-openapi-core` y `-ui` 1.1.44 son de Spring Boot 2.
> En Spring Boot 3: `springdoc-openapi-starter-webmvc-ui` 2.x, que ya trae la interfaz.

### 2:00 – 2:35 · OpenAPI a fondo

🧑‍💻 **Ejercicio 14** (35 min): metadatos, esquema de seguridad *bearer* JWT, errores con
`ProblemDetail`, grupos `v1` y `legado`, `@Hidden`, rutas por propiedades.

En los últimos 5 minutos, ⌨ pide un token a `auth-service` y úsalo en el botón *Authorize*, todo
desde el navegador:

1. Arranca `auth-service` (`./mvnw -pl infra/auth-service spring-boot:run`, otra terminal).
2. **Swagger UI de auth-service** (<http://localhost:8060/>) → `POST /api/v1/auth/login` →
   cuerpo `{"username":"admin1","password":"4dm1n"}` → **Execute** → copia el `token`.
3. **Swagger UI de petclinic-api** → **Authorize** → `bearer-jwt` → pega el token.

(La API todavía no **exige** el token: eso es la sesión 6. Pero la documentación ya lo
anuncia.)

### 2:35 – 2:55 · CORS

⌨ (5 min) Qué es: una relajación **controlada** de la política del mismo origen del
**navegador**. No protege la API de `curl`, de Postman ni de otro servidor.
🧑‍💻 (15 min) **Ejercicio 6**: `CorsConfigurationSource` para `http://localhost:3000` y la
petición `OPTIONS` de verificación previa.

Pregunta del ejercicio: ¿por qué `allowedOrigins("*")` y `allowCredentials(true)` juntos los
rechaza el navegador?

### 2:55 – 3:00 · Cierre del módulo 2 y tarea

Repaso del módulo en cinco frases: contrato, DTO, `ProblemDetail`, versionado, documentación.

- **Tarea:** terminar el ejercicio 14 y pasar el *linter* al contrato exportado.
- Leer antes de la sesión 5 la diapositiva 72 (autenticación frente a identificación).

---

## Si vas con retraso

- Ejercicio 13: quedarse con la ruta propia, la proyección y la búsqueda; el resto, tarea.
- CORS como demo de 10 minutos, sin práctica.

## Si te sobra tiempo

- Exportar el contrato y generar un cliente con OpenAPI Generator para ver el enfoque
  *contract first*.
- Añadir `spring-data-rest-hal-explorer` y navegar los enlaces HAL.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `/data-api` responde 403 | La regla final `denyAll()` de `SecurityConfiguration`. |
| Aparecen repositorios que no se anotaron | Falta `spring.data.rest.detection-strategy=annotated`. |
| La búsqueda no aparece en `/search` | El parámetro no lleva `@Param`. |
| Swagger UI vacío tras mover las rutas | La regla de seguridad sigue apuntando a `/swagger-ui.html` y `/v3/api-docs`. |
| Con grupos, `/v3/api-docs` da 404 | Con `GroupedOpenApi` cada grupo tiene su ruta: `/v3/api-docs/v1`. |
