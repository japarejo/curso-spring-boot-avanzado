# Módulo 2 · Ejercicio 8 · Solución: diseñar antes de programar

## 1. Las diez propuestas

| # | Propuesta | Qué incumple | Versión correcta |
|---|---|---|---|
| 1 | `GET /getAllOwners` | Verbo en la URI (diapositiva 45) | `GET /api/v1/owners` |
| 2 | `POST /owners/delete?id=3` | Verbo en la URI y método equivocado: borrar no es crear | `DELETE /api/v1/owners/3` |
| 3 | `GET /owners/3/pets/7/visits/` | Barra final (diapositiva 46) | `GET /api/v1/owners/3/pets/7/visits` |
| 4 | `GET /api/Owner_Pets?ownerId=3` | Mayúsculas y guion bajo (48); la relación jerárquica va en la ruta (46) | `GET /api/v1/owners/3/pets` |
| 5 | `POST /visits/create` | Verbo en la URI: `POST` sobre la colección ya significa crear | `POST /api/v1/visits` |
| 6 | `PUT /pets/7/rename?name=Leo` | Verbo en la URI; `PUT` sustituye el recurso **entero** | `PATCH /api/v1/pets/7` con `{"name":"Leo"}` |
| 7 | `GET /vets/listarConEspecialidades` | Verbo; la variante de representación es un parámetro | `GET /api/v1/vets?fields=…` o una proyección |
| 8 | `DELETE /owners` | Borra la colección entera: rara vez es lo que se quiere, y si lo es, debe ser explícito y protegido | Eliminarlo, o restringirlo a administración |
| 9 | `GET /api/v1.1/pets` | Versiones con decimales (diapositiva 47): un cambio menor no debe obligar a cambiar de URI | `GET /api/v1/pets` |
| 10 | `GET /visits?desde=…&hasta=…&ordenar=fecha&pagina=2` | Correcto en lo esencial. Solo es discutible mezclar idiomas en los parámetros | `GET /api/v1/visits?fromDate=…&toDate=…&sort=date&page=2` |

## 2. Seguridad e idempotencia

| | GET | HEAD | OPTIONS | POST | PUT | PATCH | DELETE |
|---|---|---|---|---|---|---|---|
| Seguro | Sí | Sí | Sí | No | No | No | No |
| Idempotente | Sí | Sí | Sí | **No** | Sí | **No**¹ | Sí |

¹ `PATCH` no es idempotente por definición (RFC 5789): `{"op":"add", …}` aplicado dos veces
da dos resultados. Un `PATCH` concreto **puede** serlo (un *merge patch* que fija valores lo
es), pero el cliente no puede darlo por supuesto.

Por qué importa: un cliente HTTP, un *proxy* o una pasarela pueden **reintentar** una
petición idempotente tras un corte de red sin preguntar. Un `POST` repetido crea dos visitas.
Para reintentar `POST` con seguridad hace falta una clave de idempotencia
(`Idempotency-Key`), que el servidor recuerda.

`DELETE` es idempotente aunque la segunda llamada devuelva 404: lo que se mide es **el
estado del servidor**, no la respuesta.

## 3. Códigos de estado

| Situación | Código | Comentario |
|---|---|---|
| Se crea una visita | **201 Created** + `Location` | Ver `RestfulVisitController` |
| Se borra una mascota que existía | **204 No Content** | 200 si se devuelve algo en el cuerpo |
| Se pide una mascota que no existe | **404 Not Found** | |
| El JSON de entrada no valida | **400 Bad Request** | 422 es aceptable; lo importante es ser coherente |
| Un `owner` intenta ver los datos de otro | **403 Forbidden** o **404** | 404 si no se quiere confirmar que el recurso existe |
| Nadie se ha autenticado | **401 Unauthorized** | Con `WWW-Authenticate` |
| Edición concurrente, el segundo llega tarde | **409 Conflict**, o **412** si se usó `If-Match` | Ejercicio 10 y laboratorio de transacciones avanzadas |
| Piden XML y solo hay JSON | **406 Not Acceptable** | Spring lo hace solo |

## 4. Qué incumple hoy el proyecto

- `/api/pets` no lleva versión mientras el resto sí (ejercicio 1).
- `DELETE /api/pets/{id}` devuelve 200 sin cuerpo; lo canónico sería 204.
- `PUT /api/pets/{id}` acepta en el cuerpo un `id` que puede no coincidir con el de la ruta.
- `/api/v1/bills-demo/rest-template` y compañía: son endpoints didácticos con verbos
  técnicos en la ruta. Aceptable para una demostración, no para una API pública.
