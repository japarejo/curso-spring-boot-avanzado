# Módulo 2. Ejercicios

Se trabaja sobre `apps/petclinic-api`. Soluciones en [`soluciones/`](soluciones/).

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

**Resultado esperado.** `curl -i localhost:8080/api/pets` devuelve 301 con `Location`
apuntando a `/api/v1/pets`.

**Pregunta.** ¿Qué cambios **no** exigen una versión nueva? *(Añadir campos opcionales o
recursos nuevos, si los clientes ignoran lo que no conocen.)*

---

## Ejercicio 2 · De entidad a DTO

**Objetivo.** Ver en la práctica por qué no se devuelven entidades.

1. Pide una mascota y observa la respuesta:

   ```bash
   curl -s localhost:8080/api/pets/1 | jq
   ```

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
   curl -s localhost:8080/api/v1/bills-demo/rest-client | jq
   ```

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

2. Prueba las dos variantes:

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
4. Comprueba la petición de verificación previa:

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
4. Explora lo que aparece solo:

   ```bash
   curl -s localhost:8080/data-api/vets | jq
   ```

5. Define una proyección con `@Projection` y pruébala con `?projection=...`.

**Pregunta de cierre.** ¿En qué casos usarías Spring Data REST en un proyecto real? ¿Qué
pierdes frente a controladores escritos a mano? *(Control del contrato, DTOs, validación
específica, versionado.)*
