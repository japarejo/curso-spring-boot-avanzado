# Guion docente: testing, Spring Test y cobertura

Duracion total: 2 clases de 2 horas.

Objetivo: que el alumno entienda que un test no solo comprueba codigo, tambien documenta comportamiento, delimita el contexto que carga Spring y produce evidencia en los informes.

## Preparacion previa

Desde `apps/petclinic-api` ejecuta:

```powershell
.\mvnw.cmd test
.\mvnw.cmd surefire-report:report -DskipTests
```

Ten localizados:

- `src/main/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisor.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerRepositoryJpaSliceTests.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerControllerMvcSliceTests.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/DirtiesContextExampleTests.java`
- `target/reports/surefire.html`
- `target/site/jacoco/index.html`

## Clase 1: JUnit 5, informes y cobertura

### 0:00-0:10 - Apertura

Mensaje clave:

> Un test bueno tiene tres lecturas: para la maquina, para el informe y para el siguiente desarrollador.

Actividad:

- Mostrar `PetAppointmentAdvisor`.
- Preguntar que ramas de negocio se intuyen sin mirar los tests.
- Ejecutar solo sus tests:

```powershell
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
```

### 0:10-0:30 - Anatomia de un test JUnit 5

Abrir `PetAppointmentAdvisorTests`.

Cubrir:

- `@Test`
- assertions con AssertJ
- Arrange, Act, Assert
- nombres de metodos vs `@DisplayName`
- excepciones con `assertThatThrownBy`

Demo:

- Cambiar temporalmente una expectativa para provocar fallo.
- Abrir el `.txt` o XML en `target/surefire-reports`.
- Volver a dejar el test correcto.

Mensaje clave:

> El informe debe explicar que comportamiento se rompio, no solo que metodo fallo.

### 0:30-0:50 - Ciclo de vida

Cubrir:

- `@BeforeAll`
- `@BeforeEach`
- `@AfterEach`
- `@AfterAll`
- `TestInfo`

Demo:

- Mostrar la diferencia entre estado de clase y estado por test.
- Explicar por que `@BeforeEach` tambien se ejecuta en cada invocacion parametrizada.

Pregunta:

- Que preparacion deberia ir en `@BeforeEach` y cual en `@BeforeAll`?

Respuesta esperada:

- `@BeforeEach`: datos mutables, instancias frescas, mocks reseteables.
- `@BeforeAll`: recursos caros e inmutables, configuracion compartida.

### 0:50-1:15 - Tests parametrizados

Cubrir:

- `@ParameterizedTest`
- `@CsvSource`
- `@ValueSource`
- `@NullAndEmptySource`
- atributo `name`

Actividad guiada:

- Pedir que anadan un caso a `shouldClassifyDifferentAppointmentScenarios`.
- Ejecutar solo la clase.
- Mirar como aparece cada invocacion en el informe.

Mensaje clave:

> Un parametrizado no es para esconder complejidad; es para hacer visible una tabla de comportamiento.

### 1:15-1:35 - Deshabilitacion responsable

Cubrir:

- `@Disabled`
- diferencia entre skipped, failed y passed
- por que un test deshabilitado debe tener motivo

Demo:

- Abrir `target/reports/surefire.html`.
- Localizar tests skipped.
- Comparar skipped con fallo real.

Mensaje clave:

> Un test deshabilitado sin explicacion es deuda opaca; con explicacion es una decision pendiente.

### 1:35-1:55 - Cobertura con JaCoCo

Abrir `target/site/jacoco/index.html`.

Cubrir:

- instrucciones
- ramas
- metodos
- clases
- lineas verdes, rojas y amarillas

Demo:

- Entrar en `PetAppointmentAdvisor`.
- Identificar ramas no cubiertas.
- Relacionar tests parametrizados con cobertura de ramas.

Mensaje clave:

> Cobertura alta no garantiza buenos tests; cobertura baja si garantiza zonas sin evidencia automatica.

### 1:55-2:00 - Cierre y tarea

Asignar:

- Ejercicios 1, 2, 4 y 10 de `ejercicios-testing.md`.

Punto de control:

- Cada alumno debe poder abrir Surefire y JaCoCo y explicar un dato de cada informe.

## Clase 2: Spring Test, slices y contexto

### 0:00-0:10 - Recapitulacion

Preguntas rapidas:

- Que diferencia hay entre fallo y skipped?
- Que mide JaCoCo?
- Por que parametrizar?
- Que prepararias en `@BeforeEach`?

### 0:10-0:35 - Mapa mental de tests con Spring

Dibujar o explicar esta escalera:

| Tipo | Anotacion | Coste | Uso |
| --- | --- | --- | --- |
| Unitario puro | JUnit/Mockito | Bajo | Reglas de negocio aisladas |
| JPA slice | `@DataJpaTest` | Medio | Repositorios, queries, mapeos |
| MVC slice | `@WebMvcTest` | Medio | Controladores, binding, validacion MVC |
| Contexto configurado | `@SpringJUnitConfig` | Variable | Casos pequenos con beans concretos |
| App completa | `@SpringBootTest` | Alto | Integracion amplia |

Mensaje clave:

> La pregunta no es "como arranco Spring", sino "cuanto Spring necesito para probar esto".

### 0:35-1:00 - Repositorios con `@DataJpaTest`

Abrir `OwnerRepositoryJpaSliceTests`.

Cubrir:

- `@DataJpaTest`
- repositorios reales
- entidades reales
- base H2 de test
- carga de SQL
- transaccion y rollback por test
- `TestEntityManager`

Demo:

```powershell
.\mvnw.cmd "-Dtest=OwnerRepositoryJpaSliceTests" test
```

Actividad:

- Resolver en clase el Ejercicio 5.

Preguntas:

- Por que este test no necesita `OwnerService`?
- Que bug detectaria este test que un unitario puro no detecta?

Respuestas esperadas:

- No necesita servicio porque el objetivo es el repositorio/query/mapeo.
- Detecta errores JPQL, relaciones JPA, columnas, carga de datos y flush.

### 1:00-1:25 - MVC con `@WebMvcTest`

Abrir `OwnerControllerMvcSliceTests`.

Cubrir:

- `@WebMvcTest`
- `MockMvc`
- `@MockBean`
- `@WithMockUser`
- expectativas de estado, vista y modelo
- separacion controlador/servicio/repositorio

Demo:

```powershell
.\mvnw.cmd "-Dtest=OwnerControllerMvcSliceTests" test
```

Actividad:

- Resolver el Ejercicio 7.

Mensaje clave:

> Un test MVC slice debe fallar si cambia el contrato web, no si cambia una query SQL.

Nota para Spring Boot 3.4:

- `@MockBean` aparece como deprecado, pero sigue siendo reconocible y util para explicar el patron. Puedes mencionar que en proyectos nuevos conviene revisar la alternativa vigente del stack usado.

### 1:25-1:45 - `@DirtiesContext`

Abrir `DirtiesContextExampleTests`.

Cubrir:

- cache de contextos en Spring Test
- singleton mutable
- `@DirtiesContext(methodMode = AFTER_METHOD)`
- coste de reconstruir contexto

Demo:

1. Ejecutar la clase tal cual.
2. Quitar temporalmente `@DirtiesContext`.
3. Ejecutar de nuevo para observar contaminacion.
4. Revertir el cambio.

Comando:

```powershell
.\mvnw.cmd "-Dtest=DirtiesContextExampleTests" test
```

Mensaje clave:

> `@DirtiesContext` es una herramienta de aislamiento, pero tambien una senal de coste. Usala cuando el test cambia el contexto o beans singleton de forma que no puedas limpiar localmente.

### 1:45-1:55 - Comparacion y decision tecnica

Actividad:

- Resolver en grupo la tabla del Ejercicio 11.
- Presentar tres escenarios y pedir que elijan tipo de test:

Escenario 1:

- Una regla de triaje veterinario con fechas y sintomas.
- Respuesta esperada: unitario puro.

Escenario 2:

- Una query con `left join fetch` que debe traer mascotas de un propietario.
- Respuesta esperada: `@DataJpaTest`.

Escenario 3:

- Un formulario debe devolver la vista correcta cuando faltan campos.
- Respuesta esperada: `@WebMvcTest`.

### 1:55-2:00 - Cierre

Checklist final para alumnos:

- Se ejecutar tests concretos con `-Dtest`.
- Se leer Surefire.
- Se leer JaCoCo sin confundir cobertura con calidad.
- Se cuando usar unitario, JPA slice, MVC slice y contexto completo.
- Se explicar por que `@DirtiesContext` puede ralentizar una suite.

## Evaluacion sugerida

Entrega individual o por parejas:

- 4 tests unitarios nuevos sobre `PetAppointmentAdvisor`.
- 1 test nuevo con `@DataJpaTest`.
- 1 test nuevo con `@WebMvcTest`.
- Explicacion de una mejora de cobertura en JaCoCo.
- Explicacion de un test skipped en Surefire.

Rubrica rapida:

| Criterio | Peso |
| --- | --- |
| Tests pasan y son deterministas | 30% |
| Nombres e informes son legibles | 20% |
| Uso correcto de anotaciones JUnit | 20% |
| Uso correcto de slices Spring | 20% |
| Interpretacion de cobertura | 10% |
