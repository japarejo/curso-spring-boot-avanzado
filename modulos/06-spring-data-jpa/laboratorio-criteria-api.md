# Laboratorio: Criteria API y metamodelo estático

> **Material adicional, fuera de la temporización.** El módulo 6 sigue siendo de tres
> sesiones. Este laboratorio no se imparte: se entrega. Está pensado para el alumno que
> termina antes, para quien pregunta "¿y si el filtro es dinámico?", y para llevárselo a
> casa. Si hay que tocarlo en clase, con el apartado 1 y el ejercicio 1 (unos 20 minutos)
> queda plantado el concepto.

## Por qué existe este laboratorio

En el módulo 6 aparecen tres formas de consultar con Spring Data:

| Forma | Cuándo | Dónde verlo |
|---|---|---|
| Consultas derivadas del nombre | La consulta es fija y sencilla | `OwnerRepository.findByLastName` |
| `@Query` con JPQL | La consulta es fija pero no cabe en un nombre | `OwnerRepository.findByIdWithPets` |
| `Specification` | El filtro se compone en ejecución | `repository/PetSpecification`, `VisitSpecification` |

Las `Specification` **ya son Criteria API**: por dentro reciben un `CriteriaBuilder` y
devuelven un `Predicate`. Lo que no enseñan es qué hay debajo, y por eso hay un techo claro:
una `Specification` sabe filtrar, pero no sabe **agrupar, agregar ni proyectar**. Para
`GROUP BY`, `HAVING`, `COUNT(DISTINCT ...)`, subconsultas correlacionadas o devolver un DTO
en lugar de la entidad hay que bajar a la API completa.

Aquí se baja.

## Lo que se añade al proyecto

```
apps/petclinic-api/src/main/java/.../repository/criteria/
├── VisitCriteriaRepository.java       la interfaz (el "fragmento")
├── VisitCriteriaRepositoryImpl.java   la implementación con CriteriaBuilder
├── FiltroVisitas.java                 record con los criterios, todos opcionales
└── ResumenPorTipo.java                record de salida de la agregación

apps/petclinic-api/src/test/java/.../repository/criteria/
└── VisitCriteriaRepositoryTests.java  13 pruebas, una por concepto
```

Y `VisitRepository` pasa a extender también `VisitCriteriaRepository`, de modo que las
cuatro consultas nuevas se llaman por el **mismo objeto** que las derivadas.

Comprobarlo antes de empezar:

```powershell
.\mvnw -pl apps/petclinic-api test "-Dtest=VisitCriteriaRepositoryTests"
```

## 1. El metamodelo estático: lo que hay que entender primero

Sin metamodelo, una consulta de criterios se escribe con cadenas:

```java
raiz.get("descripcion")   // no existe ese campo: se llama "description"
```

Eso compila. Falla al ejecutar, con un `IllegalArgumentException` que aparece el día del
despliegue. Y renombrar un campo de la entidad no rompe nada visible: la consulta sigue
compilando y sigue estando mal.

El metamodelo lo convierte en un error de compilación:

```java
raiz.get(Visit_.description)   // constante tipada, generada a partir de la entidad
```

`Visit_` no se escribe a mano. La genera el procesador de anotaciones
`hibernate-jpamodelgen`, declarado en `apps/petclinic-api/pom.xml`:

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
    <path>
        <groupId>org.hibernate.orm</groupId>
        <artifactId>hibernate-jpamodelgen</artifactId>
        <version>${hibernate.version}</version>
    </path>
</annotationProcessorPaths>
```

> **Trampa de configuración, y de las que cuestan una tarde.** En cuanto se declara
> `annotationProcessorPaths`, Maven deja de buscar procesadores en el classpath y usa
> **solo** los de esa lista. Si estaba Lombok y no se repite ahí, todos los `@Data` y
> `@RequiredArgsConstructor` dejan de generar código y el módulo deja de compilar con
> cientos de errores de "cannot find symbol". Por eso Lombok aparece en la lista.

Las clases se generan en `target/generated-sources/annotations/`. Verlas:

```powershell
.\mvnw -pl apps/petclinic-api compile
Get-ChildItem -Recurse apps\petclinic-api\target\generated-sources\annotations -Filter *_.java |
    Select-Object -ExpandProperty Name
```

Salen 15 clases. Abrir `Visit_.java` en el IDE y mirar dos cosas:

```java
public static volatile SingularAttribute<Visit, LocalDate> date;
public static volatile SingularAttribute<Visit, String> description;
public static volatile SingularAttribute<Visit, Pet> pet;
```

La primera: los atributos llevan el **tipo** dentro. Por eso `cb.like(...)` sobre
`Visit_.date` no compila: `like` quiere texto y ahí hay una fecha. El compilador hace de
revisor.

La segunda, mirando `Pet_.java`:

```java
public abstract class Pet_ extends org.springframework.samples.petclinic.model.NamedEntity_ {
```

El metamodelo **replica la jerarquía de las entidades**. `Pet` hereda `name` de
`NamedEntity`, así que `name` vive en `NamedEntity_`, no en `Pet_`. Se puede escribir
`Pet_.name` porque las constantes estáticas se heredan, pero el tipo real es
`SingularAttribute<NamedEntity, String>`. Funciona porque `Path<Pet>.get(...)` acepta
`SingularAttribute<? super Pet, Y>`. En el código de este laboratorio se escribe
`NamedEntity_.name` explícitamente, porque deja claro de dónde sale el atributo.

> **Si el IDE marca `Visit_` en rojo**, el código está bien: lo que pasa es que el editor no ha
> visto las fuentes generadas. Comprobar siempre primero quién tiene razón:
>
> ```powershell
> .\mvnw -pl apps/petclinic-api clean test-compile     # si dice BUILD SUCCESS, es el IDE
> ```
>
> El `pom.xml` ya trae `<m2e.apt.activation>jdt_apt</m2e.apt.activation>` para que Eclipse/STS
> y VS Code ejecuten el procesador. Si aun así sigue en rojo:
>
> - **VS Code**: `Ctrl+Shift+P` → *Java: Clean Java Language Server Workspace* → *Restart and delete*
> - **IntelliJ**: `Build > Rebuild Project`
> - **Eclipse/STS**: `Maven > Update Project` con *Force Update*
>
> Este es el precio del metamodelo y hay que decirlo en clase: cuesta un paso por máquina, y el
> síntoma cuando falta —una carpeta entera en rojo en un proyecto que compila— asusta más de lo
> que debería. Está en [`../00-preparacion-entorno.md`](../00-preparacion-entorno.md) para que
> el alumnado lo tenga a mano.

## 2. El patrón "fragmento de repositorio"

Spring Data no sabe implementar `GROUP BY` a partir de un nombre de método, pero sí sabe
**dejar un hueco** para que lo implementes tú. El patrón tiene tres piezas y una regla:

1. Una interfaz con los métodos a mano: `VisitCriteriaRepository`.
2. Una clase que la implemente: `VisitCriteriaRepositoryImpl`.
3. El repositorio principal extiende la interfaz: `VisitRepository extends ...,
   VisitCriteriaRepository`.

La regla es el nombre: la implementación **tiene** que llamarse igual que la interfaz más el
sufijo `Impl`. No es configurable por defecto. Si se llama `VisitCriteriaRepositoryImplementacion`,
Spring Data no la encuentra, intenta derivar los métodos del nombre y el arranque falla con
un mensaje que no dice nada del problema real:

```
PropertyReferenceException: No property 'buscar' found for type 'Visit'
```

Vale la pena provocarlo una vez en clase: renombrar la clase, arrancar, leer el error. Es
uno de los mensajes de Spring Data que más despista.

Lo que gana el que llama:

```java
// las dos son el mismo objeto
List<Visit> deLaMascota = visitRepository.findByPetId(7);          // derivada
List<Visit> filtradas  = visitRepository.buscar(filtro);           // Criteria
```

## 3. Las cuatro consultas, de menos a más

### Ejercicio 1 — Predicados dinámicos (nivel básico)

**Objetivo.** Entender por qué la Criteria API existe.

Mirar `VisitCriteriaRepositoryImpl.buscar(FiltroVisitas)`. El filtro tiene siete campos y
todos son opcionales, así que hay 128 combinaciones posibles. Con JPQL habría que
concatenar cadenas; con criterios se acumulan predicados en una lista.

Hay tres decisiones en ese método que conviene señalar:

```java
// 1. Los joins se declaran UNA vez, fuera de los if.
Join<Visit, Pet> mascota = visita.join(Visit_.pet, JoinType.INNER);
Join<Pet, PetType> tipo = mascota.join(Pet_.type, JoinType.LEFT);
Join<Pet, Owner> propietario = mascota.join(Pet_.owner, JoinType.LEFT);
```

Si se declaran dentro de cada `if`, cada uno crea su propio join a la misma tabla. El SQL
sale con `join pets p1_0 ... join pets p2_0 ...` y el resultado se multiplica.

```java
// 2. and() sobre una lista vacía es una conjunción cierta.
consulta.where(cb.and(predicados.toArray(Predicate[]::new)));
```

Por eso el filtro vacío devuelve todas las visitas sin ninguna rama especial.

```java
// 3. lower() en los dos lados de la comparación.
cb.equal(cb.lower(mascota.get(NamedEntity_.name)), filtro.nombreMascota().toLowerCase())
```

Sin eso el resultado depende de la intercalación (*collation*) de la base de datos: H2 es
sensible a mayúsculas por defecto y MySQL no. La misma consulta devolvería cosas distintas
en clase y en producción, y eso es peor que fallar.

**Qué hacer.**

1. Ejecutar `VisitCriteriaRepositoryTests$PredicadosDinamicos` y leer el SQL que aparece en
   la consola (`spring.jpa.show-sql=true` ya está activo).
2. Añadir un criterio nuevo al `record FiltroVisitas`: `conDiagnostico` (`Boolean`, que
   también es opcional). Traducirlo a un predicado sobre `Visit_.diagnose`.

   Ojo aquí, que tiene más miga de la que parece: `Visit.diagnose` es el lado **inverso** de
   la relación (`@OneToOne(mappedBy = "visit")`), así que la clave ajena está en la tabla
   `diagnoses`, no en `visits`. Hay que declarar un `LEFT JOIN` explícito y comprobar si su
   identificador es nulo:

   ```java
   Join<Visit, Diagnose> diagnostico = visita.join(Visit_.diagnose, JoinType.LEFT);
   // y después, según el valor del filtro:
   cb.isNotNull(diagnostico.get(BaseEntity_.id))
   ```

   Con `INNER JOIN` en lugar de `LEFT` el caso "sin diagnóstico" devolvería siempre vacío,
   porque el join ya habría descartado esas filas antes de evaluar el predicado.
3. Escribir la prueba. En `data.sql` las visitas 1 y 2 tienen diagnóstico y las 3 y 4 no, así
   que `true` debe devolver 2 y `false` las otras 2.

**Resultado esperado.** El SQL de la consulta filtrada tiene exactamente **un** `join pets`,
y añadir el criterio nuevo no obliga a tocar ninguno de los otros seis.

### Ejercicio 2 — Agregación y proyección (nivel medio)

**Objetivo.** Devolver algo que no es una entidad, y hacer que cuente la base de datos.

`resumirPorTipoDeMascota()` devuelve `List<ResumenPorTipo>`, un `record`. La pieza clave:

```java
consulta.select(cb.construct(ResumenPorTipo.class,
        tipo.get(NamedEntity_.name),
        cb.count(visita),
        cb.countDistinct(mascota)));
```

`cb.construct` hace que la consulta instancie el record directamente. La alternativa,
`multiselect()` con `Tuple`, obliga a extraer los valores por índice
(`tupla.get(0, String.class)`), que se rompe en silencio cuando alguien reordena la
selección.

**Lo que hay que preguntar en clase.** Con los datos de `data.sql` este método devuelve **una
sola fila**: `cat`. Hay seis tipos de mascota. ¿Por qué faltan cinco?

Porque la consulta parte de `Visit` y el join a `Pet` es interno: un tipo sin visitas no
aparece en ninguna fila que agrupar. Para que salgan los seis con cero visitas hay que
invertir la consulta: partir de `PetType`, hacer `LEFT JOIN` hacia las visitas y contar. Es
la diferencia entre "cuántas visitas hay por tipo" y "cuántas visitas tiene cada tipo", que
en castellano suena casi igual y en SQL no lo es.

**Qué hacer.** Escribir `resumirTodosLosTipos()` con esa segunda semántica: parte de
`Root<PetType>`, `LEFT JOIN` a `Pet_.visits` a través de las mascotas, y devuelve las seis
filas con su cuenta, incluidos los ceros.

**Resultado esperado.** Seis filas. `cat` con 4 visitas y 2 mascotas distintas; `dog`,
`lizard`, `snake`, `bird` y `hamster` con 0.

**Contraste obligatorio.** Lo mismo en JPQL cabe en tres líneas:

```java
@Query("""
    SELECT new org...ResumenPorTipo(t.name, COUNT(v), COUNT(DISTINCT p))
    FROM Visit v JOIN v.pet p JOIN p.type t
    GROUP BY t.name ORDER BY COUNT(v) DESC
    """)
List<ResumenPorTipo> resumirPorTipoConJpql();
```

Escribirlo y compararlo con las 18 líneas de la versión con criterios. La conclusión honesta
es que **para una consulta fija, `@Query` gana**. La Criteria API se paga sola cuando el
filtro es dinámico; si no lo es, solo se paga.

### Ejercicio 3 — Subconsulta correlacionada (nivel medio-alto)

**Objetivo.** Expresar "los que NO tienen", que es el caso donde las consultas derivadas se
quedan cortas.

`mascotasSinVisitas()` genera un `NOT EXISTS`. La línea que importa:

```java
Root<Pet> mascotaCorrelada = subconsulta.correlate(mascota);
subconsulta.where(cb.equal(visita.get(Visit_.pet), mascotaCorrelada));
```

`correlate()` declara que `mascota` es la fila de la consulta **externa**, no una tabla
nueva. Es el paso que se olvida siempre, y el fallo es silencioso: sin él, el `EXISTS` pasa a
significar "hay alguna visita en toda la tabla" —que es cierto—, el `NOT EXISTS` es falso
para todas las filas y el método devuelve la lista vacía. Sin excepción, sin aviso, sin log.

Para eso está la prueba `laCorrelacionEsLaQueHaceElTrabajo()`: no prueba código nuevo,
documenta la trampa. Borrar la línea del `correlate` y ver cómo cae es el ejercicio.

**Qué hacer.**

1. Comentar la línea del `correlate` y usar `mascota` directamente. Ejecutar las pruebas.
2. Comparar el SQL generado en los dos casos (buscar el `from pets` de la subconsulta).
3. Escribir el equivalente con `LEFT JOIN` + `IS NULL` (*antijoin*) y comparar planes:
   `Root<Pet>` con `LEFT JOIN` a `Pet_.visits` y `where(cb.isEmpty(mascota.get(Pet_.visits)))`.

**Resultado esperado.** 11 mascotas de 13. Con el `correlate` comentado, 0. Y en el SQL sin
correlacionar, la subconsulta tiene su propio `from pets`, que es la prueba visual del
problema.

### Ejercicio 4 — `GROUP BY` con `HAVING` (nivel medio)

**Objetivo.** Distinguir `WHERE` de `HAVING`, que es una confusión muy común.

```java
consulta.groupBy(propietario.get(Person_.lastName));
consulta.having(cb.ge(cb.count(visita), (long) minimo));
```

`WHERE` filtra **filas**, antes de agrupar. `HAVING` filtra **grupos**, después. Poner
`cb.count(...)` en el `where` no es una cuestión de estilo: es SQL inválido, porque en ese
momento el grupo todavía no existe.

**Qué hacer.**

1. Mover la condición del `having` al `where` y leer el error que devuelve la base de datos.
2. Escribir una variante que además filtre por rango de fechas: el rango va en el `where`
   (filtra visitas) y el mínimo en el `having` (filtra propietarios). Las dos cosas en la
   misma consulta.
3. Agrupar por apellido tiene un defecto de diseño: dos propietarios distintos con el mismo
   apellido se cuentan juntos. En `data.sql` hay dos `Davis`. Corregirlo agrupando por
   identificador y proyectando un record `ResumenPorPropietario(Integer id, String nombre,
   String apellido, long visitas)`.

**Resultado esperado.** `propietariosConAlMenos(4)` devuelve `["Coleman"]`; con 5, la lista
vacía. La variante agrupada por identificador devuelve el mismo resultado con estos datos,
pero deja de ser una bomba de relojería.

### Ejercicio 5 — De `Specification` a Criteria y vuelta (nivel alto)

**Objetivo.** Ver que son la misma cosa y elegir con criterio.

Abrir `repository/VisitSpecification`. Una `Specification<T>` es una interfaz funcional:

```java
Predicate toPredicate(Root<T> raiz, CriteriaQuery<?> consulta, CriteriaBuilder cb);
```

Esos son exactamente los tres objetos con los que trabaja el `Impl` de este laboratorio.

**Qué hacer.**

1. Reescribir los seis predicados simples de `buscar(...)` como `Specification<Visit>`
   independientes y combinarlas con `Specification.allOf(...)`. `VisitRepository` ya extiende
   `JpaSpecificationExecutor`, así que `findAll(spec, Sort.by(...))` funciona sin tocar nada.
2. Intentar hacer lo mismo con `resumirPorTipoDeMascota()`. **No se puede**: una
   `Specification` devuelve un `Predicate` y no puede cambiar la proyección ni añadir un
   `groupBy` al `CriteriaQuery` sin efectos raros. Ese límite es la respuesta a "¿y entonces
   para qué quiero la Criteria API si tengo Specifications?".
3. Decidir, por escrito, qué se queda como `Specification` y qué como fragmento. Argumentar.

**Resultado esperado.** Los filtros, como `Specification`: son reutilizables, componibles y
se leen mejor. Las agregaciones y subconsultas, en el fragmento. Y las consultas fijas,
en `@Query`. Las tres conviven en `VisitRepository`, y eso no es incoherencia: es cada
herramienta en su sitio.

### Ejercicio 6 — Paginación y ordenación dinámica (nivel alto)

**Objetivo.** Lo que hace falta de verdad para exponer esto en una API REST.

`buscar(...)` devuelve una `List` completa. Para un endpoint real hacen falta `Pageable` y
`Page`, y eso con Criteria API hay que montarlo a mano: **dos** consultas, una de datos y
otra de recuento.

**Qué hacer.** Escribir `Page<Visit> buscar(FiltroVisitas filtro, Pageable pageable)`:

1. Extraer la construcción de predicados a un método privado que reciba `CriteriaQuery<?>`,
   `Root<Visit>` y los joins, para poder reutilizarlo en las dos consultas.
2. Consulta de datos: aplicar `setFirstResult(...)` y `setMaxResults(...)`, y traducir el
   `Sort` del `Pageable` a `orderBy`. **Validar los campos de ordenación contra una lista
   blanca**: aceptar un nombre arbitrario del cliente es el mismo riesgo que concatenar SQL.
   Ya hay un ejemplo de lista blanca en `web/api/RestfulVisitSearchController`.
3. Consulta de recuento: `CriteriaQuery<Long>` con `cb.count(raiz)`, los mismos predicados y
   **sin** `orderBy` (ordenar un recuento es trabajo tirado, y algunas bases de datos lo
   rechazan).
4. Devolver `new PageImpl<>(contenido, pageable, total)`.
5. Exponerlo en `RestfulVisitSearchController` como `GET /api/v1/visits/buscar`.

**Resultado esperado.** Dos consultas en el log por cada petición: un `select ... limit ?
offset ?` y un `select count(...)`. Y un 400, no un 500, cuando el cliente pide
`sort=passwordHash`.

## 4. Cuándo NO usar la Criteria API

Es la parte que casi nunca se dice, y la que evita que el alumno vuelva al trabajo y llene el
proyecto de `CriteriaBuilder`:

| Situación | Usar |
|---|---|
| Consulta fija, por sencilla que sea | Método derivado |
| Consulta fija y complicada | `@Query` con JPQL |
| Filtro dinámico de predicados | `Specification` |
| Agregación, `HAVING`, subconsulta, proyección a DTO | Criteria API (este laboratorio) |
| SQL específico del motor, funciones de ventana, `CTE` | SQL nativo con `JdbcClient` |

Los tres argumentos en contra, para tenerlos a mano:

1. **Verbosidad.** De 3 líneas de JPQL a 18 de criterios, para el mismo SQL.
2. **Se lee peor.** Una consulta JPQL se entiende de un vistazo; un árbol de criterios hay que
   ejecutarlo mentalmente. En una revisión de código eso cuesta.
3. **Depende del metamodelo generado**, y eso es un paso de configuración por máquina y por
   IDE. Sin él la seguridad de tipos desaparece y solo queda la verbosidad.

A cambio, dos garantías que ninguna otra opción da: el filtro dinámico sin concatenar
cadenas, y que renombrar un campo de la entidad **rompe la compilación** en lugar de romper
la producción.

## Comandos (PowerShell, Windows)

```powershell
# Generar el metamodelo y ver las clases
.\mvnw -pl apps/petclinic-api compile
Get-ChildItem -Recurse apps\petclinic-api\target\generated-sources\annotations -Filter *_.java

# Solo las pruebas de este laboratorio
.\mvnw -pl apps/petclinic-api test "-Dtest=VisitCriteriaRepositoryTests"

# Un bloque concreto (clases anidadas: con $)
.\mvnw -pl apps/petclinic-api test "-Dtest=VisitCriteriaRepositoryTests`$Subconsulta"

# Ver el SQL con formato mientras se ejecutan
.\mvnw -pl apps/petclinic-api test "-Dtest=VisitCriteriaRepositoryTests" `
  "-Dspring.jpa.properties.hibernate.format_sql=true"
```

> En PowerShell los argumentos con `-D` van **entre comillas** o la shell se los come, y el
> `$` de las clases anidadas necesita el acento grave delante para escaparlo. La lista
> completa de equivalencias está en [`../00-comandos-windows.md`](../00-comandos-windows.md).

## Resumen para llevarse

1. El metamodelo estático convierte errores de ejecución en errores de compilación. Cuesta un
   procesador de anotaciones, y si ya hay Lombok, cuesta acordarse de repetirlo.
2. El sufijo `Impl` no es convención: es la regla que usa Spring Data para encontrar la
   implementación.
3. `cb.and()` sin argumentos es cierto. Eso elimina el caso especial "sin filtros".
4. Los joins se declaran una vez. Declararlos dentro de los `if` multiplica las filas.
5. `correlate()` en las subconsultas. Olvidarlo no da error: da resultados vacíos.
6. `WHERE` filtra filas, `HAVING` filtra grupos.
7. Para una consulta fija, `@Query` se lee mejor. La Criteria API se paga cuando el filtro
   es dinámico.
