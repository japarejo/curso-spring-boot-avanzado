# Curso: Spring Boot avanzado

Material de referencia del curso **Spring Boot avanzado** impartido por José Antonio Parejo
(IDEXA Formación). Contiene la aplicación de ejemplo, la infraestructura de apoyo y los
guiones, ejercicios y soluciones de los seis módulos del temario.

- **Java 21** (requisito del cliente)
- **Spring Boot 3.5.16**, la última de la rama 3, que es la base del material
- **Spring Cloud 2025.0.3**

> Dónde cambia Spring Boot 4.x: el código lleva notas `// En Spring Boot 4.x esto cambia a…`
> en los puntos que el temario marca (Jackson 3, starters modulares, versionado nativo de
> API, `@ImportHttpServices`, `RestTestClient`, Spring Security 7).

---

## Arranque en tres órdenes

```bash
git clone <este-repositorio>
cd curso-spring-boot-avanzado
./mvnw verify                       # compila y pasa las pruebas rápidas
./mvnw -pl apps/petclinic-api spring-boot:run
```

No hace falta instalar Maven: el wrapper (`mvnw`) va incluido. Sí hace falta un **JDK 21**.

Abrir después:

| Dirección | Qué es |
|---|---|
| <http://localhost:8080> | Aplicación (vistas JSP mínimas) |
| <http://localhost:8080/swagger-ui.html> | API REST documentada |
| <http://localhost:8080/h2-console> | Consola de la base de datos en memoria |
| <http://localhost:8080/actuator> | Puntos de diagnóstico |

### Usuarios de demostración

| Usuario | Contraseña | Autoridad |
|---|---|---|
| `admin1` | `4dm1n` | admin |
| `owner1` … `owner10` | `0wn3r` | owner |
| `vet1` | `v3t` | vet |

Las contraseñas se guardan cifradas con BCrypt (`{bcrypt}$2a$10$…`), no en claro.

---

## Mapa del temario

Cada módulo tiene su carpeta en [`modulos/`](modulos/) con el guion docente, los enunciados
y las soluciones. La columna "dónde mirar" indica el código que ilustra cada tema.

| # | Módulo | Material | Dónde mirar en el código |
|---|---|---|---|
| 1 | Introducción a Spring Boot | [`modulos/01-introduccion-spring-boot/`](modulos/01-introduccion-spring-boot/) | `pom.xml`, `PetclinicApplication`, `configuration/` |
| 2 | APIs REST avanzadas | [`modulos/02-apis-rest/`](modulos/02-apis-rest/) | `web/api/`, `apiclients/` |
| 3 | Seguridad | [`modulos/03-seguridad/`](modulos/03-seguridad/) | `configuration/SecurityConfiguration`, `infra/auth-service/` |
| 4 | Pruebas | [`modulos/04-pruebas/`](modulos/04-pruebas/) | `apps/petclinic-api/src/test/` |
| 5 | Configuración y despliegue | [`modulos/05-configuracion-despliegue/`](modulos/05-configuracion-despliegue/) | `infra/`, `docker/`, los `application-*.properties` |
| 6 | Spring Data y JPA | [`modulos/06-spring-data-jpa/`](modulos/06-spring-data-jpa/) | `model/`, `repository/`, `service/`, `db/migration/` |

Antes de la primera clase: [`modulos/00-preparacion-entorno.md`](modulos/00-preparacion-entorno.md).

---

## Estructura del repositorio

```
curso-spring-boot-avanzado/
├── apps/
│   └── petclinic-api/       Aplicación de referencia (módulos 1, 2, 3, 4 y 6)
├── infra/                   Infraestructura del módulo 5
│   ├── registry/            Eureka                      :8761
│   ├── config-server/       Spring Cloud Config         :8889
│   ├── admin-server/        Spring Boot Admin           :9090
│   ├── auth-service/        Autenticación JWT           :8060
│   └── bills-service/       Servicio cliente de la demo :8040
├── docker/                  Dockerfile y compose.yaml
└── modulos/                 Guiones, ejercicios y soluciones
```

### Perfiles de la aplicación

| Perfil | Para qué | Cómo se activa |
|---|---|---|
| *(ninguno)* | H2 en memoria, esquema generado por Hibernate, datos de `data.sql`. Es lo que se usa en clase y en las pruebas. | por defecto |
| `mysql` | MySQL externo, esquema aplicado con **Flyway**, `ddl-auto=validate` | `-Dspring-boot.run.profiles=mysql` |
| `docker` | Integración con Eureka, Config Server y Boot Admin | lo pone `docker/compose.yaml` |
| `nplus1` | Laboratorio de consultas N+1: reactiva `open-in-view` y el registro de SQL | `-Dspring-boot.run.profiles=nplus1` |

---

## Infraestructura completa

```bash
docker compose -f docker/compose.yaml up --build
```

| Dirección | Servicio |
|---|---|
| <http://localhost:8761> | Eureka: deben aparecer los servicios registrados |
| <http://localhost:9090> | Spring Boot Admin |
| <http://localhost:8889/bills-service/development> | Configuración servida por el Config Server |
| <http://localhost:8060> | API de autenticación (Swagger UI) |
| <http://localhost:8080/swagger-ui.html> | API de PetClinic |

Para levantar solo la base de datos, que es el uso habitual en clase:

```bash
docker compose -f docker/compose.yaml up -d mysql
./mvnw -pl apps/petclinic-api spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

## Ejecución de las pruebas

La separación es deliberada y es contenido del módulo 4:

```bash
./mvnw test      # solo *Test y *Tests: rápidas, sin Docker
./mvnw verify    # añade las *IT (Testcontainers) y el informe de cobertura
```

El informe de JaCoCo queda en `apps/petclinic-api/target/site/jacoco/index.html`.

Algunas pruebas están etiquetadas porque dependen de recursos externos:

```bash
./mvnw test -Dgroups='!external-api'   # excluye las que salen a Internet
```

---

## Notas de mantenimiento

- Las **claves RSA y el secreto HMAC** de `infra/auth-service` son de demostración y están
  versionados a propósito para que el curso funcione recién clonado. Están marcados como
  tales en sus ficheros. En un sistema real irían en un gestor de secretos.
- La aplicación se empaqueta como **war** y no como jar: las JSP no funcionan dentro de un
  jar ejecutable. El artefacto resultante se puede ejecutar con `java -jar` igualmente, y
  además sirve de ejemplo para el apartado de despliegue en servidores no embebidos.
- Este repositorio sustituye a `spring-petclinic-microservices`, que se conserva como
  referencia histórica.
