# Guion docente: Testcontainers con Spring Boot

Duracion estimada: 60-90 minutos.

Objetivo: que el alumno entienda cuando H2 es suficiente, cuando conviene una base de datos real en tests y como Testcontainers permite levantar esa dependencia real de forma automatica, repetible y aislada.

## Requisitos

- Docker Desktop, Rancher Desktop, Colima o un runtime compatible con Docker ejecutandose.
- Si Docker no esta disponible, estos ejemplos se omiten automaticamente gracias a `@Testcontainers(disabledWithoutDocker = true)`.
- Java 21.
- Maven wrapper del proyecto.
- Primera ejecucion con acceso a internet para descargar la imagen `mysql:8.0.36`.
- Surefire y Failsafe configurados en `apps/petclinic-api/pom.xml`.
- Dependencias de test en `apps/petclinic-api/pom.xml`:
- Version reciente de Testcontainers fijada en el modulo para evitar problemas conocidos entre Testcontainers 1.20.x y Docker Engine 29:

```xml
<testcontainers.version>2.0.5</testcontainers.version>
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-mysql</artifactId>
    <scope>test</scope>
</dependency>
```

## Idea principal

Testcontainers es una libreria de testing que arranca contenedores Docker durante los tests. En lugar de simular MySQL con H2 o depender de una base instalada a mano, el test declara la dependencia que necesita y Testcontainers la crea, espera a que este lista y la destruye al terminar.

Mensaje clave:

> No usamos Testcontainers para que todos los tests sean mas grandes. Lo usamos para los tests donde el comportamiento real de la infraestructura importa.

## Ficheros del ejemplo

- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/MySqlTestContainerSupport.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerRepositoryJpaSliceTests.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerRepositoryMySqlContainerIT.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/RestfulVisitSearchMySqlContainerIT.java`
- `src/main/resources/data.sql`

## 1. Separar pruebas rapidas e integracion

Antes de ejecutar los ejemplos, explicar la diferencia entre los dos plugins de Maven:

| Plugin | Fase Maven | Convencion de nombres | Uso en este proyecto |
| --- | --- | --- | --- |
| Surefire | `test` | `*Test`, `*Tests`, `*TestCase` | Tests unitarios, slices y feedback rapido |
| Failsafe | `integration-test` + `verify` | `*IT`, `*ITCase` | Tests de integracion con infraestructura real, como Testcontainers |

Surefire se ejecuta durante la fase `test`. Failsafe se ejecuta mas tarde, en `integration-test`, y comprueba el resultado final en `verify`. Esta separacion permite ejecutar con frecuencia los tests rapidos y reservar los tests con Docker para validaciones mas completas.

Comandos importantes:

```powershell
# Solo tests rapidos gestionados por Surefire
.\mvnw.cmd -pl apps/petclinic-api test

# Tests rapidos + tests de integracion gestionados por Failsafe
.\mvnw.cmd -pl apps/petclinic-api verify

# Ejecutar verify sin tests de integracion
.\mvnw.cmd -pl apps/petclinic-api "-DskipITs" verify
```

Mensaje clave:

> `mvn test` debe dar feedback rapido. `mvn verify` valida tambien la integracion con dependencias reales.

## 2. Empezar por el test con H2

Abrir `OwnerRepositoryJpaSliceTests`.

Explicar:

- `@DataJpaTest` carga solo la parte JPA.
- Spring Boot sustituye el `DataSource` por una base embebida.
- Es rapido, simple y muy util para la mayoria de tests de repositorio.
- Pero H2 no es MySQL: dialecto SQL, funciones, tipos, indices, locks y restricciones pueden comportarse distinto.

Ejecutar:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=OwnerRepositoryJpaSliceTests" test
```

Pregunta para clase:

> Si este test pasa con H2, garantiza que la aplicacion funcionara igual con MySQL?

Respuesta esperada: no siempre. Garantiza la logica de repositorio en un contexto JPA ligero, pero no valida todos los detalles del motor real.

## 3. Presentar el contenedor compartido

Abrir `MySqlTestContainerSupport`.

Puntos a explicar:

- `@Testcontainers` activa la extension de JUnit 5.
- `disabledWithoutDocker = true` evita que la suite completa falle en maquinas donde Docker no esta arrancado.
- `@Container` marca el contenedor que debe arrancarse para el test.
- `org.testcontainers.mysql.MySQLContainer` conoce como arrancar MySQL y como detectar que esta listo.
- `@ServiceConnection` es integracion de Spring Boot: toma host, puerto, usuario y password del contenedor y configura el `DataSource`.
- No se escribe `spring.datasource.url` a mano porque el puerto es dinamico.

Fragmento importante:

```java
@Container
@ServiceConnection
static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
    .withDatabaseName("petclinic")
    .withUsername("petclinic")
    .withPassword("petclinic");
```

Mensaje clave:

> El test no sabe en que puerto arranca MySQL. Testcontainers lo decide y Spring Boot recibe la conexion real.

## 4. Slice JPA con MySQL real

Abrir `OwnerRepositoryMySqlContainerIT`.

Comparar con `OwnerRepositoryJpaSliceTests`.

Anotaciones importantes:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OwnerRepositoryMySqlContainerIT extends MySqlTestContainerSupport
```

Explicar:

- `@DataJpaTest` mantiene el test pequeno.
- `replace = NONE` impide que Spring cambie MySQL por H2.
- Las propiedades del test hacen que Hibernate cree el esquema y `data.sql` cargue datos.
- El test `select database()` demuestra que se esta usando el contenedor.

Ejecutar:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dit.test=OwnerRepositoryMySqlContainerIT" verify
```

Explicar que se usa `-Dit.test` porque este test lo gestiona Failsafe. Para un test gestionado por Surefire se usa `-Dtest`.

Demostracion recomendada:

1. Ejecutar el test correcto.
2. Quitar temporalmente `@AutoConfigureTestDatabase(replace = NONE)`.
3. Volver a ejecutar y mostrar que Spring intenta volver al comportamiento de base embebida.
4. Restaurar la anotacion.

## 5. Integracion HTTP completa con MySQL

Abrir `RestfulVisitSearchMySqlContainerIT`.

Explicar la diferencia respecto al test anterior:

- `@SpringBootTest` carga la aplicacion completa.
- `@AutoConfigureMockMvc` permite llamar al controlador sin abrir un servidor HTTP real.
- La peticion atraviesa controlador, validacion, servicio, especificacion JPA, repositorio y MySQL.
- Es mas lento, pero cubre mas integracion.

Ejecutar:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dit.test=RestfulVisitSearchMySqlContainerIT" verify
```

Mostrar la peticion:

```java
get("/api/v1/visits")
    .param("ownerLastName", "Coleman")
    .param("petName", "Samantha")
    .param("petType", "cat")
    .param("description", "rabies")
```

Mensaje clave:

> Este test ya no comprueba solo un repositorio. Comprueba que una llamada REST real acaba consultando datos reales en MySQL.

## 6. Que ensena cada ejemplo

| Ejemplo | Tipo | Base de datos | Que demuestra |
| --- | --- | --- | --- |
| `OwnerRepositoryJpaSliceTests` | Slice JPA | H2 | Test rapido de repositorio |
| `OwnerRepositoryMySqlContainerIT` | Slice JPA de integracion | MySQL container | Misma capa, motor real |
| `RestfulVisitSearchMySqlContainerIT` | Integracion amplia | MySQL container | HTTP + Spring + JPA + MySQL |

## 7. Buenas practicas

- Usar Testcontainers en tests de integracion, no en todos los tests.
- Nombrar los tests de integracion como `*IT` para que los ejecute Failsafe en `mvn verify`.
- Mantener tests unitarios y slices con H2 cuando no importe el motor real.
- Fijar la version de la imagen Docker para evitar cambios inesperados.
- No depender de una base de datos local compartida.
- No asumir puertos fijos.
- Preferir `@ServiceConnection` en Spring Boot 3.1+.
- Crear pocos contextos de Spring distintos para que la suite no sea innecesariamente lenta.

## 8. Problemas frecuentes

Docker no esta arrancado:

- Sintoma: el test falla antes de crear el contexto de Spring.
- Solucion: iniciar Docker y repetir.

La primera ejecucion tarda:

- Sintoma: parece lento al empezar.
- Causa: descarga de la imagen `mysql:8.0.36`.
- Solucion: explicar que las siguientes ejecuciones reutilizan la imagen local.

Spring usa H2 en vez de MySQL:

- Sintoma: el test no valida el motor real.
- Solucion: revisar `@AutoConfigureTestDatabase(replace = NONE)` en tests `@DataJpaTest`.

El test con Testcontainers no se ejecuta con `mvn test`:

- Sintoma: `mvn test` termina sin arrancar Docker.
- Causa: los tests `*IT` no son de Surefire, son de Failsafe.
- Solucion: ejecutar `.\mvnw.cmd -pl apps/petclinic-api verify` o usar `-Dit.test=NombreDelIT`.

El script SQL falla solo en MySQL:

- Esto es justamente valor del test. El test ha detectado una diferencia real entre H2 y el motor de produccion.

## Cierre

Resumen para los alumnos:

> H2 da velocidad. Testcontainers da realismo. Una buena estrategia de testing usa ambos: muchos tests pequenos y algunos tests de integracion que validan la infraestructura real.
