# Módulo 5. Gestión de la configuración y despliegue

Guion docente. Duración estimada: **tres sesiones de 110 minutos**.

## Documentos

| Documento | Qué contiene |
|---|---|
| [`guion-configurabilidad.md`](guion-configurabilidad.md) | Las tres sesiones completas: Config Server, Eureka, Boot Admin y Docker, con comandos, variante con backend Git y alternativa sin Docker |
| [`ejercicios.md`](ejercicios.md) | Ejercicios de perfiles, configuración tipada, observabilidad y contenedores |

## Objetivos

- Entender el orden de precedencia con que Spring Boot resuelve las propiedades.
- Usar perfiles para que **un mismo artefacto** sirva en todos los entornos.
- Agrupar configuración relacionada con `@ConfigurationProperties` validado.
- Externalizar la configuración con Spring Cloud Config.
- Registrar servicios en Eureka y supervisarlos con Spring Boot Admin.
- Exponer métricas y trazas.
- Empaquetar la aplicación en un contenedor sin ingenuidades.

## Mapa del material

### Perfiles de la aplicación

| Perfil | Qué cambia | Fichero |
|---|---|---|
| *(ninguno)* | H2 en memoria, esquema por Hibernate, `data.sql` | `application.properties` |
| `mysql` | MySQL externo, **Flyway**, `ddl-auto=validate` | `application-mysql.properties` |
| `docker` | Eureka, Config Server y Boot Admin activos | `application-docker.properties` |
| `nplus1` | `open-in-view` y registro de SQL, para el módulo 6 | `application-nplus1.properties` |

Los perfiles se **acumulan**. En `docker/compose.yaml`:

```yaml
SPRING_PROFILES_ACTIVE: docker,mysql
```

> **Mensaje clave del módulo.** El artefacto que se despliega es el mismo en todos los
> entornos. Lo único que cambia es la configuración externa. Si hay que recompilar para
> pasar de preproducción a producción, algo está mal.

### Configuración tipada

`configuration/BillsProperties.java` es un `record` validado:

```java
@Validated
@ConfigurationProperties(prefix = "petclinic.bills")
public record BillsProperties(
        @NotBlank String baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {}
```

Tres ventajas frente a repartir `@Value` por el código, y conviene enunciarlas:

1. **Falla al arrancar** si falta una propiedad obligatoria, y dice cuál.
2. **Autocompletado** en el IDE, porque el tipo se conoce.
3. **Conversión automática**: `5s` se convierte en `Duration` sin escribir nada.

Demostración: comentar `petclinic.bills.base-url` y arrancar. El mensaje de error es
explícito.

> Contexto útil: en el proyecto anterior esa URL estaba escrita a fuego en **tres sitios
> distintos con tres valores diferentes** (8040, 8095 y una propiedad vacía), y el token
> JWT iba en el código fuente.

### Infraestructura

| Servicio | Puerto | Para qué |
|---|---|---|
| `infra/registry` | 8761 | Eureka: los servicios se encuentran por nombre |
| `infra/config-server` | 8889 | Configuración centralizada |
| `infra/admin-server` | 9090 | Panel de supervisión |
| `infra/bills-service` | 8040 | Cliente de los tres anteriores |

```bash
docker compose -f docker/compose.yaml up --build
```

Comprobaciones:

```bash
curl -s localhost:8889/bills-service/development | jq        # configuración servida
curl -s localhost:8040/api/v1/bills/whoami/ana               # valor que llega al cliente
```

> **Sobre el `config-repo`.** Está en `infra/config-server/src/main/resources/config-repo/`,
> en UTF-8 y versionado. En el proyecto anterior la propiedad `search-locations` estaba
> comentada y lo único que se servía era un fichero suelto guardado en **UTF-16 con BOM**
> por un `echo >` de cmd, que llegaba ilegible. Ese era el origen del síntoma "bills
> responde `admin`" que documentaba el guion.

### Observabilidad

```bash
curl -s localhost:8080/actuator/health | jq
curl -s localhost:8080/actuator/metrics/jvm.memory.used | jq

# Cambiar el nivel de registro sin reiniciar
curl -X POST localhost:8080/actuator/loggers/org.springframework.samples.petclinic \
     -H 'Content-Type: application/json' -d '{"configuredLevel":"TRACE"}'
curl localhost:8080/logging
```

`web/LoggingController` emite un mensaje por nivel, para que el cambio se vea en la consola.

> **Aviso para producción.** Este proyecto expone `management.endpoints.web.exposure.include=*`
> porque es cómodo en clase. `/actuator/env` y `/actuator/heapdump` revelan información
> sensible: en producción se recorta la lista y se expone en un puerto distinto.

### Contenedores

`docker/Dockerfile` tiene dos decisiones que merece la pena comentar:

1. Las dependencias se resuelven **antes** de copiar el código, para que Docker reutilice la
   capa. El Dockerfile anterior hacía `COPY . .` de entrada e invalidaba la caché con cada
   cambio de una línea.
2. Se ejecuta con un usuario sin privilegios.

También hay un `sed -i 's/\r$//' ./mvnw` que parece innecesario y no lo es: `docker build`
copia el árbol de trabajo, no lo que hay en git, así que un `mvnw` editado en Windows con
CRLF produce el desconcertante `/bin/sh: 1: ./mvnw: not found`.

Ejercicios del bloque: imagen por capas con `-Djarmode=tools … extract --layers`, buildpacks
con `spring-boot:build-image`, e hilos virtuales con `spring.threads.virtual.enabled=true`.

## Errores frecuentes en clase

| Síntoma | Causa |
|---|---|
| El puerto 8888 está ocupado | Por eso el Config Server usa el 8889. |
| Eureka no muestra los servicios | Tarda hasta 30 s en la primera sincronización. Refrescar. |
| `bills-service` responde el valor local | El Config Server no ha respondido. `spring.config.import` lleva `optional:` a propósito. |
| La construcción de Docker tarda muchísimo | La primera vez descarga todas las dependencias. Construir antes de clase con `docker compose -f docker/compose.yaml build`. |
| `mysql:5.7` no arranca en Mac | Ese era el problema del proyecto anterior: 5.7 no publica imagen arm64. Aquí se usa `mysql:8.4`. |
