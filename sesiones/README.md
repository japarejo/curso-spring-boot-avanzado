# Guiones de las sesiones · edición de 40 horas

**13 sesiones de 3 horas y una sesión final de 1 hora.** Cada sesión de 3 horas tiene dos
bloques de 85 minutos y un descanso de 10 minutos en medio.

Cada guion está pensado para seguirlo **con el guion abierto en una pantalla y el IDE en la
otra**: tiene el cronograma minuto a minuto, qué enseñar, qué decir, qué comandos lanzar y qué
ejercicio hace el alumnado en cada momento. Los ejercicios y sus soluciones están en
[`modulos/`](../modulos/); los guiones solo los enlazan.

## Calendario

| Sesión | Módulo | Contenido | Diapositivas | Ejercicios en clase |
|---|---|---|---|---|
| [1](sesion-01.md) | 1 | Spring y Spring Boot, aplicación desde cero, IoC/DI, starters, Actuator | 1–34 | M1: 6, 1, 2, 3, 10, 8 |
| [2](sesion-02.md) | 2 | Principios REST, controladores, *binding*, errores con `ProblemDetail`, validación | 35–58 | M2: 8, 1, 9, 10 |
| [3](sesion-03.md) | 2 | DTO, JSON a medida, conversores y consumo de APIs | 59–63 | M2: 2, 11, 3, 12, 4 |
| [4](sesion-04.md) | 2 | Spring Data REST, documentación OpenAPI y CORS | 64–70 | M2: 7, 13, 14, 6 |
| [5](sesion-05.md) | 3 | Seguridad por defecto, cadena de filtros, usuarios y contraseñas, autorización | 71–81 | M3: 8, 1, 2, 9, 10, 11, 3 |
| [6](sesion-06.md) | 3 | Formulario de login, JWT, servidor de recursos y OAuth 2 | 82–94 | M3: 12, 4, 5, 13 o 6, 14 |
| [7](sesion-07.md) | 4 | Conceptos de pruebas, JUnit 5, cobertura y dobles con Mockito | 95–112 | M4: 1–5, 14, 13 |
| [8](sesion-08.md) | 4 | Rebanadas de Spring, MockMvc, pruebas de seguridad, REST Assured, Postman | 113–132 | M4: 6–9, 15, 16, 17, 18 |
| [9](sesion-09.md) | 5 | Configuración externa, perfiles, configuración tipada, observabilidad y Spring Boot Admin | 133–138 | M5: 1, 2, 3, 4, 5, 9 |
| [10](sesion-10.md) | 5 | Spring Cloud Config, Tomcat externo y Docker | 139–145 | M5: 10, 11, 12, 7 |
| [11](sesion-11.md) | 6 | JPA, conexión y *pool*, entidades, claves y relaciones, migraciones | 146–158, 165–170 | M6: 9, 8, 5, 1, 2 |
| [12](sesion-12.md) | 6 | Repositorios y consultas, proyecciones, contexto de persistencia y transacciones | 159–164 | M6: 10, 3, 4, 11 + guion de transacciones |
| [13](sesion-13.md) | 6 | Rendimiento: carga perezosa, N+1, lotes; cierre del temario | 171–175 | M6: guion N+1, 12, 7 |
| [14](sesion-14.md) | — | Sesión final de 1 hora: repaso, reto integrador, evaluación de calidad | 175–177 | Repaso final |

**Total: 13 × 3 h + 1 h = 40 h.**

## Por qué este reparto

El temario oficial tiene seis módulos de peso desigual. El reparto sigue el número de
diapositivas y, sobre todo, el tiempo de práctica que exige cada tema:

| Módulo | Sesiones | Horas |
|---|---|---|
| 1. Introducción a Spring Boot | 1 | 3 |
| 2. APIs REST avanzadas | 3 | 9 |
| 3. Seguridad | 2 | 6 |
| 4. Pruebas | 2 | 6 |
| 5. Configuración y despliegue | 2 | 6 |
| 6. Spring Data y JPA | 3 | 9 |
| Cierre | 1 (de 1 h) | 1 |

## Cómo está escrito cada guion

| Sección | Para qué |
|---|---|
| **Mensaje de la sesión** | La idea que tiene que quedar. Si solo se recuerda una cosa, esta. |
| **Antes de clase** | Lo que hay que tener arrancado o descargado **antes** de que entre el alumnado. |
| **Cronograma** | La sesión en una tabla. Es lo que se mira durante la clase. |
| **Desarrollo** | Bloque a bloque: qué enseñar, qué decir, qué comandos lanzar, qué ejercicio hacen. |
| **Si vas con retraso / si te sobra tiempo** | Qué recortar y qué añadir sin romper la sesión siguiente. |
| **Errores frecuentes** | Lo que va a fallar en las máquinas del alumnado, y su arreglo. |
| **Cierre y tarea** | Qué se pide para la sesión siguiente. |

Convenciones:

- 🗣 **Mensaje clave**: frase para decir en voz alta.
- ⌨ **Demo**: lo hace el docente, proyectado.
- 🧑‍💻 **Práctica**: lo hace el alumnado. Lleva el tiempo asignado.
- 📊 **Diapositivas**: números del `.pptx` de IDEXA que acompañan el bloque.

Los comandos están escritos para **bash** (Linux, macOS, Git Bash). En Windows con PowerShell,
`./mvnw` es `.\mvnw` y `curl` es `curl.exe`; el resto de diferencias están en
[`modulos/00-comandos-windows.md`](../modulos/00-comandos-windows.md).

## Material transversal

| Documento | Uso |
|---|---|
| [`modulos/00-preparacion-entorno.md`](../modulos/00-preparacion-entorno.md) | Enviar al alumnado **una semana antes** de la sesión 1 |
| [`docs/fe-de-erratas-transparencias.md`](../docs/fe-de-erratas-transparencias.md) | Lo que está desactualizado o es incorrecto en las transparencias, diapositiva a diapositiva |
| [`docs/diapositivas-nuevas.md`](../docs/diapositivas-nuevas.md) | Contenido propuesto para diapositivas que faltan (por ejemplo, `ProblemDetail`) |
| [`modulos/reto-integrador.md`](../modulos/reto-integrador.md) | Práctica final que recorre los seis módulos; se presenta en la sesión 13 y se comenta en la 14 |
| [`postman/`](../postman/) | Colección de Postman de la API (módulo 4) |
| [`practicas/`](../practicas/) | Proyectos independientes (con su propio Maven embebido): `tareas` y `mapeos-jpa` |

## Lo que más tiempo hace perder (y cómo evitarlo)

1. **Descargas en clase.** La primera compilación descarga unos 300 MB. Pedir en el correo de
   bienvenida que se haga `./mvnw verify` en casa (ver la preparación del entorno).
2. **JDK equivocado.** `./mvnw -v` tiene que decir Java 21. Comprobarlo los cinco primeros
   minutos de la sesión 1.
3. **Puerto 8080 ocupado** por una ejecución anterior que se quedó viva. Enseñar a localizarla
   el primer día (apartado «Problemas típicos en Windows» de `00-comandos-windows.md`).
4. **Docker** no instalado o sin arrancar. Solo hace falta en las sesiones 8 (opcional), 10 y
   en algún ejercicio de la 11. Avisar con una semana de antelación.
