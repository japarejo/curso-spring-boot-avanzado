# Práctica «tareas»: una aplicación Spring Boot desde cero

Es la solución de referencia del **ejercicio 6 del módulo 1**: lo que se obtiene al generar un
proyecto con Spring Initializr y escribir encima la entidad `Tarea` de las transparencias
(diapositiva 154).

La misma aplicación se reutiliza más adelante:

| Módulo | Ejercicio | Qué se hace con ella |
|---|---|---|
| 1 | 6 | Crearla desde cero, recorrer su estructura y arrancarla |
| 1 | 7 | Cambiar el `parent` por la importación del BOM |
| 3 | 8 | Añadir `spring-boot-starter-security` y ver la seguridad **por defecto** |
| 3 | 10 | Usuarios con el esquema JDBC **canónico** de Spring Security |
| 6 | 8 | Banco de pruebas para mapear relaciones (ver también [`../mapeos-jpa`](../mapeos-jpa)) |

No forma parte del reactor del curso: es un proyecto independiente, igual que el que genera el
alumnado, y por eso lleva **su propio Maven embebido** (`mvnw`, `mvnw.cmd` y `.mvn/wrapper/`).
No hace falta tener Maven instalado; basta con un JDK 21. Se puede abrir sola en el IDE.

```bash
cd practicas/tareas
./mvnw spring-boot:run        # Windows: .\mvnw spring-boot:run
./mvnw test
```

La primera ejecución descarga Maven 3.9.7 en `~/.m2/wrapper` (la misma versión que usa el
resto del curso, así que si ya has compilado el repositorio no descarga nada).

| Dirección | Qué es |
|---|---|
| <http://localhost:8080/api/v1/tareas> | Listado de tareas |
| <http://localhost:8080/api/v1/tareas?soloPendientes=true> | Solo las pendientes, ordenadas por fecha |
| <http://localhost:8080/h2-console> | Consola H2 (URL `jdbc:h2:mem:tareas`, usuario `sa`, sin contraseña) |
| <http://localhost:8080/actuator> | Actuator |

## Estructura

```
practicas/tareas
├── mvnw, mvnw.cmd, .mvn/wrapper/   Maven embebido (wrapper)
├── pom.xml                         parent spring-boot-starter-parent 3.5.16
└── src
    ├── main
    │   ├── java/com/idexaformacion/cursos/tareas
    │   │   ├── TareasApplication.java    @SpringBootApplication
    │   │   ├── Tarea.java                entidad JPA
    │   │   ├── TareaRepository.java      Spring Data
    │   │   └── TareaController.java      API REST
    │   └── resources
    │       ├── application.properties
    │       └── data.sql
    └── test/java/com/idexaformacion/cursos/tareas
        ├── TareasApplicationTests.java   ¿arranca el contexto?
        ├── TareaControllerTests.java     @WebMvcTest + MockMvc
        └── TareaRepositoryTests.java     @DataJpaTest
```

Compárala con la de la diapositiva 14: es la misma, más el código que se escribe en clase.
