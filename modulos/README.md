# Material didáctico

Un directorio por módulo del temario. Cada uno contiene el guion docente
(`README.md`), los enunciados (`ejercicios.md`) y el código de referencia
(`soluciones/`).

Antes de la primera sesión: [`00-preparacion-entorno.md`](00-preparacion-entorno.md).

En Windows, los comandos traducidos a PowerShell y las siete trampas de la consola:
[`00-comandos-windows.md`](00-comandos-windows.md).

## Los seis módulos

| # | Módulo | Sesiones | Documentos |
|---|---|---|---|
| 1 | [Introducción a Spring Boot](01-introduccion-spring-boot/) | 1 | guion, 5 ejercicios |
| 2 | [APIs REST avanzadas](02-apis-rest/) | 2 | guion, 7 ejercicios |
| 3 | [Seguridad](03-seguridad/) | 2 (+1 opcional de ACL) | guion, 7 ejercicios, guion de ACL |
| 4 | [Pruebas](04-pruebas/) | 2 (+1 opcional de Testcontainers) | guion de clases, 12 ejercicios, guion de Testcontainers |
| 5 | [Configuración y despliegue](05-configuracion-despliegue/) | 3 | guion de configurabilidad, 8 ejercicios |
| 6 | [Spring Data y JPA](06-spring-data-jpa/) | 3 (+2 laboratorios opcionales) | guion de acceso a datos, guion de transacciones, guion de N+1, 7 ejercicios, laboratorio de Criteria API, laboratorio de transacciones avanzadas |

Total: **13 sesiones** de contenido obligatorio más 2 opcionales.

## Cómo usar este material

- El **`README.md`** de cada módulo es el guion del docente: objetivos, mapa del
  material, desarrollo cronometrado de la sesión, mensajes clave y errores
  frecuentes. No está pensado para repartir al alumnado.
- El **`ejercicios.md`** sí es para el alumnado. Cada ejercicio lleva objetivo,
  pasos y resultado esperado, para que se pueda autocorregir.
- Las **`soluciones/`** son código de referencia comentado. No forman parte de
  la compilación: están fuera del árbol de fuentes a propósito, para que el
  proyecto siga compilando aunque una solución esté a medias.

## Hilos que cruzan varios módulos

Algunas piezas del proyecto sirven en más de un módulo. Conviene saberlo para
poder retomarlas:

| Pieza | Aparece en |
|---|---|
| Inyección por constructor frente a por campo | 1 (concepto) y 4 (`ReflectionTestUtils` en las pruebas) |
| `apiclients/BillsGateway` es `@Component` y no `@Service` | 1 (estereotipos) y 4 (qué carga una rebanada) |
| DTO frente a entidad | 2 (contrato) y 3 (exposición excesiva de datos) |
| `TransactionalExamplesVerificationTests` | 4 (técnica) y 6 (semántica) |
| `@Version` de `BaseEntity` | 6 (concepto) y el laboratorio de transacciones avanzadas (bloqueo optimista) |
| Lista blanca de campos de ordenación | 2 (`RestfulVisitSearchController`) y el laboratorio de Criteria API (ejercicio 6) |
| `ProblemDetail` | 2 (contrato de error) y el laboratorio de transacciones avanzadas (409 Conflict) |
| `spring.jpa.open-in-view=false` | 2 (serialización), 5 (perfiles) y 6 (N+1) |
| Perfiles de la aplicación | 5 (concepto) y 6 (perfil `nplus1`) |
| `infra/auth-service` | 3 (JWT) y 5 (un servicio más en la infraestructura) |

## Material de ampliación

Todo lo de esta tabla está **fuera de la temporización**: son materiales para entregar, para
quien termine antes y para llevarse a casa. Ninguno es necesario para seguir el curso.

| Tema | Dónde | Código que lo acompaña |
|---|---|---|
| Permisos por instancia con ACL | [`03-seguridad/guion-acl.md`](03-seguridad/guion-acl.md) | `extras/acl-service` |
| Inicio de sesión con proveedores externos | `extras/README.md` | `extras/oauth2-login-service` |
| Pruebas contra la base de datos real | [`04-pruebas/guion-testcontainers.md`](04-pruebas/guion-testcontainers.md) | `*IT` de `apps/petclinic-api` |
| **Criteria API y metamodelo estático** | [`06-spring-data-jpa/laboratorio-criteria-api.md`](06-spring-data-jpa/laboratorio-criteria-api.md) | `repository/criteria/`, 13 pruebas |
| **Transacciones avanzadas** (autoinvocación, bloqueos, `NESTED`, eventos) | [`06-spring-data-jpa/laboratorio-transacciones-avanzadas.md`](06-spring-data-jpa/laboratorio-transacciones-avanzadas.md) | `service/transacciones/`, 12 pruebas |

Los dos laboratorios del módulo 6 traen **código que funciona y pruebas que lo demuestran**,
no solo enunciados. El alumno puede ejecutarlos antes de tocar nada:

```powershell
.\mvnw -pl apps/petclinic-api test "-Dtest=VisitCriteriaRepositoryTests"
.\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests"
```
