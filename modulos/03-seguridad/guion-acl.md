# Guion: Spring Security ACL en Petclinic

## 1. Objetivo del ejemplo

Este microservicio muestra un caso donde la autorizacion por roles no es suficiente.

En una clinica veterinaria, dos usuarios pueden tener el mismo rol `ROLE_VET`, pero no deberian poder editar todos los registros clinicos. La regla depende de cada registro concreto:

- Ana puede leer, editar y administrar el registro de Luna porque es la veterinaria propietaria.
- Bruno puede leer el registro de Luna, pero no editarlo.
- Bruno puede leer, editar y administrar el registro de Max porque es su propietario.
- Carla, como auxiliar, puede leer registros compartidos con `ROLE_ASSISTANT`, pero no editarlos.
- Admin tiene permisos ACL sobre todos los registros iniciales.

La idea clave: los roles explican que tipo de usuario eres; las ACL explican que puedes hacer sobre un objeto concreto.

## 2. Que es una ACL

ACL significa Access Control List. Es una lista de permisos asociada a una instancia de dominio.

En vez de preguntar solo:

```text
Tiene el usuario ROLE_VET?
```

preguntamos:

```text
Tiene este usuario permiso WRITE sobre el ClinicalRecord con id 1?
```

Spring Security ACL representa esa respuesta mediante entradas ACE, Access Control Entry. Cada ACE vincula:

- Un objeto de dominio concreto.
- Una identidad de seguridad, que puede ser un usuario o una autoridad.
- Un permiso, por ejemplo `READ`, `WRITE` o `ADMINISTRATION`.
- Si el permiso se concede o se deniega.

## 3. Esquema oficial usado

Spring Security ACL usa cuatro tablas. En este proyecto estan declaradas en:

```text
extras/acl-service/src/main/resources/schema.sql
```

El DDL corresponde al esquema oficial HyperSQL publicado por Spring Security. El microservicio usa H2 con `MODE=LEGACY` para mantener compatible la consulta oficial por defecto de `JdbcMutableAclService`: `call identity()`.

Las tablas son:

- `acl_sid`: guarda identidades de seguridad. Puede ser un principal, como `ana`, o una autoridad, como `ROLE_ASSISTANT`.
- `acl_class`: guarda el tipo Java del objeto protegido, por ejemplo `org.springframework.samples.aclmicroservice.model.ClinicalRecord`.
- `acl_object_identity`: representa una instancia protegida, por ejemplo `ClinicalRecord#1`.
- `acl_entry`: guarda cada permiso concedido o denegado sobre esa instancia.

Ejemplo conceptual:

```text
ClinicalRecord#1 -> ana -> READ
ClinicalRecord#1 -> ana -> WRITE
ClinicalRecord#1 -> ana -> ADMINISTRATION
ClinicalRecord#1 -> bruno -> READ
ClinicalRecord#1 -> ROLE_ASSISTANT -> READ
```

## 4. Como se mapea en Spring

La configuracion principal esta en:

```text
extras/acl-service/src/main/java/org/springframework/samples/aclmicroservice/configuration/SecurityConfiguration.java
```

Piezas relevantes:

- `@EnableMethodSecurity(prePostEnabled = true)` activa anotaciones como `@PreAuthorize` y `@PostFilter`.
- `JdbcMutableAclService` lee y escribe las ACL en las cuatro tablas oficiales.
- `BasicLookupStrategy` carga ACLs desde JDBC.
- `SpringCacheBasedAclCache` cachea ACLs en memoria para evitar consultas repetidas.
- `AclPermissionEvaluator` conecta las expresiones `hasPermission(...)` con el modulo ACL.
- `MethodSecurityExpressionHandler` registra ese `PermissionEvaluator` para que funcione en seguridad por metodo.

El objeto protegido es:

```text
ClinicalRecord
```

Spring lo identifica internamente con:

```java
new ObjectIdentityImpl(ClinicalRecord.class, recordId)
```

Cuando se concede un permiso a un usuario se usa:

```java
new PrincipalSid("ana")
```

Cuando se concede a un rol se usa:

```java
new GrantedAuthoritySid("ROLE_ASSISTANT")
```

## 5. Donde se crean las ACL del ejemplo

La carga inicial esta en:

```text
extras/acl-service/src/main/java/org/springframework/samples/aclmicroservice/configuration/DataInitializer.java
```

Al arrancar se crean dos registros:

- `Luna`, propiedad de `ana`.
- `Max`, propiedad de `bruno`.

Para cada registro se crea una ACL con:

```java
aclService.createAcl(identity(saved));
```

Despues se insertan ACEs:

```java
acl.insertAce(..., BasePermission.READ, owner, true);
acl.insertAce(..., BasePermission.WRITE, owner, true);
acl.insertAce(..., BasePermission.ADMINISTRATION, owner, true);
```

El propietario recibe `READ`, `WRITE` y `ADMINISTRATION`. Admin recibe esos mismos permisos por autoridad `ROLE_ADMIN`.

## 6. Regla de negocio implementada

La regla de negocio de autorizacion por metodo es:

Un registro clinico solo puede ser visto, editado o compartido si el usuario autenticado tiene el permiso ACL adecuado sobre esa instancia concreta.

La traduccion a permisos es:

- Listar registros: se cargan todos desde el repositorio, pero el servicio devuelve solo aquellos sobre los que el usuario tiene `READ`.
- Ver detalle: requiere `READ` sobre el `ClinicalRecord` solicitado.
- Editar: requiere `WRITE` sobre el `ClinicalRecord`.
- Crear: requiere `ROLE_VET`; el registro nuevo nace con ACL de propietario para el usuario creador.
- Compartir lectura o escritura: requiere `ADMINISTRATION` sobre el `ClinicalRecord`.

Esta regla esta definida en:

```text
extras/acl-service/src/main/java/org/springframework/samples/aclmicroservice/service/ClinicalRecordService.java
```

Ejemplos:

```java
@PostFilter("hasPermission(filterObject, 'READ')")
List<ClinicalRecord> findVisibleRecords();
```

El metodo puede consultar todos los registros, pero el resultado se filtra por ACL antes de volver al controlador.

```java
@PreAuthorize("hasPermission(#id, 'org.springframework.samples.aclmicroservice.model.ClinicalRecord', 'WRITE')")
ClinicalRecord updateRecord(Long id, RecordForm form);
```

El metodo no se ejecuta si el usuario no tiene permiso `WRITE` sobre ese registro.

```java
@PreAuthorize("hasPermission(#id, 'org.springframework.samples.aclmicroservice.model.ClinicalRecord', 'ADMINISTRATION')")
void grantRead(Long id, String username);
```

Solo quien administra el objeto puede compartirlo con otros usuarios.

## 7. Por que la autorizacion esta en servicios

La UI oculta botones cuando el usuario no tiene permiso, pero eso es solo ergonomia.

La seguridad real esta en la capa de servicio. Si alguien intenta llamar directamente a una URL de edicion, el metodo protegido bloquea la operacion igualmente.

Ejemplo practico:

- Bruno puede leer Luna porque tiene `READ`.
- Bruno no puede abrir `/records/1/edit` porque no tiene `WRITE`.
- El servicio lanza una denegacion de acceso antes de devolver el formulario de edicion.

## 8. Flujo para demostrarlo en clase

1. Arrancar el microservicio:

```powershell
.\mvnw.cmd -pl microservices\acl-microservice spring-boot:run
```

2. Abrir:

```text
http://localhost:8063/login
```

3. Entrar como `ana/demo`.

Resultado esperado: ve Luna y puede editarla. No ve Max.

4. Entrar como `bruno/demo`.

Resultado esperado: ve Luna y Max, pero solo puede editar Max.

5. Entrar como `carla/demo`.

Resultado esperado: ve registros compartidos con `ROLE_ASSISTANT`, pero no puede editarlos.

6. Entrar como `admin/admin`.

Resultado esperado: ve y administra todos los registros iniciales.

7. Como Ana, abrir Luna y compartir permiso de lectura o edicion con otro usuario.

Resultado esperado: el usuario compartido gana acceso sin cambiar su rol global.

## 9. Puntos de ingenieria a destacar

- El esquema ACL es el oficial de Spring Security para HyperSQL, no un esquema inventado.
- H2 se usa solo como base embebida de demo.
- `JdbcMutableAclService` mantiene el contrato estandar de Spring Security ACL.
- El filtrado visual de Thymeleaf no sustituye a la seguridad de negocio.
- Las reglas estan en servicios, no dispersas en controladores.
- Crear un objeto de dominio y crear su ACL son dos pasos separados; Spring Security no crea ACLs automaticamente por cada entidad JPA.
- El uso de `{noop}` en usuarios demo se limita al ejemplo formativo. En produccion se usaria un encoder fuerte y usuarios persistidos.

## 10. Referencias oficiales

- Spring Security Reference, Domain Object Security ACLs: https://docs.spring.io/spring-security/reference/servlet/authorization/acls.html
- Spring Security Reference, ACL database schema: https://docs.spring.io/spring-security/reference/servlet/appendix/database-schema.html
