# Guion docente: transacciones con Spring, JPA y Hibernate

Este guion organiza una clase práctica de transacciones para la aplicacion `apps/petclinic-api`.

## Objetivo

- Aclarar qué significa una transacción en Spring.
- Mostrar su efecto en consistencia y rollback.
- Explicar propiedades clave de `@Transactional`.
- Practicar `rollbackFor`, `readOnly`, `propagation`, `isolation` y `timeout`.

## Entorno

- Proyecto: `apps/petclinic-api`
- Clases principales:
  - `OwnerService`
  - `PetService`
  - `OwnerRepository`
  - `PetRepository`
- Inicio recomendado:

```powershell
.\mvnw.cmd -pl apps/petclinic-api spring-boot:run
```

## Preparación

1. Asegúrate de tener SQL visible si vas a demostrar efectos de transacción:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

2. Si quieres más detalle temporalmente:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
logging.level.org.springframework.transaction.interceptor=TRACE
logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG
```

Con esta configuracion deben verse dos tipos de evidencia:

- SQL de Hibernate: `insert`, `select`, `update`.
- Trazas de transaccion de Spring: entrada y salida de metodos `@Transactional`, commit o rollback.

> Nota de comprobacion: `spring.jpa.show-sql=true` ya esta activo en `apps/petclinic-api/src/main/resources/application.properties`, pero escribe SQL directamente en consola. Para verlo como log filtrable conviene activar tambien `logging.level.org.hibernate.SQL=DEBUG`.

## 1. Frontera transaccional en servicios

### Mensaje inicial

Una transacción define la unidad de consistencia de un caso de uso. No pertenece al repositorio ni al controlador; suele pertenecer al servicio.

### Ejemplo

```java
@Transactional
public void saveOwner(Owner owner) {
    ownerRepository.save(owner);
    userService.saveUser(owner.getUser());
    authoritiesService.saveAuthorities(owner.getUser().getUsername(), "owner");
}
```

### Pregunta de clase

- "¿Qué debería pasar si se crea el propietario pero falla la creación de authorities?"

### Mensaje clave

> Debe revertirse todo el caso de uso. La transacción pertenece al caso de uso, no a cada llamada a un repositorio aislado.

## 2. Ejercicio: rollback con RuntimeException

### Objetivo

Comprobar que una transacción de servicio evita que el caso de uso quede a medias.

### Pasos

1. Introduce temporalmente en `OwnerService`:

```java
@Transactional
public void saveOwner(Owner owner) {
    ownerRepository.save(owner);
    if ("ROLLBACK".equals(owner.getLastName())) {
        throw new IllegalStateException("Fallo didactico despues de guardar owner");
    }
    userService.saveUser(owner.getUser());
    authoritiesService.saveAuthorities(owner.getUser().getUsername(), "owner");
}
```

> Nota: en este ejemplo no hace falta `rollbackFor`, porque `IllegalStateException` es una unchecked exception y Spring hace rollback por defecto sobre `RuntimeException`.
> El uso de `rollbackFor` se explica en la siguiente sección con una checked exception (`DuplicatedPetNameException`).

2. Crea un propietario con apellido `ROLLBACK` desde `http://localhost:8080/owners/new`.
3. Observa que no queda persistido.
4. Quita `@Transactional` y repite.

### Resultado esperado

- Con `@Transactional`: rollback completo.
- Sin `@Transactional`: la operación puede quedar a medias.

### Mensaje clave

> `@Transactional` no está ahí para ahorrar líneas. Define qué operaciones forman una unidad atómica.

## 3. Propiedades clave de `@Transactional`

### Objetivo

Entender qué hace cada propiedad de `@Transactional` y cuándo conviene usarla.

### Propiedades y ejemplos

- `rollbackFor`: fuerza rollback para excepciones checked.

```java
@Transactional(rollbackFor = DuplicatedPetNameException.class)
public void savePet(Pet pet) throws DuplicatedPetNameException {
    petRepository.save(pet);
    if ("ROLLBACK".equals(pet.getName())) {
        throw new DuplicatedPetNameException();
    }
}
```

Contexto didáctico:
- Por defecto Spring revierte con `RuntimeException` y `Error`.
- Una excepción checked solo causa rollback si se declara explícitamente con `rollbackFor`.
- Útil cuando la excepción de negocio debe invalidar la transacción.

- `readOnly`: comunica que la transacción solo lee datos y permite optimizaciones.

```java
@Transactional(readOnly = true)
public Owner findOwnerById(int ownerId) {
    return ownerRepository.findById(ownerId);
}
```

Contexto didáctico:
- `readOnly = true` puede evitar `dirty checking` y reducir flush innecesario.
- No impide que un desarrollador cambie una entidad, por eso no es un mecanismo de seguridad.
- Ideal para servicios de consulta que no deben modificar estado.

- `propagation`: define qué sucede cuando ya existe una transacción activa.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registerAuditEvent(String message) {
    auditRepository.save(new AuditEvent(message));
}
```

Contexto didáctico:
- `REQUIRED` (por defecto) se une a la transacción actual.
- `REQUIRES_NEW` suspende la transacción actual y abre otra independiente.
- Útil para auditoría o logs que se quieran confirmar incluso si falla la transacción padre.

- `isolation`: controla qué anomalías de concurrencia se permiten.

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void recalculateStatistics() {
    // evita lecturas sucias en bases de datos que soportan READ_COMMITTED
}
```

Contexto didáctico:
- `READ_UNCOMMITTED` permite lecturas sucias.
- `READ_COMMITTED` evita lecturas sucias.
- `REPEATABLE_READ` evita lecturas no repetibles.
- `SERIALIZABLE` evita también fenómenos fantasma.
- La elección depende de la base de datos y del coste de bloqueo.

- `timeout`: limita el tiempo máximo de ejecución de la transacción.

```java
@Transactional(timeout = 5)
public void processLargeBatch() {
    // si tarda más de 5 segundos, Spring aborta la transacción
}
```

Contexto didáctico:
- Protege contra operaciones demasiado largas.
- No es una solución de rendimiento; es una red de seguridad.
- Si se supera el tiempo, la transacción se marca para rollback.

### Cómo usarlo en clase

- Explica `rollbackFor` con una excepción checked real y por qué Spring no la revierte por defecto.
- Usa `readOnly` para separar casos de uso de consulta de casos de uso de escritura.
- Presenta `propagation` con un ejemplo de auditoría o envío de notificaciones.
- Muestra `isolation` como una decisión de consistencia, no de rendimiento.
- Menciona `timeout` como una protección frente a operaciones demasiado largas.

## 4. Ejercicio corto: probar cada propiedad por separado

### Objetivo

Que los alumnos cambien una sola propiedad cada vez y comprueben el efecto en:

- La base de datos H2.
- El log SQL.
- El log de transacciones de Spring.

### Preparacion comun

Activa temporalmente:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
logging.level.org.springframework.transaction.interceptor=TRACE
logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG
```

Arranca la aplicacion:

```powershell
.\mvnw.cmd -pl apps/petclinic-api spring-boot:run
```

Para comprobar datos, usa la consola H2:

- URL: `http://localhost:8080/h2-console`
- JDBC URL habitual en esta app: `jdbc:h2:mem:testdb`
- Consulta util:

```sql
select id, first_name, last_name, city, username
from owners
order by id desc;
```

> Nota sobre la UI: `/owners/**` esta protegido por Spring Security. Puedes iniciar sesion con un usuario existente, pero para estos micro-ejercicios la verificacion mas clara es mirar H2 y el log justo despues de provocar la llamada.

### 4.1. `rollbackFor`: checked exception

En el codigo actual, `PetService.savePet` lanza `DuplicatedPetNameException` antes de guardar cuando detecta duplicado. Para ver el rollback hay que forzar temporalmente una checked exception despues del `save`:

```java
@Transactional(rollbackFor = DuplicatedPetNameException.class)
public void savePet(Pet pet) throws DataAccessException, DuplicatedPetNameException {
    petRepository.save(pet);

    if ("ROLLBACK".equals(pet.getName())) {
        throw new DuplicatedPetNameException();
    }
}
```

Prueba A:

1. Deja `rollbackFor = DuplicatedPetNameException.class`.
2. Crea una mascota llamada `ROLLBACK`.
3. Comprueba que no queda insertada.
4. En el log busca el `insert` y despues una marca de rollback de `JpaTransactionManager`.

Prueba B:

1. Quita solo `rollbackFor`.
2. Repite con otra mascota llamada `ROLLBACK`.
3. Comprueba que la checked exception no fuerza rollback por defecto.

Resultado esperado:

- Con `rollbackFor`: la transaccion se revierte.
- Sin `rollbackFor`: Spring no revierte por defecto una checked exception.

### 4.2. `readOnly`: dirty checking visible

Anade temporalmente en `OwnerService`:

```java
@Transactional
public void changeOwnerCity(int ownerId, String city) {
    Owner owner = ownerRepository.findById(ownerId);
    owner.setCity(city);
}
```

Prueba A:

1. Llama al metodo desde un test, controlador temporal o runner.
2. Comprueba en el log que aparece un `select` y luego un `update`.
3. Comprueba en H2 que la ciudad cambio.

Prueba B:

1. Cambia solo la anotacion a `@Transactional(readOnly = true)`.
2. Repite la llamada.
3. Comprueba que no debe aparecer el `update` de dirty checking al commit.

Resultado esperado:

- Sin `readOnly`: la entidad managed se sincroniza al commit.
- Con `readOnly = true`: Hibernate evita el flush automatico de esa modificacion en este caso.

### 4.3. `propagation`: misma transaccion frente a transaccion nueva

Para que sea observable, crea una operacion auxiliar que escriba algo independiente. Si no quieres crear una tabla de auditoria, puedes usar otra entidad simple ya existente solo durante la demo.

Ejemplo conceptual:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registerAuditEvent(String message) {
    auditRepository.save(new AuditEvent(message));
}
```

Prueba A:

1. Llama a `registerAuditEvent(...)` desde un metodo `@Transactional`.
2. Lanza despues una `RuntimeException` en el metodo principal.
3. Comprueba que el caso de uso principal hace rollback.
4. Comprueba que la auditoria se conserva.

Prueba B:

1. Cambia solo `REQUIRES_NEW` por `REQUIRED`.
2. Repite.
3. Comprueba que la auditoria tambien se revierte.

En el log debe verse que con `REQUIRES_NEW` Spring suspende la transaccion existente y crea otra para el metodo auxiliar.

### 4.4. `isolation`: declarar el contrato

En H2 no siempre se aprecian todas las anomalias de concurrencia igual que en una base de datos real, asi que el ejercicio corto se centra en comprobar que Spring intenta aplicar el nivel elegido.

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public Owner findOwnerWithReadCommitted(int ownerId) {
    return ownerRepository.findById(ownerId);
}
```

Prueba:

1. Llama al metodo.
2. Cambia solo `READ_COMMITTED` por `SERIALIZABLE`.
3. Repite.
4. Comprueba en el log de `JpaTransactionManager` que la transaccion se abre con una definicion distinta.

Resultado esperado:

- El SQL de consulta puede ser el mismo.
- Lo que cambia es el contrato transaccional que Spring pide al gestor de transacciones.

### 4.5. `timeout`: transaccion demasiado lenta

Anade temporalmente:

```java
@Transactional(timeout = 1)
public void slowOwnerChange(int ownerId) throws InterruptedException {
    Owner owner = ownerRepository.findById(ownerId);
    Thread.sleep(2_000);
    owner.setCity("Timeout City");
    ownerRepository.findByLastName("timeout_1");
}
```

Prueba:

1. Llama al metodo.
2. Comprueba que se supera el segundo configurado.
3. Comprueba en el log que la transaccion termina marcada para rollback o falla por timeout.
4. Comprueba en H2 que `city` no queda como `Timeout City`.

Resultado esperado:

- El timeout no optimiza la consulta.
- Solo protege la transaccion para que una operacion demasiado larga no confirme cambios.

> Matiz importante: `timeout` no interrumpe `Thread.sleep`. En esta demo la llamada final a `ownerRepository.findByLastName(...)` fuerza otra operacion de base de datos despues de superar el limite, y ahi Spring aplica el timeout de la transaccion.

## 5. Ejercicio: checked exception y `rollbackFor`

### Objetivo

Entender la diferencia entre excepciones runtime y checked en el comportamiento de rollback de Spring.

### Código base

```java
@Transactional(rollbackFor = DuplicatedPetNameException.class)
public void savePet(Pet pet) throws DuplicatedPetNameException {
    ...
}
```

### Pasos

1. Introduce temporalmente en `PetService`:

```java
if ("ROLLBACK".equals(pet.getName())) {
    petRepository.save(pet);
    throw new DuplicatedPetNameException();
}
```

2. Quita temporalmente `rollbackFor`.
3. Usa `ROLLBACK` como nombre de mascota.
4. Observa si queda persistido.
5. Restaura `rollbackFor` y repite.

### Lectura esperada

- RuntimeException -> rollback por defecto.
- Checked exception -> no rollback por defecto.
- Checked exception + `rollbackFor` -> rollback explícito.

### Mensaje clave

> Spring no puede adivinar si una checked exception invalida el caso de uso. Si debe revertir, hay que declararlo.

## 6. Ejercicio: dirty checking y `readOnly`

### Objetivo

Ver que dentro de una transacción una entidad managed se sincroniza al commit aunque no se llame a `save`.

### Pasos

1. Añade en `OwnerService`:

```java
@Transactional
public void changeOwnerCity(int ownerId, String city) {
    Owner owner = ownerRepository.findById(ownerId);
    owner.setCity(city);
}
```

2. Llama al método desde un test o controlador temporal.
3. Observa el `UPDATE` al commit.
4. Cambia a `@Transactional(readOnly = true)` y repite.

### Mensaje clave

> Dentro de una transacción, una entidad cargada queda managed. Hibernate detecta cambios y sincroniza al flush/commit.

### Discusión adicional

- `readOnly = true` comunica intención y puede optimizar.
- No es un mecanismo de seguridad de negocio.
- `entityManager.clear()` en tests obliga a volver a leer desde la BD.

## 7. Ejercicio: propagación y auditoría

### Objetivo

Comparar operaciones que deben participar en la misma transacción con operaciones que pueden confirmarse por separado.

### Planteamiento

```text
saveOwner()
  -> guarda owner
  -> guarda user
  -> guarda authorities
  -> registra auditoria
  -> falla el caso de uso principal
```

### Preguntas de clase

- "¿Debe conservarse la auditoría aunque falle el caso de uso?"
- "¿Debe participar en la misma transacción?"
- "¿Qué cambia si el método de auditoría usa `REQUIRES_NEW`?"

### Código sugerido

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registerAuditEvent(String message) {
    ...
}
```

### Mensaje clave

> `REQUIRES_NEW` no hace la operación más transaccional. Abre otra transacción independiente. Eso puede ser correcto para auditoría, pero rompe la idea de todo-o-nada del caso de uso principal.

## 8. Lectura crítica: `isolation` y `timeout`

### Objetivo

Mostrar que `isolation` y `timeout` son decisiones explícitas de infraestructura y consistencia.

### Ejemplo didáctico

```java
@Transactional(
    isolation = Isolation.READ_COMMITTED,
    timeout = 5
)
public void recalculateSomethingExpensive() {
    ...
}
```

### Preguntas de clase

- "¿Qué anomalía de concurrencia quiero evitar?"
- "¿Mi base de datos respeta exactamente este nivel de aislamiento?"
- "¿Qué debe pasar si esta operación tarda más de 5 segundos?"

### Mensaje clave

> `isolation` y `timeout` no son opciones cosméticas. Son contratos que pueden cambiar el comportamiento de la transacción.

## Cierre

### Mensajes clave

- Una transacción define una unidad de consistencia de negocio.
- `rollbackFor`, `readOnly`, `propagation`, `isolation` y `timeout` cambian el comportamiento.
- La frontera transaccional suele ser el servicio, no el repositorio.
- El SQL generado y el persistence context son parte del contrato de ejecución.

### Recursos

- `apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/service/OwnerService.java`
- `apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/service/PetService.java`
- `apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/repository/OwnerRepository.java`
