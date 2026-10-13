# Sesión 3 · La representación: DTO, JSON a medida y conversores

**Módulo 2** · Diapositivas 59–63 y «convertidores avanzados» · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Lo que viaja por el cable **no es** el modelo de dominio. Separar
> la representación (DTO, serializadores, vistas) de las entidades es lo que permite que la
> API y el dominio cambien cada uno a su ritmo, y lo que evita exponer datos de más.

## Objetivos

- Explicar con un ejemplo real por qué no se devuelven entidades.
- Escribir un DTO de entrada y otro de salida.
- Escribir y registrar un serializador/deserializador Jackson propio; conocer la configuración
  global y su riesgo.
- Dar dos representaciones al mismo objeto con `@JsonView`.
- Escribir un conversor de texto a objeto de dominio.
- Consumir una API externa con `RestClient`, con tolerancia a fallos.

## Antes de clase

- [ ] `petclinic-api` arrancado.
- [ ] `infra/bills-service` compilado (`./mvnw -pl infra/bills-service -am package -DskipTests`)
      para el último bloque.
- [ ] Corregir la tarea: ¿alguien ha hecho el ejercicio 1? Si nadie, **haz tú el cambio a
      `/api/v1/pets`** antes de clase, porque los ejercicios de hoy usan esa ruta.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso y corrección del ejercicio 1 | ⌨ | 47 | M2 ejercicio 1 |
| 0:10 | El problema de devolver entidades | ⌨ 🗣 | 59, 62–63 | Ejercicio 2, pasos 1–3 |
| 0:25 | **De entidad a DTO** | 🧑‍💻 25' | 62–63 | Ejercicio 2 |
| 0:50 | Serialización a medida | ⌨ | 59–61 | `PetTypeSerializer`, `BaseEntitySerializer` |
| 1:00 | **Tu propio serializador** | 🧑‍💻 25' | 60–61 | Ejercicio 11 |
| 1:25 | **Descanso** | | | |
| 1:35 | Dos vistas del mismo objeto | 🧑‍💻 20' | — | Ejercicio 3 |
| 1:55 | Conversores condicionales | ⌨ + 🧑‍💻 30' | — | Ejercicio 12 |
| 2:25 | Consumir APIs: `RestTemplate`, `RestClient`, Feign | ⌨ + 🧑‍💻 | 2 (objetivos) | Ejercicio 4 |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

⌨ Corrige el ejercicio 1: `PetRestController` en `/api/v1/pets` y la ruta antigua con 301.

```bash
curl -i localhost:8080/api/pets          # 301 + Location: /api/v1/pets
```

Pregunta: «¿qué cambios **no** exigen versión nueva?» (añadir campos opcionales o recursos).

### 0:10 – 0:25 · El problema de devolver entidades

📊 Diapositivas 59, 62–63.

⌨ Ejercicio 2, pasos 1–3, en pantalla:

```bash
curl -s localhost:8080/api/v1/pets/1 | jq
```

Quita el `@JsonIgnore` de `Pet.visits` y repite. Con el `@JsonIgnore` de `Owner.pets` también
quitado, el bucle infinito de la diapositiva 59 aparece de verdad. **Deshaz el cambio.**

🗣 **Mensaje clave.** «Si mañana renombramos un campo de la entidad, rompemos a todos los
clientes. Y si mañana añadimos un campo sensible, lo publicamos sin darnos cuenta: es la
exposición excesiva de datos, parte del riesgo API3:2023 del OWASP API Security Top 10.»

Enseña el ejemplo de la diapositiva 63 hecho bien: `infra/auth-service/.../web/AuthDtos.java`
(los `record` de entrada y salida; la entidad `User` nunca sale).

### 0:25 – 0:50 · De entidad a DTO

🧑‍💻 **Ejercicio 2**, pasos 4–6 (25 min): `PetResponse` y ajustar
`PetRestControllerMockMvcTests`.

Recomienda `record` para el DTO. Quien acabe: que haga también un `CreatePetRequest` validado.

### 0:50 – 1:00 · Serialización a medida

📊 Diapositivas 59–61.

⌨ Recorre lo que ya tiene el proyecto:

- `BaseEntitySerializer` / `BaseEntityDeserializer`: entidad ↔ identificador.
- `PetTypeSerializer` / `PetTypeDeserializer`: `PetType` ↔ nombre, reutilizando el
  `PetTypeFormatter` de MVC. Un único sitio para la conversión.
- Cómo se activan: anotando el **campo** (`@JsonSerialize(using = ...)` en `Pet.type`).

### 1:00 – 1:25 · Tu propio serializador

🧑‍💻 **Ejercicio 11** (25 min): `GET /api/v1/vets` y `SpecialtyJson` con `@JsonComponent`.

Reserva los últimos 5 minutos para el paso 4 en pantalla: activa
`spring.jackson.property-naming-strategy=SNAKE_CASE` y enseña cómo **toda** la API cambia de
golpe. Deshazlo.

🗣 **Mensaje clave.** «La configuración global de Jackson cambia el contrato de toda la API.
Úsala el primer día del proyecto o no la uses.»

### 1:25 – 1:35 · Descanso

### 1:35 – 1:55 · Dos vistas del mismo objeto

🧑‍💻 **Ejercicio 3** (20 min): `@JsonView` sobre `PetResponse`, resumen en el listado y
detalle en la ficha.

Cierra con la pregunta del ejercicio: «¿`@JsonView` o dos DTO?». La respuesta depende de si son
la **misma cosa vista de dos maneras** o **dos contratos** que evolucionan por separado.

### 1:55 – 2:25 · Conversores condicionales

📊 Temario: «Convertidores avanzados y condicionales en APIs REST».

1. ⌨ (10 min) Lee con el aula `configuration/GenericIdToEntityConverter`: `getConvertibleTypes()`
   ofrece los pares, `matches()` decide caso a caso, `convert()` busca la entidad con el
   `EntityManager`. Haz el paso 2 del ejercicio 12 en pantalla: `@PathVariable("pet") Pet pet`
   funciona sin una línea de código de búsqueda.
2. 🧑‍💻 (20 min) Ejercicio 12, pasos 3–5: `Converter<String, RangoFechas>`.

🗣 **Mensaje clave.** «Un conversor pone el análisis de texto en **un** sitio. Pero ojo con el
conversor genérico: carga cualquier entidad a partir de un número que manda el cliente. Si la
autorización no comprueba la propiedad del objeto, es el riesgo API1:2023 del OWASP API
Security Top 10 (autorización rota a nivel de objeto).»

### 2:25 – 2:55 · Consumir APIs

📊 Objetivo 1 del curso (diapositiva 3): «personalizar cómo se **consumen** e implementan
APIs REST».

1. ⌨ Abre `apiclients/BillsGateway`: los tres estilos uno al lado del otro.
2. ⌨ Levanta el servicio de facturas y compara:

   ```bash
   ./mvnw -pl infra/bills-service spring-boot:run           # otra terminal
   curl -s localhost:8080/api/v1/bills-demo/rest-template | jq
   curl -s localhost:8080/api/v1/bills-demo/rest-client  | jq
   curl -s localhost:8080/api/v1/bills-demo/feign        | jq
   ```

3. **Para** el servicio de facturas y repite: el cortocircuito devuelve `[]`.
4. 🧑‍💻 Ejercicio 4, paso 3 (15 min): manejar el 404 con `onStatus`.

🗣 **Mensaje clave.** «`RestTemplate` está en mantenimiento. En código síncrono nuevo,
`RestClient`. En Spring Boot 4, además, las interfaces `@HttpExchange` sustituyen a Feign.»

### 2:55 – 3:00 · Cierre y tarea

- **Tarea:** ejercicio 5 (versionado por cabecera) y terminar el 12 si no dio tiempo.
- Para la sesión 4: traer pensada una respuesta a «¿generaríais una API automáticamente a
  partir de los repositorios?».

---

## Si vas con retraso

- El ejercicio 3 (`@JsonView`) se puede hacer como demo de 5 minutos.
- En el ejercicio 11, saltar el deserializador y quedarse con el serializador.

## Si te sobra tiempo

- Ejercicio 5 en clase.
- Enseñar la nota de Spring Boot 4: Jackson 3 (`tools.jackson`) y `@JacksonComponent`.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `LazyInitializationException` al serializar | Se está devolviendo una entidad con relaciones perezosas: justo lo que resuelve el DTO. |
| El `@JsonComponent` no se aplica | La clase está fuera del paquete escaneado, o las clases internas no son `static`. |
| El conversor no se usa | No es un bean (`@Component`), o el parámetro no es del tipo exacto del conversor. |
| `conRestClient()` devuelve `[]` con el servicio arrancado | `petclinic.bills.base-url` apunta a otro puerto; ver `BillsProperties`. |
