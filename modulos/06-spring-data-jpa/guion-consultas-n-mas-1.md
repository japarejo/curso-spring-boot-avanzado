# Guion docente: problema N+1 en Spring Boot / JPA

Este guion describe una práctica didáctica para la aplicacion `apps/petclinic-api` en un curso avanzado de Spring.

## Objetivo

- Detectar el problema N+1 de consultas en JPA/Hibernate.
- Entender por qué aparece en una vista o un serializador.
- Corregirlo con `fetch join`, `@EntityGraph` y decisiones de carga local.
- Comparar con `FetchType.EAGER` y comprender sus efectos.

## Entorno

- Proyecto: `apps/petclinic-api`
- Repositorio de trabajo: `apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/repository/OwnerRepository.java`
- Ruta de arranque recomendada:

```powershell
.\mvnw.cmd -pl apps/petclinic-api spring-boot:run
```

## Preparación

1. En `apps/petclinic-api/src/main/resources/application.properties`, comprueba que está habilitado el SQL visible:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

2. Si quieres más detalle temporalmente, añade:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

## Ejercicio 1: detectar el N+1

### Objetivo

Ver que una consulta aparentemente sencilla se convierte en `1 + N` consultas al recorrer una relación lazy.

### Pasos

1. Cambia en `OwnerRepository` la consulta de `findByLastName` a:

```java
@Query("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName%")
public Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

2. Arranca la aplicacion.
3. Abre:

```text
http://localhost:8080/owners
```

4. Observa el log SQL.

### Lectura esperada

- 1 consulta carga los `Owner`.
- Al recorrer `owner.pets`, Hibernate ejecuta consultas adicionales para cada propietario.

### Pregunta de clase

- "¿Dónde se dispara el segundo grupo de consultas: en el repositorio, en el servicio o al pintar la vista?"

### Mensaje clave

> N+1 no es solo un bucle explícito en el código. A veces el bucle está en la vista, el serializador JSON o el mapeo, y Hibernate resuelve relaciones una a una.

## Ejercicio 2: corregir con `fetch join`

### Objetivo

Evitar el N+1 controlando el plan de carga en la consulta.

### Pasos

1. Restaura la consulta en `OwnerRepository` a:

```java
@Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
public Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

2. Repite la visita a:

```text
http://localhost:8080/owners
```

3. Compara el número de consultas.

### Lectura esperada

- Owners y pets se cargan en la misma consulta.
- La vista recorre `owner.pets` sin disparar consultas por propietario.

### Preguntas de clase

- "¿Por qué aparece `DISTINCT`?"
- "¿Qué asociación estamos decidiendo cargar para este caso de uso?"
- "¿Tiene sentido cargar siempre las mascotas al buscar propietarios?"

### Mensaje clave

> `fetch join` no cambia el modelo general. Cambia el plan de carga de esta consulta concreta.

## Ejercicio 3: comparar con `@EntityGraph`

### Objetivo

Mostrar otra forma de separar la definición de la consulta de la intención de carga.

### Código sugerido

```java
@EntityGraph(attributePaths = {"pets"})
@Query("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName%")
public Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

### Discusión

- `fetch join` deja el plan de carga dentro de JPQL.
- `@EntityGraph` separa el filtro de los caminos de carga.
- Ambas son mejores que cambiar el mapeo global sin pensar.

## Ejercicio 4: por qué `EAGER` no es una solución universal

### Objetivo

Comparar la decisión `LAZY` vs `EAGER` en el mapeo de entidad.

### Pasos

1. Cambia temporalmente en `Owner`:

```java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "owner", fetch = FetchType.EAGER)
private Set<Pet> pets;
```

2. Repite la consulta y observa el SQL.

3. Discutid en clase los efectos.

### Discusión

- `EAGER` puede evitar algunas cargas tardías.
- `EAGER` puede cargar datos que el caso de uso no necesita.
- `EAGER` puede empeorar el rendimiento si se encadenan colecciones.

### Pregunta de clase

- "Si una pantalla lista propietarios pero no muestra mascotas, ¿qué coste oculto introduce esta decisión?"

### Mensaje clave

> `EAGER` responde "cárgalo siempre con la entidad". `fetch join` responde "cárgalo en esta consulta porque esta pantalla lo necesita".

## Ejercicio 5: `Pet.visits` y efectos secundarios de carga global

### Objetivo

Mostrar que un mapeo `EAGER` en otra entidad puede generar costes inesperados.

### Ejemplo a revisar

```java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "pet", fetch = FetchType.EAGER)
private Set<Visit> visits;
```

### Pregunta de clase

- "Si una lista de mascotas no muestra visitas, ¿por qué sería caro que `Pet.visits` sea siempre `EAGER`?"

### Mensaje clave

> La configuración de carga es parte del rendimiento del caso de uso. Por defecto, colecciones `LAZY`; para pantallas específicas, `fetch join`, `@EntityGraph`, DTO projection o batch fetching.

## Cierre

### Ideas que deben quedar

- El N+1 es un problema de plan de carga, no solo de "Hibernate es lento".
- `fetch join` y `@EntityGraph` permiten adaptar la carga al caso de uso.
- `EAGER` como política global es peligroso.
- Revisar siempre el SQL generado en las pantallas críticas.

### Recursos

- `apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/repository/OwnerRepository.java`
- `apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/model/Owner.java`
- `apps/petclinic-api/src/main/resources/application.properties`
