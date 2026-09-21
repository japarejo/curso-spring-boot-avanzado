# Material didáctico

Un directorio por módulo del temario. Cada uno contiene el guion docente
(`README.md`), los enunciados (`ejercicios.md`) y el código de referencia
(`soluciones/`).

Antes de la primera sesión: [`00-preparacion-entorno.md`](00-preparacion-entorno.md).

## Los seis módulos

| # | Módulo | Sesiones | Documentos |
|---|---|---|---|
| 1 | [Introducción a Spring Boot](01-introduccion-spring-boot/) | 1 | guion, 5 ejercicios |
| 2 | [APIs REST avanzadas](02-apis-rest/) | 2 | guion, 7 ejercicios |
| 3 | [Seguridad](03-seguridad/) | 2 (+1 opcional de ACL) | guion, 7 ejercicios, guion de ACL |
| 4 | [Pruebas](04-pruebas/) | 2 (+1 opcional de Testcontainers) | guion de clases, 12 ejercicios, guion de Testcontainers |
| 5 | [Configuración y despliegue](05-configuracion-despliegue/) | 3 | guion de configurabilidad, 8 ejercicios |
| 6 | [Spring Data y JPA](06-spring-data-jpa/) | 3 | guion de acceso a datos, guion de transacciones, guion de N+1, 7 ejercicios |

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
| `spring.jpa.open-in-view=false` | 2 (serialización), 5 (perfiles) y 6 (N+1) |
| Perfiles de la aplicación | 5 (concepto) y 6 (perfil `nplus1`) |
| `infra/auth-service` | 3 (JWT) y 5 (un servicio más en la infraestructura) |

## Material de ampliación

| Tema | Dónde |
|---|---|
| Permisos por instancia con ACL | [`03-seguridad/guion-acl.md`](03-seguridad/guion-acl.md), módulo `extras/acl-service` |
| Inicio de sesión con proveedores externos | módulo `extras/oauth2-login-service` |
| Pruebas contra la base de datos real | [`04-pruebas/guion-testcontainers.md`](04-pruebas/guion-testcontainers.md) |
