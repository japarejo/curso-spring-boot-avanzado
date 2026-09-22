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

## Si el IDE marca código en rojo (y `mvnw` compila sin quejarse)

Pasa, y casi siempre es lo mismo. **Antes de tocar nada, comprueba quién tiene razón:**

```powershell
.\mvnw -pl apps/petclinic-api clean test-compile
```

Si eso termina en `BUILD SUCCESS`, el código compila y lo que falla es el indexador del
editor. Son dos causas:

### 1. El metamodelo de JPA no está generado

`apps/petclinic-api` genera clases en tiempo de compilación con un procesador de anotaciones
(`hibernate-jpamodelgen`): `Visit_`, `Pet_`, `Owner_`… Las usa `repository/criteria/`.

Esas clases **no están en el repositorio** —son producto de la compilación, y `target/` está
ignorado—, así que si abres el proyecto en el IDE recién clonado, sin haber compilado nunca,
el editor no las encuentra y marca toda la carpeta en rojo.

```powershell
.\mvnw -pl apps/petclinic-api compile      # esto las genera
```

El `pom.xml` ya trae `<m2e.apt.activation>jdt_apt</m2e.apt.activation>` para que Eclipse/STS y
VS Code ejecuten el procesador ellos mismos. Si aun así siguen en rojo, hay que decirle al
servidor de lenguaje que se olvide de lo que tenía indexado:

| IDE | Qué hacer |
|---|---|
| **VS Code** | `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → *Restart and delete* |
| IntelliJ IDEA | `Build > Rebuild Project`, y `File > Invalidate Caches` si persiste |
| Eclipse / STS | `Project > Clean…`, y `Maven > Update Project` con *Force Update* |

### 2. Los ficheros de `modulos/*/soluciones/`

Son **soluciones de referencia**: `.java` sueltos, fuera del árbol de fuentes a propósito,
para que el proyecto siga compilando aunque una solución esté a medias. El editor los ve como
código normal, no les encuentra classpath y los marca enteros.

No es un error. El repositorio trae `.vscode/settings.json` con la exclusión ya puesta; si
usas otro IDE, basta con saber que esos ficheros no se compilan.

### Y lo que hay dentro de `target/`

`target/` es **salida** del compilador, no fuente: clases generadas, `.class`, informes, el
`.war`. Si abres un fichero de ahí y el editor lo marca en rojo, es porque no forma parte de
ningún directorio de fuentes desde su punto de vista. Se borra entero con `clean` y se
regenera. No hay nada que arreglar ahí dentro.

---

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
| El IDE marca en rojo `repository/criteria/` | Falta generar el metamodelo. `./mvnw -pl apps/petclinic-api compile` y reiniciar el servidor de lenguaje. Ver el apartado anterior. |
| El IDE marca en rojo `modulos/*/soluciones/` | Son soluciones de referencia, fuera de la compilación a propósito. |

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
