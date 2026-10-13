# Sesión 1 · De Spring a Spring Boot

**Módulo 1** · Diapositivas 1–34 · 3 horas (85 min + 10 min de descanso + 85 min)

> **Mensaje de la sesión.** Spring Boot no añade funcionalidad a Spring: añade **decisiones**.
> Convención sobre configuración, versiones gestionadas y autoconfiguración. Y todo descansa
> en una idea: el contenedor crea los objetos y los conecta (inversión de control).

## Objetivos

Al terminar, cada participante:

- ha creado y arrancado una aplicación Spring Boot **desde cero**;
- sabe leer un `pom.xml` y explicar qué aporta un *starter* y qué es un BOM;
- distingue inversión de control de inyección de dependencias, y defiende la inyección por
  constructor;
- sabe resolver la ambigüedad cuando hay dos beans del mismo tipo;
- sabe mirar dentro de una aplicación en marcha con Actuator.

## Antes de clase

- [ ] `./mvnw verify` pasado ayer en tu máquina (caché de dependencias llena).
- [ ] `petclinic-api` arrancado una vez para comprobar que no hay nada roto.
- [ ] Pestañas abiertas: <https://start.spring.io>, el repositorio en GitHub,
      [`modulos/01-introduccion-spring-boot/ejercicios.md`](../modulos/01-introduccion-spring-boot/ejercicios.md).
- [ ] Un proyecto `tareas` generado por ti **fuera** del repositorio, por si Initializr no
      responde en el aula (plan B: copiar [`practicas/tareas`](../practicas/tareas/)).
- [ ] Enviar el enlace del repositorio por el chat del aula al empezar.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Presentación, objetivos y comprobación del entorno | ⌨ | 1–5 | `00-preparacion-entorno.md` |
| 0:15 | Spring y Spring Boot: el problema que resuelven | ⌨ 🗣 | 7–13 | `petclinic-api` |
| 0:30 | **Una aplicación desde cero** | 🧑‍💻 35' | 14, 28–31 | Ejercicio 6 |
| 1:05 | *Starters* y BOM | ⌨ + 🧑‍💻 | 27–28 | Ejercicio 1 |
| 1:25 | **Descanso** | | | |
| 1:35 | Inversión de control e inyección de dependencias | 🗣 + 🧑‍💻 | 15–25 | Ejercicio 3 |
| 2:00 | Dos beans del mismo tipo | 🧑‍💻 20' | 21–25 | Ejercicio 10 |
| 2:20 | Los objetos que crea Spring: *proxies* | ⌨ | 26 | Ejercicio 9 (demo) |
| 2:30 | Autoconfiguración y Actuator | ⌨ + 🧑‍💻 | 13, 32 | Ejercicios 2 y 8 |
| 2:50 | Arquitectura de una aplicación Spring | 🗣 | 33–34 | Ejercicio 5 |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:15 · Presentación y entorno

📊 Diapositivas 1–5.

1. Presentación personal y del curso: seis módulos, 40 horas, mucha práctica. Enseñar el
   calendario de [`sesiones/README.md`](README.md).
2. Pedir a todo el mundo que ejecute, **ahora**:

   ```bash
   ./mvnw -v          # tiene que decir Java version: 21
   git -C curso-spring-boot-avanzado log --oneline -1
   ```

3. Quien no tenga el entorno: que siga con la pantalla del docente durante el primer bloque y
   lo resuelva en el descanso. **No parar la clase por esto.**

### 0:15 – 0:30 · Spring y Spring Boot

📊 Diapositivas 7–13.

⌨ **Arranca la aplicación antes de explicar nada**:

```bash
./mvnw -pl apps/petclinic-api spring-boot:run
```

Mientras arranca, cuenta la historia: Spring nació como alternativa ligera a J2EE (diap. 8),
sus ideas acabaron en el estándar, y el ecosistema creció tanto (diap. 10–11) que hizo falta
Spring Boot para configurarlo (diap. 12–13).

Cuando termine de arrancar, enseña <http://localhost:8080> y, sobre todo,
<http://localhost:8080/swagger-ui.html>: **será la herramienta de todo el curso** para probar la
API, en lugar de `curl`. Ejecuta una operación delante del grupo (**Swagger UI** → *Visits* →
`GET /api/v1/visits` → **Execute**) y enseña las tres zonas de la respuesta: la orden `curl`
equivalente, el código de estado con las cabeceras, y el cuerpo. Busca también la etiqueta
*Actuator*: los puntos de diagnóstico se llaman desde ahí.

Guía para el docente: [`docs/swagger-ui-en-clase.md`](../docs/swagger-ui-en-clase.md).

🗣 **Mensaje clave.** «Esta aplicación tiene base de datos, seguridad, API REST documentada,
monitorización y vistas web. Ahora vais a contar cuántas líneas de configuración de
infraestructura hay: casi ninguna.»

### 0:30 – 1:05 · Una aplicación desde cero

📊 Diapositivas 14, 28–31.
🧑‍💻 **Ejercicio 6 del módulo 1** (35 min).

Es el corazón de la sesión: que cada participante salga con **su** proyecto funcionando.

1. ⌨ Haz los pasos 1 y 2 en pantalla (generar en <https://start.spring.io> y descomprimir).
   Señala en la estructura generada lo mismo que la diapositiva 14. Recuerda el paso de
   añadir `springdoc-openapi-starter-webmvc-ui` (Initializr no la ofrece): sin ella no hay
   Swagger UI para probar lo que escriban.
2. 🧑‍💻 Que lo hagan ellos hasta el paso 7. Pasea por el aula.
3. A los 25 minutos, ⌨ enseña la solución de
   [`practicas/tareas`](../practicas/tareas/) y comenta los **tres defectos** del
   `PersonController` de la diapositiva 29 (falta `@RequestBody`, `.get()` sobre un `Optional`
   vacío, 200 en lugar de 201).
4. Enseña también que las pruebas de las diapositivas 30–31 son de JUnit 4 y Spring Boot 1.x:
   compáralas con `TareaControllerTests` y `TareaRepositoryTests`.

🗣 **Mensaje clave.** «No hemos escrito ni una línea de configuración de Tomcat, de Jackson,
de Hibernate ni del `DataSource`. Eso es la autoconfiguración, y en un rato veremos cómo
decide.»

> **Si Initializr ya no ofrece 3.5.x** (solo ofrece versiones con soporte abierto): generar
> con la que haya y cambiar el `parent` a 3.5.16, o copiar el `pom.xml` de
> `practicas/tareas`. Ver la nota del paso 3 del ejercicio.

### 1:05 – 1:25 · *Starters* y BOM

📊 Diapositivas 27–28.

⌨ Sobre `apps/petclinic-api/pom.xml`:

```bash
./mvnw -pl apps/petclinic-api dependency:list | grep -c ":compile"
./mvnw -pl apps/petclinic-api dependency:tree -Dincludes=org.springframework.boot:spring-boot-starter-web
```

1. Unas 20 dependencias declaradas frente a más de 100 en el *classpath*. Ninguna lleva
   `<version>`: la fija el `parent`.
2. Enseña en el `pom.xml` raíz las **dos** formas de la diapositiva 27 conviviendo: hereda de
   `spring-boot-starter-parent` e **importa** los BOM de Spring Cloud, Boot Admin y
   Testcontainers.
3. Enseña `<flyway.version>`: así se sobrescribe una versión gestionada.

🧑‍💻 Ejercicio 1 (10 min). El ejercicio 7 (importar el BOM en `tareas`) queda de **tarea**.

### 1:25 – 1:35 · Descanso

Aprovecha para resolver los entornos que fallaron.

### 1:35 – 2:00 · Inversión de control e inyección de dependencias

📊 Diapositivas 15–25.

1. 🗣 IoC frente a DI con el ejemplo de Fowler (diap. 17–20): «todo *framework* invierte el
   control; lo que Spring añade es que **también** te da las dependencias».
2. ⌨ En el proyecto conviven los dos estilos, a propósito:

   ```java
   // service/PetService.java  -> por constructor
   // service/VisitService.java -> por campo, con @Autowired
   ```

   Pregunta: «¿cuál es mejor?». **Deja que discutan un minuto** antes de responder.
3. ⌨ Abre `src/test/.../OwnerServiceIsolatedMockTests.java` y enseña el
   `ReflectionTestUtils.setField(...)`. Ese es el precio de la inyección por campo.
4. 🧑‍💻 Ejercicio 3 (10 min): refactorizar `VisitService` a inyección por constructor.

🗣 **Mensaje clave.** «Una clase con constructor **declara** lo que necesita. Una clase con
`@Autowired` en campos privados lo **esconde**.»

### 2:00 – 2:20 · Dos beans del mismo tipo

📊 Diapositivas 21–25 (`@Component`, estereotipos, `@Bean`).
🧑‍💻 **Ejercicio 10** (20 min).

1. Que creen las dos implementaciones de `NotificadorCitas` y arranquen.
2. ⌨ Lee el error en voz alta: `NoUniqueBeanDefinitionException`. El propio mensaje propone
   tres soluciones.
3. Que prueben las cuatro. Pon en común cuándo usar cada una.
4. Termina con la cuarta (`@ConditionalOnProperty`) y enlaza con lo que viene:

🗣 **Mensaje clave.** «Casi todo lo que Spring Boot hace por vosotros es un
`@ConditionalOn...`. Vamos a verlo.»

### 2:20 – 2:30 · Los objetos que crea Spring

📊 Diapositiva 26.
⌨ **Demo** del ejercicio 9, pasos 1 y 2 (el `CommandLineRunner` que imprime las clases).

```
PetService      -> class ...PetService$$SpringCGLIB$$0
OwnerRepository -> class jdk.proxy2.$Proxy142
```

🗣 **Mensaje clave.** «Cuando Spring necesita añadir comportamiento —transacciones, seguridad,
validación—, os da **otro objeto** que envuelve el vuestro. Recordad esto, porque explica las
dos trampas más famosas de `@Transactional` y `@PreAuthorize`, que veremos en los módulos 3 y
6: los métodos privados y las llamadas internas no pasan por el envoltorio.»

El resto del ejercicio 9 (escribir un aspecto) queda de tarea.

### 2:30 – 2:50 · Autoconfiguración y Actuator

📊 Diapositivas 13, 32.

1. ⌨ Ejercicio 2, en pantalla y rápido: arrancar con `--debug`, buscar
   `DataSourceAutoConfiguration` en *Positive matches*, excluirla y ver el fallo.
2. 🧑‍💻 Ejercicio 8, **pasos 1 y 2** (12 min): recorrer los puntos de Actuator desde la etiqueta
   *Actuator* de Swagger UI y cambiar el nivel de registro sin reiniciar (el `POST` a
   `/actuator/loggers/{name}` de la diapositiva 32, con el cuerpo `{"configuredLevel":"DEBUG"}`).
3. Los pasos 3 a 5 (indicador de salud propio, grupos, `InfoContributor`) quedan de tarea.

🗣 **Mensaje clave.** «Actuator es la lupa para todo el curso. Y también es una puerta: en el
módulo 5 la cerraremos.»

### 2:50 – 2:55 · Arquitectura

📊 Diapositivas 33–34.

Recorre una petición de arriba abajo con el código delante:

```
GET /api/v1/visits
  web/api/RestfulVisitSearchController   recibe y valida
  service/VisitService                   caso de uso y transacción
  repository/VisitRepository             consulta
  model/Visit                            entidad
  web/api/dto/VisitResponse              lo que sale por el cable
```

Lanza la pregunta con trampa del ejercicio 5 (`BillsGateway` es `@Component` y no `@Service`)
y déjala para la próxima sesión.

### 2:55 – 3:00 · Cierre y tarea

- Repaso en tres frases: autoconfiguración, *starters*/BOM, inyección por constructor.
- **Tarea:**
  - Ejercicio 4 (añadir un bean y un endpoint).
  - Ejercicio 5 (situar el código en la arquitectura) y la pregunta con trampa.
  - Ejercicio 7 (importar el BOM en `tareas`).
  - Ejercicio 8, pasos 3–5 (Actuator propio).
  - Ejercicio 9, pasos 3–5 (aspecto que mide tiempos).

---

## Si vas con retraso

- Recorta el bloque de *starters* a 10 minutos (sin práctica).
- El ejercicio 10 se puede hacer solo con `@Primary` y `@Qualifier`, y dejar las otras dos
  opciones para casa.
- **No recortes** el ejercicio 6: es el único momento del curso en que crean un proyecto de
  cero.

## Si te sobra tiempo

- Ejercicio 9 completo: el aspecto y el experimento de la autoinvocación.
- Abrir `/actuator/conditions` y buscar por qué `DataSourceAutoConfiguration` se activó.
- Comentar las notas de migración a Spring Boot 4 que hay en el código:

  ```bash
  grep -rn "Spring Boot 4" apps/ infra/ --include=*.java
  ```

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `Port 8080 is already in use` al arrancar `tareas` | `petclinic-api` sigue arrancado. Pararlo, o `--server.port=8081`. |
| El IDE no encuentra las clases | El proyecto se ha importado desde el `pom.xml` de un módulo y no desde el raíz. |
| El `POST` de `tareas` guarda una tarea vacía | Falta `@RequestBody` (el defecto de la diapositiva 29). |
| `Name for argument of type [java.lang.Integer] not specified` | Proyecto sin el `parent` de Spring Boot: falta `-parameters` (ejercicio 7). |
| Alguien con JDK 17 o 25 | `./mvnw -v`. Tiene que ser 21. |
