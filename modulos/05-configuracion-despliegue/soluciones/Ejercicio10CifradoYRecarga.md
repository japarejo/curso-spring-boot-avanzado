# Módulo 5 · Ejercicio 10 · Solución: secretos cifrados y recarga en caliente

## Parte A · Cifrado

```bash
# Terminal 1: servidor de configuración con clave simétrica
ENCRYPT_KEY=una-clave-larga-del-curso ./mvnw -pl infra/config-server spring-boot:run
```

En PowerShell:

```powershell
$env:ENCRYPT_KEY="una-clave-larga-del-curso"; .\mvnw -pl infra/config-server spring-boot:run
```

```bash
# Terminal 2
curl -s localhost:8889/encrypt -d 'contraseña-de-la-bd'
# AQB3c1...   (cambia en cada llamada: el cifrado lleva un vector de inicialización aleatorio)

curl -s localhost:8889/decrypt -d 'AQB3c1...'
# contraseña-de-la-bd
```

En `infra/config-server/src/main/resources/config-repo/bills-service.properties`:

```properties
billing.demo.secreto={cipher}AQB3c1...
```

> En un fichero `.properties`, el valor va **sin comillas**. En YAML, **con** comillas
> simples: `'{cipher}AQB3c1...'`. Es el error más habitual.

Como el repositorio es `classpath:/config-repo`, hay que reiniciar el servidor (o arrancarlo
con `search-locations=file:...`, como en la parte B) para que lea el fichero nuevo.

```bash
curl -s localhost:8889/bills-service/default | jq '.propertySources[0].source'
# { "billing.demo.secreto": "contraseña-de-la-bd", ... }
```

**Dónde vive ahora el secreto.** En el repositorio de configuración solo está cifrado: quien
lea el fichero o el historial de Git no lo obtiene. La clave está **solo** en el servidor de
configuración. Pero ojo: el servidor descifra antes de servir, así que cualquiera que pueda
llamar a `/bills-service/default` lo ve en claro. Hay que proteger el servidor (Basic, como
en la diapositiva 142, o mTLS) o activar el descifrado en el cliente con
`spring.cloud.config.server.encrypt.enabled=false` (diapositiva 143), y entonces la clave
tiene que estar en cada cliente.

## Parte B · Recarga en caliente

```bash
# Terminal 1
./mvnw -pl infra/config-server spring-boot:run \
  -Dspring-boot.run.arguments=--spring.cloud.config.server.native.search-locations=file:infra/config-server/src/main/resources/config-repo

# Terminal 2
./mvnw -pl infra/bills-service spring-boot:run

# Terminal 3
curl -s localhost:8040/api/v1/bills/whoami/ana
# Hello! You're ana and you'll become a(n) Desarrollador...
```

Se edita `bills-service-development.properties`: `user.role=Arquitecta`.

```bash
curl -s localhost:8040/api/v1/bills/whoami/ana          # sigue diciendo Desarrollador
curl -s -X POST localhost:8040/actuator/refresh
# ["user.role"]
curl -s localhost:8040/api/v1/bills/whoami/ana          # ahora dice Arquitecta
```

Y la comparación que merece la pena hacer en voz alta:

```bash
curl -s localhost:8040/whoami/ana                        # ServiceInstanceRestController
```

`ServiceInstanceRestController` también lee `user.role` con `@Value`, pero **no** tiene
`@RefreshScope`: sigue mostrando el valor antiguo. `@RefreshScope` crea un *proxy* que
destruye y recrea el bean en el siguiente uso tras un `refresh`.

| Qué recoge el valor nuevo tras `/actuator/refresh` | |
|---|---|
| Beans con `@RefreshScope` | Sí |
| Beans `@ConfigurationProperties` | Sí (se vuelven a enlazar) |
| `@Value` en un bean normal | **No** |
| Propiedades leídas al arrancar (puerto, *pool* de conexiones) | **No** |
