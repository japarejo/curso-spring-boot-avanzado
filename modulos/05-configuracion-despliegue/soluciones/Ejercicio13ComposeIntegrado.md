# Módulo 5 · Ejercicio 13 · Solución: Docker Compose integrado en Spring Boot

## `practicas/tareas/compose.yaml`

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: tareas
      POSTGRES_USER: tareas
      POSTGRES_PASSWORD: tareas
    ports:
      - "5432"          # puerto aleatorio en el anfitrión: no choca con otro PostgreSQL
```

## `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

`optional` hace que la dependencia **no** viaje al jar final: es solo para desarrollo, como
DevTools.

## `application.properties`

```properties
#spring.datasource.url=jdbc:h2:mem:tareas
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=always
```

## Qué sale en la consola

```
.s.b.d.c.l.DockerComposeLifecycleManager : Using Docker Compose file .../practicas/tareas/compose.yaml
.s.b.d.c.l.DockerComposeLifecycleManager : Container tareas-postgres-1 Started
.s.b.d.c.l.DockerComposeLifecycleManager : Container tareas-postgres-1 Healthy
...
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
```

La URL, el usuario y la contraseña salen del propio `compose.yaml`: Spring Boot reconoce la
imagen `postgres`, lee sus variables de entorno y publica un `JdbcConnectionDetails`
(*service connection*). Es el mismo mecanismo que `@ServiceConnection` en las pruebas con
Testcontainers del módulo 4.

Al parar la aplicación, el contenedor se **para** (no se borra). Se controla con
`spring.docker.compose.lifecycle-management` (`start-and-stop`, `start-only`, `none`).

## Un detalle de `data.sql`

Con `ddl-auto=update` y `sql.init.mode=always`, `data.sql` se ejecuta en **cada** arranque y
las tareas de ejemplo se duplican. Dos salidas:

- en desarrollo, `ddl-auto=create-drop` (se recrea la base en cada arranque, como con H2);
- en serio, Flyway (módulo 6, ejercicios 1 y 2).
