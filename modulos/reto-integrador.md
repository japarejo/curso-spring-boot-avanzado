# Reto integrador · Recordatorios de vacunación

Una funcionalidad pequeña que recorre **los seis módulos**. Se presenta en la sesión 13 y se
revisa en la 14. Se hace sobre `apps/petclinic-api`, en una rama propia:

```bash
git switch -c reto/<tu-nombre>
```

No hay una única solución correcta: lo que se evalúa es que cada decisión esté justificada y
que la lista de comprobación del final se cumpla.

## La historia de usuario

> Como **veterinario**, quiero registrar las vacunas que pongo a cada mascota y consultar qué
> mascotas tienen una vacuna **caducada o a punto de caducar**, para avisar a sus dueños.
>
> Como **propietario**, quiero ver las vacunas de **mis** mascotas, y solo las de las mías.

## Requisitos

### Módulo 6 · Datos

- Nueva entidad `Vacunacion`: mascota, nombre de la vacuna, fecha de aplicación, fecha de
  caducidad y veterinario que la aplicó.
- `@ManyToOne` hacia `Pet` y hacia `Vet`, **perezosas**.
- Migración Flyway `V3__vacunaciones.sql` (o la siguiente libre) para el perfil `mysql`, y
  datos de ejemplo para H2 en `data.sql`.
- Consulta: vacunaciones que caducan antes de una fecha dada, **paginada**, sin N+1 al mostrar
  el nombre de la mascota y del propietario.

### Módulo 2 · API

- `POST /api/v1/pets/{petId}/vacunaciones` → 201 con `Location`.
- `GET /api/v1/pets/{petId}/vacunaciones` → las de esa mascota.
- `GET /api/v1/vacunaciones/proximas-a-caducar?dias=30&page=0&size=20` → paginado.
- DTO de entrada validado y DTO de salida. **Ninguna entidad sale por la API.**
- Errores con `ProblemDetail`: mascota inexistente (404), fecha de caducidad anterior a la de
  aplicación (400).
- Documentado en OpenAPI, con los errores y el requisito de seguridad.

### Módulo 3 · Seguridad

- Registrar una vacunación: solo autoridad `vet`.
- Consultar las próximas a caducar: `vet` o `admin`.
- Consultar las de una mascota: `vet`, `admin`, o el **propietario de esa mascota** (y nadie
  más). Esta es la regla importante: autorización a nivel de objeto.

### Módulo 5 · Configuración

- El margen por defecto de «a punto de caducar» (30 días) es una propiedad tipada y validada
  (`@ConfigurationProperties`, entre 1 y 365).
- Un contador de Micrometer `petclinic.vacunaciones.registradas`.

### Módulo 4 · Pruebas

Como mínimo:

- una prueba **unitaria** del servicio con Mockito (la regla de las fechas);
- una prueba `@DataJpaTest` de la consulta de próximas a caducar, que compruebe el número de
  sentencias SQL (sin N+1);
- una prueba `@WebMvcTest` del controlador (201, 400, 404);
- una prueba de **seguridad** por cada regla del módulo 3, incluido «un propietario no ve las
  vacunas de otro»;
- una petición nueva en la colección de Postman.

### Módulo 1 · Diseño

- Inyección por constructor en todas las clases nuevas.
- Cada clase en su capa (`model`, `repository`, `service`, `web/api`, `configuration`).

## Lista de comprobación

| # | Comprobación | Módulo |
|---|---|---|
| 1 | `./mvnw verify` pasa | todos |
| 2 | Ningún controlador nuevo devuelve ni recibe una entidad | 2 |
| 3 | `POST` devuelve 201 con `Location`; los errores son `application/problem+json` | 2 |
| 4 | Swagger UI muestra los endpoints nuevos con sus errores y el candado | 2 |
| 5 | Un `owner` recibe 403 (o 404) al pedir las vacunas de una mascota ajena, y hay prueba de ello | 3, 4 |
| 6 | No hay `permitAll()` nuevo | 3 |
| 7 | El margen de días está en `application.properties` y la aplicación no arranca con `400` | 5 |
| 8 | El contador aparece en `/actuator/metrics` | 5 |
| 9 | La consulta paginada no hace N+1 (prueba con estadísticas de Hibernate) | 6 |
| 10 | Arranca con el perfil `mysql` (la migración existe y `ddl-auto=validate` no protesta) | 6 |
| 11 | Inyección por constructor, sin `@Autowired` en campos | 1 |

## Pistas, por si os atascáis

- Paginar **y** traer una relación: `@EntityGraph` sobre una consulta paginada funciona bien
  con relaciones `@ManyToOne`; con colecciones, no (Hibernate pagina en memoria).
- La regla del propietario es la de `OwnerService.findOwnerById(int, Principal)`. Puede
  escribirse igual, o con `@PostAuthorize`, o con un *bean* de autorización:
  `@PreAuthorize("@permisosMascota.puedeVer(#petId, authentication)")`.
- Para la prueba sin N+1, la técnica del ejercicio 12 del módulo 6.
