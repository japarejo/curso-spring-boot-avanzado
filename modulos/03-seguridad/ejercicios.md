# Módulo 3. Ejercicios

Se trabaja sobre `apps/petclinic-api` e `infra/auth-service`.
Soluciones en [`soluciones/`](soluciones/).

Todo se prueba con **Swagger UI**: la de `petclinic-api` (<http://localhost:8080/swagger-ui.html>)
y la de `auth-service` (<http://localhost:8060/>). Para autenticarse se usa el botón
**Authorize** (`bearer-jwt` para un token, `basic` para usuario y contraseña) o, en
`petclinic-api`, iniciar sesión en <http://localhost:8080/login> en la misma pestaña: Swagger UI
envía la *cookie* de sesión. Para probar «sin usuario», una **ventana privada**. Guía:
[`docs/swagger-ui-en-clase.md`](../../docs/swagger-ui-en-clase.md).

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

1. En una **ventana privada** (sin sesión), abre Swagger UI y prueba *Pets* →
   `GET /api/pets` y después `DELETE /api/pets/{id}` con `id` = `1`. ¿Qué código devuelve el
   borrado?

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -s localhost:8080/api/pets | jq
   curl -i -X DELETE localhost:8080/api/pets/1
   ```

   </details>

2. Localiza en `SecurityConfiguration` la línea que lo permite.
3. Cambia la regla para que las lecturas sigan siendo públicas pero las escrituras exijan
   autenticación:

   ```java
   .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
   .requestMatchers("/api/**").authenticated()
   ```

4. Vuelve a probar. Ejecuta la batería de pruebas y comprueba qué se rompe.

**Resultado esperado.** En la ventana privada, el `DELETE` pasa a **401** (y no a una
redirección al login: `SecurityConfiguration` responde 401 a la API y redirige solo la web).
En la ventana normal, tras entrar en <http://localhost:8080/login> como `admin1`, el mismo
`DELETE` desde Swagger UI funciona: va con la *cookie* de sesión. Alguna prueba de integración
falla porque no se autentica: arréglala añadiendo `@WithMockUser`.

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
   ```

   En la **Swagger UI de auth-service** (<http://localhost:8060/>) → *Autenticacion* →
   `POST /api/v1/auth/login` → cuerpo `{"username": "vet1", "password": "v3t"}` →
   **Execute**. Copia el valor de `token`.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   TOKEN=$(curl -s -X POST localhost:8060/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"vet1","password":"v3t"}' | jq -r .token)
   echo $TOKEN
   ```

   </details>

2. Pega el token en <https://jwt.io>. Identifica las tres partes.
3. Decodifica el cuerpo tú mismo:

   ```bash
   echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null
   ```

4. Valídalo: **Authorize** → `bearer-jwt` → pega el token (sin `Bearer`) → `POST
   /api/v1/auth/validate` → **Execute**. Después pulsa **Logout** en *Authorize*, pega el token
   con **un carácter cambiado** al final y repite.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -s -X POST localhost:8060/api/v1/auth/validate \
     -H "Authorization: Bearer ${TOKEN}X" | jq
   ```

   </details>

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
4. Prueba en la Swagger UI de `petclinic-api`, *Visits* → `GET /api/v1/visits`: primero sin
   autorizar (401) y después con **Authorize** → `bearer-jwt` → el token de `auth-service` (200).

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -i localhost:8080/api/v1/visits                              # 401
   curl -i -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/visits   # 200
   ```

   </details>

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

---

# Ejercicios añadidos para la edición de 40 horas

Cubren lo que las transparencias explican y los siete ejercicios anteriores no practicaban:
la configuración **por defecto** (diapositivas 78–79), la codificación de contraseñas (74), el
esquema canónico de usuarios (81), el `SecurityContextHolder` (76), el formulario de login
(75), el filtro JWT escrito a mano (87–93) y el login con proveedores externos (94).

---

## Ejercicio 8 · Lo que Spring Boot hace solo al añadir Spring Security

**Objetivo.** Ver la configuración de seguridad **por defecto** antes de escribir ninguna.
**Diapositivas:** 78–80. **Tiempo:** 25 min. Se trabaja sobre `practicas/tareas`
(módulo 1, ejercicio 6).

1. Descomenta `spring-boot-starter-security` en `practicas/tareas/pom.xml`. **No escribas
   ninguna clase de configuración.**
2. Arranca y busca en la consola:

   ```
   Using generated security password: 8c1f...
   ```

3. Abre <http://localhost:8080/swagger-ui.html>. ¿Qué pasa? ¿De dónde sale esa página de
   login? Entra con `user` y la contraseña generada.
4. Ya dentro, en Swagger UI, anota el código de cada respuesta:
   - *tarea-controller* → `GET /api/v1/tareas`;
   - *tarea-controller* → `POST /api/v1/tareas` con `{"descripcion": "x"}`;
   - *Actuator* → `GET /actuator/health`.

   Repite `GET /actuator/health` desde una **ventana privada** (sin sesión), escribiendo la URL
   directamente. Prueba también `/logout` y `/h2-console`.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -i localhost:8080/api/v1/tareas                         # ?
   curl -i -u user:<contraseña> localhost:8080/api/v1/tareas    # ?
   curl -i -u user:<contraseña> -X POST localhost:8080/api/v1/tareas \
        -H 'Content-Type: application/json' -d '{"descripcion":"x"}'   # ?
   curl -i localhost:8080/actuator/health                       # ?
   ```

   </details>
5. Ver la cadena de filtros que se ha montado:

   ```properties
   logging.level.org.springframework.security=DEBUG
   ```

   Busca en el arranque la línea `Will secure any request with [...]` y cuenta los filtros.
6. Fija el usuario por propiedades y comprueba que la contraseña generada desaparece:

   ```properties
   spring.security.user.name=alumno
   spring.security.user.password=curso
   spring.security.user.roles=USER
   ```

**Resultado esperado.** Todo exige autenticación, incluidos Swagger UI y Actuator; Basic y
formulario funcionan a la vez; el `GET` funciona pero el `POST` **da 403** aunque el usuario sea
correcto (CSRF está activo por defecto, y Swagger UI no envía el token CSRF); la consola H2 se
ve en blanco (cabecera `X-Frame-Options: DENY`).

> **Mensaje clave.** La seguridad por defecto es **restrictiva**: deniega primero y te deja
> abrir lo que necesites. Ese es el principio «denegación por defecto» de las transparencias,
> y es justo lo contrario de lo que hacía `petclinic-api` con `/api/**` (ejercicio 2).

Solución: [`soluciones/Ejercicio08y10SeguridadTareas.java`](soluciones/Ejercicio08y10SeguridadTareas.java)

---

## Ejercicio 9 · Contraseñas: nunca en claro, y preparadas para cambiar de algoritmo

**Objetivo.** Entender `BCryptPasswordEncoder` y el `DelegatingPasswordEncoder` que usa
el proyecto.
**Diapositiva:** 74. **Tiempo:** 25 min.

1. Mira cómo están guardadas las contraseñas en `apps/petclinic-api/src/main/resources/data.sql`.
   ¿Qué significa el prefijo `{bcrypt}`? ¿Y `$2a$10$`?
2. Copia [`soluciones/Ejercicio09CodificacionContrasenasTests.java`](soluciones/Ejercicio09CodificacionContrasenasTests.java)
   a `apps/petclinic-api/src/test/java/.../security/` y ejecútalo:

   ```bash
   ./mvnw -pl apps/petclinic-api test -Dtest=Ejercicio09CodificacionContrasenasTests
   ```

   Las pruebas demuestran cuatro cosas. Léelas y explica cada una:
   - codificar dos veces la misma contraseña da dos *hashes* distintos (la sal);
   - aun así, `matches()` reconoce las dos;
   - el coste se puede subir, y cada punto **duplica** el tiempo;
   - `upgradeEncoding()` detecta las contraseñas guardadas con un algoritmo antiguo.
3. Añade a `data.sql` un usuario con la contraseña **en claro**, `{noop}prueba`, con
   autoridad `owner`. Entra con él. Funciona: ¿por qué?
4. Genera el *hash* BCrypt de `prueba` desde la prueba y sustituye la línea.

**Resultado esperado.** Ninguna línea de código de producción cambia para pasar de `{noop}` a
`{bcrypt}`: el `DelegatingPasswordEncoder` elige el algoritmo por el prefijo.

**Pregunta.** La diapositiva 74 configura `NoOpPasswordEncoder`. ¿Qué pasa si un atacante
obtiene una copia de la base de datos? ¿Y con BCrypt de coste 10?

---

## Ejercicio 10 · El esquema canónico de usuarios de Spring Security

**Objetivo.** Usar las tablas que define Spring Security en lugar de diseñar unas propias.
**Diapositivas:** 73, 81. **Tiempo:** 30 min. Se trabaja sobre `practicas/tareas`.

1. Localiza el DDL oficial dentro del jar de Spring Security (no hace falta copiarlo de la
   web):

   ```
   org/springframework/security/core/userdetails/jdbc/users.ddl
   ```

2. Haz que Spring Boot lo ejecute al arrancar:

   ```properties
   spring.sql.init.schema-locations=classpath:org/springframework/security/core/userdetails/jdbc/users.ddl
   ```

3. Declara en una clase de configuración:
   - un `PasswordEncoder` delegado;
   - un `JdbcUserDetailsManager` sobre el `DataSource`;
   - un `CommandLineRunner` que cree dos usuarios con `manager.createUser(...)`:
     `ana` (rol `USER`) y `jefe` (roles `USER` y `ADMIN`).
4. Una `SecurityFilterChain` con estas reglas:
   - `GET /api/v1/tareas/**`: `USER`;
   - `POST`, `PUT`, `DELETE` sobre `/api/v1/tareas/**`: `ADMIN`;
   - `/actuator/health`: público;
   - Swagger UI (`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`): público, para poder
     probar lo demás;
   - todo lo demás: denegado.

   Y para que el botón **Authorize** de Swagger UI ofrezca usuario y contraseña, declara el
   esquema `basic` (con `@SecurityScheme` y `@OpenAPIDefinition(security = ...)`, ver la solución).
5. Mira las tablas en la consola H2 (`users`, `authorities`) y comprueba que las
   contraseñas están cifradas.
6. Compara con `petclinic-api`. Allí las tablas se llaman igual, pero las crea Hibernate a
   partir de las entidades `User` y `Authorities`. ¿Por qué `SecurityConfiguration` tiene
   que declarar a mano las consultas `setUsersByUsernameQuery(...)`? ¿Qué diferencia hay en
   cómo se guarda el rol (`ROLE_ADMIN` frente a `admin`)?

**Resultado esperado.** En Swagger UI, con **Authorize** → `basic` → `ana`/`ana`, el `GET`
devuelve 200 y el `POST` 403. Con **Logout** y `jefe`/`jefe`, el `POST` devuelve 201.

**Pregunta.** `hasRole("ADMIN")` frente a `hasAuthority("admin")`: ¿qué añade `hasRole`?
*(El prefijo `ROLE_`. `createUser(...roles("ADMIN"))` guarda `ROLE_ADMIN`.)*

Solución: [`soluciones/Ejercicio08y10SeguridadTareas.java`](soluciones/Ejercicio08y10SeguridadTareas.java)

---

## Ejercicio 11 · ¿Quién soy? El `SecurityContextHolder`

**Objetivo.** Leer el usuario actual de las tres formas que ofrece Spring y entender dónde
se guarda.
**Diapositiva:** 76. **Tiempo:** 25 min.

1. Crea `GET /api/v1/yo` en `petclinic-api` que devuelva el nombre, las autoridades y el tipo
   de `Authentication`. Escríbelo **tres veces**, en tres endpoints:
   - leyendo `SecurityContextHolder.getContext().getAuthentication()`;
   - recibiendo `Authentication` como parámetro del método;
   - recibiendo `@AuthenticationPrincipal UserDetails`.
2. Llámalo sin iniciar sesión. `/api/**` es público (si no hiciste el ejercicio 2): ¿qué
   usuario aparece? ¿Qué devuelve la versión con `@AuthenticationPrincipal`?
3. Inicia sesión como `owner1` en el navegador y llámalo desde la misma pestaña.
4. Lanza un hilo nuevo dentro del endpoint y lee el contexto desde él. ¿Qué ves? Arréglalo
   con `DelegatingSecurityContextRunnable` o `DelegatingSecurityContextExecutor`.
5. Escribe una prueba con `@WithMockUser(username = "pepe", authorities = "vet")` y otra con
   `@WithUserDetails("owner1")`, que carga el usuario **real** de la base de datos.

**Resultado esperado.** Sin sesión aparece `anonymousUser` con `ROLE_ANONYMOUS`, y el
parámetro `@AuthenticationPrincipal` llega a `null`. En el hilo nuevo, el contexto está vacío:
se guarda en un `ThreadLocal`.

> **Mensaje clave.** Spring Security no sabe ni le importa cómo llegó el usuario al contexto
> (diapositiva 76): formulario, Basic, JWT o una prueba con `@WithMockUser`. Todo lo que
> viene detrás solo mira el `SecurityContextHolder`.

Solución: [`soluciones/Ejercicio11QuienSoy.java`](soluciones/Ejercicio11QuienSoy.java)

---

## Ejercicio 12 · Un formulario de login propio

**Objetivo.** Sustituir la página generada por una propia y configurar el ciclo completo:
entrada, error, salida y «recordarme».
**Diapositiva:** 75. **Tiempo:** 30 min.

1. Crea `WEB-INF/jsp/login.jsp` con un formulario que envíe `username` y `password` por
   `POST` a `/login`. **Incluye el token CSRF**:

   ```jsp
   <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
   ```

2. Crea un controlador que sirva la vista en `GET /login`.
3. En `SecurityConfiguration`:

   ```java
   .formLogin(form -> form
       .loginPage("/login")
       .failureUrl("/login?error")
       .defaultSuccessUrl("/welcome")
       .permitAll())
   .logout(logout -> logout
       .logoutSuccessUrl("/login?logout")
       .deleteCookies("JSESSIONID"))
   .rememberMe(remember -> remember
       .key("cambia-esta-clave")
       .tokenValiditySeconds(7 * 24 * 3600))
   ```

4. Muestra un mensaje distinto con `?error` y con `?logout`.
5. Quita el campo oculto del CSRF y vuelve a probar. ¿Qué pasa?
6. Marca «recordarme», cierra el navegador y vuelve a entrar. Mira las *cookies*.

**Resultado esperado.** Sin el token CSRF, el `POST /login` da 403. Con «recordarme», aparece
una *cookie* `remember-me` que sobrevive al cierre del navegador.

**Pregunta.** ¿Por qué el formulario necesita CSRF y la API `/api/**` no? *(El formulario va
con *cookie* de sesión, que el navegador manda sola; la API sin estado lleva el token en una
cabecera que un tercero no puede añadir.)*

Solución: [`soluciones/Ejercicio12LoginPropio.java`](soluciones/Ejercicio12LoginPropio.java)

---

## Ejercicio 13 · El filtro JWT escrito a mano

**Objetivo.** Implementar el flujo de las diapositivas 87–93 en Spring Security 6, y
compararlo con el servidor de recursos del ejercicio 6.
**Diapositivas:** 87–93. **Tiempo:** 45 min. **Requisito:** ejercicio 4.

> Haz este ejercicio **o** el 6, no los dos sobre el mismo código: son dos maneras de
> resolver el mismo problema. Lo interesante es compararlas.

1. Añade a `apps/petclinic-api/pom.xml` las tres dependencias de jjwt (las versiones ya las
   fija el `pom.xml` raíz): `jjwt-api`, `jjwt-impl` y `jjwt-jackson`.
2. Crea `JwtAuthenticationFilter extends OncePerRequestFilter` que:
   1. lea `Authorization: Bearer <token>`;
   2. verifique la firma con el **mismo secreto** que `auth-service` (`jwt.secret`);
   3. lea el sujeto y el claim `authorities`;
   4. si todo va bien, deje un `UsernamePasswordAuthenticationToken` autenticado en el
      `SecurityContextHolder`;
   5. si el token es inválido o ha caducado, **no** autentique y deje seguir la cadena.
3. Crea una `SecurityFilterChain` para `/api/**` con `securityMatcher`, sin estado,
   sin CSRF, con el filtro **antes** de `UsernamePasswordAuthenticationFilter` y con un
   `AuthenticationEntryPoint` que responda 401 en JSON.
4. Prueba en la Swagger UI de `petclinic-api`, *Visits* → `GET /api/v1/visits`: sin
   autorizar (401), con **Authorize** → `bearer-jwt` → el token (200), y con el token con un
   carácter cambiado (401). Mira la consola con `DEBUG` en el paquete de tu filtro para ver por
   qué se rechaza.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -i localhost:8080/api/v1/visits                                   # 401
   curl -i -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/visits  # 200
   curl -i -H "Authorization: Bearer ${TOKEN}x" localhost:8080/api/v1/visits # 401
   ```

   </details>

5. Compara tu filtro con la solución del ejercicio 6. Cuenta líneas y enumera qué
   comprobaciones hace cada uno (firma, caducidad, emisor, `nbf`, algoritmo).

**Resultado esperado.** El mismo comportamiento que el ejercicio 6, con unas 80 líneas
propias frente a unas 20 de configuración.

**Diferencias con las transparencias.** La diapositiva 92 vuelve a cargar el usuario de la
base de datos en cada petición (`loadUserByUsername`): con JWT eso sobra, porque las
autoridades viajan en el token. Y la 93 usa `WebSecurityConfigurerAdapter`, que ya no existe
en Spring Security 6.

Solución: [`soluciones/Ejercicio13FiltroJwt.java`](soluciones/Ejercicio13FiltroJwt.java)

---

## Ejercicio 14 · Entrar con GitHub

**Objetivo.** Configurar un inicio de sesión OAuth 2 real.
**Diapositiva:** 94. **Tiempo:** 25 min (más el registro de la aplicación).

1. En GitHub: *Settings → Developer settings → OAuth Apps → New OAuth App*:

   | Campo | Valor |
   |---|---|
   | Homepage URL | `http://localhost:8062` |
   | Authorization callback URL | `http://localhost:8062/login/oauth2/code/github` |

2. Exporta las credenciales **sin escribirlas en ningún fichero del repositorio**:

   ```bash
   export GITHUB_CLIENT_ID=...
   export GITHUB_CLIENT_SECRET=...
   ./mvnw -pl extras/oauth2-login-service spring-boot:run
   ```

   En PowerShell: `$env:GITHUB_CLIENT_ID="..."`.
3. Entra en <http://localhost:8062>, pulsa GitHub y sigue el flujo. Abre las herramientas de
   desarrollo del navegador (pestaña Red) y localiza las redirecciones: a GitHub, de vuelta
   con `?code=...`, y el intercambio final.
4. Lee `OAuth2JwtSuccessHandler` y `ExternalIdentityService`: ¿qué pasa la **primera** vez
   que alguien entra con GitHub? ¿Y la segunda?
5. Responde por escrito: la diapositiva 94 configura Facebook con dos propiedades. ¿Por qué
   para GitHub o Google tampoco hace falta escribir las URL de autorización y de token?

**Resultado esperado.** Tras autenticarte en GitHub, el servicio te devuelve **su propio
JWT**: el resto del sistema no sabe que existe GitHub.

**Pregunta.** ¿Qué diferencia hay entre `oauth2Login()` (este ejercicio) y
`oauth2ResourceServer()` (ejercicio 6)? *(El primero hace que la aplicación sea **cliente**
de un proveedor de identidad; el segundo, que **acepte** tokens emitidos por otro.)*
