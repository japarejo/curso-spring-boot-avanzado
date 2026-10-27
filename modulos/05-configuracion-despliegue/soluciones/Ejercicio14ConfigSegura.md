# Módulo 5 · Ejercicio 14 · Solución: consumir el Config Server de forma segura

Todo el código está ya en el repositorio y **desactivado por defecto**:

| Pieza | Fichero | Se activa con |
|---|---|---|
| Reglas de acceso y usuarios | `infra/config-server/src/main/java/.../config/SecurityConfiguration.java` | perfil `secure` |
| Claves de los usuarios | `infra/config-server/src/main/resources/application-secure.properties` | perfil `secure` |
| HTTPS en el servidor | `infra/config-server/src/main/resources/application-tls.properties` | perfil `tls` |
| Credenciales y TLS del cliente | `infra/bills-service/src/main/resources/application.properties` (bloque *Ejercicio 14*) | variables de entorno |
| Pruebas | `infra/config-server/src/test/java/.../config/ConfigServerSecurityTests.java` | `./mvnw -pl infra/config-server test` |

## Parte A · Basic y dos papeles

### Reglas del servidor

| Petición | Anónimo | `config-client` | `config-admin` |
|---|---|---|---|
| `GET /actuator/health`, `/actuator/info` | 200 | 200 | 200 |
| `GET /{aplicacion}/{perfil}` (leer configuración) | 401 | 200 | 200 |
| `POST /encrypt`, `POST /decrypt` | 401 | **403** | 200 |
| Resto de `/actuator/**` (`env`, `refresh`…) | 401 | 403 | 200 |

Por qué dos papeles: si `config-client` pudiera llamar a `/decrypt`, cualquier servicio
comprometido descifraría los secretos de todos los demás. Leer configuración y manejar la
clave de cifrado son permisos distintos.

`health` sigue siendo público porque Docker (`healthcheck` de `compose.yaml`) y Eureka lo
consultan sin credenciales, pero con el perfil `secure` ya no enseña detalles a un anónimo
(`show-details=when-authorized`).

### Arranque y comprobación (Windows PowerShell)

```powershell
# Consola 1: servidor
$env:CONFIG_CLIENT_PASSWORD="clave-cliente"
$env:CONFIG_ADMIN_PASSWORD="clave-admin"
$env:ENCRYPT_KEY="una-clave-larga-del-curso"
.\mvnw -pl infra/config-server spring-boot:run "-Dspring-boot.run.profiles=native,secure"
```

> El perfil `secure` se **suma** a `native`. Si se arranca solo con `secure`, se pierde
> `native` (el `spring.profiles.active` del fichero queda sustituido) y el servidor busca un
> repositorio Git que no existe.

```powershell
# Consola 2: comprobaciones
curl.exe -i http://localhost:8889/bills-service/development                                  # 401
curl.exe -i -u config-client:clave-cliente http://localhost:8889/bills-service/development   # 200
curl.exe -i -u config-client:clave-cliente http://localhost:8889/encrypt -d hola             # 403
curl.exe -u config-admin:clave-admin http://localhost:8889/encrypt -d hola                    # 200: texto cifrado
```

Con `Invoke-RestMethod`, en lugar de `-u`:

```powershell
$cliente = New-Object PSCredential("config-client", (ConvertTo-SecureString "clave-cliente" -AsPlainText -Force))
Invoke-RestMethod http://localhost:8889/bills-service/development -Credential $cliente
```

### El cliente

En `bills-service/src/main/resources/application.properties`:

```properties
spring.cloud.config.fail-fast=${CONFIG_FAIL_FAST:false}
spring.cloud.config.username=${CONFIG_CLIENT_USER:}
spring.cloud.config.password=${CONFIG_CLIENT_PASSWORD:}
```

Sin variables de entorno, el usuario y la clave quedan vacíos, el cliente no manda la
cabecera `Authorization` y todo funciona como en los ejercicios anteriores.

```powershell
# Consola 3: Bills identificándose
$env:CONFIG_CLIENT_USER="config-client"
$env:CONFIG_CLIENT_PASSWORD="clave-cliente"
$env:CONFIG_FAIL_FAST="true"
.\mvnw -pl infra/bills-service spring-boot:run
```

```powershell
Invoke-RestMethod http://localhost:8040/api/v1/bills/whoami/ana
# Hello! You're ana and you'll become a(n) Desarrollador...
```

### Lo que hay que ver en clase

1. **Sin credenciales, Bills no falla.** Arranca con `user.role=valor local (el
   config-server no ha respondido)` y en la consola solo aparece:

   ```text
   WARN ... ConfigServerConfigDataLoader : Could not locate PropertySource (... optional = true ...): 401 on GET request
   ```

   Es la consecuencia de `optional:configserver:` + `fail-fast=false`, que son cómodos en
   clase. Con seguridad son peligrosos: una clave mal puesta en producción deja el servicio
   con valores por defecto sin que nadie se entere.

2. **Con `CONFIG_FAIL_FAST=true` y clave mala, Bills no arranca:**

   ```text
   ConfigClientFailFastException: Could not locate PropertySource and the fail fast property is set, failing
   ```

   `fail-fast=true` gana a `optional:` y es lo que hay que usar en producción.

3. **Las credenciales no pueden ir en un fichero de perfil.** Se probó un
   `application-secure-config.properties` en Bills con `username`, `password` y `fail-fast`:
   el cliente pidió la configuración tres veces, las tres **sin** credenciales (401), y
   arrancó con los valores locales. La importación de `configserver:` se resuelve antes de
   activar los perfiles. Por eso las propiedades están en `application.properties`, leídas
   de variables de entorno.

## Parte B · HTTPS

### Generar el certificado

Desde la raíz del repositorio, en PowerShell. `keytool` viene con el JDK; si no está en el
`PATH`, se usa el de `JAVA_HOME`:

```powershell
$keytool = "$env:JAVA_HOME\bin\keytool.exe"
$tls = "infra/config-server/tls"
New-Item -ItemType Directory -Force $tls | Out-Null
$env:CONFIG_TLS_PASSWORD = "cambiala"

# 1. Par de claves y certificado autofirmado del servidor (válido para localhost)
& $keytool -genkeypair -alias config-server -keyalg RSA -keysize 2048 -validity 365 `
  -storetype PKCS12 -keystore "$tls/config-server.p12" -storepass $env:CONFIG_TLS_PASSWORD `
  -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"

# 2. Certificado público, sin la clave privada
& $keytool -exportcert -rfc -alias config-server -keystore "$tls/config-server.p12" `
  -storepass $env:CONFIG_TLS_PASSWORD -file "$tls/config-server.crt"

# 3. Almacén de confianza de Bills: solo contiene ese certificado
& $keytool -importcert -noprompt -alias config-server -file "$tls/config-server.crt" `
  -keystore "$tls/bills-truststore.p12" -storetype PKCS12 -storepass $env:CONFIG_TLS_PASSWORD
```

<details><summary>En bash</summary>

```bash
TLS=infra/config-server/tls; mkdir -p $TLS; export CONFIG_TLS_PASSWORD=cambiala
keytool -genkeypair -alias config-server -keyalg RSA -keysize 2048 -validity 365 \
  -storetype PKCS12 -keystore $TLS/config-server.p12 -storepass $CONFIG_TLS_PASSWORD \
  -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
keytool -exportcert -rfc -alias config-server -keystore $TLS/config-server.p12 \
  -storepass $CONFIG_TLS_PASSWORD -file $TLS/config-server.crt
keytool -importcert -noprompt -alias config-server -file $TLS/config-server.crt \
  -keystore $TLS/bills-truststore.p12 -storetype PKCS12 -storepass $CONFIG_TLS_PASSWORD
```

</details>

El `SAN` no es decorativo: sin `dns:localhost`, el cliente rechaza el certificado aunque
confíe en él, porque el nombre no coincide con el de la URL.

### Servidor por HTTPS

`application-tls.properties` usa un *SSL bundle* de Spring Boot:

```properties
server.ssl.bundle=config-server
spring.ssl.bundle.jks.config-server.keystore.location=file:${CONFIG_TLS_DIR}/config-server.p12
spring.ssl.bundle.jks.config-server.keystore.password=${CONFIG_TLS_PASSWORD}
spring.ssl.bundle.jks.config-server.keystore.type=PKCS12
spring.ssl.bundle.jks.config-server.key.alias=config-server
```

```powershell
# Consola 1 (mismas variables que en la parte A, más estas dos)
$env:CONFIG_TLS_DIR = (Resolve-Path infra/config-server/tls).Path -replace '\\','/'
$env:CONFIG_TLS_PASSWORD = "cambiala"
.\mvnw -pl infra/config-server spring-boot:run "-Dspring-boot.run.profiles=native,secure,tls"
```

> `CONFIG_TLS_DIR` tiene que ser una ruta **absoluta**: `spring-boot:run` arranca la
> aplicación desde la carpeta del módulo, no desde la raíz, y una ruta relativa no encuentra
> el fichero.

```powershell
curl.exe -i http://localhost:8889/actuator/health                                  # 400: habla HTTPS
curl.exe https://localhost:8889/actuator/health                                    # error: certificado no fiable
curl.exe --cacert infra/config-server/tls/config-server.crt https://localhost:8889/actuator/health   # {"status":"UP"}
```

### Cliente por HTTPS

En `bills-service/src/main/resources/application.properties` ya están:

```properties
spring.cloud.config.tls.enabled=${CONFIG_TLS_ENABLED:false}
spring.cloud.config.tls.trust-store=${CONFIG_TLS_TRUST_STORE:}
spring.cloud.config.tls.trust-store-password=${CONFIG_TLS_PASSWORD:}
spring.cloud.config.tls.trust-store-type=PKCS12
```

Y en su `pom.xml`, `httpclient5`: el cliente de Config Server solo aplica
`spring.cloud.config.tls.*` si encuentra Apache HttpClient 5 en el *classpath*. Sin esa
dependencia, las propiedades se ignoran.

```powershell
# Consola 3 (mismas variables que en la parte A, más estas)
$env:CONFIG_SERVER_URI = "https://localhost:8889"
$env:CONFIG_TLS_ENABLED = "true"
$env:CONFIG_TLS_TRUST_STORE = "file:" + ((Resolve-Path infra/config-server/tls/bills-truststore.p12).Path -replace '\\','/')
$env:CONFIG_TLS_PASSWORD = "cambiala"
.\mvnw -pl infra/bills-service spring-boot:run
```

Resultados comprobados:

| Cliente | Resultado |
|---|---|
| `https://` sin `CONFIG_TLS_ENABLED` | No arranca: `PKIX path building failed` (con `CONFIG_FAIL_FAST=true`) |
| `https://` con el almacén de confianza | Arranca y responde `Desarrollador` |

### Para la discusión

- **Autofirmado frente a CA.** En producción el certificado lo firma una CA (interna o
  pública) y el almacén de confianza contiene la CA, no cada certificado. Renovar un
  certificado ya no obliga a tocar todos los clientes.
- **mTLS.** Se añade `server.ssl.client-auth=need` al servidor y un `key-store` propio a
  cada cliente (`spring.cloud.config.tls.key-store`). El servidor identifica al cliente por
  su certificado y sobra la contraseña de Basic.
- **¿Y Eureka?** Con el perfil `tls`, el servidor se anuncia con su puerto seguro. Spring
  Boot Admin, que lo descubre por Eureka, necesita confiar en el mismo certificado para
  mostrarlo.
- **Las claves siguen en variables de entorno.** Es un avance frente a los ficheros, pero
  en un sistema real vienen de un almacén de secretos (Vault, Kubernetes Secrets, AWS
  Secrets Manager…) y no de la consola de quien despliega.
