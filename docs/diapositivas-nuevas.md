# Diapositivas nuevas propuestas

Contenido listo para pasar al `.pptx` de IDEXA. Cubre huecos del temario que los guiones de
sesión necesitan, o sustituye diapositivas cuyo código ya no funciona (ver la
[fe de erratas](fe-de-erratas-transparencias.md)).

| # | Título | Dónde insertarla | Sesión |
|---|---|---|---|
| A | El cuerpo de un error: Problem Details (RFC 9457) | Después de la 55 | 2 |
| B | De la excepción al `ProblemDetail` en Spring Boot | Después de A | 2 |
| C | La cadena de seguridad en Spring Security 6 | Sustituye a la 79 y complementa la 80 | 5 |
| D | JWT en Spring Security 6: dos maneras | Sustituye a la 93 | 6 |
| E | Mockito en una diapositiva | Sustituye a las 110–112 (capturas sin texto) | 7 |

---

## A · El cuerpo de un error: Problem Details (RFC 9457)

**Título:** Errores con un formato estándar: `application/problem+json`

- Un código de estado dice **qué tipo** de error ha habido; el cuerpo tiene que decir **qué ha
  pasado** y **qué hacer**.
- La RFC 9457 (antes RFC 7807) define un cuerpo común para todas las APIs HTTP:

| Campo | Significado | Ejemplo |
|---|---|---|
| `type` | URI que identifica el tipo de problema (por defecto `about:blank`) | `https://api.clinica.es/errores/mascota-duplicada` |
| `title` | Resumen corto, igual cada vez que ocurre | `Recurso no encontrado` |
| `status` | El código HTTP, repetido en el cuerpo | `404` |
| `detail` | Explicación de **este** caso concreto | `No existe la mascota 99` |
| `instance` | URI de la petición que falló | `/api/v1/pets/99` |
| *(extensiones)* | Campos propios | `"errores": [{"campo": "name", ...}]` |

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{ "type": "about:blank", "title": "Recurso no encontrado", "status": 404,
  "detail": "No existe la mascota 99", "instance": "/api/v1/pets/99" }
```

- **Nunca** en el cuerpo: trazas, SQL, nombres de clases o de tablas. Se registran en el log
  con un identificador, y se devuelve solo el identificador.

---

## B · De la excepción al `ProblemDetail` en Spring Boot

**Título:** Una excepción, un estado, un cuerpo: `ProblemDetail` y `@RestControllerAdvice`

| Nivel | Cómo | Cuerpo que sale |
|---|---|---|
| 1. `@ResponseStatus` en la excepción (diap. 55) | `@ResponseStatus(NOT_FOUND) class ResourceNotFoundException` | Solo el estado. El cuerpo es el de `/error` de Spring Boot, no estándar |
| 2. `ResponseStatusException` / `ErrorResponseException` | `throw new ResponseStatusException(NOT_FOUND, "No existe…")` | `ProblemDetail` automático |
| 3. Manejador centralizado ✔ | `@RestControllerAdvice` + `@ExceptionHandler` que devuelve `ProblemDetail` | `ProblemDetail` con el título y el detalle que decidimos |

```java
@RestControllerAdvice(basePackages = "…web.api")
class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail noEncontrado(ResourceNotFoundException ex) {
    ProblemDetail p = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    p.setTitle("Recurso no encontrado");
    p.setDetail(ex.getMessage());
    return p;                       // Spring pone el 404 y el Content-Type
  }
}
```

- Las excepciones **de dominio** (`ResourceNotFoundException`, `BadRequestException`,
  `DuplicatedPetNameException`) se mantienen: el *advice* las **traduce**, y el servicio no sabe
  nada de HTTP.
- Traducción en el proyecto: no encontrado → 404 · validación (`MethodArgumentNotValidException`)
  → 400 con detalle por campo · sin permiso (`AccessDeniedException`) → 403 · excepciones de
  Spring que ya saben su código (`ErrorResponse`: 405, 415…) → su código · cualquier otra → 500
  con referencia, sin traza.
- Sin *advice* propio, una propiedad hace que las excepciones de Spring MVC respondan con
  `ProblemDetail`: `spring.mvc.problemdetails.enabled=true`.
- Código: `web/api/GlobalExceptionHandler.java`.

**Nota del ponente.** Enseñar el `curl -i` de un 404 antes y después: primero solo con
`@ResponseStatus` (cuerpo genérico de Spring Boot) y después con el *advice*. Avisar de que
`basePackages` evita que el *advice* convierta en JSON los errores de las vistas JSP.

---

## C · La cadena de seguridad en Spring Security 6

**Título:** Configurar la seguridad: un *bean*, no una herencia

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/resources/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
        .requestMatchers("/admin/**").hasAuthority("admin")
        .requestMatchers("/owners/**").hasAnyAuthority("owner", "admin")
        .anyRequest().denyAll())              // lo no permitido, denegado
      .formLogin(form -> form.defaultSuccessUrl("/welcome").permitAll())
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
      .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
```

| Spring Security 5 (diap. 73, 75, 79) | Spring Security 6 |
|---|---|
| `extends WebSecurityConfigurerAdapter` | `@Bean SecurityFilterChain` |
| `authorizeRequests()` | `authorizeHttpRequests(...)` |
| `antMatchers(...)` | `requestMatchers(...)` |
| `.and()` encadenado | una *lambda* por apartado |
| `@EnableGlobalMethodSecurity` | `@EnableMethodSecurity` |

- **El orden importa**: gana la primera regla que encaja.
- **Denegación por defecto**: `anyRequest().denyAll()` al final.

---

## D · JWT en Spring Security 6: dos maneras

**Título:** Validar un JWT: filtro propio o servidor de recursos

| | Filtro propio (diap. 92) | Servidor de recursos |
|---|---|---|
| Dependencia | `jjwt-api`, `-impl`, `-jackson` | `spring-boot-starter-oauth2-resource-server` |
| Código | `OncePerRequestFilter` (~80 líneas) | configuración (~20 líneas) |
| Comprueba | lo que programes | firma, `exp`, `nbf`, emisor, algoritmo |
| Respuesta 401 | la escribes tú | estándar (`WWW-Authenticate: Bearer`) |

```java
@Bean @Order(1)
SecurityFilterChain api(HttpSecurity http) throws Exception {
  return http
    .securityMatcher("/api/**")
    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
    .csrf(csrf -> csrf.disable())
    .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
    .build();
}
```

- Dos cadenas: `/api/**` sin estado con JWT; el resto, con sesión y formulario.
- Un JWT está **firmado, no cifrado**.

---

## E · Mockito en una diapositiva

**Título:** Dobles de prueba con Mockito

```java
@ExtendWith(MockitoExtension.class)
class PetServiceTests {
  @Mock PetRepository repo;              // doble
  @InjectMocks PetService service;       // sujeto, inyectado por constructor

  @Test void ejemplo() {
    when(repo.findById(7)).thenReturn(leo);                        // stub
    when(repo.findById(8)).thenThrow(new DataRetrievalFailureException("caida")); // saboteador
    doThrow(new DataIntegrityViolationException("x")).when(repo).save(any());     // métodos void

    service.deletePet(7);

    verify(repo, times(1)).deleteById(7);                          // mock
    verify(repo, never()).save(any());
    verifyNoMoreInteractions(repo);
  }
}
```

- `any()`, `eq(...)`, `argThat(...)`: comparadores de argumentos.
- `ArgumentCaptor`: capturar lo que se pasó para comprobarlo después.
- `spy(objetoReal)` + `doReturn(...)`: objeto real con un método sobrescrito.
- En pruebas con Spring: `@MockitoBean` (antes `@MockBean`).
