# Módulo 6. Ejercicios

Los ejercicios de transacciones están en [`guion-transacciones.md`](guion-transacciones.md)
y los de consultas N+1 en [`guion-consultas-n-mas-1.md`](guion-consultas-n-mas-1.md).
Estos cubren migraciones, proyecciones y consultas dinámicas. Soluciones de referencia en
[`soluciones/`](soluciones/).

Y, como material adicional fuera de la temporización, dos laboratorios con código que ya
funciona y pruebas que lo demuestran:

- [`laboratorio-criteria-api.md`](laboratorio-criteria-api.md) — metamodelo estático,
  agregaciones, subconsultas correlacionadas, `HAVING`, paginación dinámica.
- [`laboratorio-transacciones-avanzadas.md`](laboratorio-transacciones-avanzadas.md) —
  autoinvocación, actualización perdida, bloqueo optimista y pesimista, `NESTED`, eventos
  ligados a la transacción.

En Windows, los comandos traducidos a PowerShell: [`../00-comandos-windows.md`](../00-comandos-windows.md).

---

## Ejercicio 1 · Las dos fuentes del esquema

**Objetivo.** Ver el precio de mantener dos mecanismos y decidir qué hacer.

El proyecto crea el esquema de dos maneras: con H2 lo genera Hibernate y los datos vienen de
`data.sql`; con MySQL lo aplica Flyway desde `db/migration/`.

1. Añade una columna a una entidad, por ejemplo `Pet.microchip`:

   ```java
   @Column(length = 20)
   private String microchip;
   ```

2. Arranca con H2 (sin perfil). Funciona: Hibernate regenera el esquema.
3. Arranca con MySQL:

   ```bash
   docker compose -f docker/compose.yaml up -d mysql
   ./mvnw -pl apps/petclinic-api spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

4. Anota el error.
5. Corrígelo añadiendo `V3__anade_microchip_a_pet.sql`:

   ```sql
   ALTER TABLE pets ADD COLUMN microchip VARCHAR(20);
   ```

6. Arranca de nuevo y comprueba el historial:

   ```bash
   docker exec -it curso-spring-boot-avanzado-mysql-1 \
     mysql -upetclinic -ppetclinic petclinic -e "SELECT version, description, success FROM flyway_schema_history;"
   ```

**Resultado esperado.** En el paso 4, `ddl-auto=validate` aborta el arranque diciendo que
falta la columna. Es exactamente para lo que está.

**Pregunta de diseño.** ¿Unificarías los dos mecanismos usando Flyway también con H2? ¿Qué
ganas y qué pierdes? *(Ganas una sola fuente de verdad; pierdes arranque instantáneo en las
pruebas y tienes que escribir SQL compatible con los dos gestores.)*

---

## Ejercicio 2 · Una migración no se edita

**Objetivo.** Entender por qué las migraciones son inmutables.

1. Con la base ya migrada, cambia una línea cualquiera de `V1__esquema_inicial.sql`.
2. Arranca con el perfil `mysql`.
3. Lee el error.
4. Deshaz el cambio y comprueba que vuelve a arrancar.

**Resultado esperado.** Flyway aborta con un error de *checksum mismatch*: guarda el hash de
cada migración aplicada y detecta que una ya aplicada ha cambiado.

> **Mensaje clave.** Un error en una migración ya aplicada no se corrige editándola: se
> corrige añadiendo otra. Si se pudiera editar, dos entornos con el mismo número de versión
> tendrían esquemas distintos.

---

## Ejercicio 3 · Proyecciones

**Objetivo.** Dejar de cargar entidades completas para mostrar dos campos.

1. Activa el registro de SQL y pide el listado de propietarios. Observa cuántas columnas
   trae la consulta.
2. Define una proyección cerrada en `OwnerRepository`:

   ```java
   interface ResumenOwner {
       Integer getId();
       String getFirstName();
       String getLastName();
   }

   List<ResumenOwner> findByCity(String city);
   ```

3. Expón un endpoint que la use y comprueba el SQL generado.
4. Prueba también una proyección basada en `record`.

**Resultado esperado.** La consulta selecciona solo las tres columnas, no la entidad entera.

**Pregunta.** ¿En qué se diferencia una proyección de un DTO del módulo 2? *(La proyección
condiciona la CONSULTA; el DTO condiciona la RESPUESTA. Se pueden usar juntos.)*

Solución: [`soluciones/Ejercicio03Proyecciones.java`](soluciones/Ejercicio03Proyecciones.java),
con las cuatro formas de proyectar y el SQL que genera cada una.

---

## Ejercicio 4 · Consultas dinámicas con Specification

**Objetivo.** Componer criterios en tiempo de ejecución sin concatenar texto.

1. Lee `repository/VisitSpecification.java`: siete predicados y un combinador que tolera
   nulos.
2. Prueba la búsqueda con distintas combinaciones:

   ```bash
   curl -s "localhost:8080/api/v1/visits?petName=Leo" | jq '.page.totalElements'
   curl -s "localhost:8080/api/v1/visits?fromDate=2013-01-01&toDate=2013-01-03" | jq '.page.totalElements'
   ```

3. Añade un predicado nuevo: filtrar por ciudad del propietario, navegando
   `visit.pet.owner.city`.
4. Expónlo como parámetro y escribe una prueba.

**Pregunta.** ¿Por qué esto es preferible a construir la consulta concatenando cadenas?
*(Además de la legibilidad: la API de criterios no permite inyección SQL.)*

Solución: [`soluciones/Ejercicio04CiudadPropietario.java`](soluciones/Ejercicio04CiudadPropietario.java).

Y si te ha sabido a poco, la continuación natural es el
[laboratorio de Criteria API](laboratorio-criteria-api.md): lo mismo pero con agregaciones,
subconsultas y proyecciones, que es donde una `Specification` ya no llega.

---

## Ejercicio 5 · Claves primarias

**Objetivo.** Elegir estrategia de generación con criterio.

1. `model/BaseEntity` usa `GenerationType.IDENTITY`. Localízalo.
2. `model/User` usa una **clave natural** (`@Id String username`). Compáralos.
3. Activa el registro de SQL y crea varias entidades en el mismo método. Observa que con
   `IDENTITY` Hibernate ejecuta un `INSERT` inmediato por cada una: no puede agrupar.
4. Cambia `BaseEntity` a `SEQUENCE` con `allocationSize = 50` y repite.

**Resultado esperado.** Con `SEQUENCE` y reserva de bloques, Hibernate agrupa las
inserciones. Con `IDENTITY` no puede, porque necesita el identificador que genera la base de
datos en cada fila.

**Pregunta.** ¿Cuándo es aceptable una clave natural como la de `User`? *(Cuando es
inmutable y única de verdad. Un `username` que se pueda cambiar es un problema: hay que
actualizar todas las claves ajenas.)*

---

## Ejercicio 6 · Auditoría

**Objetivo.** Ver la auditoría de JPA de principio a fin.

1. Lee `model/AuditableEntity`, `configuration/JpaAuditingConfiguration` y
   `configuration/AuditorAwareImpl`.
2. Autentícate y crea un pago:

   ```bash
   curl -s localhost:8080/payments/random/new | jq
   ```

3. Consulta la tabla `payment` y comprueba `created_by` y `created_date`.
4. Modifica el pago y comprueba `last_modified_by`.
5. Ahora provoca una escritura **sin** usuario autenticado. ¿Qué se guarda?

**Resultado esperado.** Sin autenticación, `AuditorAwareImpl` devuelve `Optional.empty()` y
los campos quedan a null. Es el comportamiento correcto, y es el arreglo de un
`NullPointerException` que tenía el proyecto anterior.

---

## Ejercicio 7 · Escritura por lotes

**Objetivo.** Reducir viajes a la base de datos.

1. Escribe un método que inserte 1000 mascotas y mide cuánto tarda.
2. Activa el proceso por lotes:

   ```properties
   spring.jpa.properties.hibernate.jdbc.batch_size=50
   spring.jpa.properties.hibernate.order_inserts=true
   spring.jpa.properties.hibernate.generate_statistics=true
   ```

3. Repite y compara las estadísticas de Hibernate.

**Aviso.** Si la entidad usa `GenerationType.IDENTITY`, el proceso por lotes **no funciona**
para las inserciones. Es la continuación natural del ejercicio 5.

Solución: [`soluciones/Ejercicio07EscrituraPorLotes.java`](soluciones/Ejercicio07EscrituraPorLotes.java),
con las dos versiones, cómo medirlas y por qué `IDENTITY` lo impide.

---

# Ejercicios añadidos para la edición de 40 horas

Cubren lo que las transparencias explican y los siete ejercicios anteriores no practicaban:
todos los tipos de relación y la herencia (diapositivas 154–158 y 167–170), la conexión y el
*pool* (152, 165), las consultas de Spring Data (159–164), el contexto de persistencia y las
transacciones programáticas (151–153), y la medición del problema N+1 (171–174).

---

## Ejercicio 8 · Laboratorio de mapeos: romper cada relación a propósito

**Objetivo.** Saber qué tablas y columnas produce cada anotación, y qué pasa cuando falta una.
**Diapositivas:** 154–158, 166–170. **Tiempo:** 50 min.

Se trabaja en [`practicas/mapeos-jpa`](../../practicas/mapeos-jpa/), un proyecto aparte con una
entidad (o pareja) por diapositiva y una prueba por relación.

1. Ejecuta las pruebas y abre el DDL generado:

   ```bash
   cd practicas/mapeos-jpa
   ./mvnw test                      # Windows: .\mvnw test
   ```

   Abre `target/ddl-generado.sql`. Para cada `create table`, di de qué entidad sale y por qué
   tiene esas columnas.

2. Ahora rompe cada mapeo, **uno cada vez**. Antes de ejecutar, escribe qué crees que va a
   pasar; después ejecuta, mira el DDL y la consola, y deshaz el cambio.

   | # | Cambio | Pregunta |
   |---|---|---|
   | a | Quita `mappedBy` de `Departamento.empleados` | ¿Cuántas formas de relacionar departamento y empleado hay ahora? ¿Qué prueba falla? |
   | b | Quita `mappedBy` de `Tarea.desarrolladores` | ¿Cuántas tablas de unión aparecen? |
   | c | En `Departamento.contratar`, quita la línea `empleado.setDepartamento(this)` | ¿Qué valor queda en `empleado.departamento_id`? ¿Por qué falla la prueba? |
   | d | Cambia la herencia de `Trabajador` a `JOINED` y luego a `TABLE_PER_CLASS` | Compara el DDL y la consulta polimórfica de la consola: ¿JOIN, UNION o nada? |
   | e | Quita los dos `@JoinTable` de `Articulo` | ¿Hay colisión, como dice la diapositiva 167? ¿Por qué no? (Lee el Javadoc de `Articulo`) |
   | f | Repite (e) con `spring.jpa.hibernate.naming.implicit-strategy=org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl` | ¿Y ahora? |
   | g | Cambia `allocationSize = 50` a `1` en `Factura` | ¿Cuántos `next value for factura_seq` ves en la consola? |
   | h | Quita `em.clear()` de `escribirYOlvidar()` y repite (c) | ¿Por qué ahora la prueba **pasa** aunque la clave ajena no se haya guardado? |

3. Añade tú una relación nueva: un `Empleado` tiene un **jefe**, que también es un
   `Empleado`, y un jefe tiene varios subordinados. Escribe la prueba primero.

**Resultado esperado.** Una tabla con las ocho respuestas. La (h) es la más importante:
sin vaciar el contexto de persistencia, la prueba lee los objetos de **memoria**, no de la
base de datos, y no detecta nada.

> **Mensaje clave.** En una relación bidireccional, JPA **solo escribe lo que hay en el
> lado propietario**. Mantener los dos lados sincronizados es trabajo del código: para eso
> están los métodos `contratar`, `asignar`, `facturar`…

**Erratas de las transparencias que el laboratorio corrige** (cada una está explicada en el
Javadoc de la clase): columna duplicada en la 155, falta de `mappedBy` en la 156, subclases
sin `@Entity` en la 158, `joinColumns` e `inverseJoinColumns` invertidos en la 168.

---

## Ejercicio 9 · La conexión y el *pool*

**Objetivo.** Configurar el `DataSource` y ver qué pasa cuando se agotan las conexiones.
**Diapositivas:** 151–152, 165. **Tiempo:** 30 min.

1. Mira qué ha configurado Spring Boot sin que nadie se lo pida:

   ```bash
   curl -s localhost:8080/actuator/configprops | jq '.. | objects | select(has("spring.datasource.hikari"))? // empty' | head
   curl -s localhost:8080/actuator/metrics/hikaricp.connections.max | jq '.measurements'
   ```

   ¿Qué implementación de *pool* se usa? ¿Cuántas conexiones como máximo?

2. Compara con la diapositiva 165: ¿hace falta `spring.datasource.driver-class-name`? ¿Y
   `@EnableJpaRepositories`? *(No: el *driver* se deduce de la URL y los repositorios se
   activan solos.)*

3. Reduce el *pool* a dos conexiones con espera corta:

   ```properties
   spring.datasource.hikari.maximum-pool-size=2
   spring.datasource.hikari.connection-timeout=1000
   spring.datasource.hikari.leak-detection-threshold=2000
   ```

4. Añade el endpoint de demostración de la solución (`/api/v1/demo/pool/ocupar?ms=3000`), que
   abre una transacción y la mantiene viva.
5. Lanza cinco peticiones a la vez:

   ```bash
   seq 1 5 | xargs -P 5 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
       "localhost:8080/api/v1/demo/pool/ocupar?ms=3000"
   ```

   En PowerShell: `1..5 | ForEach-Object -Parallel { curl.exe -s -o NUL -w "%{http_code}`n" "http://localhost:8080/api/v1/demo/pool/ocupar?ms=3000" }`.

6. Lee la consola: busca `Connection is not available, request timed out after 1000ms` y el
   aviso de `leak-detection`.

**Resultado esperado.** Dos peticiones responden 200 a los 3 s; las otras tres fallan al
segundo. El *pool* es un recurso **compartido y limitado**: una transacción larga lo bloquea
para todos.

**Pregunta.** ¿Por qué no se arregla simplemente con `maximum-pool-size=500`? *(Porque la base
de datos también tiene un límite de conexiones, y cada una consume memoria en el servidor. La
solución es que las transacciones sean cortas: nada de llamadas HTTP ni esperas dentro de un
`@Transactional`.)*

Solución: [`soluciones/Ejercicio09Pool.java`](soluciones/Ejercicio09Pool.java)

---

## Ejercicio 10 · Consultas con Spring Data: del nombre del método a SQL nativo

**Objetivo.** Recorrer todas las formas de declarar una consulta en un repositorio.
**Diapositivas:** 159–164. **Tiempo:** 45 min.

Añade a `OwnerRepository` un método de cada tipo y escribe para cada uno una prueba en
`OwnerRepositoryConsultasTests` (`@DataJpaTest`, con los datos de `data.sql`):

| # | Tipo | Método |
|---|---|---|
| 1 | Derivado, con orden | `List<Owner> findByCityOrderByLastNameAsc(String city)` |
| 2 | Derivado de recuento | `long countByCity(String city)` |
| 3 | Derivado de existencia | `boolean existsByTelephone(String telephone)` |
| 4 | Derivado paginado | `Page<Owner> findByLastNameStartingWith(String prefijo, Pageable pageable)` |
| 5 | Derivado con `Slice` | `Slice<Owner> findByCity(String city, Pageable pageable)` |
| 6 | JPQL con proyección de interfaz | `@Query("select o.city as ciudad, count(o) as total from Owner o group by o.city")` → `List<PropietariosPorCiudad>` |
| 7 | SQL nativo | `@Query(value = "select * from owners where telephone like :prefijo%", nativeQuery = true)` |
| 8 | Modificación masiva | `@Modifying @Query("update Owner o set o.city = :nueva where o.city = :vieja")` |

Para cada uno, mira el SQL en la consola y responde:

- ¿Qué diferencia hay en el SQL entre el 4 (`Page`) y el 5 (`Slice`)? *(`Page` lanza además un
  `count(*)` para saber el total; `Slice` pide un elemento de más para saber si hay
  siguiente.)*
- En el 8, lee una entidad **antes** de la actualización y consúltala después **en la misma
  transacción**. ¿Ves la ciudad nueva? Arréglalo con `@Modifying(clearAutomatically = true)`.
- ¿Por qué el 8 exige `@Transactional`?

**Resultado esperado.** Ocho pruebas en verde y una explicación de cada SQL generado.

**Pregunta.** La diapositiva 164 dice que cada repositorio lo usa un solo servicio. ¿Lo cumple
el proyecto? *(Busca quién usa `PetRepository` y `VisitRepository`.)*

Solución: [`soluciones/Ejercicio10ConsultasSpringData.java`](soluciones/Ejercicio10ConsultasSpringData.java)

---

## Ejercicio 11 · El contexto de persistencia, visto desde dentro

**Objetivo.** Comprobar con pruebas lo que hace el `EntityManager` sin que se le pida, y
usar transacciones programáticas.
**Diapositivas:** 150–153. **Tiempo:** 35 min.

Escribe `ContextoDePersistenciaTests` (`@DataJpaTest`) con estas cinco pruebas:

1. **Caché de primer nivel.** Dos `find` del mismo propietario devuelven **el mismo objeto**
   (`isSameAs`), y en la consola solo hay un `select`.
2. ***Dirty checking*.** Cambia el nombre de un propietario leído con `find`, **sin llamar a
   `save`**, y haz `flush()`. Aparece un `update`.
3. **Entidad separada (*detached*).** Haz `detach()` y cambia el nombre: el `flush()` no
   genera nada. Recupera los cambios con `merge()`.
4. **`TransactionTemplate`** (diapositiva 153). Con la prueba fuera de transacción
   (`@Transactional(propagation = NOT_SUPPORTED)`), usa `new TransactionTemplate(txManager)`
   para guardar un tipo de mascota y otro bloque que marque `setRollbackOnly()`. Comprueba que
   el primero queda y el segundo no.
5. **`readOnly`.** Dentro de una `TransactionTemplate` con `setReadOnly(true)`, modifica una
   entidad. ¿Se escribe?

**Resultado esperado.** Cinco pruebas que documentan el comportamiento del `EntityManager`.
La 5 suele sorprender: con Hibernate, una transacción de solo lectura **no** hace *flush*, y
el cambio se pierde sin error.

> **Mensaje clave.** «No he llamado a `save` y se ha guardado» y «he cambiado el objeto y no
> se ha guardado» son la misma pregunta: ¿estaba la entidad **gestionada** dentro de una
> transacción abierta?

Solución: [`soluciones/Ejercicio11ContextoDePersistenciaTests.java`](soluciones/Ejercicio11ContextoDePersistenciaTests.java)

---

## Ejercicio 12 · Medir el N+1, no adivinarlo

**Objetivo.** Contar las consultas con las estadísticas de Hibernate y comparar las soluciones.
**Diapositivas:** 171–174. **Tiempo:** 35 min. **Requisito:** [guion de N+1](guion-consultas-n-mas-1.md).

1. Copia la prueba de la solución a `src/test/java/.../repository/` y ejecútala. Para cada
   estrategia imprime el número de sentencias SQL ejecutadas al cargar todos los propietarios
   y recorrer sus mascotas:
   - consulta JPQL simple;
   - `LEFT JOIN FETCH`;
   - `@EntityGraph` (`findByLastName("")`).
2. Anota los tres números. ¿Por qué la versión con `JOIN FETCH` **no** da 1? *(Pista:
   `Pet.visits` es `EAGER`.)*
3. Cambia `Pet.visits` a `LAZY` y repite. Ejecuta después **toda** la batería de pruebas:
   ¿qué se rompe y por qué? (Es el ejercicio 5 del guion de N+1, ahora medido.)
4. Prueba una cuarta solución sin tocar las consultas:

   ```java
   @DataJpaTest(properties = "spring.jpa.properties.hibernate.default_batch_fetch_size=16")
   ```

   ¿Cuántas sentencias ahora?

**Resultado esperado.** Una tabla de cuatro filas con el número de consultas de cada estrategia,
y una conclusión razonada sobre cuál usar en el listado de propietarios.

**Pregunta.** La diapositiva 174 dice que el *fetch join* «viene con un coste de memoria».
¿Cuándo es preferible el *batch fetching*? *(Cuando la colección es grande o no siempre se
usa entera: trae los datos por bloques, sin multiplicar filas.)*

Solución: [`soluciones/Ejercicio12NMasUnoMedidoTests.java`](soluciones/Ejercicio12NMasUnoMedidoTests.java)
