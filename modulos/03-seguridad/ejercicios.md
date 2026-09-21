# Módulo 3. Ejercicios

Se trabaja sobre `apps/petclinic-api` e `infra/auth-service`.
Soluciones en [`soluciones/`](soluciones/).

---

## Ejercicio 1 · Ocultar no es proteger

**Objetivo.** Distinguir usabilidad de seguridad.

1. Entra como `vet1` / `v3t`. Comprueba que el menú no ofrece la sección de propietarios.
2. Navega **a mano** a `http://localhost:8080/owners/1`.
3. Anota el código de respuesta.
4. Ahora entra como `owner1` / `0wn3r` y repite.
5. Con la sesión de `owner1`, prueba `http://localhost:8080/owners/2`.

**Resultado esperado.** El paso 3 da 403: la protección de ruta funciona. El paso 5 es el
interesante: `owner1` tiene autoridad `owner`, así que el filtro le deja pasar, y quien lo
frena es la comprobación de propiedad dentro de `OwnerService`.

**Pregunta.** Si esa comprobación no existiera, ¿qué vería `owner1`? Ese es el primer riesgo
del OWASP API Security Top 10.

---

## Ejercicio 2 · La API está abierta

**Objetivo.** Encontrar un agujero real en el proyecto y taparlo.

1. Sin iniciar sesión, prueba:

   ```bash
   curl -s localhost:8080/api/pets | jq
   curl -i -X DELETE localhost:8080/api/pets/1
   ```

2. Localiza en `SecurityConfiguration` la línea que lo permite.
3. Cambia la regla para que las lecturas sigan siendo públicas pero las escrituras exijan
   autenticación:

   ```java
   .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
   .requestMatchers("/api/**").authenticated()
   ```

4. Vuelve a probar. Ejecuta la batería de pruebas y comprueba qué se rompe.

**Resultado esperado.** El DELETE pasa a 401. Alguna prueba de integración falla porque no
se autentica: arréglala añadiendo `@WithMockUser`.

**Pregunta.** ¿Por qué el `DELETE` funcionaba sin token CSRF? *(Porque `/api/**` tiene CSRF
desactivado, y eso es correcto en una API sin estado.)*

---

## Ejercicio 3 · Seguridad a nivel de método

**Objetivo.** Usar `@PreAuthorize` y `@PostAuthorize`, que el proyecto todavía no tiene.

1. Comprueba que `@EnableMethodSecurity` ya está activo en `SecurityConfiguration`.
2. Añade a `PetService.deletePet(int)`:

   ```java
   @PreAuthorize("hasAuthority('admin')")
   ```

3. Prueba a borrar una mascota autenticado como `owner1` y como `admin1`.
4. Ahora sustituye la comprobación manual de `OwnerService.findOwnerById(int, Principal)`
   por una declarativa:

   ```java
   @PostAuthorize("returnObject == null or returnObject.user.username == authentication.name")
   ```

5. Ejecuta `OwnerServiceIsolatedMockTests` y observa qué ocurre.

**Resultado esperado.** El paso 3 da 403 para `owner1`. En el paso 5, las pruebas que
instancian el servicio con `new` **no** aplican la anotación: la seguridad de método
funciona con un proxy de Spring, y ahí no hay proxy.

> **Mensaje clave.** Dos trampas de `@PreAuthorize`, las mismas que las de `@Transactional`:
> no se aplica si el método se llama desde la propia clase, ni sobre métodos privados.

---

## Ejercicio 4 · Emitir y romper un JWT

**Objetivo.** Entender qué garantiza una firma.

1. Levanta el servicio de autenticación y pide un token:

   ```bash
   ./mvnw -pl infra/auth-service spring-boot:run

   TOKEN=$(curl -s -X POST localhost:8060/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"vet1","password":"v3t"}' | jq -r .token)
   echo $TOKEN
   ```

2. Pega el token en <https://jwt.io>. Identifica las tres partes.
3. Decodifica el cuerpo tú mismo:

   ```bash
   echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null
   ```

4. Cambia un carácter del token y valídalo:

   ```bash
   curl -s -X POST localhost:8060/api/v1/auth/validate \
     -H "Authorization: Bearer ${TOKEN}X" | jq
   ```

**Resultado esperado.** El paso 3 muestra el contenido **sin ninguna clave**. El paso 4
devuelve `"valid": false`.

> **Mensaje clave.** Un JWT está firmado, no cifrado. Nunca metas dentro nada que no quieras
> que lea el usuario.

---

## Ejercicio 5 · De firma simétrica a asimétrica

**Objetivo.** Elegir algoritmo con criterio.

1. Arranca con el perfil `rsa`:

   ```bash
   ./mvnw -pl infra/auth-service spring-boot:run -Dspring-boot.run.profiles=rsa
   ```

2. Pide un token y comprueba en jwt.io que la cabecera dice `"alg": "RS256"`.
3. Lee `HmacJwtTokenService` y `RsaJwtTokenService` y enumera las diferencias.
4. Responde por escrito: tienes cinco microservicios que deben validar tokens. ¿Qué
   algoritmo eliges y por qué?

**Resultado esperado.** Con HMAC habría que repartir el secreto entre los cinco, y cualquiera
de ellos podría emitir tokens. Con RSA basta con repartir la clave pública.

---

## Ejercicio 6 · La aplicación como servidor de recursos

**Objetivo.** Conectar las dos piezas: quien emite y quien consume.

1. `apps/petclinic-api` ya tiene `spring-boot-starter-oauth2-resource-server` en el
   `pom.xml`, sin configurar. Compruébalo.
2. Configura un `JwtDecoder` que valide los tokens emitidos por `auth-service` (secreto
   compartido para HS512, o clave pública para RSA).
3. Añade una segunda `SecurityFilterChain` con `securityMatcher("/api/**")`, sin estado y
   con `oauth2ResourceServer`.
4. Prueba:

   ```bash
   curl -i localhost:8080/api/v1/visits                              # 401
   curl -i -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/visits   # 200
   ```

**Resultado esperado.** La API deja de estar abierta y acepta el token emitido por el otro
servicio.

**Pregunta.** ¿Por qué dos cadenas de filtros y no una con más reglas? *(Porque la API es sin
estado y la parte web usa sesión y formulario: son dos políticas distintas.)*

Solución: [`soluciones/Ejercicio06ResourceServer.java`](soluciones/Ejercicio06ResourceServer.java)

---

## Ejercicio 7 · Auditoría del proyecto

**Objetivo.** Aplicar la lista del OWASP API Security Top 10 a código real.

Busca en el repositorio un ejemplo de cada riesgo y propón la corrección:

| Riesgo | Pista |
|---|---|
| Autorización rota a nivel de objeto | ¿qué pasa si quitas la comprobación de `OwnerService`? |
| Exposición excesiva de datos | compara `PetRestController` con `RestfulVisitController` |
| Secretos en el repositorio | `grep -rn "jwt.secret\|private-key" infra/` |
| Tokens de vida larga | `jwt.validity-seconds` |
| Errores que filtran información | `GlobalExceptionHandler.handleUnexpected` |
| Falta de límites | ¿qué devuelve `/api/pets` si hay un millón de mascotas? |

Para el último, implementa paginación obligatoria en `PetRestController`, tomando como
modelo `RestfulVisitSearchController`.
