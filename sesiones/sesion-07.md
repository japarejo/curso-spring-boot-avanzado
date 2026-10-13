# Sesión 7 · Pruebas unitarias: JUnit 5, cobertura y dobles

**Módulo 4** · Diapositivas 95–112 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Una prueba no solo comprueba código: **documenta** un
> comportamiento. Las buenas son rápidas, independientes, repetibles, se autoverifican y se
> escriben a tiempo (FIRST). Y para probar una pieza aislada, sus colaboradores se sustituyen
> por dobles.

## Objetivos

- Escribir pruebas JUnit 5 con estructura *Arrange–Act–Assert* y ciclo de vida.
- Parametrizar pruebas y probar los casos de error con sus mensajes.
- Leer los informes de Surefire y JaCoCo, y distinguir cobertura de líneas y de ramas.
- Organizar una batería con `@Nested`, `@DisplayName`, `assertAll` y etiquetas.
- Usar los cinco tipos de doble y las verificaciones de Mockito.

## Antes de clase

- [ ] `./mvnw -pl apps/petclinic-api test` en verde y los informes generados:
      `apps/petclinic-api/target/reports/surefire.html` y `target/site/jacoco/index.html`.
- [ ] Leer el [guion de clases del módulo 4](../modulos/04-pruebas/guion-clases.md), «Clase 1»:
      esta sesión lo sigue en la primera mitad.
- [ ] Recordar que todos los comandos se lanzan **desde la raíz** con `-pl apps/petclinic-api`.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Por qué probar; la pirámide; FIRST | 🗣 | 96–98, 104 | `modulos/04-pruebas/README.md` |
| 0:10 | Anatomía de una prueba: AAA, *fixture*, sujeto | ⌨ + 🧑‍💻 | 99–100, 105 | Ejercicio 1 |
| 0:30 | Ciclo de vida y ramas | 🧑‍💻 20' | 101 | Ejercicio 2 |
| 0:50 | Pruebas parametrizadas | 🧑‍💻 20' | — | Ejercicio 3 |
| 1:10 | Excepciones y pruebas deshabilitadas | 🧑‍💻 15' | 103 | Ejercicios 4 y 5 |
| 1:25 | **Descanso** | | | |
| 1:35 | Cobertura con JaCoCo | ⌨ | — | guion de clases |
| 1:50 | **Pruebas que se leen como una especificación** | 🧑‍💻 25' | 102 | Ejercicio 14 |
| 2:15 | Dobles de prueba: tipos y Mockito | 🗣 ⌨ | 106–112 | `OwnerServiceIsolatedMockTests` |
| 2:25 | **Dobles con Mockito** | 🧑‍💻 30' | 110–112, 116–118 | Ejercicio 13 |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Por qué probar

📊 Diapositivas 96–98, 104.

- La verificación es necesaria (96); la mayoría de las pruebas deben ser unitarias (97).
- FIRST (98): pon un ejemplo de cada fallo. *Repeatable*: «una prueba que depende del día de
  hoy falla los sábados». Es justo lo que resuelve el `Clock` de `PetAppointmentAdvisor`.
- La pirámide (104): unitarias abajo, muchas y rápidas; de extremo a extremo arriba, pocas.

### 0:10 – 0:30 · Anatomía de una prueba

📊 Diapositivas 99–100, 105.

⌨ Abre `PetAppointmentAdvisorTests` y responde con el aula las preguntas de la diapositiva 100:
¿quién es el sujeto bajo prueba? ¿cuál es el *fixture*? Marca *Arrange*, *Act* y *Assert* en una
prueba.

🧑‍💻 **Ejercicio 1** (15 min): ejecutar las pruebas y localizar los informes.

```bash
./mvnw -pl apps/petclinic-api test "-Dtest=PetAppointmentAdvisorTests"
./mvnw -pl apps/petclinic-api surefire-report:report -DskipTests
```

### 0:30 – 0:50 · Ciclo de vida y ramas

📊 Diapositiva 101.
🧑‍💻 **Ejercicio 2** (20 min). Antes, ⌨ en 3 minutos: `@BeforeAll`, `@BeforeEach`,
`@AfterEach`, `@AfterAll`, y por qué `@BeforeAll` es `static`.

### 0:50 – 1:10 · Pruebas parametrizadas

🧑‍💻 **Ejercicio 3** (20 min): edades frontera con `@CsvSource` o `@CsvFileSource`.

🗣 **Mensaje clave.** «Los errores viven en las fronteras: 11 y 12, no 5 y 30.»

### 1:10 – 1:25 · Excepciones y pruebas deshabilitadas

📊 Diapositiva 103.
🧑‍💻 **Ejercicios 4 y 5** (15 min): `assertThrows` comprobando también el mensaje, y
`@Disabled` con motivo.

### 1:25 – 1:35 · Descanso

### 1:35 – 1:50 · Cobertura con JaCoCo

⌨ Abre `apps/petclinic-api/target/site/jacoco/index.html`. Navega hasta
`PetAppointmentAdvisor` y enseña una línea amarilla: cubierta, pero con una rama sin probar.

🗣 **Mensaje clave.** «La cobertura dice qué **no** está probado. No dice que lo cubierto esté
bien probado: una prueba sin aserciones cubre líneas y no comprueba nada.»

### 1:50 – 2:15 · Pruebas que se leen como una especificación

📊 Diapositiva 102.
🧑‍💻 **Ejercicio 14** (25 min): `@Nested`, `@DisplayName`, `Clock.fixed` por rama,
`assertAll`, `@EnabledOnOs`, `@Tag("critico")`.

Al terminar, ⌨ abre el informe de Surefire: tiene que leerse como la tabla de reglas de
`PetAppointmentAdvisor`. Y lanza solo las críticas:

```bash
./mvnw -pl apps/petclinic-api test "-Dgroups=critico"
```

### 2:15 – 2:25 · Dobles de prueba

📊 Diapositivas 106–112.

🗣 Los cinco tipos (108) con un ejemplo de la clínica:

| Tipo | Qué hace | Ejemplo |
|---|---|---|
| *Dummy* | Se pasa, no se usa | un `Principal` que el método ignora |
| *Stub* | Devuelve respuestas preparadas | repositorio que devuelve a Leo |
| *Spy* | Objeto real que además registra las llamadas | una lista real vigilada |
| *Mock* | Se le verifica cómo se le llamó | `verify(repo).deleteById(7)` |
| *Fake* | Implementación de verdad, pero simplificada | un repositorio en memoria, H2 |

⌨ Recuerda `OwnerServiceIsolatedMockTests` y su `ReflectionTestUtils` (sesión 1). Con
`PetService`, que recibe todo por constructor, no hace falta.

> **Errata de las diapositivas 110–112.** Son capturas sin texto. La sintaxis actual está en
> la solución del ejercicio 13, comentada línea a línea.

### 2:25 – 2:55 · Dobles con Mockito

🧑‍💻 **Ejercicio 13** (30 min): las pruebas 1 a 6. La 7 (regla de negocio) y la 8 (*spy*),
para casa.

Puntos para recorrer el aula:

- `MockitoExtension` es **estricta**: un *stub* que no se usa hace fallar la prueba.
- `when(repo.save(x))` no compila si `save` es `void`: para eso está `doThrow(...).when(repo)`.

🗣 **Mensaje clave.** «Un *stub* responde; un *mock* se comprueba. Si verificas todo lo que
haces con un doble, la prueba se rompe con cada refactorización: verifica solo lo que importa
al caso de uso.»

### 2:55 – 3:00 · Cierre y tarea

- **Tarea:** ejercicio 13 completo (pruebas 7 y 8) y la pregunta de si un `Owner` real es un
  doble.
- Para la sesión 8: tener Postman instalado y, si se puede, Node.js (para Newman) y Docker.

---

## Si vas con retraso

- Fundir los ejercicios 4 y 5 en una demo de 5 minutos.
- En el ejercicio 14, quedarse con `@Nested`, `@DisplayName` y `Clock.fixed`.

## Si te sobra tiempo

- Ejercicio 11 (TDD con una regla nueva) empezado en clase.
- Mutación: qué es *PIT* y por qué detecta pruebas que cubren pero no comprueban.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `.\mvnw.cmd` no se reconoce | La terminal está dentro de `apps\petclinic-api`. Volver a la raíz y usar `-pl apps/petclinic-api`. |
| `UnnecessaryStubbingException` | Un `when(...)` que la prueba no usa. Quitarlo, no relajar la extensión. |
| `@BeforeAll` no se ejecuta | El método no es `static` (o falta `@TestInstance(PER_CLASS)`). |
| El informe de JaCoCo sale casi vacío | Se generó con `-Dtest=UnaClase`: solo mide esa clase. Lanzar `./mvnw -pl apps/petclinic-api test` completo. |
| `-Dgroups=critico` no ejecuta nada | La etiqueta está mal escrita o la clase anidada no la hereda. |
