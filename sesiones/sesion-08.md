# Sesión 8 · Pruebas con Spring: rebanadas, seguridad y APIs

**Módulo 4** · Diapositivas 113–132 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Cada prueba debe cargar **solo lo que necesita**. Una rebanada
> (`@DataJpaTest`, `@WebMvcTest`) es rápida y precisa; `@SpringBootTest` es completa y lenta.
> Y la API se prueba como la usa un cliente: por HTTP, con REST Assured o Postman.

## Objetivos

- Probar repositorios y servicios con `@DataJpaTest` (sociable) y controladores con
  `@WebMvcTest` + `MockMvc` (con el servicio simulado).
- Saber qué cuesta un `@DirtiesContext` y cuándo evitarlo.
- Probar las reglas de seguridad con `@WithMockUser`, `@WithUserDetails` y `with(user())`.
- Probar la API de extremo a extremo con REST Assured en un puerto aleatorio.
- Mantener una colección de Postman ejecutable con Newman.
- Probar un cliente HTTP sin el servicio real, y conocer el límite de ese doble.

## Antes de clase

- [ ] Leer la «Clase 2» del [guion de clases del módulo 4](../modulos/04-pruebas/guion-clases.md).
- [ ] Postman con la colección importada: [`postman/`](../postman/).
- [ ] Node.js para `npx newman` (si no, la demo de Newman se hace desde tu máquina).
- [ ] Opcional: Docker arrancado y `docker pull mysql:8.4`, para la demo de Testcontainers.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso: Mockito y la tarea | ⌨ | — | Ejercicio 13 |
| 0:10 | Mapa de las pruebas con Spring | 🗣 | 113–116 | guion de clases |
| 0:20 | **Repositorios con `@DataJpaTest`** | 🧑‍💻 20' | 113–115 | Ejercicios 6 y 7 |
| 0:40 | **Controladores con `@WebMvcTest`** | 🧑‍💻 30' | 119–121 | Ejercicios 8 y 9 |
| 1:10 | `@DirtiesContext` y el coste del contexto | ⌨ | — | Ejercicio 10 |
| 1:25 | **Descanso** | | | |
| 1:35 | **Probar la seguridad** | 🧑‍💻 25' | 132 | Ejercicio 15 |
| 2:00 | **REST Assured contra la API propia** | 🧑‍💻 30' | 124–130 | Ejercicio 16 |
| 2:30 | Postman y Newman | ⌨ + 🧑‍💻 | 131 | Ejercicio 17 |
| 2:45 | Dobles de servicios externos y sus límites | ⌨ | 122–123 | Ejercicio 18 |
| 2:55 | Cierre del módulo 4 y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

⌨ Pruebas 7 y 8 del ejercicio 13. Respuesta a la pregunta: el `Owner` de la prueba 7 **no** es
un doble, es un objeto real del dominio.

### 0:10 – 0:20 · Mapa de las pruebas con Spring

📊 Diapositivas 113–116.

Dibuja la arquitectura de la diapositiva 114 y colorea qué carga cada anotación:

| Anotación | Carga | Base de datos | Velocidad |
|---|---|---|---|
| ninguna (Mockito) | nada | no | milisegundos |
| `@DataJpaTest` | JPA + repositorios | H2, con *rollback* | ~1–2 s |
| `@WebMvcTest(X.class)` | MVC + el controlador X | no (servicios simulados) | ~1–2 s |
| `@SpringBootTest` | todo | sí | varios segundos |
| `@SpringBootTest(RANDOM_PORT)` | todo + servidor real | sí | varios segundos |

🗣 **Mensaje clave.** «La prueba de la diapositiva 113 es **sociable**: usa el servicio real
con el repositorio real. La de la 118 es **solitaria**: el repositorio es un doble. Las dos
tienen su sitio.»

### 0:20 – 0:40 · Repositorios con `@DataJpaTest`

🧑‍💻 **Ejercicios 6 y 7** (20 min): buscar propietarios y comprobar el *rollback*.

### 0:40 – 1:10 · Controladores con `@WebMvcTest`

📊 Diapositivas 119–121.
🧑‍💻 **Ejercicios 8 y 9** (30 min).

> **Errata de las diapositivas 119–120.** `@MockBean` está **obsoleto** desde Spring Boot 3.4.
> Se usa `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`), como ya
> hacen las pruebas del proyecto.

Pregunta mientras trabajan: «¿qué demuestra que la prueba pase si la vista JSP no existe?»
(Nada: `@WebMvcTest` no renderiza JSP. Ver «Dos cosas que conviene enseñar» en el README del
módulo 4.)

### 1:10 – 1:25 · `@DirtiesContext`

⌨ Ejercicio 10 como demo: ejecuta la batería con y sin `@DirtiesContext` y compara tiempos.

🗣 **Mensaje clave.** «Spring reutiliza el contexto entre pruebas con la misma configuración.
Cada `@DirtiesContext`, cada `@MockitoBean` distinto, rompe esa caché y vuelve a arrancar.»

### 1:25 – 1:35 · Descanso

### 1:35 – 2:00 · Probar la seguridad

📊 Diapositiva 132.
🧑‍💻 **Ejercicio 15** (25 min): `ReglasDeAccesoTests`, una prueba por regla.

Dos puntos para la puesta en común:

- `@AutoConfigureMockMvc` ya aplica Spring Security: no hace falta el `MockMvcBuilders` de la
  diapositiva.
- La fila de `owners/2` devuelve **200 vacío**. Es una mejora pendiente del proyecto: escribir
  primero la prueba que exige 403 o 404.

Si en la sesión 6 se hizo el ejercicio 6, añadir las dos filas de la API con `.with(jwt())`.

### 2:00 – 2:30 · REST Assured contra la API propia

📊 Diapositivas 124–130.
🧑‍💻 **Ejercicio 16** (30 min).

> **Errata.** La diapositiva 124 cita REST Assured 4.3.0 (el proyecto usa 5.5); la 125 usa
> `DEFINED_PORT` (mejor `RANDOM_PORT` + `@LocalServerPort`); la 129 usa la cabecera
> `Authentication` (es `Authorization`); y la API de Star Wars de la 128 ya no es
> `swapi.dev` (ver `ExternalApiRestAssuredTests`).

Pregunta del ejercicio: ¿se deshacen los datos que crea la prueba 3? No: con `RANDOM_PORT` la
petición va por otro hilo y otra transacción.

### 2:30 – 2:45 · Postman y Newman

📊 Diapositiva 131.

1. ⌨ (5 min) Ejecuta la colección en Postman con *Run collection*. Enseña cómo «Crear visita»
   guarda `visitaId` y la usa la petición siguiente.
2. 🧑‍💻 (10 min) **Ejercicio 17**, pasos 4 y 5: añadir una petición con dos pruebas y
   ejecutarlo todo con Newman:

   ```bash
   npx newman run postman/petclinic-api.postman_collection.json \
       -e postman/local.postman_environment.json --folder Visitas --folder Mascotas
   ```

🗣 **Mensaje clave.** «Newman devuelve un código de salida distinto de 0 si algo falla. Eso es
todo lo que necesita un *pipeline* de integración continua.»

### 2:45 – 2:55 · Dobles de servicios externos y sus límites

📊 Diapositivas 122–123.
⌨ Demo de la solución del ejercicio 18: `MockRestServiceServer` respondiendo dos facturas; luego
el campo renombrado (`concepto`) y la prueba **sigue en verde**.

🗣 **Mensaje clave.** «El doble responde lo que **tú crees** que responde el servicio. Si el
servicio cambia, el doble no se entera. Para eso existen las pruebas de contrato (Spring Cloud
Contract, Pact).»

Si hay Docker y tiempo: un minuto para enseñar que existen las pruebas `*IT` con Testcontainers
([guion](../modulos/04-pruebas/guion-testcontainers.md)), material de ampliación.

### 2:55 – 3:00 · Cierre del módulo 4 y tarea

- **Tarea:**
  - Ejercicio 11 (nueva regla de negocio con TDD) y ejercicio 12 (comparar tipos de prueba).
  - Ejercicio 18 completo.
  - Opcional: el guion de Testcontainers.

---

## Si vas con retraso

- Ejercicios 6 y 7 como demo (10 min) para ganar tiempo para los de la segunda mitad.
- Postman solo como demo; el ejercicio 17 en casa.

## Si te sobra tiempo

- Ejecutar una prueba `*IT` con Testcontainers contra MySQL real.
- Romper a propósito el `Location` de `RestfulVisitController` y ver qué prueba lo detecta.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `@WebMvcTest` falla por un bean que falta | El controlador depende de algo que no es un servicio simulado (un conversor, un validador): simularlo también. |
| La prueba de seguridad da 302 en lugar de 403 | Usuario anónimo: el formulario redirige al login. |
| `@WithUserDetails` no encuentra el usuario | Falta un `UserDetailsService` en el contexto, o `data.sql` no se ha cargado. |
| REST Assured conecta al 8080 | Falta `RestAssured.port = port` en el `@BeforeEach`. |
| `npx newman` no existe | Falta Node.js. Alternativa: *Run collection* en Postman. |
