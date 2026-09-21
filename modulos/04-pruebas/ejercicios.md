# Ejercicios de testing y cobertura

Estos ejercicios se hacen dentro del proyecto `apps/petclinic-api`.

Antes de empezar, abre una terminal en:

```text
apps\petclinic-api
```

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

- Surefire XML/TXT: `target/surefire-reports`
- Surefire HTML: `target/reports/surefire.html`
- JaCoCo HTML: `target/site/jacoco/index.html`

## Comandos utiles

Ejecutar todos los tests del aplicacion:

```powershell
.\mvnw.cmd test
```

Ejecutar solo una clase de test:

```powershell
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
```

Generar informe HTML de Surefire a partir de los resultados existentes:

```powershell
.\mvnw.cmd surefire-report:report -DskipTests
```

Cuando un ejercicio pida revisar informes, ejecuta primero:

```powershell
.\mvnw.cmd test
.\mvnw.cmd surefire-report:report -DskipTests
```

## Bloque 1: JUnit 5 y cobertura

### Ejercicio 1: Ejecutar los tests base y localizar los informes

Objetivo:

- Confirmar que el entorno funciona.
- Saber donde se ven resultados de ejecucion y cobertura.

Ficheros a abrir:

- `src/test/java/org/springframework/samples/petclinic/testingexamples/PetAppointmentAdvisorTests.java`
- `target/reports/surefire.html`
- `target/site/jacoco/index.html`

Pasos:

1. Ejecuta:

```powershell
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
```

2. Comprueba en consola que aparece algo similar a:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

3. Abre el fichero:

```text
target/surefire-reports/org.springframework.samples.petclinic.testingexamples.PetAppointmentAdvisorTests.txt
```

4. Localiza el resumen de tests ejecutados, fallos y skipped.

5. Genera el informe HTML:

```powershell
.\mvnw.cmd surefire-report:report -DskipTests
```

6. Abre:

```text
target/reports/surefire.html
```

7. Abre:

```text
target/site/jacoco/index.html
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
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
```

4. Abre de nuevo JaCoCo:

```text
target/site/jacoco/index.html
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
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
```

5. Genera Surefire HTML:

```powershell
.\mvnw.cmd surefire-report:report -DskipTests
```

6. Abre:

```text
target/reports/surefire.html
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
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
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
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
```

4. Genera Surefire HTML:

```powershell
.\mvnw.cmd surefire-report:report -DskipTests
```

5. Abre:

```text
target/reports/surefire.html
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
.\mvnw.cmd "-Dtest=OwnerRepositoryJpaSliceTests" test
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
.\mvnw.cmd "-Dtest=OwnerRepositoryJpaSliceTests" test
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
.\mvnw.cmd "-Dtest=OwnerControllerMvcSliceTests" test
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
.\mvnw.cmd "-Dtest=OwnerControllerMvcSliceTests" test
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
.\mvnw.cmd "-Dtest=DirtiesContextExampleTests" test
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
.\mvnw.cmd "-Dtest=DirtiesContextExampleTests" test
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
.\mvnw.cmd "-Dtest=DirtiesContextExampleTests" test
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
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
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
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
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
.\mvnw.cmd "-Dtest=PetAppointmentAdvisorTests" test
```

Resultado esperado:

- La urgencia sigue devolviendo `EMERGENCY`.
- La regla nueva no rompe reglas mas prioritarias.

Parte 4: revisar cobertura

11. Ejecuta:

```powershell
.\mvnw.cmd test
```

12. Abre:

```text
target/site/jacoco/index.html
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
.\mvnw.cmd test
.\mvnw.cmd surefire-report:report -DskipTests
```

Resultado esperado global:

- La build termina con `BUILD SUCCESS`.
- No hay failures ni errors.
- Puede haber skipped por los tests deshabilitados.
- Los informes estan actualizados.
