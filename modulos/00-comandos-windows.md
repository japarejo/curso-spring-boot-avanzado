# Comandos del curso en Windows (PowerShell)

Todo el material del curso está escrito con comandos de estilo Unix (`./mvnw`, `curl -s`,
`jq`, `export`), porque es lo que se lee mejor en un documento. En Windows la mayoría
funcionan igual, pero hay **siete** que no, y tres de ellas fallan de una forma que
desconcierta bastante.

Este documento es la traducción completa. Los comandos de aquí están verificados en
**Windows 11 con Windows PowerShell 5.1**, que es la consola que sale por defecto.

## Antes de nada: qué consola estás usando

```powershell
$PSVersionTable.PSVersion
```

| Resultado | Consola | Nota |
|---|---|---|
| `5.1.x` | **Windows PowerShell** | La que sale por defecto. Todo lo de este documento aplica. |
| `7.x` | PowerShell 7 (`pwsh`) | Ahí sí existen `&&` y `??`. El resto sigue igual. |

Si tienes **Git Bash** instalado (viene con Git para Windows), los comandos de los guiones
funcionan tal cual, sin traducir nada:

```powershell
& "C:\Program Files\Git\bin\bash.exe" -l
```

Esa es la salida más rápida si algo se resiste. Pero merece la pena conocer las siete
diferencias, porque en la máquina de un cliente puede no haber Git Bash.

---

## Las siete diferencias

### 1. `curl` **no es** curl

Esta es la que más tiempo hace perder. En Windows PowerShell 5.1, `curl` es un **alias** de
`Invoke-WebRequest`, un cmdlet que no entiende ni `-s`, ni `-d`, ni `-H` de la misma manera:

```powershell
curl -s localhost:8080/api/pets
# Invoke-WebRequest: No se puede encontrar un parámetro que coincida con el nombre 's'.
```

Windows 11 **sí** trae el curl de verdad, en `C:\Windows\System32\curl.exe`. Solo hay que
llamarlo con la extensión para que el alias no se interponga:

```powershell
curl.exe -s localhost:8080/api/pets
```

Comprobarlo:

```powershell
Get-Alias curl                 # Definition: Invoke-WebRequest
(Get-Command curl.exe).Source  # C:\Windows\system32\curl.exe
```

> **Regla para todo el curso:** donde los guiones digan `curl`, escribe `curl.exe`. Nada más.

Y si prefieres lo nativo de PowerShell, `Invoke-RestMethod` devuelve **objetos ya
deserializados**, lo que hace innecesario `jq`:

```powershell
Invoke-RestMethod http://localhost:8080/api/pets | Format-Table id, name
```

### 2. `jq` no viene instalado

Los guiones usan `| jq` para leer JSON. Hay tres salidas, de menos a más cómoda:

```powershell
# a) Sin instalar nada: PowerShell sabe leer JSON
curl.exe -s localhost:8080/api/pets | ConvertFrom-Json | ConvertTo-Json -Depth 10

# b) Mejor: Invoke-RestMethod ya devuelve objetos
Invoke-RestMethod http://localhost:8080/api/pets | Select-Object id, name

# c) Y si quieres el jq de verdad, que es lo que dicen los guiones
winget install jqlang.jq
```

Equivalencias frecuentes:

| En los guiones | En PowerShell |
|---|---|
| `... \| jq` | `... \| ConvertFrom-Json \| ConvertTo-Json -Depth 10` |
| `... \| jq -r .token` | `(... \| ConvertFrom-Json).token` |
| `... \| jq '.page.totalElements'` | `(... \| ConvertFrom-Json).page.totalElements` |
| `... \| jq 'keys \| length'` | `(... \| ConvertFrom-Json).PSObject.Properties.Count` |

### 3. `&&` no existe en PowerShell 5.1

```powershell
cd apps && .\mvnw test        # error de sintaxis
```

Alternativas:

```powershell
# Encadenar sin condición
cd apps; .\mvnw test

# Encadenar solo si lo anterior fue bien
.\mvnw compile; if ($?) { .\mvnw test }
```

En PowerShell 7 (`pwsh`) `&&` y `||` sí funcionan.

### 4. El `$` de las clases anidadas hay que escaparlo

Las pruebas de este curso usan clases `@Nested`, y el filtro de Surefire las nombra con `$`.
En PowerShell, `$` empieza una variable, así que `$Subconsulta` se expande a nada:

```powershell
# MAL: el filtro llega como "VisitCriteriaRepositoryTests"
.\mvnw -pl apps/petclinic-api test -Dtest=VisitCriteriaRepositoryTests$Subconsulta

# BIEN: acento grave (`) antes del $, dentro de comillas dobles
.\mvnw -pl apps/petclinic-api test "-Dtest=VisitCriteriaRepositoryTests`$Subconsulta"

# BIEN: o comillas simples, que no interpretan nada
.\mvnw -pl apps/petclinic-api test '-Dtest=VisitCriteriaRepositoryTests$Subconsulta'
```

El acento grave es la tecla de escape de PowerShell, donde Bash usa la barra invertida.

### 5. `export` es `$env:`

| Bash | PowerShell |
|---|---|
| `export TOKEN=abc` | `$env:TOKEN = "abc"` |
| `echo $TOKEN` | `$env:TOKEN` |
| `unset TOKEN` | `Remove-Item Env:\TOKEN` |
| `TOKEN=abc ./mvnw ...` | `$env:TOKEN = "abc"; .\mvnw ...` |

Para una variable que solo usa la sesión de PowerShell y no los procesos hijos, basta
`$token = "abc"`. Los guiones del módulo 3 usan `$TOKEN` como variable de entorno porque la
leen procesos externos; en PowerShell casi siempre te vale la variable normal.

### 6. La continuación de línea es `` ` ``, no `\`

```bash
# En los guiones
curl -s -X POST localhost:8060/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin1","password":"4dm1n"}'
```

```powershell
# En PowerShell
curl.exe -s -X POST localhost:8060/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"admin1\",\"password\":\"4dm1n\"}'
```

Fíjate en el JSON: al pasarlo a un **ejecutable externo**, PowerShell 5.1 se come las comillas
dobles y hay que escaparlas con barra invertida. Es feo y es fácil equivocarse, así que para
enviar JSON es mejor `Invoke-RestMethod`, que no tiene este problema:

```powershell
$respuesta = Invoke-RestMethod -Method Post http://localhost:8060/api/v1/auth/login `
  -ContentType "application/json" `
  -Body (@{ username = "admin1"; password = "4dm1n" } | ConvertTo-Json)
$respuesta.token
```

### 7. Dos detalles de `Invoke-RestMethod` y de las URL

`Invoke-RestMethod` **exige el esquema**. Sin `http://` no adivina nada:

```powershell
Invoke-RestMethod localhost:8080/api/pets
# No se reconoce el prefijo URI.
```

Interpreta `localhost:` como un esquema de URI, igual que `mailto:` o `file:`. Con
`curl.exe` no pasa, porque curl asume `http` cuando falta. Así que en este documento todas
las llamadas a `Invoke-RestMethod` llevan `http://` y las de `curl.exe` no: no es una
inconsistencia, es esta diferencia.

Y el `&` de las URL con varios parámetros es un **operador reservado** de PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/visits?page=0&size=2
# No se permite usar el carácter de Y comercial (&).
```

Se arregla poniendo la URL **entre comillas**, que es lo que conviene hacer siempre:

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/visits?page=0&size=2"
```

### Y una que sí funciona: `.\mvnw`

`./mvnw` con barra normal no funciona, pero `.\mvnw` **sí**, sin poner `.cmd`: PowerShell
resuelve la extensión sola. Comprobado:

```powershell
.\mvnw -v
# Apache Maven 3.9.7
# Java version: 21.0.11
```

Lo único: hay que ejecutarlo **desde la raíz del repositorio**, donde está el wrapper.

---

## Tabla de equivalencias rápida

| Bash | PowerShell 5.1 |
|---|---|
| `./mvnw` | `.\mvnw` |
| `curl` | `curl.exe` (o `Invoke-RestMethod`) |
| `jq` | `ConvertFrom-Json` |
| `grep patrón` | `Select-String patrón` |
| `grep -c patrón` | `(Select-String patrón).Count` |
| `head -30` | `Select-Object -First 30` |
| `tail -f fichero` | `Get-Content fichero -Wait -Tail 20` |
| `wc -l` | `(Get-Content f \| Measure-Object -Line).Lines` |
| `find . -name "*.java"` | `Get-ChildItem -Recurse -Filter *.java` |
| `cat` | `Get-Content` |
| `rm -rf dir` | `Remove-Item -Recurse -Force dir` |
| `export V=x` | `$env:V = "x"` |
| `cmd1 && cmd2` | `cmd1; if ($?) { cmd2 }` |
| `\` al final de línea | `` ` `` al final de línea |
| `$(comando)` | `$(comando)` (igual) |
| `which java` | `(Get-Command java).Source` |
| `lsof -i :8080` | `Get-NetTCPConnection -LocalPort 8080` |
| `kill -9 PID` | `Stop-Process -Id PID -Force` |

---

## Comprobación del entorno

```powershell
# Java: tiene que decir 21
java -version

# Y sobre todo esto, que es la versión que usa Maven
.\mvnw -v

# Docker (necesario para los módulos 4 y 5)
docker --version
docker compose version

# Qué proceso ocupa el 8080, cuando "Port 8080 is already in use"
Get-NetTCPConnection -LocalPort 8080 -State Listen |
    Select-Object LocalPort, OwningProcess,
        @{ n = "Proceso"; e = { (Get-Process -Id $_.OwningProcess).ProcessName } }

# Y para liberarlo
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess -Force
```

---

## Módulo 0 — Primera puesta en marcha

```powershell
git clone <repositorio>
Set-Location curso-spring-boot-avanzado

.\mvnw -v          # Java version: 21.x
.\mvnw verify      # BUILD SUCCESS. La primera vez tarda varios minutos.

.\mvnw -pl apps/petclinic-api spring-boot:run
```

Después, en el navegador: <http://localhost:8080>, usuario `owner1`, contraseña `0wn3r`.

| Dirección | Qué es |
|---|---|
| <http://localhost:8080> | Aplicación |
| <http://localhost:8080/swagger-ui.html> | API documentada |
| <http://localhost:8080/h2-console> | Base de datos en memoria |
| <http://localhost:8080/actuator> | Diagnóstico |

> **Consejo de aula.** `.\mvnw verify` la primera vez descarga medio Maven Central. Hazlo
> **en casa la noche antes**: veinte personas descargando a la vez saturan la red del aula.

---

## Módulo 1 — Introducción

```powershell
# Cuántas dependencias arrastra un starter
.\mvnw -pl apps/petclinic-api dependency:list |
    Select-String ":compile" | Measure-Object | Select-Object -ExpandProperty Count

# De dónde sale spring-web
.\mvnw -pl apps/petclinic-api dependency:tree "-Dincludes=org.springframework:spring-web"

# Cuántos beans crea el arranque (con la aplicación en marcha)
(Invoke-RestMethod http://localhost:8080/actuator/beans).contexts.application.beans.PSObject.Properties.Count

# Ver el informe de autoconfiguración
.\mvnw -pl apps/petclinic-api spring-boot:run "-Dspring-boot.run.arguments=--debug"
```

---

## Módulo 2 — APIs REST

```powershell
# Listado y detalle
Invoke-RestMethod http://localhost:8080/api/pets | Select-Object -First 5
Invoke-RestMethod http://localhost:8080/api/pets/1

# Paginación y ordenación
Invoke-RestMethod "http://localhost:8080/api/v1/visits?page=0&size=2&sort=date,desc"

# Filtros
(Invoke-RestMethod "http://localhost:8080/api/v1/visits?petName=Leo").page.totalElements

# Un 404 bien hecho: ProblemDetail (RFC 9457).
# Invoke-RestMethod lanza excepción con los 4xx, así que aquí conviene curl.exe
curl.exe -i localhost:8080/api/pets/99999

# Lista blanca de ordenación: 400, no 500
curl.exe -i "localhost:8080/api/v1/visits?sort=unknown"

# Versionado por cabecera
curl.exe -s -H "X-API-Version: 2" localhost:8080/api/v1/pets/1

# CORS
curl.exe -i -X OPTIONS localhost:8080/api/v1/visits `
  -H "Origin: http://localhost:3000" `
  -H "Access-Control-Request-Method: POST"

# Comparativa de clientes HTTP (hace falta bills-service en otra terminal)
Invoke-RestMethod http://localhost:8080/api/v1/bills-demo/rest-template
Invoke-RestMethod http://localhost:8080/api/v1/bills-demo/rest-client
Invoke-RestMethod http://localhost:8080/api/v1/bills-demo/feign
```

> **`Invoke-RestMethod` y los errores.** Con un 4xx o 5xx lanza una excepción en lugar de
> devolver el cuerpo, lo que es un problema justo cuando lo que quieres ver **es** el cuerpo
> del error. Para eso, `curl.exe -i`. O envolverlo:
>
> ```powershell
> try { Invoke-RestMethod http://localhost:8080/api/pets/99999 }
> catch { $_.ErrorDetails.Message }
> ```

---

## Módulo 3 — Seguridad

```powershell
# El auth-service, en otra terminal
.\mvnw -pl infra/auth-service spring-boot:run

# Login y token, sin pelearse con las comillas
$credenciales = @{ username = "vet1"; password = "v3t" } | ConvertTo-Json
$sesion = Invoke-RestMethod -Method Post http://localhost:8060/api/v1/auth/login `
    -ContentType "application/json" -Body $credenciales
$env:TOKEN = $sesion.token
$env:TOKEN

# Ver el contenido del token sin validarlo (lo que hace jwt.io)
$carga = $env:TOKEN.Split(".")[1].Replace("-", "+").Replace("_", "/")
while ($carga.Length % 4) { $carga += "=" }
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($carga)) |
    ConvertFrom-Json | ConvertTo-Json

# Validar el token
Invoke-RestMethod -Method Post http://localhost:8060/api/v1/auth/validate `
    -Headers @{ Authorization = "Bearer $env:TOKEN" }

# Un token manipulado: "valid": false
Invoke-RestMethod -Method Post http://localhost:8060/api/v1/auth/validate `
    -Headers @{ Authorization = "Bearer $($env:TOKEN)X" }

# Llamar a la API con y sin token
curl.exe -i localhost:8080/api/v1/visits                                  # 401
curl.exe -i -H "Authorization: Bearer $env:TOKEN" localhost:8080/api/v1/visits   # 200

# Firma asimétrica en lugar de simétrica
.\mvnw -pl infra/auth-service spring-boot:run "-Dspring-boot.run.profiles=rsa"

# Pruebas de seguridad de la aplicación
.\mvnw -pl apps/petclinic-api test "-Dtest=PetControllerSecurityTest"
```

Credenciales de demostración:

| Usuario | Contraseña | Autoridad |
|---|---|---|
| `admin1` | `4dm1n` | admin |
| `owner1` … `owner10` | `0wn3r` | owner |
| `vet1` | `v3t` | vet |

---

## Módulo 4 — Pruebas

```powershell
# Rápidas (*Test, *Tests). No necesitan Docker.
.\mvnw test

# Todo, incluidas las *IT de Testcontainers, más el informe de cobertura.
# Necesita Docker Desktop arrancado.
.\mvnw verify

# Una clase
.\mvnw -pl apps/petclinic-api test "-Dtest=OwnerServiceTests"

# Varias, con comodín
.\mvnw -pl apps/petclinic-api test "-Dtest=Owner*Tests"

# Un solo método
.\mvnw -pl apps/petclinic-api test "-Dtest=OwnerServiceTests#shouldFindOwnersByLastName"

# Una clase anidada: ojo al acento grave antes del $
.\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests`$BloqueoOptimista"

# Excluir las que salen a Internet
.\mvnw test "-Dgroups=!external-api"

# Informe de cobertura
.\mvnw verify
Start-Process apps\petclinic-api\target\site\jacoco\index.html

# Informes de las pruebas, cuando algo falla y quieres el detalle
Get-Content apps\petclinic-api\target\surefire-reports\*.txt |
    Select-String -Pattern "FAILED|ERROR" -Context 0,5
```

---

## Módulo 5 — Configuración y despliegue

```powershell
# Perfiles
.\mvnw -pl apps/petclinic-api spring-boot:run "-Dspring-boot.run.profiles=mysql"
.\mvnw -pl apps/petclinic-api spring-boot:run "-Dspring-boot.run.profiles=nplus1"

# Sobrescribir una propiedad desde la línea de comandos
.\mvnw -pl apps/petclinic-api spring-boot:run `
  "-Dspring-boot.run.arguments=--server.port=8081 --petclinic.bills.base-url=http://localhost:8040"

# Y por variable de entorno, que es lo que se usa en un contenedor
$env:SERVER_PORT = "8081"
.\mvnw -pl apps/petclinic-api spring-boot:run
Remove-Item Env:\SERVER_PORT

# Actuator
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/actuator/metrics/jvm.memory.used
curl.exe -s localhost:8080/actuator/prometheus | Select-Object -First 30

# Cambiar el nivel de log en caliente, sin reiniciar
Invoke-RestMethod -Method Post http://localhost:8080/actuator/loggers/org.springframework.samples.petclinic `
    -ContentType "application/json" -Body '{"configuredLevel":"DEBUG"}'
Invoke-RestMethod http://localhost:8080/actuator/loggers/org.springframework.samples.petclinic

# Solo la base de datos, que es el uso habitual en clase
docker compose -f docker/compose.yaml up -d mysql
docker compose -f docker/compose.yaml logs -f mysql

# Toda la infraestructura
docker compose -f docker/compose.yaml up --build

# En segundo plano, y mirando cómo van arrancando
docker compose -f docker/compose.yaml up -d --build
docker compose -f docker/compose.yaml ps
docker compose -f docker/compose.yaml logs -f petclinic-api

# Config Server: la propiedad servida
Invoke-RestMethod http://localhost:8889/bills-service/development

# Eureka: los servicios registrados
Invoke-RestMethod http://localhost:8761/eureka/apps -Headers @{ Accept = "application/json" }

# Parar y limpiar (el -v borra también el volumen de MySQL)
docker compose -f docker/compose.yaml down
docker compose -f docker/compose.yaml down -v

# Imagen con buildpacks
.\mvnw -pl apps/petclinic-api spring-boot:build-image
```

---

## Módulo 6 — Spring Data y JPA

```powershell
# El laboratorio de N+1
.\mvnw -pl apps/petclinic-api spring-boot:run "-Dspring-boot.run.profiles=nplus1"

# Transacciones: lo del temario
.\mvnw -pl apps/petclinic-api test "-Dtest=TransactionalExamplesVerificationTests"

# Transacciones avanzadas (material adicional)
.\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests"

# Criteria API (material adicional)
.\mvnw -pl apps/petclinic-api test "-Dtest=VisitCriteriaRepositoryTests"

# Las dos tandas de transacciones seguidas
.\mvnw -pl apps/petclinic-api test "-Dtest=Transaccion*VerificationTests"

# El metamodelo estático que genera hibernate-jpamodelgen
.\mvnw -pl apps/petclinic-api compile
Get-ChildItem -Recurse apps\petclinic-api\target\generated-sources\annotations -Filter *_.java |
    Select-Object -ExpandProperty Name

# Flyway contra MySQL real
docker compose -f docker/compose.yaml up -d mysql
.\mvnw -pl apps/petclinic-api spring-boot:run "-Dspring-boot.run.profiles=mysql"

# El historial de migraciones
docker compose -f docker/compose.yaml exec mysql `
    mysql -uroot -proot petclinic -e "SELECT installed_rank, version, description, success FROM flyway_schema_history;"

# Un cliente de MySQL interactivo (para el ejercicio de bloqueo pesimista)
docker compose -f docker/compose.yaml exec mysql mysql -uroot -proot petclinic
```

---

## Extras

```powershell
.\mvnw -pl extras/acl-service spring-boot:run          # http://localhost:8063
.\mvnw -pl extras/oauth2-login-service spring-boot:run # http://localhost:8062
```

Las credenciales de los proveedores OAuth2 se pasan por entorno, y **no se versionan**:

```powershell
$env:GOOGLE_CLIENT_ID     = "..."
$env:GOOGLE_CLIENT_SECRET = "..."
$env:GITHUB_CLIENT_ID     = "..."
$env:GITHUB_CLIENT_SECRET = "..."
.\mvnw -pl extras/oauth2-login-service spring-boot:run
```

Para que sobrevivan al cierre de la consola, `setx GOOGLE_CLIENT_ID "..."`. Pero entonces
quedan en el registro de Windows, y eso hay que saberlo: para un secreto de verdad, un gestor
de credenciales, no `setx`.

---

## Problemas típicos en Windows

| Síntoma | Causa y solución |
|---|---|
| `Invoke-WebRequest: no se puede encontrar un parámetro 's'` | Has usado `curl` en lugar de `curl.exe`. |
| `No se reconoce el prefijo URI` | A `Invoke-RestMethod` le falta el `http://`. |
| `No se permite usar el carácter de Y comercial (&)` | Una URL con varios parámetros sin comillas. |
| `jq : El término 'jq' no se reconoce` | No está instalado. `winget install jqlang.jq` o usa `ConvertFrom-Json`. |
| El símbolo `&&` no es válido | PowerShell 5.1. Usa `;` o `if ($?) { ... }`. |
| `-Dtest=Clase$Anidada` ejecuta toda la clase | El `$` se expandió. Escápalo con `` ` `` o usa comillas simples. |
| `invalid target release: 21` | `JAVA_HOME` apunta a un JDK anterior. Comprueba con `.\mvnw -v`, no con `java -version`. |
| `Port 8080 is already in use` | `Get-NetTCPConnection -LocalPort 8080` y `Stop-Process`. |
| Rutas demasiado largas al compilar | Clona en `C:\dev\...` y no en `C:\Users\<usuario>\Documents\...`. O habilita rutas largas: `Set-ItemProperty "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" LongPathsEnabled 1` (como administrador). |
| Git cambia los finales de línea y `mvnw` deja de funcionar en Docker | El `.gitattributes` del repositorio ya lo fija. Si clonaste antes de eso: `git rm --cached -r . ; git reset --hard`. |
| Los `*IT` fallan con `Could not find a valid Docker environment` | Docker Desktop no está arrancado. |
| Docker Desktop y WSL2 se comen la memoria | Limitarlo en `%UserProfile%\.wslconfig` con `[wsl2]` y `memory=4GB`. |
| Acentos raros en la salida de la consola | `[Console]::OutputEncoding = [Text.Encoding]::UTF8` al empezar la sesión. |

---

## Atajo: tres funciones para la sesión

Pegar esto al abrir la consola ahorra bastante escritura durante la clase:

```powershell
# Ejecutar una prueba de la aplicación por nombre
function Prueba([string]$nombre) {
    .\mvnw -pl apps/petclinic-api test "-Dtest=$nombre"
}

# Arrancar la aplicación con los perfiles que se le pasen
function Arrancar([string]$perfiles = "") {
    if ($perfiles) {
        .\mvnw -pl apps/petclinic-api spring-boot:run "-Dspring-boot.run.profiles=$perfiles"
    }
    else {
        .\mvnw -pl apps/petclinic-api spring-boot:run
    }
}

# GET a la API, con el token si lo hay, y el JSON ya formateado
function Api([string]$ruta) {
    $cabeceras = @{}
    if ($env:TOKEN) { $cabeceras["Authorization"] = "Bearer $env:TOKEN" }
    Invoke-RestMethod "http://localhost:8080$ruta" -Headers $cabeceras |
        ConvertTo-Json -Depth 10
}
```

Uso:

```powershell
Prueba VisitCriteriaRepositoryTests
Arrancar nplus1
Api "/api/v1/visits?page=0&size=2"
```

Para tenerlas siempre, añadirlas al perfil: `notepad $PROFILE` (créalo si no existe con
`New-Item -ItemType File -Force $PROFILE`).
