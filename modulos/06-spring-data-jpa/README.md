# Módulo 6. Uso de Spring Data en Spring Boot

Guion docente. Duración estimada: **tres sesiones de 120 minutos**.

## Documentos

| Documento | Qué contiene |
|---|---|
| [`guion-acceso-a-datos.md`](guion-acceso-a-datos.md) | Las tres sesiones completas, con brújula conceptual, prácticas guiadas y troubleshooting |
| [`guion-transacciones.md`](guion-transacciones.md) | 8 ejercicios, uno por propiedad de `@Transactional` |
| [`guion-consultas-n-mas-1.md`](guion-consultas-n-mas-1.md) | 5 ejercicios sobre el problema N+1 |
| [`ejercicios.md`](ejercicios.md) | Migraciones, proyecciones y consultas dinámicas |

## Objetivos

- Situar JPA, Hibernate y Spring Data: qué es cada cosa.
- Mapear relaciones 1:1, 1:N, N:M y reflexivas, y reconocer sus trampas.
- Elegir estrategia de generación de claves primarias con criterio.
- Delimitar transacciones con `@Transactional` y conocer sus dos trampas clásicas.
- Versionar el esquema con Flyway.
- Detectar y corregir el problema N+1.
- Usar proyecciones y consultas dinámicas.

## Mapa del material

| Concepto | Dónde |
|---|---|
| Jerarquía de entidades | `model/BaseEntity`, `NamedEntity`, `Person` |
| Bloqueo optimista | `model/BaseEntity` (`@Version`) |
| Clave natural frente a autonumérica | `model/User` (`@Id String`) frente al resto |
| 1:N con cascada | `model/Owner.pets` |
| 1:1 bidireccional | `model/Visit.diagnose` |
| N:M con tabla de unión | `model/Vet.specialties` |
| Restricción de negocio personalizada | `service/businessrules/ValidatePossibleDisease` |
| Auditoría JPA | `model/AuditableEntity`, `configuration/AuditorAwareImpl` |
| Criteria API | `repository/PetSpecification`, `VisitSpecification` |
| Grafos de carga | `repository/OwnerRepository` (`@EntityGraph` y `LEFT JOIN FETCH`) |
| Transacciones | `service/PetService`, `VisitService` |
| Verificación de transacciones | `src/test/.../service/TransactionalExamplesVerificationTests.java` |
| Migraciones | `src/main/resources/db/migration/` |

---

## Dos mecanismos de esquema, y por qué

Es lo primero que hay que explicar, porque salta a la vista al abrir el proyecto:

| Entorno | Esquema | Datos |
|---|---|---|
| H2 (por defecto) | `ddl-auto=create-drop` | `data.sql` |
| MySQL (`mysql`, `docker`) | **Flyway** | `db/migration/V2__datos_iniciales.sql` |

No es un descuido: es una decisión con su precio.

- Con H2, arranque instantáneo y base limpia en cada ejecución. Es lo que quieres en clase
  y en las pruebas.
- Con MySQL, esquema versionado, reproducible y con historial. Es lo que quieres en
  cualquier entorno que sobreviva a un reinicio.

El precio es que hay **dos ficheros que mantener sincronizados**. Cuando se separan, el
síntoma aparece en las pruebas de Testcontainers, que corren contra MySQL real. Ese es el
ejercicio 1.

> **Mensaje clave.** `spring.jpa.hibernate.ddl-auto=update` es cómodo en desarrollo y
> peligroso fuera de él: nunca borra, nunca renombra, aplica cambios sin control de versión
> y deja el esquema fuera del repositorio. Con Flyway el esquema es código: numerado,
> aplicado una sola vez y registrado en `flyway_schema_history`.
>
> Y las migraciones son **inmutables**. Un error no se corrige editando el fichero ya
> aplicado, sino añadiendo uno nuevo. Flyway guarda el hash de cada migración y aborta el
> arranque si detecta que una ya aplicada ha cambiado. Demostrarlo en clase: cambiar una
> línea de `V1__esquema_inicial.sql` y arrancar.

## Carga perezosa: el ejercicio que estructura el módulo

Este proyecto tiene `spring.jpa.open-in-view=false`, que **no** es el valor por defecto de
Spring Boot. Esa única línea convierte el módulo en un laboratorio.

El recorrido completo, con `OwnerRepository.findByLastName` y la página `/owners`:

1. **Con `@EntityGraph`** (estado actual): una consulta con `LEFT JOIN`. Correcto.
2. **Sin `@EntityGraph` y con el perfil `nplus1`** (que reactiva `open-in-view`): funciona,
   pero ejecuta 1 + N consultas. El problema clásico.
3. **Sin `@EntityGraph` y sin ese perfil**: `LazyInitializationException`.

```bash
./mvnw -pl apps/petclinic-api spring-boot:run -Dspring-boot.run.profiles=nplus1
```

> **Mensaje clave.** `open-in-view` no arregla nada: cambia el síntoma. Con él activado, el
> defecto sigue ahí pero disparando una consulta por elemento fuera de la transacción. La
> solución en los dos casos es la misma: traer en la consulta los datos que la vista
> necesita, con `@EntityGraph` o con `LEFT JOIN FETCH`.

El detalle, ejercicio a ejercicio, en [`guion-consultas-n-mas-1.md`](guion-consultas-n-mas-1.md).

## Transacciones

`TransactionalExamplesVerificationTests` verifica con aserciones, no con diapositivas:
`rollbackFor`, commit con excepción comprobada, `readOnly` y comprobación de cambios,
`isolation`, `timeout` y `REQUIRES_NEW` frente a `REQUIRED`.

```bash
./mvnw -pl apps/petclinic-api test -Dtest=TransactionalExamplesVerificationTests
```

Las dos trampas que hay que enunciar sí o sí:

1. `@Transactional` funciona con un proxy: **no se aplica si el método se llama desde la
   propia clase**.
2. Por omisión solo se deshace la transacción ante excepciones **no comprobadas**. Con una
   excepción comprobada hay que indicarlo con `rollbackFor`. Ejemplo real en
   `PetService.savePet`.

El detalle en [`guion-transacciones.md`](guion-transacciones.md).

## Diagnóstico

Antes de optimizar, medir:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.SQL=DEBUG
```

Todo esto ya está en el perfil `nplus1`.

## Errores frecuentes en clase

| Síntoma | Causa |
|---|---|
| `LazyInitializationException` | Se accede a una relación perezosa fuera de la transacción. Es el ejercicio, no un fallo. |
| `MultipleBagFetchException` | Dos `JOIN FETCH` sobre colecciones de tipo `List` a la vez. Usar `Set` o dos consultas. |
| Flyway aborta con "checksum mismatch" | Se ha editado una migración ya aplicada. Es el comportamiento correcto. |
| `ddl-auto=validate` falla al arrancar | El esquema de las migraciones no coincide con las entidades. Justamente para eso está. |
| La paginación con `JOIN FETCH` avisa `HHH90003004` | Hibernate está paginando en memoria. La solución son dos consultas: primero los identificadores, después los datos. |
