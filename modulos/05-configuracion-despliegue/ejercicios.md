# Módulo 5. Ejercicios

El guion completo de la demostración de infraestructura está en
[`guion-configurabilidad.md`](guion-configurabilidad.md). Estos ejercicios cubren lo que no
entra allí: perfiles, configuración tipada, observabilidad y contenedores.

---

## Ejercicio 1 · Orden de precedencia

**Objetivo.** Saber qué gana cuando la misma propiedad está en varios sitios.

1. `server.port` no está declarado en `application.properties`; el valor por defecto es 8080.
2. Arranca de cuatro formas distintas y anota en qué puerto queda:

   ```bash
   ./mvnw -pl apps/petclinic-api spring-boot:run

   SERVER_PORT=8081 ./mvnw -pl apps/petclinic-api spring-boot:run

   ./mvnw -pl apps/petclinic-api spring-boot:run \
     -Dspring-boot.run.arguments=--server.port=8082

   # Añade server.port=8083 a application.properties y arranca sin argumentos
   ```

3. Combina dos a la vez y comprueba cuál manda.

**Resultado esperado.** Los argumentos de línea de órdenes ganan a las variables de entorno,
y estas al fichero empaquetado.

> **Mensaje clave.** Esta jerarquía es lo que permite desplegar **el mismo artefacto** en
> todos los entornos y cambiar solo la configuración externa.

---

## Ejercicio 2 · Crear un perfil

**Objetivo.** Practicar perfiles con un caso realista.

1. Crea `application-demo.properties` con:
   - puerto 8888
   - registro de SQL desactivado
   - nivel `WARN` para todo salvo el paquete de la aplicación
   - un mensaje propio en `info.app.description`
2. Arranca con él:

   ```bash
   ./mvnw -pl apps/petclinic-api spring-boot:run -Dspring-boot.run.profiles=demo
   ```

3. Comprueba: `curl -s localhost:8888/actuator/info | jq`
4. Ahora **acumula** dos perfiles: `-Dspring-boot.run.profiles=demo,mysql`. ¿Qué gana si los
   dos definen la misma propiedad?

**Resultado esperado.** El último perfil de la lista gana.

---

## Ejercicio 3 · Configuración tipada

**Objetivo.** Sustituir `@Value` por `@ConfigurationProperties`.

1. Lee `configuration/BillsProperties.java`.
2. Comenta `petclinic.bills.base-url` en `application.properties` y arranca.
3. Lee el mensaje de error: dice exactamente qué propiedad falta y dónde.
4. Restaura la propiedad.
5. Crea ahora `PetclinicProperties` para agrupar `petclinic.*`, con:
   - `String nombreClinica` obligatorio
   - `int maximoMascotasPorPropietario` entre 1 y 10
   - `Duration tiempoEsperaCita` con valor por defecto `30m`
6. Inyéctalo donde corresponda y comprueba que la validación salta con un valor de 20.

**Resultado esperado.** Con `maximoMascotasPorPropietario=20` la aplicación **no arranca**, y
el mensaje señala la restricción incumplida.

> **Mensaje clave.** Fallar al arrancar es mejor que fallar en la primera petición de un
> usuario a las tres de la madrugada.

Solución: [`soluciones/Ejercicio03PetclinicProperties.java`](soluciones/Ejercicio03PetclinicProperties.java)

---

## Ejercicio 4 · Cambiar el nivel de registro sin reiniciar

**Objetivo.** Usar Actuator para algo útil.

```bash
curl -s localhost:8080/logging
# En la consola solo se ven INFO, WARN y ERROR

curl -s localhost:8080/actuator/loggers/org.springframework.samples.petclinic | jq

curl -X POST localhost:8080/actuator/loggers/org.springframework.samples.petclinic \
  -H 'Content-Type: application/json' -d '{"configuredLevel":"TRACE"}'

curl -s localhost:8080/logging
# Ahora aparecen los cinco niveles
```

Para volver al valor original: `{"configuredLevel":null}`.

**Pregunta.** ¿Por qué esto es peligroso si `/actuator/**` está abierto en producción?

---

## Ejercicio 5 · Métricas con Micrometer

**Objetivo.** Publicar métricas en un formato que un sistema de monitorización entienda.

1. Añade `micrometer-registry-prometheus` a `apps/petclinic-api/pom.xml`.
2. Expón el punto correspondiente:

   ```properties
   management.endpoints.web.exposure.include=health,info,metrics,prometheus
   ```

3. Comprueba: `curl -s localhost:8080/actuator/prometheus | head -30`
4. Añade un contador propio: cada vez que se crea una visita, incrementa
   `petclinic.visitas.creadas`.
5. Genera unas cuantas visitas y localiza la métrica.

**Resultado esperado.** La métrica aparece en la salida de `/actuator/prometheus`.

---

## Ejercicio 6 · Hilos virtuales

**Objetivo.** Activar hilos virtuales y medir.

1. Activa:

   ```properties
   spring.threads.virtual.enabled=true
   ```

2. Comprueba en el registro que Tomcat usa hilos virtuales.
3. Lanza carga sobre un endpoint y compara con la configuración anterior.

**Aviso, y forma parte del ejercicio.** Los bloques `synchronized` de larga duración y las
variables `ThreadLocal` reutilizadas pueden anular la ventaja. Busca `synchronized` en el
proyecto antes de sacar conclusiones.

> **Mensaje clave.** Medir antes y después. Una propiedad que promete rendimiento y no se
> mide es una creencia, no una mejora.

---

## Ejercicio 7 · Imagen por capas

**Objetivo.** Entender qué se retransmite en cada despliegue.

1. Construye la imagen actual y anota su tamaño:

   ```bash
   docker build -f docker/Dockerfile --build-arg MODULE=apps/petclinic-api -t petclinic:v1 .
   docker images petclinic:v1
   ```

2. Cambia una cadena de texto en un controlador y reconstruye como `petclinic:v2`.
3. Mira cuántas capas se han reconstruido:

   ```bash
   docker history petclinic:v2 | head
   ```

4. Ahora modifica el `Dockerfile` para extraer el artefacto en capas:

   ```dockerfile
   RUN java -Djarmode=tools -jar app.artifact extract --layers --launcher --destination extraido
   ```

   y copia `dependencies/`, `spring-boot-loader/`, `snapshot-dependencies/` y
   `application/` en `COPY` separados, de menos a más volátil.
5. Repite los pasos 1 a 3 y compara.

**Resultado esperado.** Con la imagen por capas, un cambio en el código solo invalida la
última, de unos cientos de kilobytes, frente a los ~100 MB del artefacto completo.

---

## Ejercicio 8 · Buildpacks, sin Dockerfile

```bash
./mvnw -pl apps/petclinic-api spring-boot:build-image \
  -Dspring-boot.build-image.imageName=curso/petclinic:buildpack

docker run -p 8080:8080 curso/petclinic:buildpack
```

**Pregunta.** Compara la imagen resultante con la del `Dockerfile`: tamaño, usuario con el
que se ejecuta y máquina virtual elegida. ¿Cuándo preferirías cada opción?

---

# Ejercicios añadidos para la edición de 40 horas

Cubren lo que las transparencias explican y los ocho ejercicios anteriores no practicaban:
Spring Boot Admin sin Docker y con Actuator protegido (diapositivas 136–138), cifrado y
recarga en caliente con Spring Cloud Config (139–143), despliegue en un Tomcat externo (144)
y dockerizar una aplicación desde cero (145).

---

## Ejercicio 9 · Spring Boot Admin, sin Docker y sin abrir Actuator a todo el mundo

**Objetivo.** Monitorizar `petclinic-api` desde Spring Boot Admin y hacerlo sin desactivar
la seguridad, que es lo que hace la diapositiva 138.
**Diapositivas:** 136–138. **Tiempo:** 40 min.

1. Arranca el servidor de administración. Los errores de conexión con Eureka son esperables
   si no has arrancado `infra/registry`: no impiden nada.

   ```bash
   ./mvnw -pl infra/admin-server spring-boot:run      # http://localhost:9090
   ```

2. Arranca `petclinic-api` como cliente. La dependencia ya está en el `pom.xml`; solo está
   desactivada por propiedad:

   ```bash
   ./mvnw -pl apps/petclinic-api spring-boot:run \
     -Dspring-boot.run.arguments="--spring.boot.admin.client.enabled=true --logging.file.name=target/petclinic.log"
   ```

3. Recorre en <http://localhost:9090> las pestañas *Details*, *Metrics*, *Environment*,
   *Loggers*, *Threads* y *Logfile*. Cambia el nivel de `org.hibernate.SQL` a `DEBUG` desde
   la interfaz y comprueba el efecto en la consola.
4. Ahora el problema: `/actuator/**` está abierto (`permitAll()`). Cualquiera puede leer
   `/actuator/env` o descargar `/actuator/heapdump`. Protégelo:
   - crea un usuario `monitor` con autoridad `ACTUATOR` (puede ir en `data.sql`);
   - deja públicos solo `/actuator/health` y `/actuator/info`;
   - exige `ACTUATOR` para el resto, con autenticación **Basic**.
5. Spring Boot Admin deja de ver los detalles. Dale las credenciales a través de los
   metadatos del cliente:

   ```properties
   spring.boot.admin.client.instance.metadata.user.name=monitor
   spring.boot.admin.client.instance.metadata.user.password=${MONITOR_PASSWORD}
   ```

6. Comprueba:

   ```bash
   curl -i localhost:8080/actuator/env                     # 401
   curl -i -u monitor:<clave> localhost:8080/actuator/env  # 200
   curl -i localhost:8080/actuator/health                  # 200
   ```

**Resultado esperado.** Spring Boot Admin vuelve a mostrarlo todo, pero un anónimo ya no puede
leer la configuración ni descargar la memoria del proceso.

**Pregunta.** ¿Por qué no basta con `management.endpoints.web.exposure.include=health,info`
en producción? *(Bastaría para cerrar, pero entonces Admin no ve nada. Exponer y proteger
son dos decisiones distintas.)*

Solución: [`soluciones/Ejercicio09AdminConSeguridad.java`](soluciones/Ejercicio09AdminConSeguridad.java)

---

## Ejercicio 10 · Config Server: secretos cifrados y recarga en caliente

**Objetivo.** Las dos funciones de Spring Cloud Config que justifican tener un servidor de
configuración: no guardar secretos en claro y cambiar valores sin reiniciar.
**Diapositivas:** 139–143. **Tiempo:** 40 min. **Requisito:** la sesión de Config Server del
[guion de configurabilidad](guion-configurabilidad.md).

### Parte A · Cifrado

1. Da una clave simétrica al servidor de configuración (solo para clase: en un sistema real
   va en una variable de entorno o en un almacén de claves):

   ```bash
   ENCRYPT_KEY=una-clave-larga-del-curso ./mvnw -pl infra/config-server spring-boot:run
   ```

2. Cifra un valor con el propio servidor:

   ```bash
   curl -s localhost:8889/encrypt -d 'contraseña-de-la-bd'
   ```

3. Pega el resultado en `config-repo/bills-service.properties` con el prefijo `{cipher}`:

   ```properties
   billing.demo.secreto={cipher}AQB3...
   ```

4. Pide la configuración y comprueba que **sale descifrada**:

   ```bash
   curl -s localhost:8889/bills-service/default | jq '.propertySources[0].source'
   ```

5. Responde: ¿dónde vive ahora el secreto, y quién puede leerlo?

### Parte B · Recarga en caliente

`BillAPIController` de `bills-service` ya lleva `@RefreshScope`.

1. Para poder editar el repositorio **sin recompilar**, arranca el servidor leyendo de
   disco y no del *classpath*:

   ```bash
   ./mvnw -pl infra/config-server spring-boot:run \
     -Dspring-boot.run.arguments=--spring.cloud.config.server.native.search-locations=file:infra/config-server/src/main/resources/config-repo
   ```

2. Arranca `bills-service` (su perfil por defecto es `development`) y mira el valor de
   `user.role`:

   ```bash
   ./mvnw -pl infra/bills-service spring-boot:run
   curl -s localhost:8040/api/v1/bills/whoami/ana
   ```
3. Cambia `user.role` en `bills-service-development.properties`. Pide de nuevo: **no cambia**.
4. Fuerza la recarga:

   ```bash
   curl -X POST localhost:8040/actuator/refresh
   ```

5. Pide de nuevo. Ahora sí. ¿Qué devuelve el `POST` del paso 4?

**Resultado esperado.** El paso 4 devuelve la lista de propiedades que han cambiado. Solo los
beans `@RefreshScope` (y los `@ConfigurationProperties`) recogen el valor nuevo.

**Pregunta.** Con diez instancias de `bills-service`, ¿hay que llamar diez veces a
`/actuator/refresh`? *(Sí, salvo que se use Spring Cloud Bus, que lo difunde por un
*broker* de mensajes.)*

Solución: [`soluciones/Ejercicio10CifradoYRecarga.md`](soluciones/Ejercicio10CifradoYRecarga.md)

---

## Ejercicio 11 · Desplegar en un Tomcat externo

**Objetivo.** Empaquetar como `war` y desplegar en un contenedor de *servlets* que no es el
embebido.
**Diapositiva:** 144. **Tiempo:** 35 min.

`petclinic-api` ya se empaqueta como `war` (por las JSP) y ya tiene su
`SpringBootServletInitializer` (`PetclinicInitializer`). Pero le falta una de las piezas de
la diapositiva 144.

1. Compara `apps/petclinic-api/pom.xml` con la diapositiva 144 punto por punto. ¿Cuál falta?
2. Corrígelo: el Tomcat embebido debe ir con `<scope>provided</scope>`, igual que Jasper.
3. Empaqueta y mira el contenido del `war`:

   ```bash
   ./mvnw -pl apps/petclinic-api -am package -DskipTests
   jar tf apps/petclinic-api/target/petclinic-api.war | grep -E "lib(-provided)?/tomcat"
   ```

4. Despliega en Tomcat 10.1 (Jakarta EE 10; Tomcat 9 **no** sirve con Spring Boot 3).
   Elige una vía:

   - **Descarga:** <https://tomcat.apache.org/download-10.cgi>, copia el `war` a `webapps/` y
     arranca con `bin/startup.sh` (o `bin\startup.bat`).
   - **Docker:**

     ```bash
     docker run --rm -p 8888:8080 \
       -v "$PWD/apps/petclinic-api/target/petclinic-api.war:/usr/local/tomcat/webapps/petclinic.war" \
       tomcat:10.1-jdk21-temurin
     ```

5. Abre <http://localhost:8888/petclinic/>. Fíjate en la ruta de contexto.
6. Comprueba que **el mismo** `war` sigue arrancando solo:

   ```bash
   java -jar apps/petclinic-api/target/petclinic-api.war
   ```

**Resultado esperado.** Las dependencias `provided` acaban en `WEB-INF/lib-provided/`: Tomcat
las ignora, y `java -jar` las usa. Un solo artefacto sirve para los dos modos.

**Preguntas.**
- En el Tomcat externo, ¿qué pasa con `server.port` y `server.servlet.context-path`? *(Se
  ignoran: los decide el contenedor. Es el error más habitual al migrar.)*
- ¿Por qué la diapositiva fija el `finalName`? *(Para que el contexto sea `/petclinic-api` y
  no `/petclinic-api-1.0.0-SNAPSHOT`.)*

Solución: [`soluciones/Ejercicio11TomcatExterno.md`](soluciones/Ejercicio11TomcatExterno.md)

---

## Ejercicio 12 · Dockerizar una aplicación desde cero

**Objetivo.** Escribir un `Dockerfile` propio, sin partir del del curso, y entender cada línea.
**Diapositiva:** 145. **Tiempo:** 30 min. Se trabaja sobre `practicas/tareas`.

1. Escribe en `practicas/tareas/Dockerfile` la versión de la diapositiva 145 y constrúyela.
   ¿Funciona? *(No: `openjdk:8-jdk-alpine` no tiene Java 21, y además esas imágenes están
   retiradas.)*
2. Corrígelo paso a paso:
   1. imagen base `eclipse-temurin:21-jre` (JRE, no JDK: no se compila dentro);
   2. copia el jar con un nombre fijo (`app.jar`), sin la versión;
   3. ejecuta con un usuario sin privilegios;
   4. añade un `HEALTHCHECK` contra `/actuator/health`.
3. Construye y arranca en el puerto de la diapositiva:

   ```bash
   cd practicas/tareas
   ./mvnw package -DskipTests
   docker build --tag=tareas:latest .
   docker run --rm -p 8887:8080 tareas:latest
   curl -s localhost:8887/api/v1/tareas | jq
   ```

4. Pasa configuración **sin reconstruir** la imagen:

   ```bash
   docker run --rm -p 8887:8080 -e SPRING_JPA_SHOW_SQL=false -e INFO_APP_DESCRIPTION="desde Docker" tareas:latest
   curl -s localhost:8887/actuator/info | jq
   ```

5. Ahora haz una versión **multietapa** que compile dentro del contenedor, para no necesitar
   JDK en la máquina que construye. Compárala con `docker/Dockerfile` del curso.

**Resultado esperado.** Una imagen de unos 300 MB que arranca en segundos, se configura por
variables de entorno y Docker marca como `healthy`.

Solución: [`soluciones/Ejercicio12Dockerfile`](soluciones/Ejercicio12Dockerfile)

---

## Ejercicio 13 · Docker Compose integrado en Spring Boot (opcional)

**Objetivo.** Usar una base de datos real en desarrollo sin configurar nada a mano.
**Tiempo:** 25 min. Se trabaja sobre `practicas/tareas`. **Requisito:** Docker.

1. Añade a `practicas/tareas/pom.xml`:

   ```xml
   <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-docker-compose</artifactId>
     <scope>runtime</scope>
     <optional>true</optional>
   </dependency>
   <dependency>
     <groupId>org.postgresql</groupId>
     <artifactId>postgresql</artifactId>
     <scope>runtime</scope>
   </dependency>
   ```

2. Crea `practicas/tareas/compose.yaml` con un servicio `postgres:17`.
3. Comenta `spring.datasource.url` en `application.properties` y añade:

   ```properties
   spring.jpa.hibernate.ddl-auto=update
   spring.sql.init.mode=always
   ```

4. Arranca con `./mvnw spring-boot:run` y lee la consola. **No** has escrito URL, usuario ni
   contraseña de PostgreSQL. ¿De dónde salen?
5. Para la aplicación. ¿Qué pasa con el contenedor?

**Resultado esperado.** Spring Boot levanta el `compose.yaml`, espera a que PostgreSQL esté
listo y configura el `DataSource` a partir del contenedor (*service connection*). Al parar, el
contenedor también se para.

**Pregunta.** ¿Por qué hay que poner `ddl-auto=update` y `sql.init.mode=always`, que con H2 no
hacían falta? *(Con una base de datos embebida, Spring Boot usa `create-drop` y ejecuta
`data.sql` por defecto; con una externa, no toca el esquema salvo que se le pida.)*

Solución: [`soluciones/Ejercicio13ComposeIntegrado.md`](soluciones/Ejercicio13ComposeIntegrado.md)
