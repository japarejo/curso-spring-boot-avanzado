# Ejercicios de testing y cobertura

Estos ejercicios se hacen dentro del proyecto `apps/petclinic-api`.

Antes de empezar, abre una terminal en la **raíz del repositorio** (donde está `mvnw.cmd`).
Todos los comandos llevan `-pl apps/petclinic-api` para actuar solo sobre la aplicación.
En Linux o macOS, cambia `.\mvnw.cmd` por `./mvnw`.

## Ficheros de referencia

Codigo de ejemplo:

- `src/main/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisor.java`

Tests unitarios JUnit 5:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`

Tests con Spring:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerRepositoryJpaSliceTests.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerControllerMvcSliceTests.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/DirtiesContextExampleTests.java`

## Que hace `PetAppointmentAdvisor`

`PetAppointmentAdvisor` es una clase pequena de reglas de negocio inventada para practicar testing sin tener que arrancar Spring. Simula un asesor de citas para una clinica veterinaria.

Recibe tres datos:

- `petName`: nombre de la mascota.
- `age`: edad de la mascota.
- `symptom`: descripcion libre del sintoma.

Su metodo principal es:

```java
public AppointmentType classify(String petName, int age, String symptom)
```

Devuelve uno de estos valores:

```java
ROUTINE_CHECKUP
SAME_DAY
EMERGENCY
```

Reglas actuales:

- Si el nombre de la mascota esta vacio o es `null`, lanza `IllegalArgumentException`.
- Si la edad es negativa, lanza `IllegalArgumentException`.
- Si el sintoma contiene palabras de urgencia como `"sangra"`, `"no respira"` o `"convulsion"`, devuelve `EMERGENCY`.
- Si la mascota tiene 12 anios o mas, devuelve `SAME_DAY`.
- Si hoy es fin de semana, devuelve `SAME_DAY`.
- En el resto de casos, devuelve `ROUTINE_CHECKUP`.

Tambien tiene metodos auxiliares utiles para probar:

- `friendlySummary(...)`: devuelve un texto legible para humanos segun la clasificacion.
- `normalizePetName(...)`: limpia espacios y normaliza el nombre de la mascota.

Por que esta clase es buena para practicar:

- Tiene validaciones.
- Tiene varias ramas de decision.
- Tiene casos frontera, como edad `11` frente a edad `12`.
- Usa `Clock`, asi que los tests pueden fijar una fecha y no dependen del dia real en que se ejecutan.
- Permite ver claramente la diferencia entre cobertura de lineas y cobertura de ramas.

Informes:

- Surefire XML/TXT: `apps/petclinic-api/target/surefire-reports`
- Surefire HTML: `apps/petclinic-api/target/reports/surefire.html`
- JaCoCo HTML: `apps/petclinic-api/target/site/jacoco/index.html`

## Comandos utiles

Ejecutar todos los tests del aplicacion:

```powershell
.\mvnw.cmd -pl apps/petclinic-api test
```

Ejecutar solo una clase de test:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

Generar informe HTML de Surefire a partir de los resultados existentes:

```powershell
.\mvnw.cmd -pl apps/petclinic-api surefire-report:report -DskipTests
```

Cuando un ejercicio pida revisar informes, ejecuta primero:

```powershell
.\mvnw.cmd -pl apps/petclinic-api test
.\mvnw.cmd -pl apps/petclinic-api surefire-report:report -DskipTests
```

## Bloque 1: JUnit 5 y cobertura

### Ejercicio 1: Ejecutar los tests base y localizar los informes

Objetivo:

- Confirmar que el entorno funciona.
- Saber donde se ven resultados de ejecucion y cobertura.

Ficheros a abrir:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`
- `apps/petclinic-api/target/reports/surefire.html`
- `apps/petclinic-api/target/site/jacoco/index.html`

Pasos:

1. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

2. Comprueba en consola que aparece algo similar a:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

3. Abre el fichero:

```text
apps/petclinic-api/target/surefire-reports/org.springframework.samples.petclinic.testingexamples.PetAppointmentAdvisorTests.txt
```

4. Localiza el resumen de tests ejecutados, fallos y skipped.

5. Genera el informe HTML:

```powershell
.\mvnw.cmd -pl apps/petclinic-api surefire-report:report -DskipTests
```

6. Abre:

```text
apps/petclinic-api/target/reports/surefire.html
```

7. Abre:

```text
apps/petclinic-api/target/site/jacoco/index.html
```

8. En JaCoCo entra en:

```text
org.springframework.samples.petclinic.testingexamples
```

9. Entra en:

```text
PetAppointmentAdvisor
```

Resultado esperado:

- Los tests de `PetAppointmentAdvisorTests` pasan.
- Hay 1 test deshabilitado.
- JaCoCo muestra cobertura para `PetAppointmentAdvisor`.

Pregunta para responder:

- Que informacion te da Surefire que no te da JaCoCo?
- Que informacion te da JaCoCo que no te da Surefire?

### Ejercicio 2: Cubrir una rama de sintoma vacio

Objetivo:

- Practicar cobertura de ramas.
- Probar una entrada que no se ve claramente en los tests actuales.

Fichero a modificar:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`

Fichero a consultar:

- `src/main/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisor.java`

Metodo de produccion implicado:

```java
private boolean containsEmergencyKeyword(String symptom)
```

Comportamiento a probar:

- Si el sintoma es `null`, no debe ser urgencia.
- En un dia laborable y con una mascota no senior, debe devolver `ROUTINE_CHECKUP`.

Pasos:

1. Abre `PetAppointmentAdvisorTests.java`.
2. Anade este test dentro de la clase:

```java
@Test
@DisplayName("Un sintoma nulo no se considera urgencia")
void shouldTreatNullSymptomAsRoutineCheckup() {
	AppointmentType result = this.weekdayAdvisor.classify("Bimba", 3, null);

	assertThat(result).isEqualTo(AppointmentType.ROUTINE_CHECKUP);
}
```

3. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

4. Abre de nuevo JaCoCo:

```text
apps/petclinic-api/target/site/jacoco/index.html
```

5. Entra en `PetAppointmentAdvisor` y observa si hay menos amarillo/rojo en la rama de `containsEmergencyKeyword`.

Resultado esperado:

- El nuevo test pasa.
- El numero total de tests de esa clase aumenta en 1.
- La cobertura de ramas de `PetAppointmentAdvisor` mejora o se mantiene, pero ahora hay evidencia de sintoma `null`.

### Ejercicio 3: Test parametrizado con edades frontera

Objetivo:

- Usar `@ParameterizedTest`.
- Usar `@CsvSource`.
- Evitar cuatro tests casi iguales.

Fichero a modificar:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`

Comportamiento a probar:

| Edad | Sintoma | Resultado esperado |
| --- | --- | --- |
| 0 | revision inicial | `ROUTINE_CHECKUP` |
| 11 | revision | `ROUTINE_CHECKUP` |
| 12 | revision | `SAME_DAY` |
| 20 | revision | `SAME_DAY` |

Pasos:

1. Abre `PetAppointmentAdvisorTests.java`.
2. Comprueba que ya existen estos imports:

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
```

3. Anade este test:

```java
@ParameterizedTest(name = "Edad {0} con sintoma \"{1}\" -> {2}")
@CsvSource({
		"0, revision inicial, ROUTINE_CHECKUP",
		"11, revision, ROUTINE_CHECKUP",
		"12, revision, SAME_DAY",
		"20, revision, SAME_DAY"
})
@DisplayName("Clasifica correctamente edades frontera")
void shouldClassifyBoundaryAges(int age, String symptom, AppointmentType expectedType) {
	assertThat(this.weekdayAdvisor.classify("Toby", age, symptom)).isEqualTo(expectedType);
}
```

4. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

5. Genera Surefire HTML:

```powershell
.\mvnw.cmd -pl apps/petclinic-api surefire-report:report -DskipTests
```

6. Abre:

```text
apps/petclinic-api/target/reports/surefire.html
```

Resultado esperado:

- El test parametrizado cuenta como 4 ejecuciones.
- En el informe deben verse nombres con edad, sintoma y resultado esperado.
- Todos los casos pasan.

Pregunta para responder:

- Por que edad `11` y edad `12` son mas interesantes que probar solo edad `4`?

### Ejercicio 4: Excepciones y mensajes de error

Objetivo:

- Probar validaciones defensivas.
- Comprobar tipo de excepcion y mensaje.

Ficheros a abrir:

- `src/main/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisor.java`
- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`

Comportamiento a probar:

- Si el nombre de la mascota es `null`, `classify` debe lanzar `IllegalArgumentException`.
- El mensaje debe ser:

```text
El nombre de la mascota es obligatorio
```

Pasos:

1. Abre `PetAppointmentAdvisorTests.java`.
2. Comprueba que existe este import:

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

3. Anade este test:

```java
@Test
@DisplayName("No se puede clasificar una mascota sin nombre")
void shouldRejectMissingPetName() {
	assertThatThrownBy(() -> this.weekdayAdvisor.classify(null, 5, "revision"))
		.isInstanceOf(IllegalArgumentException.class)
		.hasMessage("El nombre de la mascota es obligatorio");
}
```

4. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

Resultado esperado:

- El test pasa.
- Si cambias temporalmente el mensaje esperado, el test falla mostrando claramente la diferencia.

No dejes el mensaje cambiado al terminar.

### Ejercicio 5: Test deshabilitado con intencion

Objetivo:

- Usar `@Disabled` de forma responsable.
- Ver como aparece un test skipped en Surefire.

Fichero a modificar:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`

Regla pendiente:

> Las mascotas menores de 1 anio deberian tener siempre cita el mismo dia.

Pasos:

1. Abre `PetAppointmentAdvisorTests.java`.
2. Anade este test:

```java
@Test
@Disabled("Pendiente de decision: confirmar si todos los cachorros deben tener cita el mismo dia")
@DisplayName("Los cachorros podrian requerir cita el mismo dia")
void disabledPuppyRuleExample() {
	assertThat(this.weekdayAdvisor.classify("Coco", 0, "primer chequeo")).isEqualTo(AppointmentType.SAME_DAY);
}
```

3. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

4. Genera Surefire HTML:

```powershell
.\mvnw.cmd -pl apps/petclinic-api surefire-report:report -DskipTests
```

5. Abre:

```text
apps/petclinic-api/target/reports/surefire.html
```

Resultado esperado:

- La build pasa.
- Aumenta el numero de tests skipped.
- El informe muestra que hay tests deshabilitados.

Pregunta para responder:

- Por que este test no deberia quedarse deshabilitado indefinidamente?

## Bloque 2: Spring Testing

### Ejercicio 6: Buscar propietarios con `@DataJpaTest`

Objetivo:

- Probar una query real de repositorio.
- Entender que `@DataJpaTest` carga JPA, repositorios, entidades y base de datos de test.

Fichero a modificar:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerRepositoryJpaSliceTests.java`

Fichero a consultar:

- `src/main/java/org/springframework/samples/petclinic/repository/OwnerRepository.java`

Metodo implicado:

```java
Collection<Owner> findByLastName(String lastName);
```

Comportamiento a probar:

- Buscar `"Dav"` encuentra propietarios cuyo apellido empieza por `Dav`.
- Buscar `"Xyz"` no encuentra propietarios.

Pasos:

1. Abre `OwnerRepositoryJpaSliceTests.java`.
2. Anade este test:

```java
@Test
@DisplayName("Busca propietarios por prefijo de apellido")
void shouldFindOwnersByLastNamePrefix() {
	Collection<Owner> davisOwners = this.ownerRepository.findByLastName("Dav");
	Collection<Owner> unknownOwners = this.ownerRepository.findByLastName("Xyz");

	assertThat(davisOwners).hasSize(2);
	assertThat(davisOwners).extracting(Owner::getLastName).containsOnly("Davis");
	assertThat(unknownOwners).isEmpty();
}
```

3. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=OwnerRepositoryJpaSliceTests" test
```

Resultado esperado:

- El test pasa.
- En la consola se ven sentencias SQL de Hibernate.
- No se arranca servidor web.

Pregunta para responder:

- Que parte real de la aplicacion estas probando aqui?

### Ejercicio 7: Comprobar rollback en `@DataJpaTest`

Objetivo:

- Ver que cada test de `@DataJpaTest` se ejecuta dentro de una transaccion.
- Comprobar que los datos guardados en un test no contaminan otros tests.

Fichero a modificar:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerRepositoryJpaSliceTests.java`

Datos a usar:

- Apellido unico: `RollbackDemo`
- Usuario unico: `rollback-demo`

Pasos:

1. Abre `OwnerRepositoryJpaSliceTests.java`.
2. Anade este test:

```java
@Test
@DisplayName("Guarda un propietario dentro de la transaccion del test")
void shouldSaveRollbackDemoOwnerInsideTestTransaction() {
	Owner owner = new Owner();
	owner.setFirstName("Rosa");
	owner.setLastName("RollbackDemo");
	owner.setAddress("Calle Test 1");
	owner.setCity("Granada");
	owner.setTelephone("600111222");

	User user = new User();
	user.setUsername("rollback-demo");
	user.setPassword("testing");
	user.setEnabled(true);
	owner.setUser(user);

	this.ownerRepository.save(owner);
	this.entityManager.flush();
	this.entityManager.clear();

	assertThat(this.ownerRepository.findByLastName("RollbackDemo")).hasSize(1);
}
```

3. Anade este segundo test:

```java
@Test
@DisplayName("No encuentra datos guardados por otros tests porque hubo rollback")
void shouldNotFindRollbackDemoOwnerFromAnotherTest() {
	assertThat(this.ownerRepository.findByLastName("RollbackDemo")).isEmpty();
}
```

4. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=OwnerRepositoryJpaSliceTests" test
```

Resultado esperado:

- Ambos tests pasan.
- El segundo test no encuentra `RollbackDemo`.
- Esto demuestra que el primer test no deja datos permanentes.

Importante:

- No uses `@Order` para hacer depender un test de otro.
- Cada test debe poder ejecutarse de forma independiente.

### Ejercicio 8: Primer test MVC con `@WebMvcTest`

Objetivo:

- Probar un controlador sin arrancar servidor HTTP real.
- Verificar estado HTTP, vista y modelo.

Fichero a modificar:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerControllerMvcSliceTests.java`

Controlador implicado:

- `src/main/java/org/springframework/samples/petclinic/web/OwnerController.java`

Ruta a probar:

```text
GET /owners/find
```

Comportamiento esperado:

- Devuelve `200 OK`.
- Usa la vista `owners/findOwners`.
- Incluye un atributo de modelo llamado `owner`.

Pasos:

1. Abre `OwnerControllerMvcSliceTests.java`.
2. Anade este test:

```java
@Test
@WithMockUser(username = "spring")
@DisplayName("Muestra el formulario de busqueda de propietarios")
void shouldShowFindOwnersForm() throws Exception {
	this.mockMvc.perform(get("/owners/find"))
		.andExpect(status().isOk())
		.andExpect(view().name("owners/findOwners"))
		.andExpect(model().attributeExists("owner"));
}
```

3. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=OwnerControllerMvcSliceTests" test
```

Resultado esperado:

- El test pasa.
- No necesitas datos SQL.
- No necesitas repositorios reales.

Pregunta para responder:

- Por que se usa `@WithMockUser`?

### Ejercicio 9: MVC con mock de servicio y redireccion

Objetivo:

- Mockear una dependencia del controlador.
- Probar una decision MVC: cuando hay un unico propietario, se redirige a su ficha.

Fichero a modificar:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/OwnerControllerMvcSliceTests.java`

Ruta a probar:

```text
GET /owners?lastName=Franklin
```

Comportamiento esperado:

- Si `OwnerService` devuelve un unico propietario con `id = 1`, el controlador redirige a:

```text
redirect:/owners/1
```

Imports que puedes necesitar:

```java
import java.util.List;
```

Pasos:

1. Abre `OwnerControllerMvcSliceTests.java`.
2. Anade el import `java.util.List` si no existe.
3. Anade este test:

```java
@Test
@WithMockUser(username = "spring")
@DisplayName("Redirige a la ficha cuando la busqueda encuentra un unico propietario")
void shouldRedirectWhenFindOwnerReturnsOneResult() throws Exception {
	Owner owner = new Owner();
	owner.setId(1);
	owner.setFirstName("George");
	owner.setLastName("Franklin");

	given(this.ownerService.findOwnerByLastName("Franklin")).willReturn(List.of(owner));

	this.mockMvc.perform(get("/owners").param("lastName", "Franklin"))
		.andExpect(status().is3xxRedirection())
		.andExpect(view().name("redirect:/owners/1"));
}
```

4. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=OwnerControllerMvcSliceTests" test
```

Resultado esperado:

- El test pasa.
- Si quitas el `given(...)`, el test falla porque el mock no devuelve el propietario esperado.

Pregunta para responder:

- Estas probando la query real de base de datos o la logica MVC del controlador?

### Ejercicio 10: Experimento con `@DirtiesContext`

Objetivo:

- Entender la cache de contextos de Spring Test.
- Ver como `@DirtiesContext` fuerza un contexto nuevo.

Fichero a modificar temporalmente:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/spring/DirtiesContextExampleTests.java`

Pasos:

1. Ejecuta la clase tal como esta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=DirtiesContextExampleTests" test
```

Resultado esperado:

```text
Tests run: 2, Failures: 0, Errors: 0
```

2. Abre `DirtiesContextExampleTests.java`.
3. Localiza este test:

```java
void shouldMarkContextAsDirtyAfterMutatingSingletonBean()
```

4. Comenta temporalmente esta linea:

```java
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
```

5. Ejecuta de nuevo:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=DirtiesContextExampleTests" test
```

Resultado esperado:

- El segundo test falla.
- Falla porque el contexto no se reconstruye y el bean `WaitingRoom` conserva estado.

6. Vuelve a dejar la anotacion como estaba:

```java
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
```

7. Ejecuta de nuevo para confirmar que todo pasa:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=DirtiesContextExampleTests" test
```

Resultado final esperado:

- La clase vuelve a pasar.

Preguntas para responder:

- Por que Spring reutiliza contextos entre tests?
- Por que `@DirtiesContext` puede hacer mas lenta una suite?
- En que casos estaria justificado usarlo?

## Bloque 3: Mini reto integrador

### Ejercicio 11: Nueva regla de negocio con TDD

Objetivo:

- Escribir primero un test que falle.
- Implementar una regla.
- Ver el impacto en cobertura.

Fichero de test a modificar primero:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`

Fichero de produccion a modificar despues:

- `src/main/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisor.java`

Nueva regla:

> Si el sintoma contiene "no come" y la mascota tiene 10 anios o mas, la cita debe ser `SAME_DAY`.

Parte 1: escribir el test

1. Abre `PetAppointmentAdvisorTests.java`.
2. Anade este test:

```java
@Test
@DisplayName("Una mascota senior que no come necesita cita el mismo dia")
void shouldRecommendSameDayWhenSeniorPetDoesNotEat() {
	assertThat(this.weekdayAdvisor.classify("Lola", 10, "no come desde ayer")).isEqualTo(AppointmentType.SAME_DAY);
}
```

3. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

Resultado esperado en este punto:

- El test nuevo debe fallar.
- El resultado actual sera `ROUTINE_CHECKUP` si la regla aun no esta implementada.

Parte 2: implementar la regla

4. Abre `PetAppointmentAdvisor.java`.
5. Localiza el metodo:

```java
public AppointmentType classify(String petName, int age, String symptom)
```

6. Despues de comprobar urgencias y antes de la regla de edad/fin de semana, anade una condicion equivalente a:

```java
if (age >= 10 && containsDoesNotEatKeyword(symptom)) {
	return AppointmentType.SAME_DAY;
}
```

7. Crea un metodo privado para detectar el texto `"no come"`.

Ejemplo de firma:

```java
private boolean containsDoesNotEatKeyword(String symptom) {
	// Implementar aqui
}
```

Pistas:

- Debe devolver `false` si `symptom` es `null`, vacio o solo espacios.
- Conviene comparar en minusculas con `Locale.ROOT`, como ya hace `containsEmergencyKeyword`.

8. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

Resultado esperado:

- Todos los tests de `PetAppointmentAdvisorTests` pasan.

Parte 3: comprobar que urgencias siguen ganando

9. Anade otro test en `PetAppointmentAdvisorTests.java`:

```java
@Test
@DisplayName("Una urgencia sigue teniendo prioridad sobre la regla de no comer")
void shouldKeepEmergencyPriorityOverDoesNotEatRule() {
	assertThat(this.weekdayAdvisor.classify("Lola", 10, "no come y no respira")).isEqualTo(AppointmentType.EMERGENCY);
}
```

10. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api "-Dtest=PetAppointmentAdvisorTests" test
```

Resultado esperado:

- La urgencia sigue devolviendo `EMERGENCY`.
- La regla nueva no rompe reglas mas prioritarias.

Parte 4: revisar cobertura

11. Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api test
```

12. Abre:

```text
apps/petclinic-api/target/site/jacoco/index.html
```

13. Entra en:

```text
org.springframework.samples.petclinic.testingexamples > PetAppointmentAdvisor
```

Resultado esperado:

- La nueva condicion aparece cubierta.
- Si hay amarillo en la nueva condicion, falta algun caso de rama.

### Ejercicio 12: Comparar tipos de test

Objetivo:

- Elegir la herramienta correcta para cada tipo de comportamiento.

No hay que modificar codigo.

Fichero a abrir:

- `docs/testing/ejercicios-testing.md`

Rellena esta tabla con tus palabras:

| Escenario | Tipo de test recomendado | Anotacion principal | Motivo |
| --- | --- | --- | --- |
| Regla pura sobre edad, sintoma y fecha | | | |
| Query JPQL con `left join fetch` | | | |
| Formulario MVC que devuelve una vista | | | |
| Seguridad de una ruta web | | | |
| Bean singleton que queda contaminado entre tests | | | |
| Flujo completo con base de datos, MVC y Spring Security | | | |

Opciones que puedes usar:

- Unitario puro con JUnit 5
- `@DataJpaTest`
- `@WebMvcTest`
- `@SpringJUnitConfig`
- `@SpringBootTest`
- `@DirtiesContext`
- `@WithMockUser`

Resultado esperado:

- Puedes justificar por que no siempre conviene usar `@SpringBootTest`.
- Puedes explicar que ganas y que pierdes con cada slice.


## Checklist antes de terminar

Ejecuta:

```powershell
.\mvnw.cmd -pl apps/petclinic-api test
.\mvnw.cmd -pl apps/petclinic-api surefire-report:report -DskipTests
```

Resultado esperado global:

- La build termina con `BUILD SUCCESS`.
- No hay failures ni errors.
- Puede haber skipped por los tests deshabilitados.
- Los informes estan actualizados.

---

# Ejercicios añadidos para la edición de 40 horas

Cubren lo que las transparencias explican y los doce ejercicios anteriores no practicaban:
dobles de prueba con Mockito (diapositivas 106–112 y 116–118), organización de las pruebas
con JUnit 5 (101–103), pruebas de seguridad (132), REST Assured contra la API propia
(124–130), Postman/Newman (131) y el problema de los dobles de servicios externos (122–123).

Se ejecutan desde la raíz del repositorio. En Linux o macOS, `./mvnw` en lugar de `.\mvnw.cmd`.

---

## Ejercicio 13 · Dobles de prueba con Mockito

**Objetivo.** Usar los cinco tipos de doble y las verificaciones de Mockito sobre un servicio
real, sin levantar Spring.
**Diapositivas:** 106–112, 116–118. **Tiempo:** 45 min.

`PetService` recibe sus dos repositorios por **constructor**, así que se puede probar con
`new`, sin reflexión (compáralo con `OwnerServiceIsolatedMockTests`, que necesita
`ReflectionTestUtils` porque `OwnerService` mezcla estilos).

Crea `src/test/java/.../service/PetServiceMockitoTests.java` con
`@ExtendWith(MockitoExtension.class)`, `@Mock` para los dos repositorios y `@InjectMocks`
para el servicio. Escribe una prueba para cada punto:

| # | Qué probar | Técnica de Mockito |
|---|---|---|
| 1 | `findPetById(7)` devuelve lo que da el repositorio | *stub*: `when(...).thenReturn(...)` |
| 2 | `findPetTypes()` no toca `VisitRepository` | `verifyNoInteractions(visitRepository)` |
| 3 | `deletePet(7)` llama **una vez** a `deleteById(7)` y a nada más | `verify(..., times(1))` + `verifyNoMoreInteractions` |
| 4 | `saveVisit(v)` guarda **exactamente** la visita recibida | `ArgumentCaptor<Visit>` |
| 5 | Si el repositorio falla al buscar, la excepción se propaga | saboteador: `thenThrow(...)` |
| 6 | Si `save` (que es `void`) falla, la excepción se propaga | `doThrow(...).when(repo).save(any())` |
| 7 | Guardar una mascota con nombre repetido en el mismo dueño lanza `DuplicatedPetNameException` **y no guarda** | `assertThrows` + `verify(..., never())` |
| 8 | Un *spy* sobre una lista real: `add` funciona, pero `size()` devuelve 100 | `spy(...)` + `doReturn(100).when(spy).size()` |

**Resultado esperado.** Ocho pruebas en verde en menos de un segundo. Ninguna toca la base de
datos ni levanta un contexto de Spring.

**Preguntas.**
- La prueba 7 necesita construir un `Owner` con dos mascotas. ¿Es un *dummy*, un *stub* o
  un *fake*? *(Ninguno: son objetos reales del dominio. No todo lo que aparece en una prueba
  tiene que ser un doble.)*
- ¿Por qué `when(repo.save(x)).thenThrow(...)` no compila en el punto 6?

Solución: [`soluciones/Ejercicio13PetServiceMockitoTests.java`](soluciones/Ejercicio13PetServiceMockitoTests.java)

---

## Ejercicio 14 · Pruebas que se leen como una especificación

**Objetivo.** Organizar una batería con `@Nested`, `@DisplayName`, `assertAll` y
suposiciones.
**Diapositivas:** 101–103. **Tiempo:** 30 min.

Reescribe las pruebas de `PetAppointmentAdvisor` (o crea una clase nueva) con esta forma:

```
PetAppointmentAdvisor
├── cuando los datos no son válidos
│   ├── rechaza un nombre vacío
│   └── rechaza una edad negativa
├── cuando hay síntomas de urgencia
│   └── clasifica como EMERGENCY  (parametrizada: "sangra", "no respira", "convulsion")
├── en un día laborable
│   ├── una mascota de 11 años va a revisión
│   └── una mascota de 12 años va el mismo día
└── en fin de semana
    └── todo lo que no es urgente va el mismo día
```

1. Usa una clase `@Nested` por rama y `@DisplayName` en castellano.
2. Fija el día con `Clock.fixed(...)` en el `@BeforeEach` de cada clase anidada: un lunes
   para «día laborable», un sábado para «fin de semana».
3. En la prueba de datos no válidos, comprueba **también el mensaje** de la excepción
   (diapositiva 103).
4. Agrupa varias comprobaciones de `friendlySummary(...)` en un solo `assertAll`, y rompe una
   a propósito: ¿cuántos fallos informa?
5. Añade una prueba que **solo** se ejecute en Windows con `assumeTrue` o
   `@EnabledOnOs(OS.WINDOWS)` y comprueba en el informe que aparece como *skipped* en
   Linux.
6. Etiqueta las pruebas de urgencia con `@Tag("critico")` y ejecútalas solas:

   ```powershell
   .\mvnw.cmd -pl apps/petclinic-api test "-Dgroups=critico"
   ```

**Resultado esperado.** El informe de Surefire (`apps/petclinic-api/target/reports/surefire.html`)
se lee como la tabla de reglas del principio de este documento.

**Pregunta.** ¿Por qué `assertAll` y no cinco `assertEquals` seguidos? *(Con aserciones
seguidas, el primer fallo oculta los demás.)*

Solución: [`soluciones/Ejercicio14PruebasAnidadasTests.java`](soluciones/Ejercicio14PruebasAnidadasTests.java)

---

## Ejercicio 15 · Probar la seguridad

**Objetivo.** Comprobar con pruebas automáticas que las reglas del módulo 3 se cumplen.
**Diapositiva:** 132. **Tiempo:** 35 min.

Crea `src/test/java/.../web/security/ReglasDeAccesoTests.java` con `@SpringBootTest` y
`@AutoConfigureMockMvc` (no hace falta construir el `MockMvc` a mano como en la diapositiva).
Una prueba por fila:

| Petición | Usuario | Esperado |
|---|---|---|
| `GET /owners/1` | anónimo | 302 a `/login` |
| `GET /owners/1` | `vet` | 403 |
| `GET /owners/1` | `owner1` real (`@WithUserDetails`) | 200 |
| `GET /owners/2` | `owner1` real | 200, pero **sin** propietario en el modelo |
| `POST /owners/new` **sin** CSRF | `admin` | 403 |
| `POST /owners/new` con `with(csrf())` y datos válidos (incluidos `user.username` y `user.password`) | `admin` | 302 |
| `GET /admin/cualquiera` | `owner` | 403 |
| `GET /actuator/health` | anónimo | 200 |

Usa las tres formas de indicar el usuario y explica cuándo conviene cada una:
`@WithMockUser`, `@WithUserDetails` y `.with(user("x").authorities(...))`.

Si has hecho el ejercicio 6 o el 13 del módulo 3, añade dos filas más para la API:

| `GET /api/v1/visits` | sin token | 401 |
| `GET /api/v1/visits` | `.with(jwt().authorities(...))` | 200 |

**Resultado esperado.** Una batería que falla en cuanto alguien abre una ruta por error en
`SecurityConfiguration`. Pruébalo: cambia `/owners/**` a `permitAll()` y ejecuta.

**Preguntas.**
- La fila de `owners/2` depende de `OwnerService`, no de `SecurityConfiguration`. ¿Por qué
  `@WithMockUser(username = "owner1")` **no** sirve para esa prueba? *(La comprobación busca
  el `Owner` asociado al usuario en la base de datos; con un usuario ficticio, la regla se
  cumple o falla por motivos que no son los que se quieren probar.)*
- Esa misma fila responde **200** con la página vacía. ¿Es correcto? ¿Qué debería devolver?
  *(Lo razonable es 403, o 404 si no se quiere revelar que el propietario existe. Es una
  mejora pendiente del proyecto: escribe primero la prueba que lo exige y después el
  cambio.)*

Solución: [`soluciones/Ejercicio15ReglasDeAccesoTests.java`](soluciones/Ejercicio15ReglasDeAccesoTests.java)

---

## Ejercicio 16 · REST Assured contra la API propia

**Objetivo.** Probar la API de extremo a extremo, por HTTP real, con la aplicación arrancada
en un puerto aleatorio.
**Diapositivas:** 124–130. **Tiempo:** 40 min.

`ExternalApiRestAssuredTests` prueba APIs de **terceros**. Ahora toca la propia.

1. Crea `src/test/java/.../web/api/VisitApiRestAssuredTests.java` con
   `@SpringBootTest(webEnvironment = RANDOM_PORT)`. Inyecta el puerto con `@LocalServerPort`
   y asígnalo a `RestAssured.port` en un `@BeforeEach`. (La diapositiva 125 usa
   `DEFINED_PORT` y la URL completa: si el 8080 está ocupado, la prueba falla.)
2. Escribe estas pruebas:
   1. `GET /api/v1/visits?size=2` devuelve 200, dos elementos y `page.totalElements` > 2;
   2. `GET /api/v1/visits?sort=unknown` devuelve 400 con `Content-Type:
      application/problem+json`;
   3. `POST /api/v1/visits` con una visita válida devuelve 201 y una cabecera `Location`
      que, al seguirla, devuelve la misma visita;
   4. `POST` sin descripción devuelve 400 y el detalle menciona `description`;
   5. el listado responde en menos de 2 segundos (`time(lessThan(2000L))`).
3. Activa el registro de petición y respuesta **solo cuando falle** una prueba:
   `RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()`.
4. Opcional: autentícate con el formulario de Spring Security y pide la ficha HTML de un
   propietario:

   ```java
   given().auth().form("owner1", "0wn3r", FormAuthConfig.springSecurity().withAutoDetectionOfCsrf())
   ```

**Resultado esperado.** Cinco pruebas en verde. Rompe a propósito el `Location` en
`RestfulVisitController` y comprueba que la prueba 3 lo detecta.

**Pregunta.** La prueba 3 crea datos. ¿Se deshacen al terminar, como en `@DataJpaTest`?
*(No: con `RANDOM_PORT` la petición va por otro hilo y otra transacción. Hay que diseñar las
aserciones para que no dependan del orden, o limpiar a mano.)*

Solución: [`soluciones/Ejercicio16VisitApiRestAssuredTests.java`](soluciones/Ejercicio16VisitApiRestAssuredTests.java)

---

## Ejercicio 17 · Postman y Newman

**Objetivo.** Mantener una colección de pruebas de API que también se pueda ejecutar en la
integración continua.
**Diapositiva:** 131. **Tiempo:** 30 min. **Requisito:** Postman y, para Newman, Node.js.

1. Importa en Postman la colección y el entorno del repositorio:
   - [`postman/petclinic-api.postman_collection.json`](../../postman/petclinic-api.postman_collection.json)
   - [`postman/local.postman_environment.json`](../../postman/local.postman_environment.json)
2. Arranca la aplicación y ejecuta la colección completa con *Run collection*. Lee los
   *scripts* de la pestaña *Tests* de cada petición.
3. Fíjate en cómo «Crear visita» guarda el identificador en una variable que usa la petición
   siguiente.
4. Añade tú una petición: `GET /api/v1/visits?petName=Leo` con dos pruebas: que el estado sea
   200 y que **todas** las visitas devueltas sean de Leo.
5. Ejecuta la colección sin interfaz gráfica, como lo haría un servidor de integración
   continua:

   ```bash
   npx newman run postman/petclinic-api.postman_collection.json \
       -e postman/local.postman_environment.json
   ```

6. Opcional: si `auth-service` está arrancado, ejecuta también la carpeta «Autenticación»,
   que pide un token y lo reutiliza.

**Resultado esperado.** Newman termina con 0 fallos y devuelve código de salida 0. Con la
aplicación parada, devuelve un código distinto de 0: eso es lo que hace fallar un *pipeline*.

**Pregunta.** ¿Qué aporta esta colección que no aporten las pruebas de REST Assured, y al
revés? *(La colección la pueden mantener personas que no programan en Java y documenta la
API con ejemplos; REST Assured vive con el código y se ejecuta en cada `mvnw verify`.)*

---

## Ejercicio 18 · Dobles de servicios externos, y sus límites

**Objetivo.** Probar un cliente HTTP sin el servicio real, y ver qué riesgo queda.
**Diapositivas:** 122–123. **Tiempo:** 30 min.

1. Escribe una prueba **unitaria** de `BillsGateway.conRestClient()` con
   `MockRestServiceServer`:

   ```java
   RestClient.Builder builder = RestClient.builder();
   MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
   BillsGateway gateway = new BillsGateway(propiedades, mock(BillClient.class), builder, new RestTemplateBuilder());
   ```

2. Programa el servidor simulado para que responda a `GET /api/v1/bills` con dos facturas
   y comprueba lo que devuelve el método.
3. Ahora simula un 500. ¿Qué devuelve `conRestClient()`? ¿Se activa el cortocircuito?
4. Cambia en la respuesta simulada `concept` por `concepto`, como si el equipo de facturas
   hubiera renombrado el campo. ¿Falla la prueba?

**Resultado esperado.** En el paso 3 la prueba recibe la excepción: el `@CircuitBreaker`
**no** actúa porque se ha creado el objeto con `new` y no hay *proxy* (módulo 1,
ejercicio 9). En el paso 4 la prueba **sigue en verde** si no compruebas el campo, y el
`concept` llega a `null`.

> **Mensaje clave.** El doble responde lo que **tú** crees que responde el servicio
> (diapositiva 122). Si el servicio cambia, tu doble no se entera (123). La respuesta a eso
> son las pruebas de **contrato** dirigidas por el consumidor (Spring Cloud Contract, Pact),
> o una prueba de integración contra el servicio real en un entorno compartido.

Solución: [`soluciones/Ejercicio18BillsGatewayTests.java`](soluciones/Ejercicio18BillsGatewayTests.java)
