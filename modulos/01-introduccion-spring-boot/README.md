# Módulo 1. Introducción a Spring Boot

Guion docente. Duración estimada: **una sesión de 120 minutos**.

## Objetivos

Al terminar, el alumnado debe ser capaz de:

- Explicar qué problema resuelve Spring Boot sobre Spring Framework.
- Leer un `pom.xml` y saber qué aporta cada starter.
- Distinguir inversión de control de inyección de dependencias, y justificar por qué la
  inyección por constructor es preferible a la de campo.
- Situar cualquier clase del proyecto en la arquitectura por capas.
- Saber en qué versión de Spring Boot está y qué implicaría subir a 4.x.

## Mapa del material

| Concepto | Dónde se ve |
|---|---|
| Starters y gestión de versiones | `pom.xml` raíz y `apps/petclinic-api/pom.xml` |
| `@SpringBootApplication` | `apps/petclinic-api/src/main/java/.../PetclinicApplication.java` |
| Beans y estereotipos | `service/`, `repository/`, `web/` |
| Inyección por constructor | `service/PetService.java` |
| Inyección por campo | `service/VisitService.java` |
| Configuración con `@Bean` | `configuration/SecurityConfiguration.java` |
| Arquitectura por capas | `web/` → `service/` → `repository/` → `model/` |

## Desarrollo de la sesión

### 0:00 – 0:15 · De Spring a Spring Boot

Arrancar la aplicación **antes** de explicar nada:

```bash
./mvnw -pl apps/petclinic-api spring-boot:run
```

Mientras arranca, mostrar el `pom.xml` y contar cuántas dependencias hay declaradas
(unas veinte) frente a cuántos jars acaban en el classpath:

```bash
./mvnw -pl apps/petclinic-api dependency:list | wc -l
```

> **Mensaje clave.** Spring Boot no añade funcionalidad a Spring: añade *decisiones*.
> Convención sobre configuración, gestión de versiones y autoconfiguración.

### 0:15 – 0:35 · Starters y BOM

Recorrer `apps/petclinic-api/pom.xml`. Está agrupado por módulo del temario a propósito,
para que se vea qué dependencia sirve para qué.

Señalar que **ninguna dependencia lleva `<version>`**. La versión la fija el
`spring-boot-starter-parent`. Comprobarlo:

```bash
./mvnw -pl apps/petclinic-api dependency:tree -Dincludes=org.springframework:spring-web
```

Pregunta de clase: *¿qué pasa si necesito una versión distinta de una librería que gestiona
Spring Boot?* Respuesta: se sobrescribe la propiedad, y hay un ejemplo real en el `pom.xml`
raíz — `<flyway.version>`, subida porque la que trae Spring Boot 3.5.16 todavía no conoce
MySQL 8.4.

### 0:35 – 0:55 · Inversión de control e inyección de dependencias

El proyecto tiene los dos estilos conviviendo, y no es casualidad:

```java
// service/PetService.java — por constructor
public PetService(PetRepository petRepository, VisitRepository visitRepository) { ... }

// service/VisitService.java — por campo
@Autowired
private VisitRepository visitRepository;
```

Preguntar cuál es mejor y **dejar que lo discutan** antes de dar la respuesta. Después
abrir `src/test/java/.../testingexamples/spring/OwnerServiceIsolatedMockTests.java`:

```java
ReflectionTestUtils.setField(this.ownerService, "userService", this.userService);
```

> **Mensaje clave.** Ese `ReflectionTestUtils` es el precio de la inyección por campo. Los
> campos privados no se pueden rellenar desde fuera, así que la prueba tiene que recurrir a
> reflexión. Con inyección por constructor sería `new OwnerService(repo)`. La diferencia no
> es estética: es que una clase con constructor explícito **declara** lo que necesita.

### 0:55 – 1:05 · Descanso

### 1:05 – 1:30 · Arquitectura de la aplicación

Recorrer una petición completa de arriba abajo, con el código delante:

```
GET /api/v1/visits
   web/api/RestfulVisitSearchController   recibe y valida la petición
   service/VisitService                   caso de uso y frontera transaccional
   repository/VisitRepository             consulta
   model/Visit                            entidad
   web/api/dto/VisitResponse              lo que sale por el cable
```

Insistir en la última línea: **la entidad no sale**. Por qué, en el módulo 2.

Mostrar también la excepción a la regla, que está a propósito:
`web/api/PetRestController` sí devuelve la entidad `Pet`. Es el contraejemplo del módulo 2.

### 1:30 – 1:50 · Versiones y ciclo de vida

- Una versión menor cada seis meses, ~13 meses de soporte abierto.
- Este material usa **3.5.16**, la última de la rama 3.
- Java 21 en este curso por requisito del cliente; la línea base de 3.x y 4.x es 17.

Buscar en el código las notas de migración:

```bash
grep -rn "Spring Boot 4" apps/ infra/ --include=*.java
```

### 1:50 – 2:00 · Cierre y ejercicios

Plantear los ejercicios de [`ejercicios.md`](ejercicios.md).

## Errores frecuentes en clase

| Síntoma | Causa |
|---|---|
| "No arranca, dice que el puerto está ocupado" | Ha quedado una ejecución anterior viva. |
| "Mi IDE no encuentra las clases" | El proyecto se importa desde el `pom.xml` **raíz**, no desde el de un módulo. |
| "He añadido una dependencia y no la encuentra" | Falta recargar el proyecto Maven en el IDE. |
| Alguien con JDK 17 o 25 | `./mvnw -v` para ver qué JDK usa Maven; tiene que ser 21. |

## Material de apoyo

- [Documentación de referencia de Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/reference/)
- [Matriz de soporte de versiones](https://spring.io/projects/spring-boot#support)
