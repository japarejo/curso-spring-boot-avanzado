# Módulo 4. Pruebas de aplicaciones Spring Boot

Guion docente. Duración estimada: **dos sesiones de 120 minutos**, más una opcional de
Testcontainers.

Este módulo es el que llega más rodado: el material detallado ya existe y está probado en
aula. Este fichero es el índice y la orientación; el detalle está en los tres guiones.

## Documentos

| Documento | Qué contiene |
|---|---|
| [`guion-clases.md`](guion-clases.md) | Las dos sesiones cronometradas, bloque a bloque |
| [`ejercicios.md`](ejercicios.md) | 12 ejercicios en tres bloques, con resultado esperado |
| [`guion-testcontainers.md`](guion-testcontainers.md) | Sesión opcional: pruebas contra MySQL real |

## Objetivos

- Escribir pruebas unitarias que cumplan FIRST.
- Usar dobles de prueba con Mockito y saber cuándo **no** usarlos.
- Elegir la rebanada adecuada: `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest`.
- Probar la API con MockMvc y con RestAssured.
- Probar la seguridad.
- Probar contra la base de datos real con Testcontainers.
- Leer un informe de cobertura sin convertirlo en un objetivo.

## La pirámide, sobre un mismo caso de uso

El proyecto tiene los cinco niveles aplicados al **mismo** caso de uso, `Owner`. Recorrerlos
en orden es la mejor manera de explicar qué aporta cada uno y qué cuesta:

| Nivel | Fichero | Qué levanta | Coste |
|---|---|---|---|
| Unitaria aislada | `testingexamples/spring/OwnerServiceIsolatedMockTests.java` | nada de Spring | ms |
| Rebanada de persistencia | `testingexamples/spring/OwnerRepositoryJpaSliceTests.java` | JPA + H2 | ~1 s |
| Rebanada web | `testingexamples/spring/OwnerControllerMvcSliceTests.java` | MVC, servicio simulado | ~2 s |
| Integración con BD real | `testingexamples/spring/OwnerRepositoryMySqlContainerIT.java` | JPA + MySQL en contenedor | ~20 s |
| Integración completa | `testingexamples/spring/RestfulVisitSearchMySqlContainerIT.java` | todo | ~30 s |

```bash
./mvnw test      # los tres primeros niveles
./mvnw verify    # añade los dos últimos (requiere Docker)
```

## Piezas destacadas

- **`testingexamples/PetAppointmentAdvisor.java`** y su prueba. Clase diseñada para enseñar
  cobertura: ramas anidadas, `switch` de expresión y un `Clock` inyectado para poder probar
  "es fin de semana" sin esperar al sábado. La prueba usa `@ParameterizedTest` con
  `@CsvFileSource`, `@CsvSource`, `@ValueSource` y `@NullAndEmptySource`.

- **`DataJpaBeforeEachBeforeAllTransactionExampleTests`**. Demuestra con aserciones que lo
  insertado en `@BeforeAll` persiste y lo insertado en `@BeforeEach` se revierte. Es la
  forma más clara de explicar la semántica transaccional de las pruebas.

- **`DirtiesContextExampleTests`**. Contamina un singleton y fuerza la reconstrucción del
  contexto. Sirve para explicar por qué una batería de pruebas tarda minutos.

- **`service/TransactionalExamplesVerificationTests`**. Verifica `rollbackFor`, `readOnly`,
  `isolation`, `timeout` y `REQUIRES_NEW` capturando los registros de Hibernate. Es
  material compartido con el módulo 6.

- **`apiclient/BillApiClientTest`**. Prueba un cliente HTTP levantando un servidor real en
  un puerto libre y apuntando la configuración con `@DynamicPropertySource`. Sin WireMock.

## Dos cosas que conviene enseñar aunque incomoden

### Una prueba en verde no garantiza que la vista exista

`VisitControllerTests.testShowVisits` comprueba `view().name("visitList")` y pasa. En el
proyecto anterior **el fichero `visitList.jsp` no existía**, así que ese endpoint devolvía
500 en ejecución con la prueba en verde.

> `@WebMvcTest` comprueba el *nombre* de la vista; no la renderiza. Una rebanada web no
> sustituye a una prueba que atraviese la capa de vista.

### Una rebanada debe cargar solo lo suyo

`@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))` incluye **todos** los
beans `@Service`. Si una pasarela HTTP estuviera anotada como `@Service`, cada prueba de
persistencia intentaría levantar clientes HTTP. Por eso `apiclients/BillsGateway` es
`@Component` y no `@Service`: está comentado en la propia clase.

## Cobertura

```bash
./mvnw -pl apps/petclinic-api test
# apps/petclinic-api/target/site/jacoco/index.html
```

> **Mensaje clave.** La cobertura mide qué código se ha ejecutado, no si está bien probado.
> Un 90 % con aserciones vacías no dice nada. Es un indicador de riesgo, no un objetivo.

## Errores frecuentes en clase

| Síntoma | Causa |
|---|---|
| Los `*IT` no se ejecutan con `./mvnw test` | Es correcto: los ejecuta Failsafe en `verify`. |
| Los `*IT` fallan por Docker | `MySqlTestContainerSupport` usa `disabledWithoutDocker = true`: sin Docker se saltan. |
| La batería tarda muchísimo | Cada `@MockitoBean`, cada propiedad distinta y cada `@DirtiesContext` crean un contexto nuevo. |
| `ExternalApiRestAssuredTests` falla | Sale a Internet. Excluir con `-Dgroups='!external-api'`. |
| `[ERROR] Surefire is going to kill self fork JVM` al final de `verify` | **No es un fallo.** Aparece tras los `*IT` y el build termina en `BUILD SUCCESS`. Testcontainers y el pool de conexiones dejan hilos no demonio vivos, así que la JVM hija no muere en los 30 s que Surefire le da tras el `System.exit(0)` y la mata él. Conviene avisar en clase: es una línea roja que no significa nada, y si no se explica, alguien la da por rota. |
