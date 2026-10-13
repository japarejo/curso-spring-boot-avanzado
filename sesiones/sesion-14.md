# Sesión 14 · Sesión final (1 hora)

**Cierre del curso** · Diapositivas 175–177 · 1 hora, sin descanso

> **Mensaje de la sesión.** Lo aprendido se consolida **aplicándolo a un problema entero**, no
> tema a tema. Y el siguiente paso natural es Spring Boot 4: casi todo lo visto sigue igual, y
> lo que cambia está señalado en el código.

## Objetivos

- Repasar el curso con preguntas que mezclan módulos.
- Revisar en común soluciones del reto integrador y sus errores más comunes.
- Señalar qué cambia en Spring Boot 4 y dónde seguir aprendiendo.
- Completar la evaluación de calidad.

## Antes de clase

- [ ] Pedir por el chat, el día anterior, que quien quiera comparta su reto integrador (enlace
      a su repositorio o una rama).
- [ ] Elegir **dos** soluciones para comentar: una que funcione bien y otra con algún error
      interesante (con permiso de su autor o autora).
- [ ] Tener abierto el [repaso final](repaso-final.md) y el enlace de la encuesta de calidad
      (diapositiva 176).

## Cronograma

| Hora | Bloque | Tipo | Material |
|---|---|---|---|
| 0:00 | Repaso final: diez preguntas que cruzan módulos | 🗣 | [`repaso-final.md`](repaso-final.md) |
| 0:15 | Revisión del reto integrador | ⌨ 🗣 | [`reto-integrador.md`](../modulos/reto-integrador.md) |
| 0:35 | Spring Boot 4 y siguientes pasos | 🗣 ⌨ | notas del código |
| 0:45 | Dudas abiertas | 🗣 | |
| 0:50 | Evaluación de calidad y despedida | | Diapositivas 176–177 |

---

## Desarrollo

### 0:00 – 0:15 · Repaso final

Diez preguntas de [`repaso-final.md`](repaso-final.md), en voz alta o con una herramienta de
votación. Cada una cruza al menos dos módulos (por ejemplo: «¿por qué `@PreAuthorize` y
`@Transactional` fallan en el mismo caso?»). Dedica más tiempo a las que el grupo falle.

### 0:15 – 0:35 · Revisión del reto integrador

⌨ Proyecta las dos soluciones elegidas. Recorre la **lista de comprobación** del reto (está al
final del enunciado) y marca qué cumple cada una.

Errores que suelen aparecer, para comentarlos aunque no estén en las soluciones elegidas:

- la entidad devuelta directamente en el controlador (módulo 2);
- la regla nueva sin proteger o con `permitAll()` «para probar» (módulo 3);
- pruebas con `@SpringBootTest` donde bastaba una rebanada (módulo 4);
- un valor «mágico» en el código en lugar de una propiedad tipada (módulo 5);
- el listado con N+1 o la migración que falta para MySQL (módulo 6).

### 0:35 – 0:45 · Spring Boot 4 y siguientes pasos

⌨ Las notas de migración están en el propio código:

```bash
grep -rn "Spring Boot 4" apps/ infra/ --include=*.java
```

🗣 Lo esencial que cambia:

| Tema | Spring Boot 3.5 (curso) | Spring Boot 4 |
|---|---|---|
| Jackson | 2.x (`com.fasterxml.jackson`) | 3.x (`tools.jackson`), `@JacksonComponent` |
| *Starters* | `spring-boot-starter-web` | *starters* modulares (`...-webmvc`) |
| Versionado de API | a mano (ejercicios 1 y 5 del módulo 2) | nativo: `@GetMapping(version = ...)` |
| Clientes HTTP declarativos | Feign | `@HttpExchange` + `@ImportHttpServices` |
| Pruebas de API | `MockMvc`, REST Assured | además, `RestTestClient` |
| Seguridad | Spring Security 6 | Spring Security 7 |
| Línea base | Java 17 | Java 17 (y Jakarta EE 11) |

Recursos para seguir: documentación de referencia de Spring Boot, guías de
<https://spring.io/guides>, y el material de ampliación del repositorio (Criteria API,
transacciones avanzadas, ACL, OAuth 2, Testcontainers).

### 0:45 – 0:50 · Dudas abiertas

Tiempo libre para preguntas. Si no hay, pregunta: «¿qué vais a aplicar el lunes en vuestro
proyecto?».

### 0:50 – 1:00 · Evaluación de calidad y despedida

📊 Diapositivas 176–177.

- Enlace de la encuesta de calidad (diapositiva 176). **Dejar el tiempo dentro de la sesión**:
  si se deja para después, responde una fracción.
- Contacto: `tutoria@idexaformacion.com` (diapositiva 177).
- Recordar que el repositorio sigue disponible y que las soluciones están en `modulos/*/soluciones/`.
