# Módulo 2. Técnicas avanzadas para APIs REST

Guion docente. Duración estimada: **dos sesiones de 120 minutos**.

## Objetivos

- Diseñar URIs y respuestas conformes a los principios REST.
- Separar el modelo de dominio de la representación con DTOs, y saber por qué.
- Centralizar los errores con `ProblemDetail` (RFC 9457).
- Validar la entrada con Jakarta Bean Validation y traducir los fallos a respuestas útiles.
- Personalizar la serialización JSON: serializadores, `@JsonView`, conversores.
- Consumir APIs con `RestClient`, y saber por qué no con `RestTemplate`.
- Documentar la API con OpenAPI sin escribir documentación aparte.

## Mapa del material

| Concepto | Fichero |
|---|---|
| Controlador REST básico | `web/api/PetRestController.java` |
| Flujo DTO completo, 201 + Location | `web/api/RestfulVisitController.java` + `web/api/dto/` |
| Paginación, ordenación segura y enlaces | `web/api/RestfulVisitSearchController.java` |
| Versionado por URI (v1 y v2) | `web/api/PetVisitSearchController.java` |
| Errores con `ProblemDetail` | `web/api/GlobalExceptionHandler.java` |
| Serializadores Jackson | `web/api/BaseEntitySerializer.java`, `PetTypeSerializer.java` |
| Conversión de tipos | `configuration/GenericIdToEntityConverter.java`, `web/PetTypeFormatter.java` |
| Clientes HTTP comparados | `apiclients/BillsGateway.java`, `BillClient.java` |
| Configuración de OpenAPI | `configuration/OpenApi30Config.java` |

---

## Sesión 1

### 0:00 – 0:20 · Principios REST sobre la API real

Abrir <http://localhost:8080/swagger-ui.html> y recorrer los endpoints existentes.
Localizar las tres convenciones de ruta que conviven:

```
/api/pets            sin versión
/api/v1/visits       versión en la ruta
/api/v2/pets         otra versión
```

> **Mensaje clave.** No es un ejemplo de buen diseño: es un ejemplo real de lo que pasa
> cuando una API crece sin una convención acordada. Preguntar al aula qué convención
> elegirían y aplicarla en el ejercicio 1.

Repasar seguridad e idempotencia con la tabla del temario y contrastarla con lo que hace
cada endpoint de verdad.

### 0:20 – 0:50 · DTOs: el mensaje central del módulo

Comparar los dos controladores de visitas, que existen precisamente para eso:

| | `PetRestController` | `RestfulVisitController` |
|---|---|---|
| Qué devuelve | la entidad `Pet` | `VisitResponse` |
| Qué acepta | la entidad `Pet` | `CreateVisitRequest` validado |
| Respuesta al crear | 201 | 201 + cabecera `Location` |

Demostrar el problema en vivo:

```bash
curl -s localhost:8080/api/pets/1 | jq
```

Preguntar: *si mañana renombramos un campo de la entidad, ¿qué les pasa a los clientes?*

> **Mensaje clave.** El DTO no es ceremonia. Es lo que permite que el modelo de dominio y
> el contrato de la API evolucionen por separado. Y es lo que evita el segundo riesgo del
> OWASP API Security Top 10, la exposición excesiva de datos.

### 0:50 – 1:00 · Descanso

### 1:00 – 1:30 · Gestión de errores con ProblemDetail

Provocar los tres casos y mirar la respuesta:

```bash
curl -i localhost:8080/api/pets/99999                      # 404
curl -i -X POST localhost:8080/api/v1/visits \
     -H 'Content-Type: application/json' -d '{}'           # 400 con detalle por campo
```

Abrir `GlobalExceptionHandler` y señalar tres decisiones:

1. `@RestControllerAdvice(basePackages = "...web.api")` — el advice se acota a la API. Sin
   eso, también captura los errores de los controladores que devuelven JSP.
2. Hay un manejador explícito para `AccessDeniedException`. Sin él, el catch-all convertiría
   los 403 en 500.
3. El catch-all **registra** el mensaje de la excepción pero devuelve una referencia, no la
   traza. Filtrar trazas o SQL al cliente es uno de los errores del módulo 3.

### 1:30 – 2:00 · Validación

Recorrer `web/api/dto/CreateVisitRequest.java` y el manejador de
`MethodArgumentNotValidException`. Mostrar que las mismas anotaciones alimentan el esquema
de OpenAPI: ver el `schema` en `/v3/api-docs`.

---

## Sesión 2

### 0:00 – 0:30 · Serialización JSON a medida

Recorrer el bloque de Jackson, que es lo más avanzado del proyecto:

- `BaseEntitySerializer` / `BaseEntityDeserializer`: entidad ↔ identificador numérico.
- `PetTypeSerializer` / `PetTypeDeserializer`: `PetType` ↔ nombre, reutilizando el
  `Formatter` de MVC. Un solo sitio para la conversión.
- `@JsonIgnore` en `Owner.pets` y `Pet.visits`: el remedio a los bucles infinitos de las
  asociaciones bidireccionales.

### 0:30 – 1:00 · Paginación, ordenación y enlaces

`RestfulVisitSearchController` es el ejemplo completo:

```bash
curl -s "localhost:8080/api/v1/visits?page=0&size=2&sort=date,desc" | jq
```

Señalar la lista blanca `SORT_PROPERTIES`: sin ella, el cliente puede ordenar por cualquier
propiedad de la entidad, incluidas las que no queremos exponer.

```bash
curl -i "localhost:8080/api/v1/visits?sort=unknown"   # 400, no 500
```

### 1:00 – 1:10 · Descanso

### 1:10 – 1:40 · Consumo de APIs

`apiclients/BillsGateway` tiene los tres estilos uno al lado del otro. Levantar el servicio
de facturas y comparar:

```bash
./mvnw -pl infra/bills-service spring-boot:run       # en otra terminal
curl -s localhost:8080/api/v1/bills-demo/rest-template | jq
curl -s localhost:8080/api/v1/bills-demo/rest-client  | jq
curl -s localhost:8080/api/v1/bills-demo/feign        | jq
```

Después **parar** el servicio de facturas y repetir: el cortocircuito de resilience4j
devuelve lista vacía en lugar de propagar el error.

> **Mensaje clave.** `RestTemplate` está en modo mantenimiento desde Spring 5. En
> aplicaciones síncronas la opción de hoy es `RestClient`: misma API fluida que `WebClient`,
> sin arrastrar el modelo reactivo.

### 1:40 – 2:00 · Documentación con OpenAPI

Recorrer las anotaciones de `PetVisitSearchController`, que incluye la descripción en
markdown con el SQL resultante. Comprobar que las anotaciones de validación aparecen
reflejadas en el esquema.

---

## Ejercicios

En [`ejercicios.md`](ejercicios.md). Cubren los huecos que el código todavía no tiene:
`@JsonView`, versionado por cabecera, CORS y Spring Data REST.

## Errores frecuentes en clase

| Síntoma | Causa |
|---|---|
| El `curl` de POST devuelve 403 | Falta el `Content-Type: application/json`, o se está atacando una ruta con CSRF activo. `/api/**` lo tiene desactivado. |
| `LazyInitializationException` al serializar | Se está devolviendo una entidad con relaciones perezosas en lugar de un DTO. Es el ejercicio 2. |
| Swagger UI vacío | La aplicación no ha terminado de arrancar, o se está mirando `/swagger-ui.html` sin la barra final. |
