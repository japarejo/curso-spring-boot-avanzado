# Módulo 5 · Ejercicio 11 · Solución: desplegar en un Tomcat externo

## 1. Lo que falta respecto a la diapositiva 144

| Diapositiva 144 | `apps/petclinic-api` |
|---|---|
| `<packaging>war</packaging>` | Sí |
| `<finalName>` sin versión | Sí (`petclinic-api`) |
| `SpringBootServletInitializer` | Sí (`PetclinicInitializer`) |
| `spring-boot-starter-tomcat` con `provided` | **No** |

## 2. La corrección

En `apps/petclinic-api/pom.xml`, junto a las dependencias de las JSP:

```xml
<!-- El contenedor externo ya trae Tomcat y Jasper. `provided`: se usan para
     compilar y para `java -jar`, pero no se despliegan dentro del war. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-jasper</artifactId>
    <scope>provided</scope>
</dependency>
```

## 3. El contenido del war

```bash
jar tf apps/petclinic-api/target/petclinic-api.war | grep -E "lib(-provided)?/tomcat"
```

Antes de la corrección:

```
WEB-INF/lib/tomcat-embed-core-10.1.x.jar
WEB-INF/lib/tomcat-embed-el-10.1.x.jar
WEB-INF/lib/tomcat-embed-jasper-10.1.x.jar
...
```

Después:

```
WEB-INF/lib-provided/tomcat-embed-core-10.1.x.jar
WEB-INF/lib-provided/tomcat-embed-el-10.1.x.jar
WEB-INF/lib-provided/tomcat-embed-jasper-10.1.x.jar
...
```

`WEB-INF/lib-provided` es una convención del `spring-boot-maven-plugin`: el Tomcat externo no la
mira (solo carga `WEB-INF/lib`), y el lanzador de `java -jar` sí.

Sin la corrección, el despliegue en un Tomcat externo **puede** funcionar, porque el cargador
de clases de Tomcat se niega a cargar desde la aplicación las clases del propio contenedor,
pero arrastra unos 5 MB inútiles y avisos en el arranque. Si la versión del Tomcat embebido y
la del externo no coinciden, los fallos son difíciles de diagnosticar.

## 4–5. Despliegue

```bash
./mvnw -pl apps/petclinic-api -am package -DskipTests
docker run --rm -p 8888:8080 \
  -v "$PWD/apps/petclinic-api/target/petclinic-api.war:/usr/local/tomcat/webapps/petclinic.war" \
  tomcat:10.1-jdk21-temurin
```

En PowerShell, `${PWD}` en lugar de `$PWD`.

- <http://localhost:8888/petclinic/>: la ruta de contexto la da **el nombre del fichero**
  en `webapps/`.
- <http://localhost:8888/petclinic/actuator/health>: Actuator funciona igual.
- En el registro de Tomcat aparece el *banner* de Spring Boot: quien arranca la aplicación es
  `PetclinicInitializer.configure(...)`, llamado por el contenedor, no el `main`.

## 6. El mismo artefacto, también ejecutable

```bash
java -jar apps/petclinic-api/target/petclinic-api.war
```

## Preguntas

- `server.port`, `server.servlet.context-path` y en general `server.*` **se ignoran** en un
  contenedor externo: los decide el contenedor (`conf/server.xml`, nombre del war).
- Tomcat 9 implementa `javax.servlet`; Spring Boot 3 usa `jakarta.servlet`. Con Tomcat 9 la
  aplicación ni se detecta.
- Por qué existe todavía esta opción: hay organizaciones que operan servidores de
  aplicaciones centralizados (Tomcat, WildFly, WebSphere Liberty) y no admiten procesos Java
  sueltos. Es la diapositiva de «servidores de aplicaciones no embebidos» del temario.
