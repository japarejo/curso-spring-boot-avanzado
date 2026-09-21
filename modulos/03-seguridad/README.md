# Módulo 3. Seguridad en aplicaciones Spring Boot

Guion docente. Duración estimada: **dos sesiones de 120 minutos**.

## Objetivos

- Distinguir identificación, autenticación y autorización.
- Configurar Spring Security 6 publicando beans, no heredando de clases base.
- Diseñar reglas de acceso por ruta y por método.
- Entender qué es un JWT, cómo se firma y cómo se valida.
- Elegir entre firma simétrica y asimétrica con criterio.
- Razonar cuándo se puede desactivar CSRF y cuándo no.
- Reconocer los errores del OWASP API Security Top 10 en código real.

## Mapa del material

| Concepto | Dónde |
|---|---|
| `SecurityFilterChain` | `apps/petclinic-api/.../configuration/SecurityConfiguration.java` |
| Usuarios en base de datos | el mismo fichero, bean `UserDetailsManager` |
| Cifrado de contraseñas | el mismo fichero, bean `PasswordEncoder` |
| Autorización en la vista | `webapp/WEB-INF/tags/menu.tag` |
| Control de acceso por propiedad | `service/OwnerService.findOwnerById(int, Principal)` |
| Emisión y validación de JWT | `infra/auth-service/` |
| Firma simétrica / asimétrica | `auth/security/HmacJwtTokenService`, `RsaJwtTokenService` |
| Pruebas de seguridad | `src/test/.../web/security/PetControllerSecurityTest.java` |
| ACL por instancia (ampliación) | [`guion-acl.md`](guion-acl.md) |

---

## Sesión 1 · Autenticación y autorización

### 0:00 – 0:20 · Conceptos y punto de partida

Arrancar y entrar con `owner1` / `0wn3r`. Mostrar que Spring genera la página de login sin
que hayamos escrito ninguna vista.

Después, entrar con `admin1` / `4dm1n` y con `vet1` / `v3t`, y comparar el menú: el bloque
`<sec:authorize access="hasAuthority('admin')">` solo aparece para el primero.

> **Mensaje clave inmediato.** Ocultar un enlace es usabilidad, no seguridad. Demostrarlo:
> con la sesión de `vet1` abierta, navegar a mano a `/owners/1`. El servidor responde,
> porque la protección real está en el filtro, no en la vista.

### 0:20 – 0:50 · La cadena de filtros

Recorrer `SecurityConfiguration` de arriba abajo. Cuatro puntos que merecen parada:

1. **`dispatcherTypeMatchers(FORWARD, ERROR).permitAll()`** — sin esto, cada `forward`
   interno al renderizar una JSP se evalúa como si fuera una petición nueva y acaba en
   `denyAll`. Es la causa de los "500 misteriosos" al añadir seguridad a una app con vistas.
2. **`.anyRequest().denyAll()`** al final, no `.authenticated()`. Con `denyAll`, una ruta
   nueva que nadie haya autorizado explícitamente queda cerrada. Con `authenticated`, queda
   abierta a cualquiera que haya iniciado sesión.
3. **CSRF desactivado solo en `/h2-console`, `/actuator` y `/api`**, no globalmente.
4. **`PasswordEncoderFactories.createDelegatingPasswordEncoder()`**, no
   `new BCryptPasswordEncoder()`. Enseñar `data.sql`:

   ```sql
   INSERT INTO users(username,password,enabled)
   VALUES ('admin1','{bcrypt}$2a$10$3Qh8FLZ...',TRUE);
   ```

   El prefijo `{bcrypt}` es lo que permite migrar de algoritmo sin invalidar las contraseñas
   ya guardadas.

### 0:50 – 1:00 · Descanso

### 1:00 – 1:30 · Seguridad a nivel de método

`@EnableMethodSecurity` está activo. Preguntar al aula cuántas anotaciones `@PreAuthorize`
hay en el proyecto:

```bash
grep -rn "@PreAuthorize\|@PostAuthorize" apps/ --include=*.java | wc -l
```

La respuesta es cero, y es deliberado: es el ejercicio 3.

Mientras tanto, mostrar cómo está resuelto **a mano** el control por propiedad en
`OwnerService.findOwnerById(int, Principal)`:

```java
Owner current = ownerRepository.findByUserName(p.getName());
if (current == null || current.getId() != id) {
    return null;
}
```

> **Mensaje clave.** Esto es autorización a nivel de objeto, el **primer** riesgo del OWASP
> API Security Top 10. Comprobar el rol no basta: hay que comprobar la propiedad del
> recurso. Un `owner` autenticado no puede ver las mascotas de otro `owner`.

### 1:30 – 2:00 · Pruebas de seguridad

Abrir `PetControllerSecurityTest` y ejecutarlo:

```bash
./mvnw -pl apps/petclinic-api test -Dtest=PetControllerSecurityTest
```

Un test comprueba que un `vet` recibe 403 en el alta de mascota y otro que un `owner`
recibe 200. Señalar `@WithMockUser(authorities = {"vet"})`: la prueba se ejecuta como ese
usuario sin necesidad de credenciales reales.

---

## Sesión 2 · JWT

### 0:00 – 0:20 · Qué es un JWT

Levantar el servicio de autenticación:

```bash
./mvnw -pl infra/auth-service spring-boot:run
```

Pedir un token y **pegarlo en <https://jwt.io>** para ver las tres partes:

```bash
curl -s -X POST localhost:8060/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin1","password":"4dm1n"}' | jq
```

Que vean que el cuerpo **se lee sin ninguna clave**. Un JWT está firmado, no cifrado.

> **Mensaje clave.** Nunca metas en un JWT nada que no quieras que lea el usuario. La firma
> garantiza que nadie lo ha modificado, no que nadie pueda leerlo.

### 0:20 – 0:50 · Emisión y validación

Recorrer `infra/auth-service/src/main/java/.../security/`:

- `JwtTokenService` — la interfaz.
- `AbstractJwtTokenService` — construcción de claims, emisor, caducidad.
- `HmacJwtTokenService` / `RsaJwtTokenService` — lo único que cambia entre ambos.

Demostrar que el token manipulado se rechaza:

```bash
TOKEN=$(curl -s -X POST localhost:8060/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"vet1","password":"v3t"}' | jq -r .token)

curl -s -X POST localhost:8060/api/v1/auth/validate \
  -H "Authorization: Bearer $TOKEN" | jq

# Cambiar un carácter del token y repetir: valid pasa a false
```

### 0:50 – 1:00 · Descanso

### 1:00 – 1:30 · Simétrico frente a asimétrico

Reiniciar con el otro perfil:

```bash
./mvnw -pl infra/auth-service spring-boot:run -Dspring-boot.run.profiles=rsa
```

Pedir otro token y comprobar en jwt.io que la cabecera dice `"alg": "RS256"`.

> **Mensaje clave.** Con HMAC, todo servicio que **valide** tokens puede **emitirlos**,
> porque comparte el secreto. Con RSA, los servicios de recursos solo necesitan la clave
> pública. Por eso los proveedores de identidad usan firma asimétrica y publican su clave
> en un punto JWKS.

Abrir `application-rsa.properties` y señalar el aviso: las claves están versionadas a
propósito **para el curso**, y eso en un sistema real es uno de los errores de la lista.

### 1:30 – 2:00 · Buenas prácticas y cierre

Recorrer los errores frecuentes del temario buscándolos en el código:

| Riesgo | Dónde mirar |
|---|---|
| Autorización rota a nivel de objeto | `OwnerService.findOwnerById(int, Principal)` |
| Exposición excesiva de datos | `PetRestController` devuelve la entidad; `RestfulVisitController` no |
| Secretos en el repositorio | `infra/auth-service/src/main/resources/application-rsa.properties` |
| Tokens de vida larga | `jwt.validity-seconds` |
| Errores que filtran información | `GlobalExceptionHandler.handleUnexpected` |
| Falta de límites | paginación obligatoria en `RestfulVisitSearchController` |

---

## Ejercicios

En [`ejercicios.md`](ejercicios.md).

Ampliación: [`guion-acl.md`](guion-acl.md), permisos por instancia con `spring-security-acl`.

## Errores frecuentes en clase

| Síntoma | Causa |
|---|---|
| Todo devuelve 403 al probar con curl | CSRF. En rutas de formulario hace falta el token; `/api/**` lo tiene desactivado. |
| "He iniciado sesión pero sigo sin poder entrar" | La autoridad del usuario no coincide con la del matcher. Comprobar `data.sql`. |
| El JWT se rechaza siempre | El secreto de quien firma no coincide con el de quien valida, o el reloj de la máquina va desajustado. |
| `IllegalArgumentException` al arrancar con HS512 | El secreto tiene menos de 512 bits. jjwt lo comprueba a propósito. |
