# Mapeo entidad–DTO con MapStruct

Material de ampliación del **módulo 2**. En `petclinic-api` los DTOs se rellenan a mano
(`OwnerResponse.from()`). Aquí se hace lo mismo con [MapStruct](https://mapstruct.org/):
se declara una interfaz y MapStruct **genera el código de copia al compilar**. No usa
reflexión ni hace nada en tiempo de ejecución: lo generado es Java normal, con getters y
setters, y se puede abrir y depurar.

## Qué hay

| Paquete | Contenido |
|---|---|
| `modelo` | Entidades JPA `Propietario`, `Mascota` y `Usuario` (con Lombok). Nunca salen por la API. |
| `dto` | Records: `PropietarioResponse` (detalle), `PropietarioResumen` (listado), `MascotaResponse` y `PropietarioRequest` (alta y PATCH). |
| `mapper` | `PropietarioMapper`: **solo la interfaz**. Es el protagonista. |
| `servicio` | Repositorio y servicio. Trabajan con entidades y no saben nada de DTOs. |
| `web` | El controlador recibe DTOs, llama al servicio con entidades y convierte la respuesta con el mapper. |

Casos de mapeo que cubre `PropietarioMapper`, de más fácil a más difícil:

| Caso | Cómo se resuelve |
|---|---|
| Campos con el mismo nombre | Se copian solos. No se escribe nada. |
| Aplanar `usuario.username` (que puede ser null) | `@Mapping(source = "usuario.username")`. MapStruct genera la comprobación de null. |
| Lista de objetos anidados | Se convierte sola con el método `toResponse(Mascota)`. |
| `LocalDate` → texto con formato | `dateFormat = "dd/MM/yyyy"` |
| Campo calculado con lógica propia | Método `default` con `@Named` y `qualifiedByName` (`edad`) |
| Campo calculado de una línea | `expression = "java(...)"` (`nombreCompleto`) |
| DTO → entidad nueva (alta) | `toEntity`, con `ignore = true` en lo que el cliente no puede fijar |
| Actualización parcial (PATCH) | `@MappingTarget` + `NullValuePropertyMappingStrategy.IGNORE` |

## Cómo se usa

Lleva su propio Maven embebido. Desde esta carpeta:

```bash
./mvnw test              # Windows: .\mvnw test
./mvnw spring-boot:run   # y luego GET localhost:8080/propietarios/1
```

Después, abre
**`target/generated-sources/annotations/.../mapper/PropietarioMapperImpl.java`**. Es el
código que ha escrito MapStruct: ponlo al lado de `OwnerResponse.from()` en `petclinic-api`.

`PropietarioMapperTests` prueba el mapper sin Spring (`Mappers.getMapper(...)`).
`PropietarioControllerTests` prueba la API completa con MockMvc.

## Ejercicios

1. **El compilador como red de seguridad.** Añade un campo `String email` a
   `PropietarioResponse` y compila. Falla con `Unmapped target property: "email"` gracias a
   `unmappedTargetPolicy = ERROR`. Quita esa opción del `@Mapper` y vuelve a compilar:
   ahora solo sale un aviso, y `email` llegaría siempre a `null`. Con ModelMapper o
   `BeanUtils.copyProperties` ni siquiera habría aviso.
2. **Trampa de Lombok.** En el `pom.xml`, quita `lombok-mapstruct-binding` y pon
   `mapstruct-processor` delante de `lombok` en `annotationProcessorPaths`. Compila y lee
   el error. Vuelve a dejarlo como estaba.
3. **Lo que el DTO necesita, el repositorio lo trae.** Quita el `@EntityGraph` de
   `findConMascotasById` y ejecuta las pruebas. El detalle falla con
   `LazyInitializationException`: el mapper recorre las mascotas cuando la transacción ya
   se ha cerrado, y `open-in-view=false` no deja ocultarlo.
4. **Mass assignment.** La prueba `alta` envía `"id": 1` en el JSON y comprueba que se
   ignora. Cambia el controlador para que reciba la entidad `Propietario` en vez de
   `PropietarioRequest` y razona qué pasaría.
5. **Un DTO más.** Añade `MascotaResumen(String nombre, String nombrePropietario)` y su
   método en el mapper, aplanando `propietario.nombre`.

## Cuándo usar qué

| Herramienta | Cuándo |
|---|---|
| Mapeo a mano (`from()`) | Pocos DTOs y pocos campos. No añade dependencias, pero un campo olvidado no avisa. |
| **MapStruct** | El caso general en una API con varios DTOs. Detecta errores al compilar y no cuesta nada en ejecución. |
| ModelMapper, `BeanUtils.copyProperties` | Mejor evitarlas. Funcionan por reflexión y, si un nombre no coincide, el campo queda a null sin avisar. |
| Proyecciones de Spring Data | Endpoints de solo lectura: el repositorio devuelve el DTO directamente y el `SELECT` trae solo esas columnas. |
| `@JsonView` (ver `OwnerViewsDemoRestController`) | Las representaciones son subconjuntos de un mismo objeto. Si cambian de forma, mejor un DTO por representación. |
