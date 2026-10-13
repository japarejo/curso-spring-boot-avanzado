# Sesión 11 · JPA: conexión, entidades, claves y relaciones

**Módulo 6** · Diapositivas 146–158, 165–170 · 3 horas (85 + 10 + 85 min)

> **Mensaje de la sesión.** JPA traduce objetos a tablas a partir de **anotaciones**, y cada
> anotación tiene una consecuencia concreta en el esquema. Hay que saber leer ese esquema. Y en
> una relación bidireccional, **solo se guarda lo que hay en el lado propietario**.

## Objetivos

- Situar JDBC, JPA, Hibernate y Spring Data: qué es cada cosa.
- Configurar el `DataSource` y entender el *pool* de conexiones y su límite.
- Mapear una entidad y elegir la estrategia de generación de claves.
- Mapear relaciones 1:1, 1:N, N:M, reflexivas y herencia, y leer el DDL resultante.
- Versionar el esquema con Flyway y saber por qué una migración no se edita.

## Antes de clase

- [ ] `practicas/mapeos-jpa` probado: `cd practicas/mapeos-jpa && ./mvnw test` y
      `target/ddl-generado.sql` generado.
- [ ] Docker con `mysql:8.4` descargada (para Flyway). Sin Docker, el bloque de Flyway es
      una demo desde tu máquina.
- [ ] Repasar la «brújula conceptual» del
      [guion de acceso a datos](../modulos/06-spring-data-jpa/guion-acceso-a-datos.md).

## Cronograma

| Hora | Bloque | Tipo | Diapositivas | Material |
|---|---|---|---|---|
| 0:00 | Repaso del módulo 5 | 🗣 | — | |
| 0:10 | JDBC, JPA, Hibernate y Spring Data | 🗣 | 147–153 | guion de acceso a datos |
| 0:30 | **La conexión y el *pool*** | 🧑‍💻 25' | 151–152, 165 | M6 ejercicio 9 |
| 0:55 | Entidades y claves primarias | ⌨ + 🧑‍💻 | 154, 166 | Ejercicio 5 |
| 1:25 | **Descanso** | | | |
| 1:35 | **Laboratorio de mapeos** | 🧑‍💻 50' | 155–158, 167–170 | Ejercicio 8 |
| 2:25 | **Migraciones con Flyway** | 🧑‍💻 30' | — | Ejercicios 1 y 2 |
| 2:55 | Cierre y tarea | | | |

---

## Desarrollo

### 0:00 – 0:10 · Repaso del módulo 5

Tres preguntas rápidas: orden de precedencia, qué recarga `@RefreshScope`, qué no funciona en un
Tomcat externo.

### 0:10 – 0:30 · JDBC, JPA, Hibernate y Spring Data

📊 Diapositivas 147–153.

🗣 Las cuatro piezas y quién hace qué:

| Pieza | Qué es | En el proyecto |
|---|---|---|
| JDBC | API de bajo nivel: conexiones, sentencias, `ResultSet` | por debajo de todo |
| JPA | **Especificación**: `@Entity`, `EntityManager`, JPQL | `jakarta.persistence.*` |
| Hibernate | **Implementación** de JPA | la trae el *starter* |
| Spring Data JPA | Repositorios generados a partir de interfaces | `repository/` |

Responde la pregunta de la diapositiva 150: «¿por qué JPA y no Hibernate directamente?»
(portabilidad y una API estándar; en la práctica casi todo el mundo usa Hibernate).

Dibuja la diapositiva 151: `DataSource` → *pool* → `EntityManagerFactory` → `EntityManager`
(uno por transacción) → `TransactionManager`.

### 0:30 – 0:55 · La conexión y el *pool*

📊 Diapositivas 151–152, 165.
🧑‍💻 **Ejercicio 9** (25 min): ver la configuración de HikariCP, reducir el *pool* a 2 y
agotarlo con cinco peticiones simultáneas.

> **Erratas de la diapositiva 165.** `mysql:mysql-connector-java` ahora es
> `com.mysql:mysql-connector-j`; `@EnableJpaRepositories` y `driver-class-name` sobran en Spring
> Boot.

🗣 **Mensaje clave.** «El *pool* es un recurso compartido y limitado. Una transacción que
espera a otro sistema bloquea una conexión para todos. Transacciones **cortas**.»

### 0:55 – 1:25 · Entidades y claves primarias

📊 Diapositivas 154, 166.

1. ⌨ (10 min) La entidad `Tarea` de la diapositiva 154 ya la tienen en `practicas/tareas`
   desde la sesión 1. Ábrela y mira el `create table tarea` en la consola. Recorre
   `model/BaseEntity` (con `@Version`) y `model/User` (clave natural).
2. 🧑‍💻 (20 min) **Ejercicio 5**: `IDENTITY` frente a `SEQUENCE` con `allocationSize=50`,
   contando las sentencias en la consola.

🗣 **Mensaje clave.** «Con `IDENTITY`, Hibernate necesita el identificador **de cada fila** al
insertarla: no puede agrupar inserciones. Con `SEQUENCE` y reserva de bloques, sí.»

### 1:25 – 1:35 · Descanso

### 1:35 – 2:25 · Laboratorio de mapeos

📊 Diapositivas 155–158, 167–170.
🧑‍💻 **Ejercicio 8** (50 min) en [`practicas/mapeos-jpa`](../practicas/mapeos-jpa/).

Organización recomendada:

1. (10 min) Todos juntos: ejecutar, abrir `target/ddl-generado.sql` y relacionar cada
   `create table` con su entidad.
2. (30 min) Por parejas, los cambios (a)–(h) de la tabla del ejercicio. Cada pareja anota su
   **predicción antes** de ejecutar.
3. (10 min) Puesta en común. Detente en la (c) y la (h).

🗣 **Mensaje clave.** «JPA escribe **solo el lado propietario**. Mantener los dos lados
sincronizados es trabajo nuestro: por eso las entidades tienen métodos como `contratar` o
`asignar`. Y una prueba que no vacía el contexto de persistencia lee de **memoria**, no de la
base de datos.»

Las erratas de las diapositivas 155, 156, 158 y 168 están explicadas en el Javadoc de cada
entidad del laboratorio: léelas en voz alta según salgan.

### 2:25 – 2:55 · Migraciones con Flyway

🧑‍💻 **Ejercicios 1 y 2** (30 min).

```bash
docker compose -f docker/compose.yaml up -d mysql
./mvnw -pl apps/petclinic-api spring-boot:run -Dspring-boot.run.profiles=mysql
```

1. Ejercicio 1: añadir `Pet.microchip`, ver que con H2 funciona y con MySQL `ddl-auto=validate`
   aborta; corregirlo con `V3__...sql`.
2. Ejercicio 2: editar una migración ya aplicada y leer el error de *checksum*.

🗣 **Mensaje clave.** «Una migración aplicada no se edita: se corrige con otra. Si no, dos
entornos con la misma versión tendrían esquemas distintos.»

### 2:55 – 3:00 · Cierre y tarea

- **Tarea:** paso 3 del ejercicio 8 (relación reflexiva jefe–subordinados, prueba primero).
- Para la sesión 12: repasar las diapositivas 159–164 (repositorios).

---

## Si vas con retraso

- Ejercicio 5 como demo (10 min).
- Laboratorio: hacer solo (a), (c), (d) y (h).
- Flyway como demo.

## Si te sobra tiempo

- Cambios (e) y (f) del laboratorio: la colisión de la diapositiva 167 que **no** ocurre con
  Spring Boot, y por qué.
- Cápsula «cuándo importa distinguir JPA e Hibernate» del guion de configurabilidad.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `Column 'pedido_id' is duplicated` | Se ha copiado la diapositiva 155 tal cual: campo y relación sobre la misma columna. |
| Aparece una tabla `departamento_empleados` inesperada | Falta `mappedBy`: son dos relaciones, no una. |
| `TABLE_PER_CLASS` falla con `IDENTITY` | Esa estrategia no admite `IDENTITY` (por eso `Trabajador` usa `SEQUENCE`). |
| MySQL: `Schema-validation: missing column` | Falta la migración Flyway del cambio en la entidad. Es lo esperado en el ejercicio 1. |
| `Connection is not available, request timed out` | El *pool* agotado del ejercicio 9. Restaurar el tamaño al terminar. |
