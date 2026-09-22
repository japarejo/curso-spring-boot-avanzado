# Laboratorio: transacciones avanzadas

> **Material adicional, fuera de la temporización.** El módulo 6 sigue siendo de tres
> sesiones. [`guion-transacciones.md`](guion-transacciones.md) cubre lo que entra en el
> temario: frontera transaccional, `rollbackFor`, `readOnly`, propagación, aislamiento y
> `timeout`. Este documento cubre lo que viene **después**: las cinco cosas con las que el
> alumno se va a encontrar en su primer proyecto de verdad y que no se ven en ninguna
> diapositiva porque ninguna falla de forma ruidosa.
>
> Si hay hueco para tocar algo en clase, el bloque 1 (autoinvocación, 15 minutos) y el
> bloque 5 (eventos, 20 minutos) son los que más rentabilidad dan.

## Las cinco trampas

| # | Trampa | Cómo se manifiesta |
|---|---|---|
| 1 | Autoinvocación | La anotación no hace nada. Sin error. |
| 2 | Actualización perdida | Un dato desaparece. Sin error. |
| 3 | Bloqueo pesimista | Se desconoce que existe, y se reintenta en bucle |
| 4 | `NESTED` | `NestedTransactionNotSupportedException` al arrancar el caso de uso |
| 5 | Efectos no transaccionales | Se envía el correo de una operación que se revirtió |

Tres de las cinco **no lanzan ninguna excepción**. Ese es el hilo conductor del laboratorio y
la razón de que tenga pruebas: lo que no falla ruidosamente solo se puede enseñar
afirmándolo.

## Lo que se añade al proyecto

```
apps/petclinic-api/src/main/java/.../service/transacciones/
├── AutoinvocacionService.java        el proxy y sus tres arreglos
├── EdicionConcurrenteService.java    actualización perdida, optimista, pesimista, NESTED
├── RegistroDeVisitasService.java     publica el evento de dominio
├── VisitaRegistrada.java            el evento (un record)
└── NotificacionesDeVisita.java       los tres escuchadores, para comparar

apps/petclinic-api/src/test/java/.../service/transacciones/
└── TransaccionesAvanzadasVerificationTests.java   12 pruebas
```

Y dos métodos nuevos en `repository/VisitRepository`: `findByIdBloqueando` (con
`@Lock(PESSIMISTIC_WRITE)`) y `actualizarDescripcionEnMasa` (con `@Modifying`).

```powershell
.\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests"
```

## Cómo se simula la concurrencia, y por qué así

Es la decisión de diseño más importante del laboratorio, y conviene explicarla antes de
mirar el código.

Reproducir una condición de carrera con dos hilos y esperas produce pruebas que fallan un día
de cada veinte. En un curso eso es lo peor que puede pasar: el alumno deja de creerse las
pruebas. Aquí el "segundo usuario" se simula con `REQUIRES_NEW`:

```
transacción A: lee la visita (versión 1)
  transacción B (REQUIRES_NEW): lee, escribe y CONFIRMA (versión 2)
transacción A: escribe... y aquí se decide todo
```

`REQUIRES_NEW` suspende la transacción de fuera y abre otra con **su propio contexto de
persistencia** y su propia conexión. Para la base de datos el efecto es exactamente el mismo
que el de otro usuario trabajando a la vez, pero el orden está garantizado por el flujo del
programa. Ni hilos, ni esperas, ni sorpresas.

> **Nota para el aula.** Esto es una simulación, no una demostración de que el problema ocurre
> con concurrencia real. El problema ocurre igual; lo que se gana es poder afirmarlo de forma
> determinista. Merece la pena decirlo en voz alta: en un curso hay que distinguir el
> experimento del fenómeno.

## Bloque 1 — Autoinvocación

### El problema

`@Transactional` se implementa con un **proxy** que envuelve al bean:

```
cliente ──▶ [proxy] ──▶ instancia real       aquí se abre la transacción
                        instancia.metodo()    esto NO pasa por el proxy
```

Cuando un método llama a otro del mismo objeto con `this`, la llamada va directa a la
instancia y se salta el proxy. La anotación sigue en el código, se lee perfectamente en una
revisión, y no hace absolutamente nada.

```java
public boolean llamandoConThis() {
    return this.hayTransaccionActiva();     // devuelve false
}

@Transactional
public boolean hayTransaccionActiva() {
    return TransactionSynchronizationManager.isActualTransactionActive();
}
```

La versión cara es esta otra, que aparece en proyectos reales con toda naturalidad:

```java
@Transactional
public void procesarPedido(...) {
    ...
    this.auditar(...);        // @Transactional(REQUIRES_NEW), y da igual
    ...                       // si esto falla, la auditoría se va con él
}
```

La intención era que la auditoría sobreviviese al fallo. Como la llamada es con `this`, la
auditoría acaba en la **misma** transacción y se revierte con todo lo demás. El día que hace
falta el registro de auditoría para investigar un incidente, no está.

### Los tres arreglos

| Arreglo | Cómo | Cuándo |
|---|---|---|
| Autoinyección | `public Servicio(@Lazy Servicio self)` | Rápido, pero deja a la vista que algo raro pasa. Muchos equipos lo prohíben por eso. |
| `TransactionTemplate` | `transactionTemplate.execute(estado -> ...)` | Cuando el ámbito transaccional no coincide con la frontera de un método. |
| **Mover a otro bean** | El método transaccional vive en otra clase | Lo preferible casi siempre: la llamada vuelve a atravesar un proxy y de paso la responsabilidad queda mejor repartida. |

El `@Lazy` de la autoinyección no es decorativo: sin él, Spring intentaría construir el bean
para inyectarlo dentro de su propio constructor y fallaría con una dependencia circular.

### Ejercicio 1.1 — Verlo fallar (nivel básico)

1. Ejecutar el bloque `Autoinvocacion` de las pruebas.
2. En `AutoinvocacionService`, cambiar `llamandoAlProxy()` para que use `this` en lugar de
   `self`. Ejecutar. La prueba `losTresArreglosFuncionan` falla.
3. Volver a dejarlo como estaba.

**Resultado esperado.** `llamandoConThis()` devuelve `false` y los tres arreglos `true`. En el
log, `Creating new transaction` aparece una vez por cada arreglo y **ninguna** en el caso con
`this`.

### Ejercicio 1.2 — El `REQUIRES_NEW` fantasma (nivel medio)

Mirar `auditoriaQueNoSobrevive()` y `auditoriaQueSiSobrevive()`. Las dos llaman al mismo
método `REQUIRES_NEW`; una con `this` y otra por el proxy. La prueba comprueba el **nombre**
de la transacción activa, que es la forma más limpia de distinguirlas.

1. Ejecutar con `logging.level.org.springframework.transaction.interceptor=TRACE` y buscar
   en la salida `Suspending current transaction`. Aparece una sola vez: en el caso bueno.
2. Escribir una prueba que lo demuestre con datos y no con nombres: un método
   `@Transactional` que escriba una visita, llame a un auditor `REQUIRES_NEW` que escriba
   otra cosa, y después lance una excepción. Comprobar que con `this` se pierden las dos y
   por el proxy solo se pierde la primera. `TransactionalExamplesVerificationTests`
   (`requiresNewCommitsAuditWhenParentRollsBack`) tiene el esqueleto.

**Resultado esperado.** Sin proxy, el nombre de la transacción sigue siendo
`...auditoriaQueNoSobrevive`. Con proxy, pasa a ser `...nombreDeLaTransaccionActual`.

### Ejercicio 1.3 — Los otros dos casos en que el proxy no entra (nivel medio)

`@Transactional` tampoco funciona sobre métodos `private`, `final` ni `static`. Con proxies
CGLIB, que es lo que usa Spring Boot por defecto, un método `final` no se puede sobrescribir
y por tanto no se puede interceptar.

1. Marcar `hayTransaccionActiva()` como `final` y ejecutar las pruebas. Fallan: el método ya
   no se intercepta, así que `llamandoAlProxy()` devuelve `false` igual que el caso con
   `this`.
2. Volver a ejecutar activando el registro del proxy, que es donde Spring lo cuenta:

   ```powershell
   .\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests`$Autoinvocacion" `
     "-Dlogging.level.org.springframework.aop.framework.CglibAopProxy=DEBUG"
   ```

   Aparece esto, que es donde Spring lo cuenta:

   ```text
   DEBUG o.s.aop.framework.CglibAopProxy : Final method [public final boolean
   ...AutoinvocacionService.hayTransaccionActiva()] cannot get proxied via CGLIB:
   Calls to this method will NOT be routed to the target instance and might lead
   to NPEs against uninitialized fields in the proxy instance.
   ```
3. Compararlo con lo que pasa al marcarlo `private`: ahí no hay ni mensaje.
4. Dejarlo como estaba.

**Resultado esperado.** Con `final`, el método no se intercepta y el aviso solo se ve subiendo
el nivel de log de `CglibAopProxy` a DEBUG, que por defecto está apagado. Con `private`, la
anotación se ignora sin decir absolutamente nada.

Y ahí está la lección de los tres casos juntos: **los métodos transaccionales, `public`, no
`final`, y llamados desde fuera del bean.** Las tres condiciones a la vez, y ninguna de las
tres la comprueba el compilador.

## Bloque 2 — Actualización perdida y bloqueo optimista

### El problema

Dos usuarios editan la misma visita:

```
usuario A: lee la descripción         "rabies shot"
usuario B: lee la descripción         "rabies shot"
usuario B: escribe y confirma         "vacuna antirrábica"
usuario A: escribe y confirma         "vacuna de la rabia"
```

El cambio de B ha desaparecido. Y desde la base de datos todo es correcto: dos transacciones
que confirman, sin conflicto de bloqueos, cada una haciendo lo que se le pidió. El nivel de
aislamiento **no** resuelve esto: incluso con `SERIALIZABLE` las dos escrituras son legales si
no se solapan en el tiempo. Es un problema de modelo, no de aislamiento.

### La solución que ya está en el proyecto

`BaseEntity` tiene, desde el primer día del curso y sin que nadie lo haya usado todavía:

```java
@Version
private Integer version;
```

Con eso, Hibernate genera:

```sql
update visits set description=?, version=2 where id=? and version=1
```

Si otro ya dejó la fila en versión 2, el `update` afecta a **cero** filas. Hibernate lo
detecta, lanza `OptimisticLockException` y Spring lo traduce a
`ObjectOptimisticLockingFailureException`.

Se llama **optimista** porque no bloquea nada: asume que el conflicto es raro y se limita a
detectarlo cuando ocurre. Es lo correcto para una aplicación web, donde entre la lectura y la
escritura hay un usuario pensando y no se puede tener una fila bloqueada durante minutos.

> **Detalle que hay que señalar.** La excepción **no** sale de `save()`. Sale del `flush` que
> ocurre al confirmar la transacción. El fallo aparece lejos de la línea que lo provoca, y eso
> desconcierta la primera vez. Mirar la pila de la prueba
> `laEntidadGestionadaDetectaElConflicto` y localizar el `commit`.

### Y el agujero: el `UPDATE` masivo

```java
@Modifying
@Query("UPDATE Visit visit SET visit.description = :descripcion WHERE visit.id = :id")
int actualizarDescripcionEnMasa(Integer id, String descripcion);
```

Esta consulta va **directa** a la base de datos. Dos consecuencias, y ninguna es obvia:

1. **No incrementa `version`**, así que se salta el bloqueo optimista por completo. La
   actualización perdida vuelve, y en silencio.
2. **No pasa por el contexto de persistencia**, así que las entidades ya cargadas se quedan
   con el valor viejo. Si después se modifica una de ellas y se confirma, el valor viejo se
   vuelve a escribir encima.

Los arreglos, por orden de preferencia:

1. No mezclar `UPDATE` masivo y entidades vivas en la misma transacción.
2. `@Modifying(clearAutomatically = true, flushAutomatically = true)`.
3. `entityManager.refresh(entidad)`, que es lo que hace
   `desincronizacionTrasUpdateMasivo` para dejarlo a la vista.

### Ejercicio 2.1 — La actualización perdida (nivel medio)

1. Ejecutar `elUpdateMasivoPierdeLaActualizacion`. Comprobar que **no hay excepción** y que el
   texto de B ha desaparecido.
2. Ejecutar `laEntidadGestionadaDetectaElConflicto`. Mismo escenario, escribiendo por la
   entidad, y ahora sí hay excepción.
3. En `EdicionConcurrenteService.editarConEntidadGestionada`, añadir
   `System.out.println(visita.getVersion())` antes y después de la llamada a la transacción
   B. Explicar por qué no cambia.

**Resultado esperado.** Con `UPDATE` masivo, la descripción final es la de A y la de B se ha
perdido sin rastro. Con la entidad gestionada, `OptimisticLockingFailureException` y la
descripción final es la de B, el que confirmó primero.

### Ejercicio 2.2 — Reintentar, que es lo que falta (nivel alto)

Detectar el conflicto solo sirve si se hace algo con él. Lo que se hace es **reintentar**:
recargar el dato, volver a aplicar el cambio y volver a confirmar.

1. Escribir `editarConReintentos(int idVisita, String texto, int intentosMaximos)`:

   ```java
   for (int intento = 1; intento <= intentosMaximos; intento++) {
       try {
           return this.self.editarEnTransaccionPropia(idVisita, texto);
       }
       catch (OptimisticLockingFailureException ex) {
           if (intento == intentosMaximos) {
               throw ex;
           }
       }
   }
   ```

2. **Lo importante del ejercicio:** el bucle tiene que estar **fuera** de la transacción. Si
   se pone dentro, el reintento ocurre en una transacción ya marcada para revertirse y falla
   siempre. Probar las dos versiones: es la mejor forma de entender qué significa
   "marcada para *rollback*".
3. Escribir la prueba con el mismo truco del `REQUIRES_NEW`: el primer intento choca, el
   segundo sale bien.

**Resultado esperado.** Con el bucle fuera, el segundo intento confirma. Con el bucle dentro,
`UnexpectedRollbackException` o el mismo fallo repetido hasta agotar los intentos.

**Pista para el paso siguiente.** Spring Retry (`@Retryable`) hace esto de forma declarativa,
pero hay que poner el `@Retryable` en un bean **distinto** del que tiene el `@Transactional`,
por la misma razón del bloque 1: dos interceptores en el mismo método y el orden importa.

### Ejercicio 2.3 — La versión también viaja al cliente (nivel medio)

En una API REST el bloqueo optimista solo funciona si el cliente devuelve la versión que
leyó. Si no, el servidor siempre lee la última y nunca detecta nada.

1. Mirar `web/api/dto/`. Comprobar si la versión se expone.
2. Añadirla al DTO de visita y hacer que el `PUT` la exija. Si no coincide, responder **409
   Conflict** con `ProblemDetail`, no 500.
3. Registrar el manejador en `GlobalExceptionHandler` (módulo 2):
   `OptimisticLockingFailureException` → 409.

**Resultado esperado.** Dos `PUT` con la misma versión: el primero devuelve 200, el segundo
409 con un cuerpo `application/problem+json` que explica qué ha pasado. Esta es la forma
correcta de exponer el bloqueo optimista, y es la unión natural de los módulos 2 y 6.

## Bloque 3 — Bloqueo pesimista

### Cuándo hace falta

Optimista: escribe y ya veremos. Pesimista: **bloquea la fila desde que la lee**, y quien
llegue después espera.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT visit FROM Visit visit WHERE visit.id = :id")
Optional<Visit> findByIdBloqueando(@Param("id") Integer id);
```

Eso genera `SELECT ... FOR UPDATE`.

| | Optimista (`@Version`) | Pesimista (`@Lock`) |
|---|---|---|
| Cuándo detecta | Al confirmar | Al leer |
| Coste si no hay conflicto | Ninguno | Una fila bloqueada |
| Coste si hay conflicto | Reintentar todo | Esperar |
| Sirve para | Web, formularios, editar y guardar | Transacciones cortas y automáticas |
| Riesgo propio | Muchos reintentos | Interbloqueos y esperas |

La regla práctica: **si entre la lectura y la escritura hay un ser humano, optimista**. Nadie
puede tener una fila bloqueada mientras alguien rellena un formulario o se va a comer.
Pesimista para transacciones cortas, automáticas y con contención real: reservar el último
asiento, decrementar un stock, asignar un número de serie.

Los modos que hay que conocer:

| Modo | Qué hace |
|---|---|
| `PESSIMISTIC_READ` | Bloqueo compartido: otros pueden leer, nadie puede escribir |
| `PESSIMISTIC_WRITE` | Bloqueo exclusivo: `SELECT ... FOR UPDATE` |
| `PESSIMISTIC_FORCE_INCREMENT` | Exclusivo **y** además incrementa `@Version` |
| `OPTIMISTIC_FORCE_INCREMENT` | Incrementa la versión aunque la entidad no cambie |

El último es más útil de lo que parece: sirve para que modificar un **hijo** marque el
**padre** como cambiado, y así dos usuarios que editan mascotas distintas del mismo
propietario entren en conflicto si el invariante es del propietario.

### Ejercicio 3.1 — Ver el bloqueo (nivel básico)

1. Ejecutar `laLecturaConLockBloqueaLaFila`. Buscar `for update` en el SQL de la consola.
2. Ejecutar `laLecturaNormalNoBloquea` y comprobar que ahí el modo es `NONE`.
3. Quitar el `@Transactional` de `editarConBloqueoPesimista` y ejecutar.

**Resultado esperado.** En el paso 3, `TransactionRequiredException`. Tiene todo el sentido:
un bloqueo que se suelta inmediatamente no bloquea nada, así que JPA prefiere fallar antes de
dar una falsa sensación de seguridad.

### Ejercicio 3.2 — La espera, de verdad (nivel alto)

Este es el único ejercicio del laboratorio que necesita dos hilos, y se hace **a mano**, no
como prueba automática, justamente por lo que se explicó al principio.

1. Arrancar la aplicación con MySQL:

   ```powershell
   docker compose -f docker/compose.yaml up -d mysql
   .\mvnw -pl apps/petclinic-api spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```

2. Abrir **dos** terminales y en cada una un cliente de MySQL contra el contenedor. El
   servicio se llama `mysql` y la contraseña de root es `root` (ver `docker/compose.yaml`):

   ```powershell
   docker compose -f docker/compose.yaml exec mysql mysql -uroot -proot petclinic
   ```

3. En el primero:

   ```sql
   START TRANSACTION;
   SELECT * FROM visits WHERE id = 1 FOR UPDATE;
   ```

4. En el segundo, lo mismo. **Se queda esperando.**
5. Confirmar en el primero (`COMMIT`) y ver cómo el segundo se desbloquea al instante.
6. Repetir sin confirmar y esperar 50 segundos: `ERROR 1205 (HY000): Lock wait timeout
   exceeded`. Ese es el valor por defecto de `innodb_lock_wait_timeout` en MySQL.

**Resultado esperado.** Haber visto una espera real y un *timeout* real. Es la diferencia
entre saber que existen y haberlos sufrido.

**Ampliación.** Configurar el tiempo de espera desde JPA:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
```

Y probar el interbloqueo: dos transacciones que bloquean las filas 1 y 2 en orden opuesto.
MySQL detecta el ciclo y mata a una de las dos con `ERROR 1213: Deadlock found`. La lección
que hay que sacar: **bloquear siempre en el mismo orden**, por ejemplo por identificador
ascendente. Es la regla más barata y la que más interbloqueos evita.

## Bloque 4 — `NESTED`, la propagación que no funciona donde todos creen

`NESTED` promete lo que todo el mundo quiere: un punto de retorno intermedio. Si falla la
parte anidada se deshace **solo** esa parte y la transacción de fuera continúa. Se apoya en
`SAVEPOINT` de JDBC.

Con `JpaTransactionManager`, que es el que configura Spring Boot cuando hay JPA, esto lanza:

```
NestedTransactionNotSupportedException: Transaction manager does not allow nested
transactions by default - specify 'nestedTransactionAllowed' property with value 'true'
```

**No es un fallo de configuración del proyecto.** El gestor viene con los *savepoints*
desactivados a propósito, y la razón es buena: un `SAVEPOINT` deshace el estado de la **base de
datos**, pero no deshace lo que Hibernate tiene en su contexto de persistencia. Los dos
quedarían descuadrados, y a partir de ahí Hibernate escribiría cosas que ya no se corresponden
con la base de datos.

Se puede activar con `nestedTransactionAllowed = true`, y hay casos legítimos. Pero para el
99 % de lo que la gente quiere hacer con `NESTED` —"esto puede fallar sin tirar el resto"— la
respuesta es `REQUIRES_NEW`, que ya se vio en clase y que sí funciona con JPA.

### Ejercicio 4.1 — Provocarlo y decidir (nivel medio)

1. Ejecutar `nestedNoEstaSoportado`. Leer el mensaje completo.
2. Comprobar que **no escribió nada**: el gestor rechaza la propagación antes de ejecutar el
   método anidado.
3. Activar los *savepoints* con un `TransactionManagerCustomizer`:

   ```java
   @Bean
   TransactionManagerCustomizer<JpaTransactionManager> permitirNested() {
       return (gestor) -> gestor.setNestedTransactionAllowed(true);
   }
   ```

4. Volver a ejecutar. Ahora funciona. Y ahora la pregunta: ¿se debe dejar así?

**Resultado esperado.** Con los *savepoints* activados el caso pasa, pero al meter entidades
gestionadas en la parte anidada empiezan las incoherencias. La conclusión que hay que llegar
a escribir: **`REQUIRES_NEW` para lo que puede fallar por separado**, y `NESTED` solo con un
motivo explícito y por escrito.

### Ejercicio 4.2 — La tabla completa (nivel básico, pero conviene tenerla)

Rellenar de memoria y comprobar con el código:

| Propagación | Con transacción activa | Sin transacción activa |
|---|---|---|
| `REQUIRED` (por defecto) | Se une a la existente | Crea una |
| `REQUIRES_NEW` | Suspende la de fuera y crea otra | Crea una |
| `SUPPORTS` | Se une | No crea ninguna |
| `NOT_SUPPORTED` | Suspende la de fuera y trabaja sin transacción | No crea ninguna |
| `MANDATORY` | Se une | **Falla** (`IllegalTransactionStateException`) |
| `NEVER` | **Falla** | No crea ninguna |
| `NESTED` | *Savepoint*, o falla con JPA | Se comporta como `REQUIRED` |

Las dos filas que hay que recordar: `MANDATORY` es la buena para un método de repositorio que
exige estar dentro de un caso de uso, y `NOT_SUPPORTED` es la que usan las pruebas de este
laboratorio para desactivar la transacción que Spring pone alrededor de cada método.

## Bloque 5 — Efectos que no se pueden revertir

### El problema, con nombre y apellidos

Hay efectos que un *rollback* no deshace: enviar un correo, cobrar una tarjeta, publicar un
mensaje en una cola, invalidar una caché compartida, llamar a otro servicio.

El síntoma clásico es este, y en producción se ve más de lo que parece:

> El cliente recibe el correo de "cita confirmada" de una cita que no existe en la base de
> datos.

Pasó porque el correo se envió **dentro** de la transacción, y la transacción se revirtió
después.

### La solución de Spring

Publicar un evento dentro de la transacción y escucharlo con `@TransactionalEventListener`,
que lo entrega **después** de confirmar:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void alConfirmar(VisitaRegistrada evento) { ... }
```

Las cuatro fases:

| Fase | Cuándo se entrega | Para qué |
|---|---|---|
| `BEFORE_COMMIT` | Justo antes de confirmar | Validar algo que aún puede vetar la operación |
| `AFTER_COMMIT` (por defecto) | Después de confirmar | Lo que no se puede deshacer: correos, colas, cachés |
| `AFTER_ROLLBACK` | Después de revertir | Compensar, avisar a un operador, registrar el intento |
| `AFTER_COMPLETION` | Después, en cualquier caso | Limpieza |

### Las tres trampas de esta solución

**1. Si no hay transacción, el evento se descarta en silencio.**

`@TransactionalEventListener` no tiene ningún `commit` al que engancharse, así que no se
ejecuta. Ni excepción ni aviso: el escuchador simplemente no corre. Es agotador de depurar. Se
arregla con `fallbackExecution = true`, que lo entrega igualmente cuando no hay transacción.

**2. Escribir en la base de datos dentro de un `AFTER_COMMIT` no funciona.**

En ese punto la transacción ya está confirmada. Cualquier escritura necesita
`@Transactional(propagation = REQUIRES_NEW)`. Sin eso, con `JpaTransactionManager`, el cambio
se descarta sin avisar. Otra vez el mismo patrón: silencio.

**3. Es síncrono por defecto.**

El escuchador se ejecuta en el mismo hilo, **después** del `commit` pero **antes** de que el
método del controlador devuelva. Un envío de correo lento hace lenta la petición. Con `@Async`
pasa a otro hilo, y entonces hay que decidir qué ocurre si falla: ya no hay transacción a la
que echarle la culpa, y el fallo se pierde salvo que se registre.

### Ejercicio 5.1 — El correo fantasma (nivel básico)

1. Ejecutar `alRevertirSoloSeEntregaElNoTransaccional`. Leer las cuatro aserciones despacio:
   el dato no está en la base de datos, pero el `@EventListener` normal ya actuó.
2. Cambiar `@TransactionalEventListener` por `@EventListener` en `alConfirmar` y ejecutar.
   Ahora los dos envían el correo fantasma.
3. Dejarlo como estaba y ejecutar `alConfirmarSeEntreganLosDos`.

**Resultado esperado.** Con la transacción revertida: `entregadasAlPublicar` tiene el evento,
`entregadasAlConfirmar` está vacía y `entregadasAlRevertir` tiene el evento. Con la
transacción confirmada: las dos primeras tienen el evento y la tercera está vacía.

### Ejercicio 5.2 — El evento que se pierde (nivel medio)

1. Ejecutar `sinTransaccionElEventoTransaccionalSePierde`.
2. Añadir `fallbackExecution = true` al escuchador y volver a ejecutar. La prueba falla, y eso
   es lo correcto: el comportamiento ha cambiado.
3. Decidir qué se quiere y ajustar la prueba para que documente la decisión.

**Resultado esperado.** Entender que `@TransactionalEventListener` **depende** de que exista
una transacción, y que eso convierte en frágil cualquier servicio que se pueda llamar desde
dentro y desde fuera de una transacción.

### Ejercicio 5.3 — Escribir en la base de datos tras confirmar (nivel alto)

1. Hacer que `alConfirmar` escriba una fila de auditoría con un repositorio.
2. Ejecutar. No se escribe nada, y no hay error.
3. Añadir `@Transactional(propagation = Propagation.REQUIRES_NEW)` al escuchador. Ahora sí.
4. Provocar un fallo dentro del escuchador **después** de haber añadido el `REQUIRES_NEW`.
   ¿Se revierte la operación principal?

**Resultado esperado.** No. La operación principal ya confirmó y es irreversible. Si el efecto
posterior tiene que ser fiable, hace falta el patrón de la **bandeja de salida**
(*transactional outbox*): escribir el efecto pendiente como una fila **dentro** de la misma
transacción, y que un proceso aparte lo procese y lo marque. Es el diseño correcto y merece la
pena nombrarlo aunque no se implemente: es la respuesta a "¿y si el correo tiene que salir sí
o sí?".

### Ejercicio 5.4 — Asíncrono (nivel alto)

1. Añadir `@EnableAsync` y `@Async` al escuchador `AFTER_COMMIT`.
2. Ejecutar las pruebas. Fallan de forma intermitente: las aserciones se evalúan antes de que
   el otro hilo haya terminado.
3. Arreglar las pruebas con Awaitility (`await().atMost(...).until(...)`).
4. Provocar una excepción dentro del escuchador asíncrono. Comprobar dónde aparece.

**Resultado esperado.** Con `@Async` desaparece la garantía de orden y el fallo ya no se
propaga a nadie: hay que registrarlo explícitamente con un `AsyncUncaughtExceptionHandler` o
se pierde. Y de paso el alumno acaba de descubrir por sí mismo por qué las pruebas de este
laboratorio evitan la concurrencia.

## Cómo se ha hecho para que las pruebas no sean frágiles

Tres decisiones, útiles fuera de este laboratorio:

**1. `@Transactional(propagation = NOT_SUPPORTED)` en la clase de prueba.**

Por defecto Spring envuelve cada método de prueba en una transacción que revierte al
terminar. Con eso, los `@Transactional` del servicio se **unen** a la transacción de la
prueba, nunca confirman, y ninguna de estas demostraciones funciona: no hay `commit`, así que
no hay conflicto optimista ni evento `AFTER_COMMIT`. Desactivarla es el primer paso, y es lo
primero que se rompe al copiar estas pruebas a otro sitio.

**2. Una visita distinta por bloque, y restaurar al terminar.**

Sin la transacción de la prueba, los cambios persisten entre métodos. `data.sql` trae cuatro
visitas, así que cada bloque usa la suya y devuelve el valor original al acabar. El orden de
ejecución deja de importar.

**3. `@DirtiesContext(classMode = AFTER_CLASS)`.**

Esta clase deja la base de datos tocada y añade beans que otras pruebas no esperan. Marcar el
contexto como sucio evita que se reutilice. Cuesta un arranque de contexto y ahorra fallos
cruzados que aparecen solo cuando cambia el orden de las clases.

## Comandos (PowerShell, Windows)

```powershell
# Todo el laboratorio
.\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests"

# Un bloque (clase anidada: el $ se escapa con acento grave)
.\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests`$EventosTransaccionales"

# Con el detalle de las transacciones en el log
.\mvnw -pl apps/petclinic-api test "-Dtest=TransaccionesAvanzadasVerificationTests" `
  "-Dlogging.level.org.springframework.transaction=TRACE"

# Junto con el laboratorio del temario, para ver los dos seguidos
.\mvnw -pl apps/petclinic-api test "-Dtest=Transaccion*VerificationTests"
```

## Resumen para llevarse

1. `@Transactional` se aplica con un proxy. Con `this`, `private`, `final` o `static`, no se
   aplica. Y no avisa.
2. El nivel de aislamiento no resuelve la actualización perdida. Lo resuelve `@Version`.
3. La excepción del bloqueo optimista llega al **confirmar**, no al guardar.
4. `@Modifying` es rápido porque se salta el contexto de persistencia y la versión. Saber a
   qué se renuncia.
5. Si entre la lectura y la escritura hay una persona, bloqueo optimista. Pesimista solo para
   transacciones cortas y automáticas.
6. Bloquear siempre en el mismo orden, y así no hay interbloqueos.
7. `NESTED` no funciona con JPA por defecto, y casi siempre lo que se quería era
   `REQUIRES_NEW`.
8. Lo que no se puede deshacer va **después** del `commit`, con `@TransactionalEventListener`.
   Y si tiene que salir sí o sí, bandeja de salida.
