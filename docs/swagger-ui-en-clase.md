# Swagger UI en clase: probar la API sin `curl`

En este curso la API se prueba **desde la documentación interactiva** (Swagger UI) siempre que
se pueda. Es más visual en el proyector, no depende de la consola (adiós a las diferencias
entre bash y PowerShell) y refuerza una idea del módulo 2: la documentación **es** el contrato.

`curl` se reserva para lo que Swagger UI no puede hacer. Los guiones y ejercicios lo indican
siempre así: primero la ruta en Swagger UI y, desplegable, el equivalente con `curl`.

## Dónde está cada Swagger UI

| Servicio | Swagger UI | Documento OpenAPI |
|---|---|---|
| `apps/petclinic-api` | <http://localhost:8080/swagger-ui.html> | `/v3/api-docs` |
| `infra/auth-service` | <http://localhost:8060/> | `/doc` |
| `infra/bills-service` | <http://localhost:8040/> | `/doc` |
| `practicas/tareas` | <http://localhost:8080/swagger-ui.html> | `/v3/api-docs` |
| `extras/oauth2-login-service` | <http://localhost:8062/swagger-ui.html> | `/v3/api-docs` |

Todos llevan la misma configuración de clase (en su `application.properties`):

| Propiedad | Efecto |
|---|---|
| `springdoc.swagger-ui.try-it-out-enabled=true` | Los formularios salen ya editables: no hay que pulsar *Try it out* |
| `springdoc.swagger-ui.display-request-duration=true` | Muestra cuánto tardó cada petición (útil en el módulo 6) |
| `springdoc.swagger-ui.persist-authorization=true` | El token o el usuario de *Authorize* sobrevive a recargar la página |
| `springdoc.swagger-ui.filter=true` | Caja de búsqueda por etiqueta |
| `springdoc.show-actuator=true` *(petclinic-api y tareas)* | Los endpoints de **Actuator** aparecen en Swagger UI y se pueden llamar, incluido el `POST` a `loggers` |

## Cómo se escriben las instrucciones

> **Swagger UI** → *Visits* → `GET /api/v1/visits` → `size` = `2` → **Execute**.

Se lee así: en Swagger UI, abrir la etiqueta *Visits*, desplegar la operación
`GET /api/v1/visits`, rellenar el parámetro `size` con `2` y pulsar **Execute**. Debajo
aparecen la petición equivalente en `curl` (útil para copiarla), el código de estado, las
cabeceras y el cuerpo de la respuesta.

## Las cinco operaciones que más se repiten

### 1. Enviar un cuerpo JSON

En las operaciones `POST`/`PUT`, el cuadro *Request body* viene relleno con un **ejemplo**
generado a partir del esquema (y de los `@Schema(example = ...)` del DTO). Se edita ahí mismo.
Truco: si el ejemplo no sirve, conviene mejorarlo en el DTO, no en clase cada vez.

### 2. Ver las cabeceras de la respuesta

*Response headers* debajo del código de estado: `Location` tras un 201, `ETag`, `Cache-Control`,
`X-Saludo`… Ojo: el navegador solo deja ver a Swagger UI las cabeceras que el servidor expone;
en el mismo origen (nuestro caso) se ven todas.

### 3. Autenticarse

Botón **Authorize** (arriba a la derecha):

| Esquema | Cuándo | Qué se pone |
|---|---|---|
| `bearer-jwt` | La API exige JWT (módulo 3, ejercicios 6 y 13) | El token **sin** el prefijo `Bearer` |
| `basic` | Actuator protegido (módulo 5, ejercicio 9), `tareas` con Basic (módulo 3, ejercicio 10) | Usuario y contraseña |

Y una tercera vía, sin *Authorize*: si se ha iniciado sesión en la aplicación web
(<http://localhost:8080/login>) en la **misma pestaña del navegador**, Swagger UI envía la
*cookie* de sesión y las peticiones van autenticadas como ese usuario.

Para obtener un JWT sin salir del navegador: Swagger UI de `auth-service`
(<http://localhost:8060/>) → `POST /api/v1/auth/login` → copiar el `token` de la respuesta.

### 4. Cambiar el nivel de registro sin reiniciar

**Swagger UI** → *Actuator* → `POST /actuator/loggers/{name}` → `name` =
`org.hibernate.SQL` → cuerpo `{"configuredLevel": "DEBUG"}` → **Execute**.

### 5. Errores con `ProblemDetail`

Los errores se ven igual que las respuestas correctas: código (400, 404, 405…) y el cuerpo
`application/problem+json` con `title`, `detail` y `status`. Es la forma más clara de enseñar
la diapositiva nueva B ([`diapositivas-nuevas.md`](diapositivas-nuevas.md)).

## Cuándo **no** sirve Swagger UI (y se usa `curl`)

| Caso | Por qué | Dónde aparece |
|---|---|---|
| Enviar *cookies* propias | El navegador no deja que una página fije la cabecera `Cookie` | M2 ejercicio 9 (`ultimaCiudad`) |
| Peticiones `OPTIONS` de CORS desde otro origen | La verificación previa la hace el navegador, no se lanza a mano | M2 ejercicio 6 |
| Varias peticiones **a la vez** | Swagger UI las lanza de una en una | M6 ejercicio 9 (agotar el *pool*) |
| Ver la redirección de una ruta antigua (301) | `fetch` sigue la redirección solo y muestra el destino | M2 ejercicio 1 |
| Servicios sin API documentada | El servidor de configuración (`/encrypt`, `/{app}/{perfil}`), Eureka | M5 ejercicio 10 |
| Probar con un token **manipulado** | Se puede pegar en *Authorize*, pero es más claro en la consola | M3 ejercicio 4 (opcional) |
| Automatizar | Scripts, integración continua: `curl`, Newman o REST Assured | M4 ejercicios 16 y 17 |

Para los `GET` de servicios sin Swagger UI (Config Server, `/actuator/...` de otros servicios),
basta el **navegador**: el JSON se ve formateado en Firefox y en Chrome/Edge.

## Si Swagger UI no aparece

| Síntoma | Causa |
|---|---|
| 404 en `/swagger-ui.html` | La aplicación no ha terminado de arrancar, o falta la dependencia `springdoc-openapi-starter-webmvc-ui`. |
| Redirige al login | La ruta de Swagger UI no está abierta en la cadena de seguridad (`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`). |
| *Failed to fetch* al ejecutar | La aplicación se ha parado, o se está mirando un Swagger UI de otro puerto. |
| La operación devuelve 401/403 | Falta pulsar *Authorize*, o el token ha caducado (1 hora en `auth-service`). |
| No aparece *Actuator* | Falta `springdoc.show-actuator=true`. |
