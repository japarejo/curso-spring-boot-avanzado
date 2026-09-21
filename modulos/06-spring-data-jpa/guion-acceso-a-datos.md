# Guion didactico: acceso a datos con Spring Boot, JPA, Hibernate y transacciones

Duracion: 3 sesiones de 2 horas.

Estructura recomendada de cada sesion:

- 55 minutos de clase y demo.
- 10 minutos de descanso.
- 45 minutos de practica guiada.
- 10 minutos de cierre, dudas y lectura critica del codigo.

La historia del modulo es esta:

```text
1. Spring Boot arranca una aplicacion con DataSource, JPA, Hibernate y datos iniciales.
2. Modelamos el dominio con entidades, relaciones, validacion y ciclo de vida.
3. Consultamos con Spring Data JPA, JPQL, query methods y Specifications.
4. Controlamos el coste de las consultas: lazy/eager, fetch join, N+1 y paginacion.
5. Definimos limites transaccionales en servicios, no en controladores.
6. Probamos persistencia con @DataJpaTest, H2, MySQL y Testcontainers.
```

## Objetivos de aprendizaje

Al terminar, el alumno debe poder explicar y demostrar:

- Que autoconfigura Spring Boot cuando detecta `spring-boot-starter-data-jpa`.
- Que papel juega Hibernate como implementacion de JPA.
- Diferenciar `DataSource`, persistence unit, `EntityManager` y persistence context.
- Como se traducen entidades y relaciones a tablas, claves foraneas y consultas SQL.
- Diferencias practicas entre query method, JPQL con `@Query` y `Specification`.
- Que problema resuelven `fetch join`, `LAZY`, `EAGER` y `@Transactional(readOnly = true)`.
- Donde poner las transacciones y por que el servicio suele ser la frontera correcta.
- Que significa rollback por excepcion checked/no checked y como se configura.
- Como probar repositorios con `@DataJpaTest`, H2 y Testcontainers.
- Que riesgos aparecen al mezclar persistencia, serializacion JSON y vistas web.

## Mapa del material del proyecto

Usaremos principalmente la aplicacion porque concentra mejor el modulo de datos.

| Pieza | Ruta | Uso didactico |
|---|---|---|
| Configuracion JPA | `apps/petclinic-api/src/main/resources/application.properties` | H2, `ddl-auto`, SQL visible, inicializacion |
| Datos iniciales | `apps/petclinic-api/src/main/resources/data.sql` | Dataset estable para demos y tests |
| Entidades base | `model/BaseEntity.java`, `model/NamedEntity.java` | `@Id`, `@GeneratedValue`, `@Version`, herencia JPA |
| Modelo principal | `model/Owner.java`, `model/Pet.java`, `model/Visit.java`, `model/Diagnose.java` | Relaciones, cascadas, lazy/eager |
| Repositorios | `repository/OwnerRepository.java`, `PetRepository.java`, `VisitRepository.java` | Spring Data JPA, JPQL, Specifications |
| Consultas dinamicas | `repository/PetSpecification.java`, `VisitSpecification.java` | Criteria API sin escribir SQL |
| Servicios | `service/OwnerService.java`, `PetService.java`, `VisitService.java` | Frontera transaccional |
| Auditoria | `model/AuditableEntity.java`, `configuration/JpaAuditingConfiguration.java` | `@CreatedDate`, `@CreatedBy`, listeners |
| Tests JPA | `testingexamples/spring/OwnerRepositoryJpaSliceTests.java` | Slice test de repositorios |
| Tests transaccionales | `testingexamples/spring/DataJpaBeforeEachBeforeAllTransactionExampleTests.java` | Rollback en tests |
| MySQL real | `testingexamples/spring/OwnerRepositoryMySqlContainerIT.java` | Testcontainers |

## Brujula conceptual: cuatro piezas que se confunden

Usa esta seccion temprano en la primera sesion y recuperala en la tercera. Los alumnos suelen usar estos terminos como sinonimos, pero responden a preguntas distintas.

| Concepto | Pregunta que responde | Que es en la practica | Que no es |
|---|---|---|---|
| `DataSource` | A que base de datos me conecto y con que credenciales | Fabrica/pool de conexiones JDBC hacia H2, MySQL, etc. | No sabe nada de entidades JPA |
| Persistence unit | Que conjunto de entidades y configuracion JPA forman una unidad de persistencia | Configuracion JPA que produce un `EntityManagerFactory`; en Spring Boot suele autoconfigurarse sin `persistence.xml` | No es una conexion ni una transaccion |
| `EntityManager` | Con que API trabajo con entidades: persistir, buscar, eliminar, hacer queries | Fachada JPA que usa Hibernate por debajo; Spring suele inyectar un proxy ligado a la transaccion actual | No es directamente el pool de conexiones |
| Persistence context | Que entidades estan siendo gestionadas ahora | Mapa de entidades managed dentro de una unidad de trabajo; tambien se llama cache de primer nivel | No es la base de datos ni una cache compartida entre peticiones |

Relacion entre piezas:

```text
application.properties
  -> DataSource
      -> conexiones JDBC

persistence unit
  -> EntityManagerFactory
      -> EntityManager
          -> persistence context
              -> entidades managed
```

Lectura desde Spring Boot:

```text
1. Boot lee propiedades de datasource y JPA.
2. Crea un DataSource.
3. Crea un EntityManagerFactory para la persistence unit.
4. Los repositorios usan un EntityManager.
5. En una transaccion, ese EntityManager trabaja con un persistence context.
6. Hibernate sincroniza el persistence context con la base de datos al hacer flush/commit.
```

Frases utiles:

- `DataSource`: "donde esta la base de datos".
- Persistence unit: "que modelo JPA y que proveedor voy a usar".
- `EntityManager`: "la herramienta JPA con la que opero".
- Persistence context: "la memoria de entidades gestionadas durante esta unidad de trabajo".

Ejemplo con este proyecto:

| Pieza | Donde mirarla |
|---|---|
| `DataSource` | `application.properties` y dependencia `h2`/`mysql-connector-j` |
| Persistence unit | Autoconfiguracion de Boot a partir de entidades `@Entity` y propiedades `spring.jpa.*` |
| `EntityManager` | Lo usan internamente `OwnerRepository`, `PetRepository`, `VisitRepository`; aparece explicitamente como `TestEntityManager` en tests |
| Persistence context | Se ve al usar `flush()`, `clear()`, dirty checking y rollback en tests |

## Preparacion antes de clase

Compilar la aplicacion:

```powershell
.\mvnw.cmd -pl apps/petclinic-api -am package -DskipTests
```

Arrancar la aplicacion:

```powershell
.\mvnw.cmd -pl apps/petclinic-api spring-boot:run
```

URLs utiles:

```text
Aplicacion: http://localhost:8080
H2 console: http://localhost:8080/h2-console
Swagger UI: http://localhost:8080/swagger-ui.html
```

Datos de H2 habituales en Spring Boot si no se define otra cosa:

```text
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password: <vacio>
```

Tests utiles para validar el modulo:

```powershell
.\mvnw.cmd -pl apps/petclinic-api -Dtest=OwnerRepositoryJpaSliceTests test
.\mvnw.cmd -pl apps/petclinic-api -Dtest=DataJpaBeforeEachBeforeAllTransactionExampleTests test
.\mvnw.cmd -pl apps/petclinic-api -Dtest=OwnerRepositoryMySqlContainerIT test
```

Nota: el test con MySQL necesita Docker.

## Sesion 1: de Spring Boot a Hibernate, entidades y repositorios

### Mensaje de la sesion

Spring Boot no elimina JPA ni Hibernate. Los configura por nosotros. Si no entendemos que entidades, relaciones y transacciones hay debajo, solo hemos escondido la complejidad.

### 0-10 min: situar el problema

Pregunta inicial:

- "Que tiene que ocurrir desde que pulso Guardar mascota hasta que aparece una fila en `pets`?"
- "Quien abre la conexion?"
- "Quien genera el SQL?"
- "Quien decide si hay insert o update?"

Dibujo:

```text
Controller -> Service -> Repository -> EntityManager -> Hibernate -> JDBC -> H2/MySQL
```

Completa el dibujo con las cuatro piezas:

```text
DataSource ---------> conexiones JDBC ---------> H2/MySQL
Persistence unit ---> EntityManagerFactory ----> EntityManager ----> persistence context
```

Idea para verbalizar:

> El `DataSource` lleva a la base de datos. La persistence unit define el mundo JPA. El `EntityManager` es la puerta de entrada a ese mundo. El persistence context es lo que el `EntityManager` tiene gestionado ahora.

Frase util:

> Spring Data JPA nos ahorra escribir muchos repositorios, pero no nos ahorra entender el modelo relacional ni el ciclo de vida de las entidades.

### 10-25 min: autoconfiguracion de datos en Spring Boot

Muestra:

```text
apps/petclinic-api/pom.xml
apps/petclinic-api/src/main/resources/application.properties
```

Puntos clave:

| Elemento | Lectura didactica |
|---|---|
| `spring-boot-starter-data-jpa` | Activa JPA, repositorios y transacciones |
| `h2` | Base de datos en memoria para desarrollo |
| `mysql-connector-j` | Driver para cambiar de motor |
| `spring.jpa.hibernate.ddl-auto=create-drop` | Hibernate crea y borra el esquema |
| `spring.sql.init.platform=h2` | Selecciona scripts de inicializacion |
| `spring.jpa.show-sql=true` | Permite ver SQL durante la demo |

Explica la diferencia:

```text
JPA       = especificacion
Hibernate = implementacion
JDBC      = API de bajo nivel usada por el driver
H2/MySQL  = motor de base de datos
```

Anade aqui la distincion de infraestructura:

```text
DataSource             = fabrica/pool de conexiones JDBC.
Persistence unit       = configuracion JPA del modelo persistente.
EntityManagerFactory   = fabrica creada para esa persistence unit.
EntityManager          = API JPA usada por repositorios y queries.
Persistence context    = conjunto de entidades managed por un EntityManager.
```

En Spring Boot moderno normalmente no escribimos un `persistence.xml`. Boot crea la persistence unit a partir del classpath, las entidades encontradas, el `DataSource` y las propiedades `spring.jpa.*`.

### 25-45 min: entidades y mapeo

Muestra:

```text
model/BaseEntity.java
model/NamedEntity.java
model/Owner.java
model/Pet.java
model/Visit.java
```

Recorrido sugerido:

1. `BaseEntity`: `@Id`, `@GeneratedValue(strategy = IDENTITY)`, `@Version`.
2. `NamedEntity`: `@MappedSuperclass` y columna `name`.
3. `Owner`: `@OneToMany(mappedBy = "owner")`.
4. `Pet`: `@ManyToOne` hacia `Owner` y `PetType`, `@OneToMany` hacia `Visit`.
5. `Visit`: `@ManyToOne` hacia `Pet` y `@OneToOne` hacia `Diagnose`.

Preguntas:

- "Donde esta la clave foranea real entre `owners` y `pets`?"
- "Que lado es propietario de la relacion?"
- "Que diferencia hay entre `cascade` y `fetch`?"
- "Por que `@Version` no es una columna decorativa?"

Mensaje:

> En JPA, `mappedBy` no significa "no hay relacion"; significa "la clave foranea la gestiona el otro lado".

### 45-55 min: repositorios Spring Data

Muestra:

```text
repository/OwnerRepository.java
repository/PetRepository.java
repository/VisitRepository.java
```

Compara tres estilos:

| Estilo | Ejemplo | Cuando usarlo |
|---|---|---|
| Query method | `findByPetId(Integer petId)` | Consultas simples y legibles |
| JPQL con `@Query` | `findByLastName` con `left join fetch` | Cuando necesitas controlar joins o fetch |
| `JpaSpecificationExecutor` | `findAll(Specification, Pageable)` | Filtros dinamicos combinables |

Mini demo:

1. Arrancar la aplicacion.
2. Buscar propietarios por apellido desde la UI.
3. Observar el SQL generado.
4. Abrir H2 console y comparar tablas `owners`, `pets`, `visits`.

### 55-65 min: descanso

Deja visible el SQL de una consulta a propietarios.

### 65-95 min: practica guiada 1

Ejercicio A: leer el modelo desde la base de datos.

1. Abrir H2 console.
2. Ejecutar:

```sql
select * from owners;
select * from pets;
select * from visits;
select o.last_name, p.name, v.description
from owners o
join pets p on p.owner_id = o.id
left join visits v on v.pet_id = p.id
order by o.id, p.id, v.id;
```

3. Relacionar cada columna con una anotacion JPA del codigo.

Ejercicio B: anadir una consulta derivada.

Propuesta:

```java
Collection<Pet> findByOwnerLastName(String lastName);
```

Discusion esperada:

- Funciona por navegacion de propiedades.
- Es comoda, pero se vuelve fragil si el nombre crece demasiado.
- Puede generar joins implicitos que conviene mirar en SQL.

Ejercicio C: cambiar una relacion y observar el efecto.

1. Localizar `Pet.visits`.
2. Cambiar temporalmente `fetch = FetchType.EAGER` a `fetch = FetchType.LAZY`.
3. Repetir una pantalla o endpoint que lea visitas.
4. Observar si aparece `LazyInitializationException` o si cambia el SQL.

No dejar el cambio como solucion final sin discutirlo.

### 95-110 min: cierre

Ideas de cierre:

- Una entidad no es un DTO: vive dentro de un contexto de persistencia.
- Spring Data reduce codigo repetitivo, pero las consultas siguen existiendo.
- Las relaciones son decisiones de dominio y de rendimiento a la vez.

Pregunta final:

- "Que preferis para una pantalla de busqueda: cargar todo el grafo o decidir explicitamente que asociaciones hacen falta?"

## Sesion 2: consultas, Hibernate, fetch, Specifications y rendimiento

### Mensaje de la sesion

La pregunta no es solo "como obtengo los datos", sino "que SQL estoy obligando a generar y cuanto grafo estoy trayendo a memoria".

### 0-10 min: recuperar la sesion anterior

Repite el mapa:

```text
Repository method -> JPQL/Criteria -> SQL generado -> filas -> entidades gestionadas
```

Pregunta:

- "Si un metodo devuelve `Owner`, devuelve una fila, un objeto o un grafo?"

Respuesta buscada:

- Devuelve una entidad, pero puede disparar carga de asociaciones dependiendo del fetch y de la transaccion.

### 10-30 min: JPQL y `fetch join`

Muestra:

```java
@Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

Explica:

| Pieza | Significado |
|---|---|
| `Owner owner` | JPQL habla en entidades, no en tablas |
| `left join fetch owner.pets` | Carga la asociacion en la misma consulta |
| `DISTINCT` | Evita propietarios duplicados en el resultado de entidades |
| `LIKE :lastName%` | Parametro JPQL, no concatenacion SQL manual |

Demo:

1. Ejecutar busqueda de `Davis`.
2. Observar SQL.
3. Quitar mentalmente el `fetch` y explicar el riesgo de consultas adicionales.

Mensaje:

> `fetch join` es una decision para un caso de uso concreto. No es una configuracion global.

### 30-55 min: Specifications y Criteria API

Muestra:

```text
repository/VisitSpecification.java
service/VisitService.java
web/api/PetVisitSearchController.java
```

Explica el problema:

```text
Buscar visitas por:
- mascota
- apellido del propietario
- tipo de mascota
- descripcion
- rango de fechas
- paginacion
```

Sin Specifications, las alternativas malas son:

- Muchos metodos `findBy...And...And...`.
- Construir strings JPQL a mano.
- Meter la logica de filtros en el controlador.

Lectura guiada:

```java
return Specification.where(byPetId(petId))
    .and(byOwnerLastName(ownerLastName))
    .and(byPetName(petName))
    .and(byPetType(petType))
    .and(byDescription(description))
    .and(fromDate(fromDate))
    .and(toDate(toDate));
```

Punto importante:

> En Spring Data JPA, una `Specification` que devuelve `null` se ignora en la composicion. Eso permite filtros opcionales sin ifs en el servicio.

### 55-65 min: descanso

Deja abierta la clase `PetSpecification`.

### 65-85 min: LEFT JOIN real frente a INNER JOIN accidental

Muestra:

```text
repository/PetSpecification.java
service/PetService.java
web/api/PetVisitSearchController.java
```

Usa el comentario del propio proyecto:

```text
LEFT JOIN + WHERE v.description LIKE ...       -> se comporta como INNER JOIN
LEFT JOIN + (v.description LIKE ... OR v.id IS NULL) -> conserva mascotas sin visitas
```

Demo sugerida:

1. Buscar mascotas por descripcion de visita usando `trueLeftJoin=true`.
2. Repetir con `trueLeftJoin=false`.
3. Comparar resultados.

Si no quieres depender de Swagger, prepara dos llamadas `curl` contra el endpoint real de busqueda de mascotas/visitas que ya exponga el proyecto.

Mensaje:

> El tipo de join no se entiende mirando solo el `JOIN`. Tambien hay que mirar el `WHERE`.

### 85-105 min: practica guiada 2

Ejercicio A: ampliar `VisitSpecification`.

Anadir un filtro opcional por nombre de enfermedad del diagnostico:

```text
diagnose.disease.name
```

Condiciones:

- Si el parametro es `null` o vacio, no debe filtrar.
- Debe ser case-insensitive.
- Debe preservar visitas sin diagnostico cuando el filtro no se usa.

Discusion:

- Que join hace falta.
- Que ocurre si el filtro esta en el `WHERE`.
- Como probarlo con `@DataJpaTest`.

Ejercicio B: laboratorio N+1 y configuracion de carga.

Objetivo:

```text
Ver el antes/despues: una consulta de propietarios puede convertirse en 1 + N consultas al recorrer owner.pets.
Despues se corrige el caso de uso con fetch join, @EntityGraph o una decision EAGER bien discutida.
```

Preparacion:

1. Confirma que el SQL es visible:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

2. Si quieres mas detalle, anade temporalmente:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

Paso 1: provocar el N+1.

En `OwnerRepository`, cambia temporalmente:

```java
@Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
public Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

por:

```java
@Query("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName%")
public Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

Ejecuta:

```text
http://localhost:8080/owners
```

Lectura esperada:

```text
1 consulta carga owners.
La JSP ownersList.jsp recorre owner.pets.
Hibernate dispara consultas adicionales para resolver las mascotas de cada propietario.
```

Pregunta:

- "Donde esta el bucle que provoca el N+1: en Java, en la vista, en Hibernate o en SQL?"

Respuesta que debe quedar:

> El bucle visible esta en la vista, pero el coste aparece porque cada acceso a una coleccion lazy obliga a Hibernate a resolver una asociacion que no estaba cargada.

Paso 2: resolver con `fetch join`.

Restaura:

```java
@Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
public Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

Repite:

```text
http://localhost:8080/owners
```

Lectura esperada:

```text
Owners y pets se cargan en la consulta de busqueda.
La vista puede pintar owner.pets sin lanzar una consulta por propietario.
```

Preguntas:

- "Por que aparece `DISTINCT`?"
- "Que pasaria si la pantalla no mostrara mascotas?"
- "Estamos arreglando el modelo o el plan de carga de esta consulta?"

Mensaje:

> `fetch join` es una decision local del caso de uso. No obliga a cargar mascotas cada vez que se cargue un `Owner`.

Paso 3: resolver con `@EntityGraph`.

Como alternativa, deja la query sin `fetch` y anade un grafo de carga:

```java
@EntityGraph(attributePaths = "pets")
@Query("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName%")
public Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

Import necesario:

```java
import org.springframework.data.jpa.repository.EntityGraph;
```

Discusion:

- `fetch join` deja el plan de carga dentro de JPQL.
- `@EntityGraph` separa mejor "que filtro aplico" de "que asociaciones necesito".
- Ambas opciones son mejores que cambiar el mapping global sin pensar.

Paso 4: probar `EAGER` y discutir por que no es una solucion universal.

En `Owner`, cambia temporalmente:

```java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
private Set<Pet> pets;
```

por:

```java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "owner", fetch = FetchType.EAGER)
private Set<Pet> pets;
```

Import necesario:

```java
import jakarta.persistence.FetchType;
```

Repite la busqueda y observa el SQL.

Discusion:

- Puede evitar una carga lazy tardia.
- Puede cargar mascotas aunque el caso de uso no las necesite.
- Puede empeorar mucho si se encadenan colecciones `EAGER`.
- Puede ocultar el problema hasta que el volumen de datos crece.

Mensaje:

> `EAGER` significa "cargalo siempre con la entidad". `fetch join` y `@EntityGraph` significan "cargalo para esta consulta porque esta pantalla lo necesita".

Paso 5: comparar con `Pet.visits`.

Muestra:

```java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "pet", fetch = FetchType.EAGER)
private Set<Visit> visits;
```

Pregunta:

- "Si una pantalla lista mascotas pero no muestra visitas, que coste oculto introduce este `EAGER`?"

Resultado didactico:

```text
La configuracion de carga es parte del rendimiento del caso de uso.
Por defecto, colecciones LAZY; para pantallas concretas, fetch join, @EntityGraph, DTO projection o batch fetching.
```

Ejercicio C: paginacion y fetch join.

1. Intentar paginar una consulta con coleccion `fetch`.
2. Observar advertencias o comportamiento.
3. Discutir alternativas:

```text
consulta paginada de ids -> segunda consulta con fetch
DTO projection
@EntityGraph en consultas concretas
batch size de Hibernate
```

### 105-120 min: cierre

Tabla de cierre:

| Necesidad | Herramienta |
|---|---|
| Busqueda simple | Query method |
| Controlar exactamente asociaciones | JPQL `@Query` con `fetch join` |
| Filtros opcionales | `Specification` |
| Lectura optimizada para pantalla | DTO projection o query especifica |
| Evitar carga accidental | Revisar SQL, fetch y transaccion |

Pregunta final:

- "Que consulta del proyecto os parece inocente pero podria escalar mal con miles de filas?"

## Sesion 3: transacciones, consistencia, auditoria y testing de persistencia

### Mensaje de la sesion

Una transaccion no es una anotacion decorativa. Es el limite de consistencia de un caso de uso.

### 0-15 min: frontera transaccional en servicios

Muestra:

```text
service/OwnerService.java
service/PetService.java
service/VisitService.java
```

Ejemplo principal:

```java
@Transactional
public void saveOwner(Owner owner) {
    ownerRepository.save(owner);
    userService.saveUser(owner.getUser());
    authoritiesService.saveAuthorities(owner.getUser().getUsername(), "owner");
}
```

Pregunta:

- "Que deberia pasar si se crea el propietario pero falla la creacion de authorities?"

Respuesta:

- Debe revertirse todo el caso de uso.

Mensaje:

> La transaccion pertenece al caso de uso, no a cada llamada aislada al repositorio.

### 15-35 min: rollback, excepciones y `rollbackFor`

Muestra:

```java
@Transactional(rollbackFor = DuplicatedPetNameException.class)
public void savePet(Pet pet) throws DuplicatedPetNameException
```

Explica:

| Caso | Comportamiento por defecto |
|---|---|
| `RuntimeException` | rollback |
| `Error` | rollback |
| checked exception | no rollback salvo `rollbackFor` |

Ejercicio rapido en pizarra:

```text
1. Guardar mascota
2. Detectar nombre duplicado
3. Lanzar excepcion checked
4. Que queda en base de datos?
```

Mensaje:

> Si una excepcion checked expresa fallo de negocio que invalida el caso de uso, hay que decirle a Spring que haga rollback.

Tabla de propiedades que deben quedar claras:

| Propiedad | Pregunta que responde | Ejemplo en clase |
|---|---|---|
| `readOnly` | Este caso de uso solo lee? | `findOwnerByLastName` y `findPetById` |
| `rollbackFor` | Que excepciones checked tambien deben revertir? | `DuplicatedPetNameException` |
| `propagation` | Que pasa si ya existe una transaccion abierta? | Auditoria con `REQUIRES_NEW` frente a operacion principal |
| `isolation` | Que cambios concurrentes puede ver esta transaccion? | Lecturas sucias, no repetibles o fantasma |
| `timeout` | Cuanto puede durar antes de abortar? | Consulta o proceso de datos demasiado lento |

Frase:

> Las propiedades de `@Transactional` no son documentacion. Cambian el contrato de ejecucion del caso de uso.

### 35-55 min: contexto de persistencia y dirty checking

Antes de entrar en dirty checking, separa conceptos:

```text
EntityManager      = objeto/API con metodos como find, persist, merge, remove y createQuery.
Persistence context = estado interno asociado: que entidades estan managed, dirty o removed.
```

Un `EntityManager` puede abrir un persistence context, pero no son exactamente lo mismo. En una aplicacion Spring tipica, el `EntityManager` que reciben los repositorios es un proxy; Spring lo asocia al contexto transaccional activo.

Explica con una secuencia:

```text
1. Empieza @Transactional.
2. Spring vincula un EntityManager a esa transaccion.
3. Ese EntityManager tiene un persistence context.
4. Repository carga una entidad.
5. La entidad queda managed dentro del persistence context.
6. Cambiamos un atributo.
7. Al commit, Hibernate hace flush.
8. Se genera UPDATE aunque no llamemos explicitamente a save.
```

Demo sugerida:

Crear temporalmente un metodo en `OwnerService`:

```java
@Transactional
public void changeOwnerCity(int ownerId, String city) {
    Owner owner = ownerRepository.findById(ownerId);
    owner.setCity(city);
}
```

Observar SQL `update` al commit.

Despues discutir:

- Que ocurre si el metodo es `readOnly = true`.
- Que ocurre si la entidad esta detached.
- Por que llamar siempre a `save` no es la explicacion completa de JPA.
- Por que dos cargas del mismo id dentro del mismo persistence context devuelven la misma instancia Java.
- Por que `entityManager.clear()` en un test obliga a volver a leer desde base de datos.

### 55-65 min: descanso

Deja preparada la clase de tests JPA.

### 65-90 min: testing de persistencia

Muestra:

```text
testingexamples/spring/OwnerRepositoryJpaSliceTests.java
testingexamples/spring/DataJpaBeforeEachBeforeAllTransactionExampleTests.java
testingexamples/spring/OwnerRepositoryMySqlContainerIT.java
```

Explica:

| Test | Que ensena |
|---|---|
| `OwnerRepositoryJpaSliceTests` | Slice JPA rapido con H2 y rollback por test |
| `DataJpaBeforeEachBeforeAllTransactionExampleTests` | Diferencia entre datos de `@BeforeAll` y rollback de cada test |
| `OwnerRepositoryMySqlContainerIT` | Mismo repositorio contra MySQL real con Testcontainers |

Comandos:

```powershell
.\mvnw.cmd -pl apps/petclinic-api -Dtest=OwnerRepositoryJpaSliceTests test
.\mvnw.cmd -pl apps/petclinic-api -Dtest=DataJpaBeforeEachBeforeAllTransactionExampleTests test
.\mvnw.cmd -pl apps/petclinic-api -Dtest=OwnerRepositoryMySqlContainerIT test
```

Puntos didacticos:

- `@DataJpaTest` no carga toda la aplicacion.
- Por defecto, cada test es transaccional y se revierte.
- H2 es rapido, pero no siempre reproduce MySQL.
- Testcontainers reduce la distancia entre test e infraestructura real.

### 90-105 min: practica guiada 3

Ejercicio A: ver rollback de una `RuntimeException`.

Objetivo:

```text
Comprobar que la transaccion del servicio evita que el caso de uso quede a medias.
```

En `OwnerService`, introduce temporalmente:

```java
@Transactional
public void saveOwner(Owner owner) throws DataAccessException {
    ownerRepository.save(owner);
    if ("ROLLBACK".equals(owner.getLastName())) {
        throw new IllegalStateException("Fallo didactico despues de guardar owner");
    }
    userService.saveUser(owner.getUser());
    authoritiesService.saveAuthorities(owner.getUser().getUsername(), "owner");
}
```

Pasos:

1. Arrancar la aplicacion.
2. Crear un propietario desde:

```text
http://localhost:8080/owners/new
```

3. Usar `ROLLBACK` como apellido.
4. Comprobar en H2 o en los logs que no queda persistido.
5. Quitar temporalmente `@Transactional` y repetir.

Lectura esperada:

```text
Con @Transactional: rollback del caso de uso completo.
Sin @Transactional en el servicio: puede quedar una parte confirmada antes del fallo.
```

Mensaje:

> La transaccion pertenece al caso de uso. Si cada repositorio confirma por separado, la consistencia queda al azar del punto donde falle.

Ejercicio B: checked exception y `rollbackFor`.

Crear un test que demuestre que `savePet` revierte cuando hay `DuplicatedPetNameException`.

Preparacion didactica:

En `PetService.savePet`, introduce temporalmente este caso al principio:

```java
if ("ROLLBACK".equals(pet.getName())) {
    petRepository.save(pet);
    throw new DuplicatedPetNameException();
}
```

Primera pasada:

1. Quitar temporalmente `rollbackFor`:

```java
@Transactional
public void savePet(Pet pet) throws DataAccessException, DuplicatedPetNameException {
    ...
}
```

2. Crear o editar una mascota con nombre `ROLLBACK`.
3. Comprobar si el cambio queda en base de datos.

Segunda pasada:

1. Restaurar:

```java
@Transactional(rollbackFor = DuplicatedPetNameException.class)
public void savePet(Pet pet) throws DataAccessException, DuplicatedPetNameException {
    ...
}
```

2. Repetir la operacion.

Lectura esperada:

```text
RuntimeException -> rollback por defecto.
Checked exception -> no rollback por defecto.
Checked exception + rollbackFor -> rollback explicito.
```

Variante como test:

1. Cargar un propietario existente con mascotas.
2. Crear una mascota nueva con nombre duplicado.
3. Llamar a `petService.savePet`.
4. Verificar excepcion.
5. Verificar que no hay fila nueva.

Mensaje:

> Spring no puede adivinar si una checked exception es recuperable o invalida el caso de uso. Si debe revertir, se declara.

Ejercicio C: `readOnly`, dirty checking y flush.

Objetivo:

```text
Ver que dentro de una transaccion una entidad managed se sincroniza al commit aunque no se llame a save.
```

Anade temporalmente en `OwnerService`:

```java
@Transactional
public void changeOwnerCity(int ownerId, String city) {
    Owner owner = ownerRepository.findById(ownerId);
    owner.setCity(city);
}
```

Pasos:

1. Llamar al metodo desde un test o controlador temporal.
2. Observar el `update` al commit.
3. Cambiarlo a:

```java
@Transactional(readOnly = true)
public void changeOwnerCity(int ownerId, String city) {
    Owner owner = ownerRepository.findById(ownerId);
    owner.setCity(city);
}
```

4. Repetir y discutir el resultado.

Ideas que deben quedar:

- Una entidad cargada dentro del persistence context queda `managed`.
- Hibernate detecta cambios en entidades managed.
- `readOnly = true` comunica intencion y permite optimizaciones, pero no debe usarse como mecanismo de seguridad de negocio.
- Para verificar de verdad, conviene hacer `flush()` y `clear()` en tests antes de volver a leer.

Ejercicio D: propagacion y auditoria.

Planteamiento:

```text
saveOwner()
  -> guarda owner
  -> guarda user
  -> guarda authorities
  -> registra auditoria
  -> falla el caso de uso principal
```

Preguntas:

- "Debe conservarse la auditoria aunque falle el caso de uso?"
- "Debe participar en la misma transaccion?"
- "Que cambia si el metodo de auditoria usa `Propagation.REQUIRES_NEW`?"

Mini implementacion opcional:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registerAuditEvent(String message) {
    ...
}
```

Mensaje:

> `REQUIRES_NEW` no hace la operacion "mas transaccional". Abre otra transaccion independiente. Eso puede ser correcto para auditoria, pero rompe la idea de todo-o-nada del caso de uso principal.

Ejercicio E: isolation y timeout como decisiones explicitas.

No hace falta implementarlo si la clase va justa. Usalo como lectura critica:

```java
@Transactional(
    isolation = Isolation.READ_COMMITTED,
    timeout = 5
)
public void recalculateSomethingExpensive() {
    ...
}
```

Preguntas:

- "Que anomalia de concurrencia quiero evitar?"
- "Mi base de datos respeta exactamente este nivel de aislamiento?"
- "Que debe pasar si esta operacion tarda mas de 5 segundos?"

Mensaje:

> `isolation` y `timeout` son decisiones de infraestructura y consistencia. Conviene declararlas solo cuando hay un motivo real, no por costumbre.

Ejercicio F: test de optimistic locking.

Usar `@Version` de `BaseEntity`.

Escenario:

1. Cargar la misma mascota en dos contextos de persistencia separados.
2. Modificar y confirmar la primera.
3. Modificar y confirmar la segunda.
4. Esperar `ObjectOptimisticLockingFailureException` o excepcion equivalente.

Ejercicio G: auditar una entidad.

1. Hacer que una entidad concreta herede de `AuditableEntity`.
2. Exponer getters para los campos de auditoria.
3. Crear un test que verifique `createdDate` y `lastModifiedDate`.
4. Implementar `AuditorAware` real o de test para `creator` y `modifier`.

### 105-120 min: cierre

Ideas de cierre:

- La consistencia se disena en servicios.
- Hibernate sincroniza cambios en entidades gestionadas al hacer flush/commit.
- El SQL generado es parte del contrato de rendimiento.
- Los tests JPA deben cubrir tanto el modelo como las consultas importantes.

Pregunta final:

- "Que caso de uso del proyecto no deberia quedar nunca a medias en base de datos?"

## Ejercicios adicionales sugeridos

### Nivel 1: consolidacion

- Crear `findByCity(String city)` en `OwnerRepository` y testearlo con `@DataJpaTest`.
- Crear una consulta JPQL que devuelva mascotas por tipo ordenadas por nombre.
- Anadir un filtro opcional por fecha a una busqueda existente.
- Escribir un test que use `TestEntityManager.flush()` y `clear()` para demostrar que se lee de base de datos, no solo del contexto de persistencia.

### Nivel 2: rendimiento y consultas

- Reemplazar un `FetchType.EAGER` por `LAZY` y resolver el caso de uso con `fetch join`.
- Crear una proyeccion DTO para listar propietarios con numero de mascotas.
- Medir numero de consultas antes y despues de un `fetch join`.
- Implementar `@EntityGraph` como alternativa a JPQL en `OwnerRepository`.
- Anadir paginacion a la busqueda de mascotas y discutir por que no conviene paginar colecciones `fetch`.

### Nivel 3: transacciones y concurrencia

- Crear un servicio `TransferVisitService` que mueva una visita de una mascota a otra dentro de una unica transaccion.
- Simular fallo a mitad del caso de uso y verificar rollback.
- Crear un test de optimistic locking con dos transacciones.
- Probar `@Transactional(propagation = REQUIRES_NEW)` en un registro de auditoria y discutir el coste.
- Comparar comportamiento de una excepcion checked con y sin `rollbackFor`.

### Nivel 4: integracion real

- Ejecutar los mismos tests JPA contra H2 y MySQL con Testcontainers.
- Anadir un indice a una columna buscada frecuentemente y revisar el plan de ejecucion.
- Introducir Flyway o Liquibase y dejar de depender de `ddl-auto=create-drop`.
- Separar datos de demo, datos de test y migraciones de esquema.

## Cambios que haria en el proyecto para reforzar el modulo

No aplicaria todos antes de clase. Los prepararia como ramas o commits incrementales para poder ensenar el antes y el despues.

### Cambio 1: crear un paquete de ejemplos de datos

Propuesta:

```text
apps/petclinic-api/src/test/java/.../datajpa/
```

Contenido:

- Tests pequenos y autocontenidos para query methods.
- Tests de JPQL con `fetch join`.
- Tests de Specifications.
- Tests de rollback y optimistic locking.

Motivo:

> Ahora los ejemplos existen, pero estan mezclados con el bloque de testing. Para este modulo conviene tener un recorrido propio de acceso a datos.

### Cambio 2: anadir endpoints docentes solo para busquedas

Propuesta:

```text
/api/teaching/owners/search
/api/teaching/pets/search
/api/teaching/visits/search
```

Caracteristicas:

- Devuelven DTOs, no entidades.
- Permiten activar variantes: `fetchJoin=true`, `entityGraph=true`, `trueLeftJoin=true`.
- Incluyen en la respuesta metadatos didacticos: pagina, filtros aplicados, variante usada.

Motivo:

> Facilita demostrar consultas desde Swagger o curl sin depender de pantallas JSP ni de serializar entidades completas.

### Cambio 3: sustituir `FetchType.EAGER` en `Pet.visits`

Estado actual:

```java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "pet", fetch = FetchType.EAGER)
private Set<Visit> visits;
```

Propuesta:

```java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "pet")
private Set<Visit> visits;
```

Y resolver cada caso de lectura con query especifica.

Motivo:

> `EAGER` en colecciones es comodo para empezar, pero es un mal habito para un modulo avanzado. Es mejor ensenar carga explicita por caso de uso.

### Cambio 4: introducir DTO projections

Ejemplo:

```java
record OwnerSummary(Integer id, String firstName, String lastName, long petCount) {}
```

Usarlo en una pantalla/listado o endpoint de busqueda.

Motivo:

> Permite separar "modelo de persistencia" de "dato necesario para una respuesta" y evita cargar grafos completos para listados.

### Cambio 5: completar auditoria JPA

Estado actual:

- Existe `AuditableEntity`.
- Existe `@EnableJpaAuditing`.
- Falta convertirlo en una demo cerrada y verificable.

Propuesta:

- Hacer que una entidad secundaria herede de `AuditableEntity`.
- Anadir getters.
- Implementar o revisar `AuditorAwareImpl`.
- Crear test que pruebe `createdDate`, `lastModifiedDate`, `creator` y `modifier`.

Motivo:

> Es una forma natural de explicar entity listeners, ciclo de vida y contexto de seguridad.

### Cambio 6: migraciones con Flyway o Liquibase

Propuesta:

```text
src/main/resources/db/migration/V1__create_schema.sql
src/main/resources/db/migration/V2__insert_reference_data.sql
```

Y cambiar la configuracion docente:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

Motivo:

> `ddl-auto=create-drop` es util para una primera demo, pero en un curso avanzado conviene explicar por que produccion necesita migraciones versionadas.

### Cambio 7: mejorar observabilidad SQL para clase

Crear un perfil:

```text
application-jpa-teaching.properties
```

Con:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
logging.level.org.springframework.transaction=TRACE
```

Motivo:

> Evita ensuciar la configuracion normal y permite activar logs intensos solo durante el modulo.

### Cambio 8: documentar comandos de clase

Crear:

```text
docs/acceso-datos-comandos.md
```

Contenido:

- Arranque de la app.
- Acceso a H2.
- Consultas SQL de inspeccion.
- Comandos Maven de tests.
- Llamadas curl a endpoints docentes.

Motivo:

> Reduce friccion en clase y permite que los alumnos vuelvan a ejecutar la sesion despues.

## Orden recomendado de cambios

Si hay poco tiempo:

1. Crear tests docentes en paquete separado.
2. Crear perfil `jpa-teaching` con logs SQL/transacciones.
3. Anadir un endpoint docente de busqueda con DTOs.
4. Preparar una rama con `Pet.visits` en `LAZY` para demostrar antes/despues.

Si hay mas tiempo:

1. Introducir DTO projections.
2. Cerrar auditoria con test.
3. Introducir Flyway o Liquibase.
4. Preparar test de optimistic locking.

## Checklist final del profesor

Antes de dar el modulo, comprueba:

- El aplicacion compila.
- La aplicacion arranca con H2.
- H2 console permite ver `owners`, `pets`, `visits`, `diagnoses`.
- La busqueda de propietarios por `Davis` devuelve dos propietarios.
- Los logs SQL son visibles.
- `OwnerRepositoryJpaSliceTests` pasa.
- `DataJpaBeforeEachBeforeAllTransactionExampleTests` pasa.
- El test de MySQL con Testcontainers pasa si Docker esta disponible.
- Tienes preparada una rama o diff para demostrar `EAGER` frente a `LAZY`.
- Tienes preparada una rama o diff para demostrar rollback con excepcion checked.

## Solucion de problemas rapida

### No aparece H2 console

Revisar:

```properties
spring.h2.console.enabled=true
```

Y abrir:

```text
http://localhost:8080/h2-console
```

### No se ven los SQL

Revisar:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### Un test ve datos creados por otro test

Comprobar si los datos se crean en:

```text
@BeforeAll -> fuera de la transaccion del test, suelen quedar confirmados
@BeforeEach -> dentro de la transaccion del test, suelen revertirse
@Test -> dentro de la transaccion del test, suelen revertirse
```

### H2 pasa pero MySQL falla

Posibles causas:

- SQL no portable.
- Palabra reservada.
- Diferencia de tipos.
- Diferencia en constraints.
- Inicializacion distinta de esquema/datos.

Mensaje didactico:

> H2 es una herramienta rapida de feedback, no una prueba definitiva de compatibilidad con el motor real.
