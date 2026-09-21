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
