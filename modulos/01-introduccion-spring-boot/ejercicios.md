# Módulo 1. Ejercicios

Se trabaja sobre `apps/petclinic-api`. Antes de empezar:

```bash
cd curso-spring-boot-avanzado
./mvnw -pl apps/petclinic-api spring-boot:run
```

Las soluciones están en [`soluciones/`](soluciones/).

---

## Ejercicio 1 · Qué arrastra un starter

**Objetivo.** Entender que un starter es un descriptor de dependencias, no una librería.

1. Cuenta las dependencias declaradas en `apps/petclinic-api/pom.xml`.
2. Cuenta las que acaban realmente en el classpath:

   ```bash
   ./mvnw -pl apps/petclinic-api dependency:list | grep -c ":compile"
   ```

3. Averigua qué arrastra `spring-boot-starter-web`:

   ```bash
   ./mvnw -pl apps/petclinic-api dependency:tree -Dincludes=org.springframework.boot:spring-boot-starter-web
   ```

**Resultado esperado.** Unas 20 dependencias declaradas frente a más de 100 en el classpath.
`spring-boot-starter-web` trae por sí solo Spring MVC, Jackson, validación y Tomcat embebido.

**Pregunta.** ¿Por qué ninguna dependencia del `pom.xml` lleva `<version>`? ¿Dónde está?

---

## Ejercicio 2 · Quitar una autoconfiguración y ver qué se rompe

**Objetivo.** Ver la autoconfiguración en acción, quitándola.

1. Arranca con el informe de condiciones:

   ```bash
   ./mvnw -pl apps/petclinic-api spring-boot:run -Dspring-boot.run.arguments=--debug
   ```

2. Busca en la salida `CONDITIONS EVALUATION REPORT` y localiza
   `DataSourceAutoConfiguration` en *Positive matches*.
3. Desactívala temporalmente en `PetclinicApplication`:

   ```java
   @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
   ```

4. Arranca de nuevo.

**Resultado esperado.** La aplicación no arranca. El mensaje de `FAILED TO START` explica
que no hay `DataSource` configurado y sugiere cómo arreglarlo.

**Mensaje clave.** La autoconfiguración es condicional: se activa por lo que encuentra en el
classpath y por las propiedades. El informe `--debug` dice exactamente qué se activó y por qué.

**Deshaz el cambio antes de seguir.**

---

## Ejercicio 3 · De inyección por campo a inyección por constructor

**Objetivo.** Comprobar que la forma de inyectar cambia lo fácil que es probar una clase.

1. Abre `service/VisitService.java`. Usa `@Autowired` sobre el campo.
2. Intenta escribir una prueba que lo instancie sin Spring:

   ```java
   VisitService servicio = new VisitService(repositorioSimulado);  // no compila
   ```

3. Refactoriza `VisitService` para que reciba sus dependencias por constructor.
4. Comprueba que la aplicación sigue arrancando y las pruebas siguen pasando:

   ```bash
   ./mvnw -pl apps/petclinic-api test
   ```

**Resultado esperado.** Todo sigue en verde. Además, ahora la clase se puede instanciar en
una prueba sin levantar Spring ni usar reflexión.

**Pregunta.** ¿Hace falta `@Autowired` sobre el constructor? (No, desde Spring 4.3, si la
clase tiene un único constructor.)

---

## Ejercicio 4 · Añadir un bean y verlo en el contexto

**Objetivo.** Practicar `@Bean` y `@Component`.

1. Crea una clase `EstadisticasService` en `service/` anotada con `@Service`, con un método
   `long numeroDeMascotas()` que use `PetRepository`.
2. Inyéctala en un nuevo `@RestController` que exponga `GET /api/v1/estadisticas`.
3. Comprueba:

   ```bash
   curl localhost:8080/api/v1/estadisticas
   ```

4. Lista todos los beans del contexto:

   ```bash
   curl -s localhost:8080/actuator/beans | jq '.contexts.application.beans | keys | length'
   ```

**Resultado esperado.** El endpoint responde y el bean aparece en `/actuator/beans`.

Solución: [`soluciones/Ejercicio04Estadisticas.java`](soluciones/Ejercicio04Estadisticas.java)

---

## Ejercicio 5 · Situar el código en la arquitectura

**Objetivo.** Saber dónde va cada cosa.

Para cada una de estas clases, di a qué capa pertenece y por qué:

| Clase | Capa |
|---|---|
| `web/api/RestfulVisitController` | |
| `service/PetService` | |
| `repository/VisitSpecification` | |
| `model/Owner` | |
| `web/api/dto/VisitResponse` | |
| `apiclients/BillsGateway` | |
| `configuration/BillsProperties` | |

**Pregunta con trampa.** `apiclients/BillsGateway` está anotada con `@Component` y no con
`@Service`, aunque hace cosas de servicio. Busca el comentario de la clase y explica el motivo.

*(Pista: tiene que ver con `@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))`
del módulo 4.)*

---

# Ejercicios añadidos para la edición de 40 horas

Cubren las partes de las transparencias que los cinco ejercicios anteriores no tocaban:
crear un proyecto desde cero (diapositiva 14), el BOM (27), Actuator (32), los objetos que
Spring crea por nosotros (26) y los mecanismos para elegir entre varios beans (21–25).

---

## Ejercicio 6 · Una aplicación desde cero con Spring Initializr

**Objetivo.** Generar, entender y arrancar un proyecto Spring Boot sin partir de nada.
**Diapositivas:** 12–14, 28–31. **Tiempo:** 35 min.

1. Genera el proyecto. Elige **una** de las tres vías:

   - **Web:** <https://start.spring.io> con estos valores:

     | Campo | Valor |
     |---|---|
     | Project | Maven |
     | Language | Java |
     | Spring Boot | la 3.5.x si aparece; si no, la que haya (ver paso 3) |
     | Group | `com.idexaformacion.cursos` |
     | Artifact | `tareas` |
     | Packaging | Jar |
     | Java | 21 |
     | Dependencies | Spring Web, Validation, Spring Data JPA, H2 Database, Spring Boot Actuator, Spring Boot DevTools |

   - **IDE:** IntelliJ (*New Project → Spring Boot*) o VS Code (*Spring Initializr: Create a
     Maven Project*). Por debajo llaman al mismo servicio.

   - **Terminal:**

     ```bash
     curl https://start.spring.io/starter.zip \
       -d type=maven-project -d javaVersion=21 \
       -d groupId=com.idexaformacion.cursos -d artifactId=tareas -d name=tareas \
       -d packageName=com.idexaformacion.cursos.tareas \
       -d dependencies=web,validation,data-jpa,h2,actuator,devtools \
       -o tareas.zip
     ```

2. Descomprímelo **fuera** del repositorio del curso y compara su estructura con la de la
   diapositiva 14. Localiza `mvnw`, `pom.xml`, la clase `*Application` y la prueba generada.

3. Abre el `pom.xml`. Fija el `parent` en la versión del curso:

   ```xml
   <version>3.5.16</version>
   ```

   > Initializr solo ofrece las versiones con soporte abierto. Si te ha generado una 4.x, al
   > bajar a 3.5.16 algunos starters cambian de nombre (`spring-boot-starter-webmvc` pasa a
   > ser `spring-boot-starter-web`). Si te atascas, copia el `pom.xml` de
   > [`practicas/tareas`](../../practicas/tareas/pom.xml).

4. Crea la entidad `Tarea` de la diapositiva 154, pero con `LocalDate` en lugar de `Date` y
   con `jakarta.persistence` (no `javax`). Crea `TareaRepository extends JpaRepository<Tarea,
   Integer>`.

5. Crea `TareaController` en `/api/v1/tareas` con listar, buscar por id y crear. Parte del
   `PersonController` de la diapositiva 29 y **corrige sus tres defectos**: el POST no lleva
   `@RequestBody`, buscar un id inexistente acaba en un 500 por el `.get()` sobre un
   `Optional` vacío, y crear devuelve 200 en lugar de 201.

6. Arranca y prueba:

   ```bash
   ./mvnw spring-boot:run
   curl -i -X POST localhost:8080/api/v1/tareas \
        -H 'Content-Type: application/json' \
        -d '{"descripcion":"Mi primera tarea","fechaLimite":"2026-12-31"}'
   curl -s localhost:8080/api/v1/tareas | jq
   ```

7. Ejecuta la prueba que generó Initializr (`./mvnw test`) y explica qué comprueba
   realmente `contextLoads()`.

**Resultado esperado.** La aplicación arranca en menos de cinco segundos, el POST responde
`201 Created` con cabecera `Location` y el listado devuelve la tarea.

**Preguntas.**
- ¿Dónde está la base de datos? ¿Qué pasa con los datos al reiniciar?
- ¿Por qué no has tenido que escribir ni una línea de configuración de Tomcat, Jackson,
  Hibernate o del `DataSource`?

Solución completa, con pruebas: [`practicas/tareas`](../../practicas/tareas/).

---

## Ejercicio 7 · Heredar o importar el BOM

**Objetivo.** Ver las dos formas de usar un BOM y qué se pierde con cada una.
**Diapositiva:** 27. **Tiempo:** 15 min.

1. En tu proyecto `tareas`, sustituye el bloque `<parent>` por la **importación** del BOM:

   ```xml
   <dependencyManagement>
     <dependencies>
       <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-dependencies</artifactId>
         <version>3.5.16</version>
         <type>pom</type>
         <scope>import</scope>
       </dependency>
     </dependencies>
   </dependencyManagement>
   ```

2. Ejecuta `./mvnw package`. ¿Compila? ¿Se puede ejecutar el jar con `java -jar`?
3. Averigua qué tres cosas daba el `parent` que el BOM importado no da. Pista: compara

   ```bash
   ./mvnw help:effective-pom > con-bom.xml      # y lo mismo con el parent
   ```

4. Arréglalo declarando lo que falta.

**Resultado esperado.** Las **versiones** de las dependencias siguen gestionadas, pero se
pierde la configuración de plugins: el `spring-boot-maven-plugin` ya no tiene versión ni
ejecución `repackage` (el jar deja de ser ejecutable), el compilador no recibe
`-parameters` ni `java.version`, y se pierde el filtrado de recursos `@...@`.

**Pregunta.** ¿Cuándo usarías la importación? *(Cuando el proyecto ya tiene un `parent`
corporativo, como suele pasar en empresas grandes: Maven solo admite un padre.)*

> El `pom.xml` raíz de este repositorio hace **las dos cosas**: hereda de Spring Boot e
> importa los BOM de Spring Cloud, Spring Boot Admin y Testcontainers.

Solución: [`soluciones/Ejercicio07PomConBomImportado.xml`](soluciones/Ejercicio07PomConBomImportado.xml)

---

## Ejercicio 8 · Actuator: mirar dentro de una aplicación que está corriendo

**Objetivo.** Usar Actuator para diagnosticar y añadirle información propia.
**Diapositivas:** 13, 32. **Tiempo:** 30 min.

Se trabaja sobre `apps/petclinic-api`.

1. Recorre los puntos de diagnóstico y anota para qué sirve cada uno:

   ```bash
   curl -s localhost:8080/actuator | jq '._links | keys'
   curl -s localhost:8080/actuator/health | jq
   curl -s localhost:8080/actuator/metrics | jq '.names | length'
   curl -s localhost:8080/actuator/metrics/http.server.requests | jq
   curl -s localhost:8080/actuator/conditions | jq '.contexts.application.positiveMatches | keys | length'
   curl -s localhost:8080/actuator/mappings | jq '[.. | .patterns? // empty] | flatten | length'
   ```

2. Cambia el nivel de registro **sin reiniciar** (diapositiva 32):

   ```bash
   curl -X POST localhost:8080/actuator/loggers/org.hibernate.SQL \
        -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}'
   curl -s localhost:8080/api/v1/visits > /dev/null     # mira la consola
   curl -X POST localhost:8080/actuator/loggers/org.hibernate.SQL \
        -H 'Content-Type: application/json' -d '{"configuredLevel":null}'
   ```

3. Crea un `HealthIndicator` propio, `FacturasHealthIndicator`, que compruebe si responde el
   servicio de facturas (`petclinic.bills.base-url`). Arranca **sin** el servicio de facturas y
   mira `/actuator/health`.
4. Lo que acabas de ver es un problema: la aplicación entera aparece `DOWN` (y responde con
   HTTP 503) porque una dependencia externa no responde, aunque ella esté perfectamente.
   Separa las preguntas con **grupos**:

   ```properties
   # Sondas de Kubernetes: /actuator/health/liveness y /actuator/health/readiness
   management.endpoint.health.probes.enabled=true
   # Un grupo propio para las dependencias externas
   management.endpoint.health.group.externos.include=facturas
   management.endpoint.health.group.externos.show-details=always
   ```

   Arranca de nuevo sin facturas y compara las cuatro respuestas:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/actuator/health
   curl -s localhost:8080/actuator/health/liveness
   curl -s localhost:8080/actuator/health/readiness
   curl -s localhost:8080/actuator/health/externos | jq
   ```

5. Añade un `InfoContributor` que publique el número de mascotas registradas, y la
   información de compilación con la meta `build-info` del `spring-boot-maven-plugin`.

**Resultado esperado.** El `health` general da 503, pero `liveness` y `readiness` siguen en
`UP`: la aplicación está viva y lista aunque facturas no lo esté. `/actuator/health/externos`
muestra el detalle de facturas. `/actuator/info` incluye `build.version`, `build.time` y
`clinica.mascotas`.

**Pregunta.** En Kubernetes, ¿qué pasaría si la sonda de *liveness* apuntara a un `health`
que depende de facturas? *(El orquestador reiniciaría la aplicación cada vez que facturas se
cayera, y no arreglaría nada. Para eso existen los grupos `liveness` y `readiness`.)*

Solución: [`soluciones/Ejercicio08Actuator.java`](soluciones/Ejercicio08Actuator.java)

---

## Ejercicio 9 · Los objetos que Spring crea por ti

**Objetivo.** Ver los *proxies* de la diapositiva 26 y escribir un aspecto.
**Diapositiva:** 26. **Tiempo:** 25 min.

1. Añade a `PetclinicApplication` un `CommandLineRunner` que imprima la clase real de tres
   beans:

   ```java
   @Bean
   CommandLineRunner verProxies(PetService pets, VisitService visits, OwnerRepository owners) {
       return args -> {
           System.out.println("PetService      -> " + pets.getClass());
           System.out.println("VisitService    -> " + visits.getClass());
           System.out.println("OwnerRepository -> " + owners.getClass());
       };
   }
   ```

2. Arranca y explica cada línea. ¿Por qué `PetService` aparece con `$$SpringCGLIB$$` y el
   repositorio como `jdk.proxy...`?
3. Escribe un aspecto que mida cuánto tarda cada método público de los servicios:

   ```java
   @Aspect
   @Component
   public class MedidorDeTiempos {
       @Around("within(org.springframework.samples.petclinic.service..*)")
       public Object medir(ProceedingJoinPoint pjp) throws Throwable { ... }
   }
   ```

   Si `@Aspect` no se resuelve, añade `spring-boot-starter-aop` al `pom.xml`.
4. Navega por la aplicación y mira la consola.
5. Ahora haz que un método de `PetService` llame a otro método **público** de la misma
   clase. ¿Se mide la llamada interna?

**Resultado esperado.** Se miden las llamadas que entran desde fuera, pero **no** la
llamada interna: no pasa por el proxy. Es exactamente la misma razón por la que
`@Transactional` y `@PreAuthorize` no funcionan en autoinvocaciones (módulos 3 y 6).

Solución: [`soluciones/Ejercicio09ProxiesYAspectos.java`](soluciones/Ejercicio09ProxiesYAspectos.java)

---

## Ejercicio 10 · Dos beans del mismo tipo

**Objetivo.** Resolver la ambigüedad cuando hay varias implementaciones de una interfaz.
**Diapositivas:** 21–25. **Tiempo:** 25 min.

1. Crea en `service/notificaciones/` una interfaz y dos implementaciones:

   ```java
   public interface NotificadorCitas { String notificar(Visit visita); }

   @Component class NotificadorPorEmail implements NotificadorCitas { ... }
   @Component class NotificadorPorSms   implements NotificadorCitas { ... }
   ```

2. Inyecta `NotificadorCitas` en un `RecordatorioService`. Arranca.
3. Lee el error. Resuélvelo de **cuatro** maneras distintas, una cada vez, y anota cuándo
   tiene sentido cada una:
   1. `@Primary` en una implementación.
   2. `@Qualifier("sms")` en el punto de inyección.
   3. Inyectar `List<NotificadorCitas>` y notificar por todos los canales.
   4. `@ConditionalOnProperty(name = "petclinic.notificaciones.canal", havingValue = "sms")`
      en cada implementación, y elegir el canal en `application.properties`.
4. Con la opción 4, arranca con `--petclinic.notificaciones.canal=email` y después con
   `sms`, sin recompilar.

**Resultado esperado.** El error inicial es `NoUniqueBeanDefinitionException` y dice
exactamente qué dos beans compiten. Con la opción 4, el canal cambia sin tocar código.

**Pregunta.** ¿Cuál de las cuatro opciones es la que usa la propia autoconfiguración de
Spring Boot? *(La 4: casi todo lo que hace Spring Boot por ti es un `@Conditional...`.
Compruébalo con `/actuator/conditions`.)*

Solución: [`soluciones/Ejercicio10VariasImplementaciones.java`](soluciones/Ejercicio10VariasImplementaciones.java)
