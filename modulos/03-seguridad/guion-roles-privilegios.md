# Guion: roles, privilegios y jerarquía de roles

Ampliación del módulo 3. Código: [`extras/roles-privilegios-service`](../../extras/roles-privilegios-service) · puerto **8064**.

Basado en el modelo del artículo de Baeldung
[*Spring Security – Roles and Privileges*](https://www.baeldung.com/role-and-privilege-for-spring-security-registration),
con una corrección importante que el artículo no cubre (sección 4).

```bash
./mvnw -pl extras/roles-privilegios-service spring-boot:run
# Swagger UI: http://localhost:8064/swagger-ui.html   (botón Authorize → Basic)
# Usuarios: eva, rita, victor, pablo, admin · contraseña de todos: demo
```

---

## 1. La pregunta que responde

> *"¿`hasRole('ADMIN')` o `hasAuthority('admin')`? ¿Qué diferencia hay entre un
> rol y un permiso? Y si el administrador puede hacer todo lo que hace un
> veterinario, ¿tengo que escribir `hasAnyRole('VET', 'ADMIN')` en todas partes?"*

## 2. Para Spring Security, rol y privilegio son lo mismo

Spring Security solo conoce una cosa: `GrantedAuthority`, una cadena. La
diferencia entre rol y privilegio es **una convención de diseño**, no del
framework:

| | Rol | Privilegio |
|---|---|---|
| Qué representa | Un **puesto**: recepcionista, veterinario | Una **acción**: registrar una visita |
| Granularidad | Gruesa | Fina |
| Quién lo decide | El negocio, cambia con el organigrama | El código, cambia con los casos de uso |
| Prefijo por convención | `ROLE_` | ninguno |
| Cómo se comprueba | `hasRole('RECEPCION')` → busca `ROLE_RECEPCION` | `hasAuthority('VISITA_REGISTRAR')` |
| Dónde vive | Tabla `roles` + qué privilegios agrupa | Tabla `privilegios` + anotaciones en el código |

`hasRole('X')` es solo azúcar sintáctico para `hasAuthority('ROLE_X')`. Se puede
comprobar en clase: `@PreAuthorize("hasAuthority('ROLE_EMPLEADO')")` funciona
igual que `hasRole('EMPLEADO')`.

> **Mensaje clave.** El código pregunta por **privilegios** (qué se quiere
> hacer). Los roles sirven para **administrar**: agrupar privilegios y
> asignárselos a personas. Así, cambiar quién puede hacer algo es un `INSERT`,
> no un despliegue.

### El modelo de datos

```
usuarios ──< usuarios_roles >── roles ──< roles_privilegios >── privilegios
```

Un usuario nunca tiene privilegios sueltos: le llegan siempre a través de sus
roles. Datos del ejemplo (`configuration/DatosIniciales`):

| Rol | Privilegios **propios** |
|---|---|
| `ROLE_EMPLEADO` | `MASCOTA_LEER` |
| `ROLE_RECEPCION` | `MASCOTA_EDITAR`, `FACTURA_VER` |
| `ROLE_VETERINARIO` | `VISITA_REGISTRAR` |
| `ROLE_ADMIN` | `SEGURIDAD_GESTIONAR` |

| Usuario | Roles asignados |
|---|---|
| `eva` | `ROLE_EMPLEADO` |
| `rita` | `ROLE_RECEPCION` |
| `victor` | `ROLE_VETERINARIO` |
| `pablo` | `ROLE_RECEPCION` **y** `ROLE_VETERINARIO` (composición) |
| `admin` | `ROLE_ADMIN` |

Fíjate en que ningún rol repite los privilegios de los que "hereda": de eso se
encarga la jerarquía.

## 3. La jerarquía de roles

`SecurityConfiguration.roleHierarchy()`:

```
              ROLE_ADMIN
             /          \
   ROLE_RECEPCION    ROLE_VETERINARIO
             \          /
            ROLE_EMPLEADO
```

```java
@Bean
static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
            .role("ADMIN").implies("RECEPCION", "VETERINARIO")
            .role("RECEPCION").implies("EMPLEADO")
            .role("VETERINARIO").implies("EMPLEADO")
            .build();
}

// Para que @PreAuthorize también la use:
@Bean
static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    handler.setRoleHierarchy(roleHierarchy);
    return handler;
}
```

El artículo de Baeldung usa la forma en texto, que sigue siendo válida:
`RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_RECEPCION\nROLE_RECEPCION > ROLE_EMPLEADO ...")`.

Tres cosas que hay que tener claras:

1. **Se aplica al decidir, no al autenticar.** El `Authentication` de `admin`
   contiene solo `ROLE_ADMIN`. Cuando se evalúa `hasRole('EMPLEADO')`, Spring
   pregunta a la jerarquía qué autoridades son *alcanzables* desde las que hay.
   `GET /api/yo` enseña las dos listas lado a lado.
2. **Es un grafo, no un árbol.** `ADMIN` llega a `EMPLEADO` por dos caminos.
3. **Solo baja.** `RECEPCION` no alcanza `VETERINARIO` ni al revés.

**Jerarquía ≠ composición.** `pablo` tiene dos roles a la vez: sus permisos son
la unión. `admin` tiene uno solo, que *implica* otros. Las dos cosas se pueden
combinar.

## 4. La trampa: la jerarquía no sabe nada de privilegios

`RoleHierarchy` trabaja con **nombres**. Sabe que `ROLE_RECEPCION` implica
`ROLE_EMPLEADO`, pero no que `ROLE_EMPLEADO` agrupa `MASCOTA_LEER`: esa relación
está en la tabla `roles_privilegios`.

Si el `UserDetailsService` solo expande los privilegios de los roles
**asignados** (como hace el artículo de Baeldung), ocurre esto con `rita`:

| Comprobación | Resultado | Por qué |
|---|---|---|
| `hasRole('EMPLEADO')` | ✅ sí | lo resuelve la jerarquía |
| `hasAuthority('MASCOTA_LEER')` | ❌ **no** | nadie le ha dado ese privilegio |

Es decir: *"rita es empleada, pero no puede hacer lo que hace un empleado"*.

La corrección está en `security/UsuarioDetailsService`: antes de recoger
privilegios, se pregunta a la jerarquía por los roles alcanzables.

```java
Collection<Rol> efectivos = heredarPrivilegios ? alcanzables(asignados) : asignados;
efectivos.forEach(rol -> rol.getPrivilegios()
        .forEach(p -> autoridades.add(new SimpleGrantedAuthority(p.getNombre()))));
```

Se controla con `demo.roles.heredar-privilegios` en `application.properties`
para poder enseñar los dos comportamientos.

## 5. Mapa del código

| Fichero | Qué muestra |
|---|---|
| `model/Usuario`, `Rol`, `Privilegio` | Las tres entidades y las dos tablas intermedias |
| `configuration/DatosIniciales` | El `SetupDataLoader` de Baeldung, idempotente |
| `configuration/SecurityConfiguration` | Regla por **ruta** con `hasRole`, bean `RoleHierarchy` y `MethodSecurityExpressionHandler` |
| `security/UsuarioDetailsService` | Dónde roles y privilegios se convierten en `GrantedAuthority` |
| `service/ClinicaService` | `hasRole` (turnos) frente a `hasAuthority` (todo lo demás) |
| `service/GestionRolesService` | Conceder y retirar privilegios a un rol en caliente |
| `web/YoController` | `GET /api/yo`: autoridades guardadas frente a efectivas |

### Matriz de accesos (con herencia de privilegios)

| | eva | rita | victor | pablo | admin |
|---|:-:|:-:|:-:|:-:|:-:|
| `GET /api/turnos` · `hasRole('EMPLEADO')` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `GET /api/mascotas` · `MASCOTA_LEER` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `POST /api/mascotas` · `MASCOTA_EDITAR` | ❌ | ✅ | ❌ | ✅ | ✅ |
| `POST /api/visitas` · `VISITA_REGISTRAR` | ❌ | ❌ | ✅ | ✅ | ✅ |
| `GET /api/facturas` · `FACTURA_VER` | ❌ | ✅ | ❌ | ✅ | ✅ |
| `/api/admin/**` · ruta, `hasRole('ADMIN')` | ❌ | ❌ | ❌ | ❌ | ✅ |

Está entera como prueba parametrizada en `MatrizDeAccesoTests`.

---

## Ejercicios

### Ejercicio 1 · ¿Qué tengo guardado y qué puedo hacer?

Con Swagger UI, autenticarse sucesivamente como `eva`, `rita`, `pablo` y `admin`
y llamar a `GET /api/yo`.

1. ¿Por qué `ROLE_EMPLEADO` no aparece en `rolesAsignados` de `rita` pero sí en
   `autoridadesEfectivas`?
2. ¿Por qué `MASCOTA_LEER` sí aparece en `privilegios` de `rita` si no es un
   privilegio de `ROLE_RECEPCION`?
3. Compara `pablo` y `admin`. ¿Qué diferencia hay entre tener dos roles y tener
   un rol que implica otros dos?

<details><summary>Solución</summary>

1. Los roles heredados **no se guardan**: los calcula la jerarquía en cada
   decisión. El `Authentication` refleja solo lo asignado.
2. Porque `UsuarioDetailsService` pregunta a la jerarquía por los roles
   alcanzables (`RECEPCION` → `EMPLEADO`) y recoge también sus privilegios.
3. `pablo` tiene la **unión** de dos roles, pero no `SEGURIDAD_GESTIONAR` ni
   `ROLE_ADMIN`. `admin` tiene un solo rol guardado, y alcanza los demás por la
   jerarquía. Si mañana se añade un rol bajo `ADMIN`, `admin` lo gana solo;
   `pablo` no.
</details>

### Ejercicio 2 · La trampa del modelo ingenuo

1. Poner `demo.roles.heredar-privilegios=false` y reiniciar.
2. Como `rita`, llamar a `GET /api/turnos` y a `GET /api/mascotas`.
3. Como `admin`, llamar a `POST /api/visitas`.
4. Explicar el resultado. ¿Por qué `eva` no nota la diferencia?

<details><summary>Solución</summary>

- `rita` → `/api/turnos` **200** (`hasRole('EMPLEADO')`, lo resuelve la
  jerarquía) y `/api/mascotas` **403** (`hasAuthority('MASCOTA_LEER')`: ese
  privilegio es de `ROLE_EMPLEADO`, y solo se expanden los de los roles asignados).
- `admin` → `POST /api/visitas` **403**, aunque "es" veterinario.
- `eva` tiene asignado directamente `ROLE_EMPLEADO`: no hereda nada, así que
  no le afecta.

Moraleja: si se combinan jerarquía de roles y privilegios en base de datos, los
privilegios hay que expandirlos **siguiendo la jerarquía**. Lo comprueba
`SinHerenciaDePrivilegiosTests`.
</details>

### Ejercicio 3 · `hasRole` frente a `hasAuthority`

Recepción contrata a un becario que debe ver los turnos, pero nada más.

1. Hacerlo **sin tocar Java**: ¿qué hay que añadir en `DatosIniciales`?
2. Ahora, el becario también debe poder leer mascotas. ¿Basta con datos?
3. ¿Y si en lugar de eso el becario debe poder ver los turnos **y no** debe
   considerarse empleado a ningún efecto? ¿Qué tienes que tocar?

<details><summary>Solución</summary>

1. No se puede solo con datos: `turnos()` pregunta por `hasRole('EMPLEADO')`.
   O se le asigna `ROLE_EMPLEADO` (y entonces gana también `MASCOTA_LEER`), o
   hay que tocar la jerarquía o la anotación.
2. Si se le asigna `ROLE_EMPLEADO`, sí. Si se le crea un `ROLE_BECARIO` propio,
   basta con darle `MASCOTA_LEER` en `roles_privilegios`: es un `INSERT`.
3. Hay que cambiar el código: crear un privilegio `TURNO_VER`, poner
   `@PreAuthorize("hasAuthority('TURNO_VER')")` en `turnos()` y dárselo a
   `ROLE_EMPLEADO` y `ROLE_BECARIO`.

Es exactamente el argumento a favor de que el código pregunte por privilegios:
el único método que ha obligado a recompilar es el que preguntaba por un rol.
</details>

### Ejercicio 4 · Cambiar permisos sin redesplegar

Como `admin`:

```http
PUT    /api/admin/roles/RECEPCION/privilegios/VISITA_REGISTRAR
DELETE /api/admin/roles/EMPLEADO/privilegios/MASCOTA_LEER
```

1. Tras el `PUT`, ¿puede `rita` registrar visitas? ¿Hace falta que vuelva a
   iniciar sesión?
2. Tras el `DELETE`, ¿a quién afecta? ¿También a `admin`?
3. Si la API emitiera un JWT con las autoridades dentro (como `auth-service`),
   ¿se notaría el cambio igual de rápido?

<details><summary>Solución</summary>

1. Sí, en la siguiente petición. La API es `STATELESS` con Basic: el usuario se
   carga de la base de datos en **cada** petición.
2. A todos, incluido `admin`: todos alcanzan `ROLE_EMPLEADO` por la jerarquía,
   y ese era el único rol con `MASCOTA_LEER`.
3. No. Las autoridades quedan congeladas en el token hasta que caduca. Es el
   precio de no consultar la base de datos: tokens de vida corta, o meter en el
   token solo roles y resolver los privilegios en el servidor de recursos.

Lo comprueba `CambioDePrivilegiosTests`.
</details>

### Ejercicio 5 · Dos niveles de protección

`/api/admin/**` exige `hasRole('ADMIN')` en la cadena de filtros, y además
`conceder`/`retirar` exigen `hasAuthority('SEGURIDAD_GESTIONAR')` en el servicio.

1. Retirar `SEGURIDAD_GESTIONAR` a `ROLE_ADMIN` (con un `DELETE`). ¿Puede
   `admin` seguir listando roles? ¿Y concederlos?
2. Con una sola regla, ¿cuál de las dos dejarías y por qué?

<details><summary>Solución</summary>

1. Puede listar (`GET /api/admin/roles` solo tiene la regla por ruta), pero ya
   no puede conceder ni retirar: **403**. Se ha quitado a sí mismo la capacidad
   de deshacerlo; hay que arreglarlo en la base de datos. Buena excusa para
   hablar de no dejar sin protección la autogestión de permisos.
2. La del servicio: protege el caso de uso aunque se llame desde otro
   controlador, un `@Scheduled` o un mensaje. La regla por ruta es una red de
   seguridad gruesa y barata.
</details>

### Ejercicio 6 (reto) · La jerarquía también en base de datos

Ahora la jerarquía está en Java y los roles en base de datos: si se crea un rol
nuevo, hay que recompilar para colocarlo en la jerarquía.

1. Añadir a `Rol` una relación `@ManyToMany Set<Rol> implica`.
2. Construir el bean `RoleHierarchy` a partir de esa tabla (pista: generar el
   texto `"ROLE_A > ROLE_B\n..."` y pasarlo a `RoleHierarchyImpl.fromHierarchy`).
3. ¿Qué problema aparece si se modifica la jerarquía con la aplicación en marcha?
   ¿Cómo lo resolverías?

<details><summary>Pistas</summary>

- El bean se construye al arrancar: los cambios posteriores no se ven. Opciones:
  una implementación propia de `RoleHierarchy` que consulte (con caché) la
  tabla en cada llamada, o refrescar la instancia al modificar la tabla.
- Cuidado con los ciclos (`A > B > A`): `RoleHierarchyImpl` los detecta y lanza
  `CycleInRoleHierarchyException`. Hay que validarlo antes de guardar.
</details>

---

## Puntos de ingeniería a destacar

- **No mezclar convenciones.** `petclinic-api` usa autoridades sin prefijo
  (`hasAuthority('admin')`). Funciona, pero `hasRole('admin')` fallaría en
  silencio, porque buscaría `ROLE_admin`. Elegir una convención y mantenerla.
- **Las cadenas son frágiles.** Un error tipográfico en
  `hasAuthority('VISITA_REGISTAR')` no da error de compilación: simplemente deja
  a todo el mundo fuera. La matriz de pruebas parametrizada es la red de
  seguridad.
- **El prefijo se puede cambiar** con un bean `GrantedAuthorityDefaults`, pero
  rara vez compensa.
- **Cuándo no hace falta todo esto.** Con tres roles fijos que no cambian, un
  `hasRole` y nada más es perfectamente razonable. El modelo rol/privilegio
  compensa cuando el negocio quiere reorganizar permisos sin pasar por desarrollo.

## Referencias

- Baeldung, [*Spring Security – Roles and Privileges*](https://www.baeldung.com/role-and-privilege-for-spring-security-registration)
- Spring Security, [*Hierarchical Roles*](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#authz-hierarchical-roles)
