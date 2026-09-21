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
