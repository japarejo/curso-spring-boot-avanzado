# Sesión 5 · Spring Security: quién eres y qué puedes hacer

**Módulo 3** · Diapositivas 71–81 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Spring Security es una **cadena de filtros** delante de la
> aplicación. Hace dos preguntas, y conviene no mezclarlas: ¿quién eres? (autenticación) y ¿qué
> puedes hacer? (autorización). Y su configuración por defecto es la correcta: denegar primero.

## Objetivos

- Distinguir identificación, autenticación y autorización.
- Ver la configuración **por defecto** de Spring Boot antes de escribir ninguna.
- Leer y modificar una `SecurityFilterChain` con reglas por ruta y por método HTTP.
- Guardar contraseñas correctamente y preparar el cambio de algoritmo.
- Usar el esquema canónico de usuarios de Spring Security.
- Leer el usuario actual del `SecurityContextHolder`.
- Proteger métodos con `@PreAuthorize` / `@PostAuthorize`.

## Antes de clase

- [ ] `practicas/tareas` con `spring-boot-starter-security` **comentado** (estado inicial).
- [ ] `petclinic-api` arrancado. Dos navegadores (o uno normal y otro privado) para entrar con
      dos usuarios a la vez.
- [ ] Tabla de usuarios a mano: `admin1/4dm1n`, `owner1/0wn3r`, `vet1/v3t`.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Conceptos: identificar, autenticar, autorizar | 🗣 | 72, 77–78 | |
| 0:10 | **Lo que Spring Boot hace solo** | 🧑‍💻 25' | 78–80 | M3 ejercicio 8 |
| 0:35 | La cadena de filtros del proyecto | ⌨ | 79–80 | `SecurityConfiguration` |
| 0:55 | **Ocultar no es proteger / la API abierta** | 🧑‍💻 30' | 79–80 | Ejercicios 1 y 2 |
| 1:25 | **Descanso** | | | |
| 1:35 | **Contraseñas** | ⌨ + 🧑‍💻 20' | 73–74 | Ejercicio 9 |
| 1:55 | **El esquema canónico de usuarios** | 🧑‍💻 30' | 73, 81 | Ejercicio 10 |
| 2:25 | **¿Quién soy?** `SecurityContextHolder` | 🧑‍💻 20' | 76 | Ejercicio 11 |
| 2:45 | Seguridad a nivel de método | ⌨ | — | Ejercicio 3 (demo) |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Conceptos

📊 Diapositivas 72, 77–78.

Tres palabras y un ejemplo cotidiano: en un hotel, decir tu nombre en recepción es
**identificarte**; enseñar el DNI es **autenticarte**; que tu tarjeta abra la habitación 214
pero no la 215 es **autorización**.

🗣 **Mensaje clave.** «Casi todos los fallos de seguridad de una API no son de autenticación:
son de autorización. El usuario es quien dice ser, pero accede a lo que no es suyo.»

### 0:10 – 0:35 · Lo que Spring Boot hace solo

📊 Diapositivas 78–80.
🧑‍💻 **Ejercicio 8** (25 min) sobre `practicas/tareas`.

Puntos para la puesta en común (últimos 5 minutos):

- Todo exige autenticación, incluido Actuator.
- Basic y formulario conviven.
- El `POST` con Basic da **403** con credenciales correctas: CSRF está activo.
- En el arranque, `Will secure any request with [...]`: cuéntalos. Unos quince filtros.

🗣 **Mensaje clave.** «La seguridad por defecto deniega primero. Nuestro trabajo es abrir lo
justo. Ese es el consejo de la diapositiva 175: seguridad desde el diseño y denegación por
defecto.»

### 0:35 – 0:55 · La cadena de filtros del proyecto

📊 Diapositivas 79–80 (la versión de Spring Boot 3).

⌨ Recorre `configuration/SecurityConfiguration` de arriba abajo. Las tres piezas: cadena,
origen de usuarios, codificador. En la cadena, explica lo que dicen las notas de la
diapositiva 80:

1. Cada `requestMatchers(...)` dice **ruta** y, opcionalmente, **método HTTP**.
2. Lo que va detrás dice la **regla**: `permitAll`, `authenticated`, `hasAuthority`.
3. `/x/**` cubre todo lo que cuelga de `/x`.
4. **El orden importa**: gana la primera regla que encaja. Por eso `denyAll()` va al final.

Señala también `dispatcherTypeMatchers(FORWARD, ERROR)`: sin ella, las JSP fallan.

> **Errata de las diapositivas 73, 75, 79 y 93.** Usan `WebSecurityConfigurerAdapter`,
> `authorizeRequests()` y `antMatchers()`, que **ya no existen** en Spring Security 6. Lo
> actual es lo que tienes en pantalla.

### 0:55 – 1:25 · Ocultar no es proteger / la API abierta

🧑‍💻 **Ejercicios 1 y 2** (30 min).

1. Ejercicio 1 (10 min): navegar a mano a lo que el menú no ofrece.
2. Ejercicio 2 (20 min): descubrir que `/api/**` está abierto, cerrarlo para escrituras y
   arreglar la prueba que se rompe con `@WithMockUser`.

🗣 **Mensaje clave.** «Quitar un botón del menú es usabilidad, no seguridad.»

### 1:25 – 1:35 · Descanso

### 1:35 – 1:55 · Contraseñas

📊 Diapositivas 73–74.

1. ⌨ Pregunta de la diapositiva 74: «¿qué *password encoder* usa PetClinic?». Abre
   `data.sql`: `{bcrypt}$2a$10$...`. Explica cada trozo.
2. 🧑‍💻 **Ejercicio 9** (15 min): ejecutar la prueba
   `Ejercicio09CodificacionContrasenasTests` y explicar sus cinco resultados.

🗣 **Mensaje clave.** «El prefijo `{bcrypt}` es lo que permite cambiar de algoritmo mañana
sin obligar a nadie a cambiar su contraseña.»

### 1:55 – 2:25 · El esquema canónico de usuarios

📊 Diapositivas 73, 81.
🧑‍💻 **Ejercicio 10** (30 min) sobre `practicas/tareas`: DDL oficial, `JdbcUserDetailsManager`,
dos usuarios con roles y reglas por método HTTP.

Puesta en común: ¿por qué `petclinic-api` declara las consultas a mano y `tareas` no? ¿Qué
añade `hasRole("ADMIN")` respecto a `hasAuthority("admin")`?

### 2:25 – 2:45 · ¿Quién soy?

📊 Diapositiva 76.
🧑‍💻 **Ejercicio 11** (20 min): las tres formas de leer el usuario, el usuario anónimo y el
experimento del hilo nuevo.

🗣 **Mensaje clave.** «A Spring Security no le importa cómo llegó el usuario al contexto
(diapositiva 76). Formulario, Basic, JWT o `@WithMockUser`: lo de detrás solo mira el
`SecurityContextHolder`. Por eso el JWT de la próxima sesión se integra sin tocar nada más.»

### 2:45 – 2:55 · Seguridad a nivel de método

⌨ Demo del ejercicio 3, pasos 2–3: `@PreAuthorize("hasAuthority('admin')")` en
`PetService.deletePet` y probar con `owner1` y `admin1`.

Enlaza con la sesión 1: «¿os acordáis del *proxy*? `@PreAuthorize` tiene las mismas dos
trampas que `@Transactional`: métodos privados y llamadas internas».

### 2:55 – 3:00 · Cierre y tarea

- **Tarea:** ejercicio 3 completo (incluido el `@PostAuthorize` y el porqué de que la prueba
  aislada no lo aplique).
- Para la sesión 6: traer instalado Postman o tener `jq`; vamos a trabajar con *tokens*.

---

## Si vas con retraso

- Ejercicio 10: se puede hacer solo hasta el paso 3 (usuarios creados) y dejar las reglas.
- Ejercicio 11: quedarse con las dos primeras formas.

## Si te sobra tiempo

- Ejercicio 3 completo en clase.
- `logging.level.org.springframework.security=TRACE` en `petclinic-api` y seguir una petición
  filtro a filtro.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| Tras añadir la cadena propia ya no funciona el usuario `user` | Un bean `SecurityFilterChain` propio retira **toda** la configuración por defecto. |
| 403 en un `POST` con credenciales correctas | CSRF. En una API sin estado, desactivarlo para esas rutas; en un formulario, enviar el token. |
| `hasRole("ADMIN")` no deja pasar a un usuario con autoridad `admin` | `hasRole` añade `ROLE_`. Usar `hasAuthority("admin")`. |
| `There is no PasswordEncoder mapped for the id "null"` | Contraseña guardada sin prefijo `{...}` con un `DelegatingPasswordEncoder`. |
| La consola H2 se ve en blanco | `X-Frame-Options: DENY`. Ver cómo lo resuelve `petclinic-api` (`frameOptions().sameOrigin()`). |
