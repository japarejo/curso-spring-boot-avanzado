# Fe de erratas de las transparencias

Revisión del `.pptx` «Spring Boot avanzado» (IDEXA, 177 diapositivas) frente a lo que enseña el
curso: **Java 21, Spring Boot 3.5.16, Spring Security 6, Hibernate 6, JUnit 5**.

Cada fila dice qué pone la diapositiva, qué es lo correcto hoy y dónde se ve en el repositorio.
Las marcadas con ⚠ son **errores** (el código no funciona o la afirmación es falsa); el resto
son **desactualizaciones** (funcionaba en una versión anterior).

Los guiones de sesión avisan de cada errata en el momento en que aparece la diapositiva.

## Módulo 1

| Diap. | Qué pone | Qué es lo correcto | Dónde se ve |
|---|---|---|---|
| 29 | `@RequestMapping(value = "/person", method = POST) public Person addperson(Person person)` | ⚠ Falta `@RequestBody`: los datos JSON llegan vacíos. Además, `@PostMapping`, y 201 + `Location` | `practicas/tareas/.../TareaController` |
| 29 | `.findFirst().get()` | ⚠ Con un id inexistente lanza `NoSuchElementException` → 500. Debe ser 404 | ídem |
| 30 | `@SpringApplicationConfiguration`, `@WebAppConfiguration`, `@Before`, `MockMvcBuilders.webAppContextSetup` | JUnit 5 + `@WebMvcTest` (o `@SpringBootTest` + `@AutoConfigureMockMvc`) con `MockMvc` inyectado. `@SpringApplicationConfiguration` desapareció en Spring Boot 1.4 | `TareaControllerTests` |
| 30 | `MediaType` con `Charset.forName("utf8")` | `MediaType.APPLICATION_JSON` (el *charset* ya no se añade) | ídem |
| 31 | `@RunWith(SpringJUnit4ClassRunner.class)`, `personRepository.findOne(id)` | JUnit 5 + `@DataJpaTest`; `findOne` no existe desde Spring Data 2: es `findById`, que devuelve `Optional` | `TareaRepositoryTests` |
| 31 | `PersonRepository PersonRepository` (variable con mayúscula) y luego `personRepository` | ⚠ No compila | — |

## Módulo 2

| Diap. | Qué pone | Qué es lo correcto | Dónde se ve |
|---|---|---|---|
| 54 | `@Autowired private IFooService service` | Inyección por constructor | M1, ejercicio 3 |
| 56 | `@CookieParam`, `@HeadersParam` | ⚠ Son de JAX-RS (y la segunda se llama `@HeaderParam`). En Spring: `@CookieValue` y `@RequestHeader` | M2, ejercicio 9 |
| 57 | `@ResponseStatus(OK)` en un `DELETE` que no devuelve nada | Lo canónico es 204 No Content | M2, ejercicio 10 |
| 57 | `new ResponseEntity<>(null, headers, OK)` (falta `;`) | ⚠ No compila. Mejor `ResponseEntity.noContent().header(...).build()` | ídem |
| 58 | `"birthDate": "2010/09/07"`, `owner: 1` | Es el formato que usa el proyecto (`@JsonFormat`), pero no es ISO-8601; conviene decirlo | `model/Pet` |
| 55, 57 | No hay ninguna diapositiva sobre el **cuerpo** del error | Falta `ProblemDetail` (RFC 9457) | [`diapositivas-nuevas.md`](diapositivas-nuevas.md) |
| 65 | Dependencia `org.springframework.data:spring-data-jpa` para Spring Data REST | ⚠ Es `org.springframework.boot:spring-boot-starter-data-rest` | M2, ejercicio 13 |
| 65 | Con la estrategia `DEFAULT`, se exportan todos los repositorios públicos | Correcto, y peligroso: en este proyecto incluye `UserRepository`. Recomendar `annotated` | ídem |
| 67 | Diapositiva sin contenido de texto (proyección por defecto) | `@RepositoryRestResource(excerptProjection = ...)` | ídem |
| 68 | `springdoc-openapi-core` y `springdoc-openapi-ui` 1.1.44 | Spring Boot 3: `springdoc-openapi-starter-webmvc-ui` 2.x (una sola dependencia) | `pom.xml` raíz |
| 69 | `@GET`, `@PUT`, `@Consumes`, `Response` | Son de JAX-RS. En Spring MVC, `@GetMapping`, `@PutMapping`, `consumes = ...`, `ResponseEntity` | `PetRestController` |

## Módulo 3

| Diap. | Qué pone | Qué es lo correcto | Dónde se ve |
|---|---|---|---|
| 73, 75, 79, 93, 94 | `extends WebSecurityConfigurerAdapter`, `configure(HttpSecurity)`, `authorizeRequests()`, `antMatchers()`, `.and()` | ⚠ No existen en Spring Security 6. Bean `SecurityFilterChain`, `authorizeHttpRequests`, `requestMatchers`, DSL con lambdas | `configuration/SecurityConfiguration` |
| 74 | `NoOpPasswordEncoder` como ejemplo | Solo para contrastar. Lo correcto, `PasswordEncoderFactories.createDelegatingPasswordEncoder()` | M3, ejercicio 9 |
| 82 | «Genera una cadena de texto encriptada» | ⚠ Un JWT (JWS) **no** está cifrado: va en Base64URL y **firmado**. Cifrado sería JWE | M3, ejercicio 4 |
| 82–83 | «La tercera parte encripta las otras dos usando SHA-256» | ⚠ Es una **firma** HMAC-SHA256 (o RSA, ECDSA), no un cifrado | ídem |
| 85 | `io.jsonwebtoken:jjwt:0.9.1` | jjwt 0.12 en tres artefactos (`jjwt-api`, `-impl`, `-jackson`). La API cambió (`Jwts.parser().verifyWith(...)`) | `infra/auth-service` |
| 88 | `.signWith(SignatureAlgorithm.HS512, secret)`, `setSubject`, `setIssuedAt` | jjwt 0.12: `.signWith(clave, Jwts.SIG.HS512)`, `.subject()`, `.issuedAt()` | `AbstractJwtTokenService` |
| 90 | Comprueba la caducidad a mano | jjwt la comprueba al verificar la firma | ídem |
| 91 | `@CrossOrigin` sin restricciones y `throws Exception` genérico | CORS configurado en un único sitio; credenciales incorrectas → 401 con `ProblemDetail` | `infra/auth-service/.../AuthController` |
| 92 | `@Component` en el filtro JWT | ⚠ Se registra **dos veces** (como filtro de servlet y en la cadena). Crear con `new` | M3, ejercicio 13 |
| 92 | `loadUserByUsername` en cada petición | Innecesario con JWT: las autoridades viajan firmadas en el token | ídem |
| 92 | `System.out.println` para los errores | Registrar con el *logger* a nivel `debug` | ídem |
| 94 | `spring-security-oauth2-client` + `-jose` | `spring-boot-starter-oauth2-client` | `extras/oauth2-login-service` |

## Módulo 4

| Diap. | Qué pone | Qué es lo correcto | Dónde se ve |
|---|---|---|---|
| 110–112 | Capturas sin texto de la sintaxis de Mockito | Ver la solución comentada del ejercicio 13 | M4, ejercicio 13 |
| 119–120 | `@MockBean` | Obsoleto desde Spring Boot 3.4: `@MockitoBean` | `PetRestControllerMockMvcTests` |
| 124 | REST Assured 4.3.0 | 5.5 (la versión la fija el `pom.xml` raíz) | `pom.xml` raíz |
| 125 | `@SpringBootTest(webEnvironment = DEFINED_PORT)` + URL completa | `RANDOM_PORT` + `@LocalServerPort` + `RestAssured.port` | M4, ejercicio 16 |
| 128 | `https://swapi.dev/api/people/1/` | El proyecto usa `swapi.info`. Las APIs públicas cambian o se caen: comprobar la URL antes de clase (la prueba se salta sola si no responde) | `ExternalApiRestAssuredTests` |
| 129 | `.header("Authentication", "Bearer ...")` | ⚠ La cabecera es `Authorization` | M4, ejercicio 16 |
| 132 | `MockMvcBuilders.webAppContextSetup(...).apply(springSecurity())` + `RANDOM_PORT` + `@DirtiesContext` | `@SpringBootTest` + `@AutoConfigureMockMvc` (ya aplica la seguridad); sin puerto ni `@DirtiesContext` | M4, ejercicio 15 |
| 132 | Textos en inglés en una diapositiva en castellano | Traducir | — |

## Módulo 5

| Diap. | Qué pone | Qué es lo correcto | Dónde se ve |
|---|---|---|---|
| 136 | Tabla de compatibilidad hasta Spring Boot 3.0 | Spring Boot Admin 3.5.x para Spring Boot 3.5 | `pom.xml` raíz |
| 137 | `@Configuration @EnableAutoConfiguration @EnableAdminServer` | `@SpringBootApplication @EnableAdminServer` | `infra/admin-server` |
| 138 | Seguridad desactivada entera (`anyRequest().permitAll()`, `csrf().disable()`) para que Admin acceda | ⚠ Abre toda la aplicación. Proteger solo Actuator con un usuario técnico | M5, ejercicio 9 |
| 138 | `.csrf().disable()` | Spring Security 6.1+: `.csrf(csrf -> csrf.disable())` | ídem |
| 140–141 | La 141 repite la 140 | La 141 debería mostrar el cliente | — |
| 143 | Cliente con `spring.cloud.config.uri` sin más | Desde Spring Boot 2.4 hace falta `spring.config.import=optional:configserver:` | `infra/bills-service` |
| 144 | `<finalName>${artifactId}</finalName>` | `${project.artifactId}` (la forma sin `project.` está obsoleta) | `apps/petclinic-api/pom.xml` |
| 145 | `FROM openjdk:8-jdk-alpine` | ⚠ Java 8 no ejecuta Spring Boot 3, y las imágenes `openjdk` están retiradas. `eclipse-temurin:21-jre`, usuario sin privilegios, capas | M5, ejercicio 12 |

## Módulo 6

| Diap. | Qué pone | Qué es lo correcto | Dónde se ve |
|---|---|---|---|
| 154 | `java.util.Date`, `int id` | `LocalDate`; `Integer`/`Long` para distinguir entidades nuevas | `practicas/tareas/.../Tarea` |
| 155 | Campo `pedidoId` con `@Column("PEDIDO_ID")` **y** relación con `@JoinColumn("Pedido_ID")` | ⚠ Dos mapeos a la misma columna: Hibernate no arranca | `practicas/mapeos-jpa/.../Factura` |
| 156 | `@OneToMany(targetEntity = ...)` sin `mappedBy` + `@ManyToOne` al otro lado | ⚠ Son dos relaciones distintas (columna **y** tabla de unión) | `.../Departamento` |
| 156 | `private List employeelist` sin tipo | `List<Empleado>` | ídem |
| 158 | Subclases sin `@Entity` | ⚠ Sin ella no se persisten sus campos | `.../herencia` |
| 165 | `mysql:mysql-connector-java` | `com.mysql:mysql-connector-j` | `apps/petclinic-api/pom.xml` |
| 165 | «Poner `@EnableJpaRepositories`» y `driver-class-name` | Innecesarios en Spring Boot | M6, ejercicio 9 |
| 167 | La colisión de nombres de tablas de unión | Según JPA, sí; **con Spring Boot no ocurre** (`SpringImplicitNamingStrategy` usa el nombre del campo). Sigue siendo buena práctica `@JoinTable` | `.../jointable/Articulo` |
| 168 | `joinColumns = "aurhor_id"`, `inverseJoinColumns = "authored_paper_id"` | ⚠ Invertidos (y una errata en *author*): `joinColumns` apunta al propietario (el artículo) | ídem |
| 173 | Nota del ponente: «este ejemplo está en `ExampleServiceTests`» | No existe en este repositorio. Usar el guion de N+1 con el perfil `nplus1` | `guion-consultas-n-mas-1.md` |
| 173 | `p.getVisits().getDescription()` | ⚠ `getVisits()` devuelve una lista | — |
| 174 | «Resuelve el problema de las N+1 queries» | Solo para la colección traída; con `Pet.visits` `EAGER` sigue habiendo N+1 en el siguiente nivel | M6, ejercicio 12 |

## Generales

- Las diapositivas 14 y 86 conservan el texto de la plantilla («Escribir texto o inserción de
  imágenes o vídeos. DUPLICAR TANTAS VECES COMO SEA NECESARIO»).
- Erratas tipográficas: «Aprente» (3), «Intializr» (4, 14), «Principos» (36–53),
  «aplicacione» (5), «DataSourse» (151), «DataSoruceTransactionManager» y «platnilla» (153),
  «Departmento» (156), «representacies» (66), «encriptación» por «cifrado» (en todo el módulo 3).
