# Material de ampliación

Dos módulos que no entran en el temario obligatorio pero que responden a
preguntas que salen prácticamente siempre en el módulo 3.

No son necesarios para seguir el curso: el reactor los compila, pero se pueden
excluir del `pom.xml` raíz sin que nada más se resienta.

---

## `acl-service` — permisos por instancia · puerto 8063

**La pregunta que responde:** *"Vale, `@PreAuthorize("hasAuthority('vet')")`
protege el método. ¿Pero cómo digo que este veterinario concreto puede ver el
historial de esta mascota concreta y no el de las demás?"*

Las reglas del módulo 3 protegen **rutas** y **métodos**. Una ACL (*Access
Control List*) protege **objetos**: guarda en tablas quién puede hacer qué sobre
cada instancia.

```bash
./mvnw -pl extras/acl-service spring-boot:run
# http://localhost:8063
```

Guion completo: [`../modulos/03-seguridad/guion-acl.md`](../modulos/03-seguridad/guion-acl.md)

Lo interesante de mirar:

| Fichero | Qué muestra |
|---|---|
| `resources/schema.sql` | El esquema **oficial** de Spring Security ACL: `acl_sid`, `acl_class`, `acl_object_identity`, `acl_entry` |
| `service/AclPermissionService` | Cómo se conceden permisos sobre una instancia con `MutableAclService` |
| `service/ClinicalRecordServiceImpl` | `@PreAuthorize` y `@PostAuthorize` con `hasPermission(...)` |
| `configuration/DataInitializer` | Datos de ejemplo con sus permisos |

> **Aviso honesto para clase.** Las ACL son potentes y caras: cuatro tablas más,
> una consulta adicional por comprobación y una complejidad que se nota. Para la
> mayoría de los casos, una comprobación de propiedad como la de
> `OwnerService.findOwnerById(int, Principal)` resuelve el problema con una línea.
> Merece la pena enseñar las dos y decir cuándo compensa cada una.

---

## `oauth2-login-service` — inicio de sesión externo · puerto 8062

**La pregunta que responde:** *"¿Y si no quiero gestionar contraseñas? ¿Cómo
dejo que entren con Google o con la cuenta corporativa?"*

Muestra el flujo completo: el usuario se autentica en un proveedor externo, la
aplicación enlaza esa identidad externa con un usuario interno y emite **su
propio JWT**, de modo que el resto del sistema no necesita saber nada del
proveedor.

```bash
./mvnw -pl extras/oauth2-login-service spring-boot:run
# http://localhost:8062
```

| Fichero | Qué muestra |
|---|---|
| `configuration/SecurityConfiguration` | `oauth2Login()` conviviendo con el login local |
| `model/ExternalIdentity` | Enlace entre identidad externa y usuario interno |
| `service/ExternalIdentityService` | Alta automática la primera vez que alguien entra |
| `util/OAuth2JwtSuccessHandler` | Emisión del JWT propio tras el éxito del proveedor |
| `util/LocalJwtSuccessHandler` | Lo mismo para el login con usuario y contraseña |

### Credenciales de los proveedores

Sin configurarlas, el servicio arranca y el **login local** funciona; los botones
de Google y GitHub darán error al pulsarlos, porque los identificadores son
marcadores de posición.

Para probar de verdad hay que registrar la aplicación en el proveedor y exportar:

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export GITHUB_CLIENT_ID=...
export GITHUB_CLIENT_SECRET=...
```

> **Nunca se versionan.** Es uno de los errores de la lista del módulo 3. El
> `.gitignore` del repositorio ya ignora `register-oauth2-credentials.bat` y
> `application-local.properties` por ese motivo.

### Sobre las claves RSA

Las de `application.properties` son **de demostración**, generadas para este
curso y versionadas a propósito para que el módulo funcione recién clonado.
Están marcadas con un aviso en el propio fichero y se sobrescriben con
`JWT_PRIVATE_KEY` y `JWT_PUBLIC_KEY`.

---

## Comparación con `infra/auth-service`

Los dos emiten JWT, y compararlos es un buen ejercicio de diseño:

| | `infra/auth-service` | `extras/oauth2-login-service` |
|---|---|---|
| Autenticación | usuario y contraseña locales | proveedor externo **y** local |
| Firma | HS512 o RS256, elegible por perfil | RS256 |
| Diseño | interfaz `JwtTokenService` con dos implementaciones seleccionadas por perfil | una única clase `JwtTokenUtil` |
| Módulo | 3 (obligatorio) | ampliación |

**Pregunta para el aula:** ¿cuál de los dos diseños preferirías mantener, y por
qué? ¿Qué coste tiene la interfaz con dos implementaciones frente a la clase
única?
