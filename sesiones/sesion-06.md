# Sesión 6 · Login, JWT y OAuth 2

**Módulo 3** · Diapositivas 75, 82–94 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Una aplicación web con sesión y una API sin estado necesitan
> **políticas de seguridad distintas**, y Spring Security permite tener las dos a la vez con dos
> cadenas de filtros. Un JWT está **firmado, no cifrado**: garantiza quién lo emitió, no oculta
> lo que lleva.

## Objetivos

- Sustituir la página de login generada por una propia, con error, salida y «recordarme».
- Leer, emitir y validar un JWT; saber qué garantiza la firma y qué no.
- Elegir entre firma simétrica (HS512) y asimétrica (RS256) con criterio.
- Proteger la API con un filtro JWT escrito a mano **y** con `oauth2ResourceServer`, y
  compararlos.
- Delegar el inicio de sesión en un proveedor externo (GitHub) con `oauth2Login`.

## Antes de clase

- [ ] `infra/auth-service` compilado y probado en los dos perfiles (por defecto y `rsa`).
- [ ] Una **OAuth App de GitHub** ya registrada por ti (Homepage `http://localhost:8062`,
      *callback* `http://localhost:8062/login/oauth2/code/github`) y sus credenciales en
      variables de entorno. El alumnado la registrará en casa.
- [ ] <https://jwt.io> abierto.
- [ ] Decidir si el grupo hará el **ejercicio 13** (filtro a mano, 45 min) o el **6**
      (servidor de recursos, 25 min). Con un grupo con menos experiencia, el 6.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso: `@PostAuthorize` y el *proxy* | ⌨ | — | Ejercicio 3 |
| 0:10 | **Un formulario de login propio** | 🧑‍💻 25' | 75 | M3 ejercicio 12 |
| 0:35 | Qué es un JWT | 🗣 | 82–86 | |
| 0:45 | **Emitir y romper un JWT** | 🧑‍💻 20' | 82–84 | Ejercicio 4 |
| 1:05 | **Simétrico frente a asimétrico** | 🧑‍💻 20' | 85, 88 | Ejercicio 5 |
| 1:25 | **Descanso** | | | |
| 1:35 | Cómo se valida: componentes de Spring Security | 🗣 | 87, 89–92 | |
| 1:45 | **El filtro JWT a mano** (o servidor de recursos) | 🧑‍💻 40' | 90–93 | Ejercicio 13 (o 6) |
| 2:25 | Comparación: filtro a mano frente a `oauth2ResourceServer` | ⌨ | — | Soluciones 6 y 13 |
| 2:35 | OAuth 2: entrar con GitHub | ⌨ | 94 | Ejercicio 14 (demo) |
| 2:50 | Errores de seguridad típicos (OWASP API Top 10) | 🗣 | — | Ejercicio 7 |
| 2:55 | Cierre del módulo 3 y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

⌨ Corrige el ejercicio 3: con `@PostAuthorize` en `OwnerService`, la prueba
`OwnerServiceIsolatedMockTests` **no** aplica la regla porque instancia el servicio con `new`.
No hay *proxy*.

### 0:10 – 0:35 · Un formulario de login propio

📊 Diapositiva 75.
🧑‍💻 **Ejercicio 12** (25 min).

Los dos momentos que merece la pena provocar:

- Quitar el campo oculto del CSRF → `POST /login` da 403.
- Marcar «recordarme» y ver la *cookie* `remember-me` en las herramientas del navegador.

🗣 **Mensaje clave.** «El formulario necesita CSRF porque viaja con la *cookie* de sesión, que
el navegador manda sola. Una API sin estado lleva el token en una cabecera que un tercero no
puede añadir. Por eso `petclinic-api` desactiva CSRF **solo** en `/api/**`.»

### 0:35 – 0:45 · Qué es un JWT

📊 Diapositivas 82–86.

Cabecera, cuerpo, firma. Los *claims* estándar (`iss`, `sub`, `aud`, `exp`, `nbf`, `iat`,
`jti`). La interacción de la diapositiva 84.

> **Errata de las diapositivas 82 y 83.** Dicen que la cadena va «encriptada» y que la tercera
> parte «encripta» las otras dos. No: el cuerpo va en Base64URL **sin cifrar**; la tercera
> parte es una **firma** (HMAC o RSA). Y la diapositiva 85 cita jjwt 0.9.1: el proyecto usa
> 0.12, con otra API (`Jwts.parser().verifyWith(...)`).

### 0:45 – 1:05 · Emitir y romper un JWT

🧑‍💻 **Ejercicio 4** (20 min).

🗣 **Mensaje clave.** «Habéis leído el contenido del token sin ninguna clave. Un JWT está
firmado, no cifrado. Nunca metáis dentro nada que no queráis que lea el usuario.»

### 1:05 – 1:25 · Simétrico frente a asimétrico

🧑‍💻 **Ejercicio 5** (20 min): perfil `rsa`, comparar `HmacJwtTokenService` y
`RsaJwtTokenService`, y la pregunta de los cinco microservicios.

Puesta en común: con HMAC, quien puede **validar** también puede **emitir**. Con RSA basta con
repartir la clave pública.

### 1:25 – 1:35 · Descanso

### 1:35 – 1:45 · Cómo se valida en Spring Security

📊 Diapositivas 87, 89–92.

Recorre el diagrama de la diapositiva 89 y sitúa cada pieza: el filtro lee la cabecera, valida
el token, deja una `Authentication` en el `SecurityContextHolder` (sesión 5) y la cadena sigue.
Las reglas de autorización **no cambian**: solo cambia cómo llega el usuario al contexto.

### 1:45 – 2:25 · El filtro JWT a mano

📊 Diapositivas 90–93.
🧑‍💻 **Ejercicio 13** (40 min), o **ejercicio 6** si el grupo va más justo (25 min, y los 15
restantes para el ejercicio 7).

Puntos para recorrer el aula en el ejercicio 13:

- El filtro **no** debe ser `@Component` (se registraría dos veces).
- Si el token es inválido, **no** se responde desde el filtro: se deja seguir y la regla
  `authenticated()` responde 401.
- `@Order(1)` y `securityMatcher("/api/**")` en la cadena de la API.

### 2:25 – 2:35 · Comparación

⌨ Proyecta las dos soluciones una al lado de la otra
([ejercicio 13](../modulos/03-seguridad/soluciones/Ejercicio13FiltroJwt.java) y
[ejercicio 6](../modulos/03-seguridad/soluciones/Ejercicio06ResourceServer.java)) y la tabla de
comprobaciones del final de la solución 13.

🗣 **Mensaje clave.** «Escribir el filtro una vez enseña **cómo** funciona. En un proyecto real,
`oauth2ResourceServer()`: menos código, más comprobaciones y respuestas estándar.»

### 2:35 – 2:50 · OAuth 2: entrar con GitHub

📊 Diapositiva 94.
⌨ **Demo** del ejercicio 14 con tus credenciales:

```bash
export GITHUB_CLIENT_ID=...  GITHUB_CLIENT_SECRET=...
./mvnw -pl extras/oauth2-login-service spring-boot:run
```

Con las herramientas del navegador abiertas (pestaña Red), sigue las redirecciones: a GitHub,
vuelta con `?code=`, intercambio y el JWT propio al final. Enseña `OAuth2JwtSuccessHandler`.

🗣 **Mensaje clave.** «`oauth2Login` hace a la aplicación **cliente** de un proveedor de
identidad. `oauth2ResourceServer` hace que **acepte** tokens emitidos por otro. Son las dos
mitades del mismo ecosistema.»

### 2:50 – 2:55 · Errores típicos

🗣 Recorre la tabla del ejercicio 7 (OWASP API Security Top 10 en el propio proyecto). Se hace
en casa.

### 2:55 – 3:00 · Cierre del módulo 3 y tarea

- **Tarea:**
  - Ejercicio 7 (auditoría del proyecto con el OWASP API Top 10).
  - Ejercicio 14: registrar vuestra propia OAuth App y entrar con GitHub.
  - Quien hizo el 6, que intente el 13 (o al revés).

---

## Si vas con retraso

- El ejercicio 12 se puede dejar solo con `loginPage` y `failureUrl` (sin «recordarme»).
- Hacer el **ejercicio 6** en lugar del 13.

## Si te sobra tiempo

- Ejercicio 15 del módulo 4 adelantado: pruebas automáticas de las reglas que acabáis de
  escribir, incluido `.with(jwt())`.
- Rotación de claves: ¿qué pasa con los tokens emitidos si cambia el secreto?

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `WeakKeyException` al arrancar | Secreto HS512 de menos de 512 bits. Usar el de `auth-service`. |
| 401 con un token recién emitido | Secreto distinto en `auth-service` y `petclinic-api`, o falta el prefijo `Bearer `. |
| El filtro se ejecuta dos veces | Está anotado con `@Component`. |
| La cadena de la API no se aplica | Falta `@Order` o el `securityMatcher` no encaja. |
| GitHub responde *redirect_uri mismatch* | La URL de *callback* registrada no es exactamente `http://localhost:8062/login/oauth2/code/github`. |
