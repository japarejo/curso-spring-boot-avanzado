# Sesión 10 · Configuración centralizada y despliegue

**Módulo 5** · Diapositivas 139–145 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** Cuando hay varios servicios, la configuración se **centraliza**
> (Spring Cloud Config), los secretos se **cifran** y los cambios se aplican **sin reiniciar**.
> Y el mismo artefacto se puede desplegar de tres maneras: `java -jar`, en un servidor de
> aplicaciones externo o en un contenedor.

## Objetivos

- Servir configuración desde Spring Cloud Config, por aplicación y perfil.
- Cifrar secretos con el servidor de configuración y recargar valores en caliente.
- Desplegar el `war` en un Tomcat externo y explicar qué cambia.
- Escribir un `Dockerfile` desde cero, sin privilegios y con *healthcheck*.
- Construir imágenes por capas y saber qué se retransmite en cada despliegue.

## Antes de clase

- [ ] **Docker Desktop arrancado** e imágenes descargadas (`eclipse-temurin:21-jre`,
      `tomcat:10.1-jdk21-temurin`, `mysql:8.4`).
- [ ] `docker compose -f docker/compose.yaml build` hecho de antemano (tarda).
- [ ] Repasar las sesiones 1 y 3 del [guion de configurabilidad](../modulos/05-configuracion-despliegue/guion-configurabilidad.md):
      esta sesión las condensa.
- [ ] Plan B sin Docker: la «Alternativa completa sin Docker» del mismo guion, y el Tomcat
      descargado en ZIP.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso: hilos virtuales | 🗣 | — | Ejercicio 6 |
| 0:10 | Spring Cloud Config: el problema y la solución | 🗣 | 139–143 | |
| 0:20 | Config Server en marcha | ⌨ + 🧑‍💻 | 140–143 | guion de configurabilidad, sesión 1 |
| 0:50 | **Secretos cifrados y recarga en caliente** | 🧑‍💻 35' | 142–143 | Ejercicio 10 |
| 1:25 | **Descanso** | | | |
| 1:35 | **Desplegar en un Tomcat externo** | 🧑‍💻 30' | 144 | Ejercicio 11 |
| 2:05 | **Dockerizar desde cero** | 🧑‍💻 30' | 145 | Ejercicio 12 |
| 2:35 | Imágenes por capas y *buildpacks* | ⌨ | 145 | Ejercicios 7 y 8 |
| 2:50 | La infraestructura completa con Compose | ⌨ | — | `docker/compose.yaml` |
| 2:55 | Cierre del módulo 5 y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso

Que dos personas cuenten qué midieron en el ejercicio 6 y con qué herramienta.

🗣 **Mensaje clave.** «Una propiedad que promete rendimiento y no se mide es una creencia.»

### 0:10 – 0:20 · Spring Cloud Config

📊 Diapositivas 139–143.

Dibuja: un servidor de configuración, un repositorio (carpeta o Git) y N clientes que piden
`/{aplicación}/{perfil}`. Recorre la API de la diapositiva 142.

> **Erratas.** Las diapositivas 140 y 141 son iguales (la 141 debería mostrar el cliente). La
> 143 usa `spring.cloud.config.uri` sin más: desde Spring Boot 2.4 el cliente necesita además
> `spring.config.import=optional:configserver:` (ver `infra/bills-service`).

### 0:20 – 0:50 · Config Server en marcha

⌨ + 🧑‍💻 Versión condensada de la sesión 1 del guion de configurabilidad:

```bash
./mvnw -pl infra/config-server spring-boot:run
curl -s localhost:8889/bills-service/development | jq
./mvnw -pl infra/bills-service spring-boot:run
curl -s localhost:8040/api/v1/bills/whoami/ana
```

Que lo repitan y cambien el perfil de `bills-service` a `classroom`
(`--spring.profiles.active=classroom`). La misma aplicación responde distinto.

### 0:50 – 1:25 · Secretos cifrados y recarga en caliente

🧑‍💻 **Ejercicio 10** (35 min): parte A (cifrado con `/encrypt` y `{cipher}`) y parte B
(`@RefreshScope` y `/actuator/refresh`).

🗣 **Mensaje clave.** «Solo se recargan los beans `@RefreshScope` y los
`@ConfigurationProperties`. Un `@Value` en un bean normal se queda con el valor viejo, y es
justo lo que enseña `ServiceInstanceRestController`.»

### 1:25 – 1:35 · Descanso

### 1:35 – 2:05 · Desplegar en un Tomcat externo

📊 Diapositiva 144.
🧑‍💻 **Ejercicio 11** (30 min).

El descubrimiento del ejercicio: `petclinic-api` ya es `war` y tiene su
`SpringBootServletInitializer`, pero **le falta** `spring-boot-starter-tomcat` con
`provided`. Que lo encuentren comparando con la diapositiva.

🗣 **Mensaje clave.** «En un contenedor externo, `server.port` y `server.servlet.context-path`
no significan nada: los decide el contenedor. Es el error número uno al migrar.»

### 2:05 – 2:35 · Dockerizar desde cero

📊 Diapositiva 145.
🧑‍💻 **Ejercicio 12** (30 min) sobre `practicas/tareas`.

Primero que escriban **exactamente** el `Dockerfile` de la diapositiva y vean que falla
(`openjdk:8` no tiene Java 21 y está retirada). Después lo mejoran paso a paso.

### 2:35 – 2:50 · Imágenes por capas y *buildpacks*

⌨ Ejercicio 7 en pantalla (el antes y el después de `jarmode=tools extract --layers`) y la
orden de *buildpacks* del ejercicio 8, que la diapositiva 145 menciona:

```bash
./mvnw -pl apps/petclinic-api spring-boot:build-image \
  -Dspring-boot.build-image.imageName=curso/petclinic:buildpack
```

(La construcción con *buildpacks* tarda varios minutos la primera vez: lánzala al principio del
bloque.)

### 2:50 – 2:55 · La infraestructura completa

⌨ `docker compose -f docker/compose.yaml up` y enseña Eureka con todos los servicios
registrados y Spring Boot Admin con todos en verde. El resto de la sesión 3 del guion
(dos instancias de `bills-service`) queda de tarea.

### 2:55 – 3:00 · Cierre del módulo 5 y tarea

- **Tarea:**
  - Ejercicio 8 (*buildpacks*) y compararlo con vuestro `Dockerfile`.
  - Ejercicio 13 (Docker Compose integrado en Spring Boot con PostgreSQL).
  - Sesión 3 del guion de configurabilidad: dos instancias de `bills-service`.

---

## Si vas con retraso

- La parte B del ejercicio 10 (recarga) como demo.
- Ejercicio 11 por la vía Docker (más rápida que descargar Tomcat).
- Saltar el bloque de Compose: se ve en la tarea.

## Si te sobra tiempo

- Config Server con *backend* Git (variante del guion de configurabilidad).
- `HEALTHCHECK` y `docker inspect --format '{{.State.Health.Status}}'`.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `/encrypt` responde 404 | El servidor arrancó sin `ENCRYPT_KEY`. |
| El valor `{cipher}` llega tal cual al cliente | En YAML, falta entrecomillarlo; o la clave no es la misma con la que se cifró. |
| `/actuator/refresh` devuelve `[]` | El servidor de configuración lee del *classpath*: arrancarlo con `search-locations=file:...`. |
| Tomcat no despliega el `war` | Tomcat 9 (`javax`) en lugar de 10.1 (`jakarta`). |
| `docker build` no encuentra el jar | No se ejecutó `./mvnw package` antes, o el `COPY` apunta a otro nombre. |
| `docker: command not found` / no conecta | Docker Desktop no está arrancado. |
