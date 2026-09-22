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
