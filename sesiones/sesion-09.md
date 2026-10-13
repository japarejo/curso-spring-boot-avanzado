# Sesión 9 · Configuración externa y observabilidad

**Módulo 5** · Diapositivas 133–138 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** **El mismo artefacto** se despliega en todos los entornos; lo que
> cambia es la configuración externa. Spring Boot la lee de muchos sitios con un orden de
> precedencia preciso, y Actuator (con Spring Boot Admin encima) permite ver y ajustar la
> aplicación mientras corre… siempre que no se deje abierto a todo el mundo.

## Objetivos

- Predecir qué valor gana cuando una propiedad está en varios sitios.
- Usar perfiles, combinarlos y saber cuál prevalece.
- Sustituir `@Value` por `@ConfigurationProperties` validadas que fallan al arrancar.
- Cambiar niveles de registro y publicar métricas propias sin reiniciar.
- Monitorizar varias aplicaciones con Spring Boot Admin, con Actuator protegido.
- Entender qué aporta Eureka (registro y descubrimiento).

## Antes de clase

- [ ] Compilada la infraestructura sin Docker:
      `./mvnw -pl infra/registry,infra/admin-server -am package -DskipTests`.
- [ ] Repasar la sesión 2 del [guion de configurabilidad](../modulos/05-configuracion-despliegue/guion-configurabilidad.md)
      (Eureka y Admin): el último bloque de hoy la condensa.
- [ ] Hoy **no** hace falta Docker.

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | El problema de la configuración | 🗣 ⌨ | 134–135 | |
| 0:10 | **Orden de precedencia** | 🧑‍💻 20' | 135 | M5 ejercicio 1 |
| 0:30 | **Perfiles** | 🧑‍💻 20' | 134 | Ejercicio 2 |
| 0:50 | **Configuración tipada** | 🧑‍💻 25' | — | Ejercicio 3 |
| 1:15 | Registro en caliente | ⌨ | 32 | Ejercicio 4 |
| 1:25 | **Descanso** | | | |
| 1:35 | **Métricas con Micrometer** | 🧑‍💻 20' | — | Ejercicio 5 |
| 1:55 | Spring Boot Admin: modelo cliente-servidor | 🗣 | 136–138 | |
| 2:05 | **Admin sin Docker y con Actuator protegido** | 🧑‍💻 40' | 136–138 | Ejercicio 9 |
| 2:45 | Eureka: registro y descubrimiento | ⌨ | — | guion de configurabilidad, sesión 2 |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · El problema de la configuración

📊 Diapositivas 134–135.

⌨ Empaqueta y arranca el artefacto como en producción:

```bash
./mvnw -pl apps/petclinic-api -am package -DskipTests
java -jar apps/petclinic-api/target/petclinic-api.war
```

🗣 **Mensaje clave.** «Si para cambiar de entorno hay que recompilar, no tenemos un artefacto:
tenemos uno por entorno. La configuración se saca **fuera**.»

Recorre los perfiles del proyecto (tabla del README raíz): ninguno, `mysql`, `docker`,
`nplus1`. Cada uno solo **sobrescribe** lo que cambia.

### 0:10 – 0:30 · Orden de precedencia

🧑‍💻 **Ejercicio 1** (20 min): el mismo `server.port` desde cuatro sitios.

Puesta en común con la regla: argumentos de línea de órdenes > variables de entorno >
ficheros externos > ficheros empaquetados (con los de perfil por encima de los generales).

### 0:30 – 0:50 · Perfiles

🧑‍💻 **Ejercicio 2** (20 min): perfil `demo` y combinación `demo,mysql`.

### 0:50 – 1:15 · Configuración tipada

🧑‍💻 **Ejercicio 3** (25 min): leer `BillsProperties`, romperlo y crear `PetclinicProperties`
validado.

🗣 **Mensaje clave.** «Fallar al arrancar es mejor que fallar en la primera petición de un
usuario a las tres de la madrugada.»

### 1:15 – 1:25 · Registro en caliente

⌨ Ejercicio 4 en pantalla (ya se vio en la sesión 1), todo desde el navegador:

1. Abre <http://localhost:8080/logging> y mira la consola: solo INFO, WARN y ERROR.
2. **Swagger UI** → *Actuator* → `POST /actuator/loggers/{name}` →
   `name` = `org.springframework.samples.petclinic` → cuerpo `{"configuredLevel":"TRACE"}` →
   **Execute**.
3. Recarga <http://localhost:8080/logging>: aparecen los cinco niveles.

Pregunta para el descanso: «¿qué pasaría si cualquiera pudiera hacer ese `POST` en producción?».

### 1:25 – 1:35 · Descanso

### 1:35 – 1:55 · Métricas con Micrometer

🧑‍💻 **Ejercicio 5** (20 min): `micrometer-registry-prometheus` y el contador
`petclinic.visitas.creadas`.

### 1:55 – 2:05 · Spring Boot Admin

📊 Diapositivas 136–138.

🗣 Servidor que agrega los Actuator de muchas aplicaciones. Cliente que se registra (o servidor
que las descubre en Eureka).

> **Errata de la diapositiva 136.** La tabla de compatibilidad llega a Spring Boot 3.0. Hoy:
> Spring Boot Admin 3.5.x para Spring Boot 3.5 (la versión la fija el `pom.xml` raíz). Y la
> diapositiva 138 resuelve el acceso **desactivando toda la seguridad**: es lo que vamos a
> evitar en el ejercicio.

### 2:05 – 2:45 · Admin sin Docker y con Actuator protegido

🧑‍💻 **Ejercicio 9** (40 min).

1. Pasos 1–3 (15 min): arrancar Admin y `petclinic-api` como cliente; recorrer las pestañas;
   cambiar un nivel de registro desde la interfaz.
2. Pasos 4–6 (25 min): proteger `/actuator/**` con una cadena propia y un usuario `ACTUATOR`,
   y dar las credenciales a Admin por metadatos.

🗣 **Mensaje clave.** «Exponer y proteger son dos decisiones distintas. `exposure.include`
decide **qué** existe; Spring Security decide **quién** lo ve.»

### 2:45 – 2:55 · Eureka

⌨ Versión corta de la sesión 2 del guion de configurabilidad:

```bash
./mvnw -pl infra/registry spring-boot:run        # http://localhost:8761
./mvnw -pl infra/bills-service spring-boot:run   # se registra solo
```

Enseña el panel de Eureka con `BILLS-SERVICE` y explica por qué `petclinic-api` tiene
`eureka.client.enabled=false` por defecto (para que arrancar la aplicación sola no llene la
consola de errores).

### 2:55 – 3:00 · Cierre y tarea

- **Tarea:** ejercicio 6 (hilos virtuales: activar y **medir**).
- Para la sesión 10: **Docker Desktop instalado y arrancado**, y las imágenes descargadas
  (lista en [`00-preparacion-entorno.md`](../modulos/00-preparacion-entorno.md)).

---

## Si vas con retraso

- Ejercicios 1 y 2 juntos en 25 minutos.
- Ejercicio 5 como demo.
- Eureka se puede mover al principio de la sesión 10.

## Si te sobra tiempo

- Grupos de *health* y sondas de Kubernetes (ejercicio 8 del módulo 1, pasos 3–4) si no se
  hicieron.
- `management.server.port=9091`: Actuator en otro puerto, que no se publica hacia fuera.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| Admin no muestra la aplicación | `spring.boot.admin.client.enabled=false` (es el valor por defecto en este proyecto). |
| Admin la muestra pero en rojo | Actuator protegido y Admin sin credenciales en los metadatos. |
| La pestaña *Logfile* no aparece | Falta `logging.file.name`. |
| `SERVER_PORT` no surte efecto en PowerShell | Es `$env:SERVER_PORT="8081"`, no `SERVER_PORT=8081`. |
| La aplicación no arranca tras el ejercicio 3 | ¡Bien! Es la validación de `@ConfigurationProperties`. Leer el mensaje. |
