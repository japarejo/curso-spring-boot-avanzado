# Escenario: búsqueda avanzada de pacientes con la Criteria API

> **Para qué sirve.** El [laboratorio de Criteria API](laboratorio-criteria-api.md) explica las
> piezas de la API una por una. Este escenario responde a la pregunta que siempre llega
> después: *"¿y cuándo me compensa escribir todo esto?"*. Lo responde con una pantalla en la
> que cualquier otra opción es claramente peor.
>
> **Tiempo:** unos 40 minutos (15 de demostración, 25 de código). Las ampliaciones del final
> sirven de ejercicio para casa.

## 1. La pantalla

Recepción de la clínica quiere localizar pacientes con criterios que **cambian cada vez**:
*"gatos de Madison que no han venido desde marzo"*, *"perros diagnosticados de diabetes por la
Dra. Leary"*, *"mascotas con más de tres visitas, las más recientes primero"*...

La maqueta está en la propia aplicación y funciona contra la API real:

```text
http://localhost:8080/resources/busqueda-mascotas.html
```

```text
┌──────────────────────────────┬─────────────────────────────────────────────────────────────┐
│ MASCOTA                      │ 2 mascotas                                  Por página [10] │
│ [x] Nombre contiene [____]   │ (Tipo de animal: cat ×) (Vive en: Monona ×) (Visitas: ≥1 ×) │
│ [x] Tipo  [x]cat [ ]dog ...  │ ┌─────────┬──────┬────────────┬──────────┬────────┬───┬───┐ │
│ [ ] Nacida entre [__] y [__] │ │Mascota ▲│ Tipo │ Nacimiento │Propietar.│ Ciudad │Vis│Últ│ │
│ PROPIETARIO                  │ ├─────────┼──────┼────────────┼──────────┼────────┼───┼───┤ │
│ [ ] Apellido empieza [____]  │ │Max      │ cat  │ 2012-09-04 │J. Coleman│ Monona │ 2 │...│ │
│ [x] Vive en [ ]Madison [x].. │ │Samantha │ cat  │ 2012-09-04 │J. Coleman│ Monona │ 2 │...│ │
│ HISTORIAL DE VISITAS         │ └─────────┴──────┴────────────┴──────────┴────────┴───┴───┘ │
│ [x] Nº visitas entre [1]y[_] │ 1–2 de 2 · página 1 de 1             [← Anterior][Sig. →]   │
│ [ ] Ha venido entre [__][__] │                                                             │
│ [ ] Revisión pendiente [___] │ ▸ Qué se ha pedido (vista del docente)                      │
│ DIAGNÓSTICOS                 │     GET /api/v1/busqueda-mascotas?tipos=cat&ciudades=...    │
│ [ ] Diagnosticada de [ ]...  │     tipos      → cb.lower(tipo.name).in(lista)              │
│ [ ] Diagnosticada por [▼]    │     visitas ≥1 → cb.ge(subconsulta count correlacionada, 1) │
│ [Buscar] [Quitar filtros]    │                                                             │
└──────────────────────────────┴─────────────────────────────────────────────────────────────┘
```

Lo que hay que fijar en la pantalla:

- **Cada criterio tiene su casilla "aplicar".** Si no está marcada, el criterio no se envía y
  no genera SQL. Tocar un control marca su casilla.
- **Los chips** de arriba resumen los filtros activos y se quitan con la ×.
- **Las columnas se ordenan** pulsando la cabecera, también *Visitas* y *Última visita*, que
  son datos que calcula la base de datos.
- **La vista del docente** (desplegable al pie) enseña la petición enviada y qué pieza de la
  Criteria API activa cada filtro. El SQL real está en la consola de la aplicación.

## 2. Por qué aquí la Criteria API, y no otra cosa

Hay **13 criterios opcionales** y cada uno puede estar activado o no: 2¹³ = **8192
combinaciones**. A eso se suman la ordenación por 7 columnas y la paginación.

Pregunta para la clase, antes de enseñar código: *"¿cómo lo haríais con lo que ya sabéis?"*

| Alternativa | Qué pasa |
|---|---|
| Métodos derivados del nombre | Un método por combinación. Imposible. |
| Una `@Query` con `(:x is null or campo = :x)` para cada criterio | Funciona para filtros simples. Con listas (`in`), subconsultas opcionales y orden por agregados se vuelve ilegible, y el optimizador de la base de datos sufre con tanto `or`. |
| Concatenar JPQL con `if` | Ilegible y, al primer descuido, **inyección**. |
| `Specification` | Sirve para los filtros, pero devuelve entidades: no sabe proyectar a un DTO con `count` y `max`, ni ordenar por un agregado. |
| **Criteria API** | Cada criterio activado añade un `Predicate`. Tipada con el metamodelo. Proyecta, agrupa y ordena por lo que haga falta. |

Y su precio, que también hay que decir: es verbosa. Para una consulta fija, `@Query` gana.

## 3. Mapa: de cada control de la pantalla a la API

Todo está en [`BusquedaMascotasRepositoryImpl`](../../apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/repository/criteria/BusquedaMascotasRepositoryImpl.java).

| Control | Parámetro | Pieza de la Criteria API |
|---|---|---|
| Nombre contiene | `nombre` | `cb.like(cb.lower(p.name), "%x%")` |
| Tipo de animal | `tipos` (varios) | `cb.lower(tipo.name).in(lista)` |
| Nacida entre | `nacidaDesde`, `nacidaHasta` | `greaterThanOrEqualTo` / `lessThanOrEqualTo` |
| Apellido empieza por | `apellidoPropietario` | `cb.like(cb.lower(o.lastName), "x%")` |
| Vive en | `ciudades` (varios) | `o.city.in(lista)` |
| Número de visitas | `minVisitas`, `maxVisitas` | `cb.ge`/`cb.le` sobre una **subconsulta correlacionada** con `count` |
| Ha venido entre | `visitadaDesde`, `visitadaHasta` | `cb.exists(subconsulta)` |
| Revisión pendiente | `sinVisitasDesde` | `cb.not(cb.exists(subconsulta))` |
| Diagnosticada de / por | `enfermedades`, `veterinario` | `cb.exists(subconsulta con join Diagnose → Visit)` |
| Columnas *Visitas* y *Última visita* | (siempre) | `LEFT JOIN` + `groupBy` + `cb.count` / `cb.greatest`, proyectado con `cb.construct(FichaMascota.class, ...)` |
| Ordenar por columna | `sort=campo,dir` | `orderBy` dinámico con lista blanca + desempate por `id` |
| Paginación | `page`, `size` | `setFirstResult` / `setMaxResults` + consulta del total |

El resto de piezas:

| Fichero | Papel |
|---|---|
| [`FiltroMascotas`](../../apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/repository/criteria/FiltroMascotas.java) | `record` con un campo por control; `null` = "no aplicar". Spring MVC lo rellena desde la query string. |
| [`FichaMascota`](../../apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/repository/criteria/FichaMascota.java) | Fila de la tabla: proyección, no entidad. |
| [`BusquedaMascotasRepository`](../../apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/repository/criteria/BusquedaMascotasRepository.java) | Fragmento que `PetRepository` incorpora. |
| [`BusquedaMascotasRestController`](../../apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/web/api/BusquedaMascotasRestController.java) | `GET /api/v1/busqueda-mascotas` y `/opciones` (valores de los desplegables). |
| [`busqueda-mascotas.html`](../../apps/petclinic-api/src/main/resources/static/resources/busqueda-mascotas.html) | La maqueta: HTML y JavaScript sin dependencias. |
| [`BusquedaMascotasRepositoryTests`](../../apps/petclinic-api/src/test/java/org/springframework/samples/petclinic/repository/criteria/BusquedaMascotasRepositoryTests.java) | 18 pruebas: una por criterio, combinaciones, orden y paginación. |

## 4. Cómo contarlo

### 4.1 Demostración con la pantalla (15 min)

Arranca la aplicación con el SQL visible (ya lo está por defecto) y abre la pantalla al lado
de la consola.

1. **Sin filtros.** 13 mascotas. En la consola: una sola `select` con `left join visits`,
   `group by` y `order by`, y **otra** `select count(...)` para el paginador. Pregunta: *"¿por
   qué dos consultas?"*
2. **Activa *Tipo: cat*.** Aparece un `in` en el `where`. Activa también *Vive en: Monona*: se
   añade un `and`. Desactiva el tipo con la × del chip: el `in` desaparece del SQL.
   **Este es el mensaje central: el SQL se compone con lo que el usuario ha activado.**
3. **Número de visitas ≥ 1.** Aparece una subconsulta `(select count(...) from visits v
   where v.pet_id = p.id) >= ?`. Señala la correlación `v.pet_id = p.id`.
4. **Revisión pendiente desde 2013-01-04.** Salen 12: todas menos Samantha. Es un
   `not exists`. Pregunta: *"¿por qué sale Max, si tiene visitas?"* (Las suyas son anteriores.)
5. **Ordena por *Última visita*.** Se ordena por `max(v.visit_date)`, un agregado.
6. **Diagnosticada de Diabetes + Diagnosticada por James Carter.** Cero resultados, aunque
   Samantha tiene diabetes y Carter ha diagnosticado a Max. Lee la ayuda de la pantalla: es el
   **mismo diagnóstico**. Abre el debate: ¿era eso lo que quería el usuario? Es una decisión de
   negocio que la consulta tiene que reflejar.

### 4.2 El código (25 min)

Recorre `BusquedaMascotasRepositoryImpl` en este orden:

1. **`predicados(...)`.** Un `if` por criterio, cada uno añade un `Predicate` a la lista.
   `cb.and()` con la lista vacía es una conjunción siempre cierta, así que "sin filtros" no es
   un caso especial.
2. **Las subconsultas** `numeroDeVisitas`, `visitasEntre` y `diagnosticos`. En las tres está
   `sub.correlate(mascota)`.
3. **`buscarMascotas`.** `cb.construct` hacia el `record`, `groupBy` y el orden dinámico.
4. **`contar`.** La consulta del total.

Y cuatro decisiones que son el núcleo de la clase. Cada una tiene su prueba:

| Decisión | Si se hace al revés... | Prueba |
|---|---|---|
| **Los filtros de visitas van en subconsultas, no en el `LEFT JOIN` de visitas** que sirve para contar. | Filtrar "visitada el día 1" sobre ese join recorta también las columnas calculadas: Samantha saldría con 1 visita en vez de 2, y el `LEFT JOIN` se comporta como `INNER`. | `losFiltrosDeVisitasNoRecortanLasColumnasCalculadas` |
| **`minVisitas`/`maxVisitas` con subconsulta, no con `having`.** | La consulta del total no agrupa: no puede aplicar el `having` y el paginador muestra un total que no cuadra con las filas. | `paginacion` (11 sin visitas, no 13) |
| **Los predicados se construyen con un método que se llama una vez por consulta.** | Reutilizar los `Predicate` de la consulta principal en la del total falla: están atados a otra raíz. | (lo impide el diseño) |
| **El orden usa una lista blanca**, nunca `root.get(nombreRecibido)`. | El cliente podría ordenar por cualquier atributo, incluidos los que no ve, y deducir sus valores ordenando. | `ordenNoPermitido` y el 400 del controlador |

Dos detalles más que conviene señalar:

- **Desempate por `id` en el `orderBy`.** Sin un orden total, una mascota puede salir en dos
  páginas (o en ninguna) cuando hay empates, como las dos *Lucky*.
- **Todo lo que no es agregado va al `groupBy`.** H2 lo perdona en algunos casos; MySQL, con
  `ONLY_FULL_GROUP_BY`, no.

## 5. Ejercicios

Para hacer en clase si sobra tiempo, o para casa. Cada uno añade una pieza al filtro, a la
pantalla y a las pruebas.

1. **Rompe `correlate()`.** En `numeroDeVisitas`, cambia `sub.correlate(mascota)` por
   `sub.from(Pet.class)`. ¿Qué pruebas fallan y por qué? (La subconsulta cuenta las visitas de
   todas las mascotas.) Deshaz el cambio.
2. **Mueve `minVisitas` al `having`.** Hazlo en la consulta principal y ejecuta `paginacion`.
   Explica el total que sale. Deshaz el cambio.
3. **Nuevo criterio: edad.** `edadMinima`/`edadMaxima` sobre `Pet.age`. Fíjate en que `age`
   puede ser `null`: ¿qué debe pasar con esas mascotas? Decide y escribe la prueba.
4. **Nuevo criterio: especialidad del veterinario que diagnosticó.** Por ejemplo *"pacientes
   diagnosticados por un cirujano"*. Añade un join `Vet → specialties` dentro de la subconsulta
   de `diagnosticos`. ¿Sigue siendo el mismo diagnóstico si también se elige la enfermedad?
5. **Nueva columna ordenable: número de diagnósticos.** Pista: no basta con otro `LEFT JOIN`,
   porque multiplicaría las filas y `count(v)` dejaría de ser correcto. Usa una subconsulta en
   el `select` o `countDistinct`.
6. **Criterio con `OR`.** *"Nombre o apellido del propietario contiene..."* en un único campo de
   texto. Es la primera vez que un criterio no se combina con `and`: `cb.or(...)` dentro de la
   lista de predicados.

## 6. Comandos (PowerShell)

```powershell
# Arrancar la aplicación y abrir la pantalla
.\mvnw -pl apps/petclinic-api spring-boot:run
start http://localhost:8080/resources/busqueda-mascotas.html

# Pruebas del escenario
.\mvnw -pl apps/petclinic-api test "-Dtest=BusquedaMascotasRepositoryTests,BusquedaMascotasRestControllerMockMvcTests"

# La API sin la pantalla (también en Swagger UI, grupo "Busqueda avanzada de mascotas")
curl "http://localhost:8080/api/v1/busqueda-mascotas?tipos=cat&ciudades=Monona&minVisitas=1&sort=ultimaVisita,desc"
curl "http://localhost:8080/api/v1/busqueda-mascotas/opciones"
```

> Los datos de `data.sql` son pocos y desequilibrados: solo Samantha y Max (los dos de Jean
> Coleman, en Monona) tienen visitas, y solo hay dos diagnósticos. Para que los filtros de
> historial den juego en la demostración, da de alta antes un par de visitas desde la API o
> desde la pantalla de propietarios.
