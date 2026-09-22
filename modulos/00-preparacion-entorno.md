# Preparación del entorno

Para hacer antes de la primera sesión. Son quince minutos; hacerlo en clase cuesta una hora
y se lleva por delante el primer bloque del temario.

## Lo imprescindible

| Herramienta | Versión | Comprobación |
|---|---|---|
| JDK | **21** | `java -version` debe decir `21.x` |
| Git | cualquiera reciente | `git --version` |
| IDE | IntelliJ IDEA, VS Code con Extension Pack for Java, o Eclipse/STS | — |

**Maven no hace falta instalarlo.** El repositorio trae el wrapper: siempre se invoca como
`./mvnw` (Linux y macOS) o `.\mvnw` (Windows, desde la raíz del repositorio; PowerShell
resuelve solo la extensión `.cmd`).

> **Si trabajas en Windows**, lee [`00-comandos-windows.md`](00-comandos-windows.md) antes de
> la primera sesión. Son cinco minutos y evita las dos sorpresas que más tiempo hacen perder:
> `curl` es un alias de `Invoke-WebRequest` y no funciona como el curl de los guiones, y `jq`
> no viene instalado. Ahí está todo el curso traducido a PowerShell.

> **Java 21 y no otra cosa.** Es un requisito del cliente del curso. Si tienes varias
> versiones instaladas, comprueba que `JAVA_HOME` apunta a la 21, porque es la que usa
> Maven, no la que responda a `java -version` en el PATH:
>
> ```bash
> ./mvnw -v      # la línea "Java version:" tiene que decir 21
> ```

## Lo recomendable

| Herramienta | Para qué | Módulo |
|---|---|---|
| **Docker Desktop** | MySQL real, Testcontainers y la infraestructura completa | 4 y 5 |
| **curl** | Probar la API desde la terminal | 2 y 3 |
| **Postman** | Colecciones de pruebas de API | 4 |
| **jq** | Leer respuestas JSON en la terminal | 2 |

Sin Docker se puede seguir el 90 % del curso: la aplicación arranca con H2 en memoria. Lo
que no se podrá hacer es la parte de Testcontainers del módulo 4 ni la demostración de
infraestructura del módulo 5.

## Primera comprobación

```bash
git clone <repositorio>
cd curso-spring-boot-avanzado

./mvnw -v          # Java version: 21.x
./mvnw verify      # debe terminar en BUILD SUCCESS
```

La primera ejecución descarga las dependencias y tarda varios minutos. **Hazla en casa, no
en clase**: veinte personas descargando el repositorio de Maven a la vez saturan la red del
aula.

Después:

```bash
./mvnw -pl apps/petclinic-api spring-boot:run
```

Y en el navegador <http://localhost:8080>. Entrar con `owner1` / `0wn3r`.

## Si algo falla

| Síntoma | Causa y solución |
|---|---|
| `invalid target release: 21` | `JAVA_HOME` apunta a un JDK anterior. Comprobar con `./mvnw -v`. |
| `Port 8080 is already in use` | Otro proceso ocupa el puerto. `server.port=8081` o parar el otro proceso. |
| `Could not resolve dependencies` en la primera ejecución | Sin acceso a repo.maven.apache.org. Un proxy corporativo suele requerir `~/.m2/settings.xml`. |
| `mvnw: Permission denied` (Linux y macOS) | `chmod +x mvnw` |
| `mvnw.cmd` no se reconoce (Windows) | Ejecutarlo desde la raíz del repositorio, con `.\mvnw` |
| `curl: no se puede encontrar un parámetro 's'` (Windows) | `curl` es un alias de `Invoke-WebRequest`. Usar `curl.exe`. Ver [`00-comandos-windows.md`](00-comandos-windows.md). |
| Las pruebas `*IT` fallan | Necesitan Docker. Sin Docker se saltan solas; `./mvnw test` no las ejecuta. |

## Para el docente

Antes de cada sesión conviene tener:

1. El repositorio clonado y `./mvnw verify` pasado **el día anterior**, para que las
   dependencias estén en la caché local.
2. Docker Desktop arrancado si la sesión toca los módulos 4 o 5.
3. Las imágenes ya descargadas, que es lo que más tarda:

   ```bash
   docker pull mysql:8.4
   docker pull eclipse-temurin:21-jdk
   docker pull eclipse-temurin:21-jre
   ```

4. Para el módulo 5, la infraestructura construida de antemano:

   ```bash
   docker compose -f docker/compose.yaml build
   ```
