// =============================================================================
// Modulo 6 - Ejercicio 4: un predicado nuevo para VisitSpecification.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
//
// Filtrar visitas por la ciudad del propietario, navegando
// visit -> pet -> owner -> city.
// =============================================================================
package org.springframework.samples.petclinic.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.samples.petclinic.model.Visit;

public final class Ejercicio04CiudadPropietario {

	private Ejercicio04CiudadPropietario() {
	}

	// -------------------------------------------------------------------------
	// 1. El predicado, en el estilo de VisitSpecification
	// -------------------------------------------------------------------------
	//
	// Devolver null cuando el criterio no viene es lo que hace que la
	// combinacion con and(...) funcione sin escribir un solo `if`: Spring Data
	// descarta los predicados nulos.
	public static Specification<Visit> byOwnerCity(String ownerCity) {
		return (root, query, cb) -> ownerCity == null || ownerCity.isBlank() ? null
				: cb.equal(cb.lower(root.get("pet").get("owner").get("city")), ownerCity.toLowerCase());
	}

	// -------------------------------------------------------------------------
	// 2. Y el combinador, con el parametro nuevo al final
	// -------------------------------------------------------------------------
	//
	// Anadirlo al final y no en medio no es una minucia: los siete parametros ya
	// son demasiados y todos son String. Cambiar el orden de dos de ellos
	// compila perfectamente y devuelve resultados incorrectos.
	//
	// Cuando se llega a ocho parametros del mismo tipo, la senal es clara: toca
	// pasar a un objeto de criterios. El laboratorio de Criteria API lo hace con
	// el record FiltroVisitas, que ademas se lee mucho mejor en la llamada.
	public static Specification<Visit> filterVisits(Integer petId, String ownerLastName, String petName,
			String petType, String description, java.time.LocalDate fromDate, java.time.LocalDate toDate,
			String ownerCity) {
		return Specification.where(VisitSpecification.byPetId(petId))
			.and(VisitSpecification.byOwnerLastName(ownerLastName))
			.and(VisitSpecification.byPetName(petName))
			.and(VisitSpecification.byPetType(petType))
			.and(VisitSpecification.byDescription(description))
			.and(VisitSpecification.fromDate(fromDate))
			.and(VisitSpecification.toDate(toDate))
			.and(byOwnerCity(ownerCity));
	}

	// -------------------------------------------------------------------------
	// 3. Exponerlo: un parametro mas en RestfulVisitSearchController
	// -------------------------------------------------------------------------
	//
	//     @GetMapping
	//     public PagedModel<VisitResponse> buscar(
	//             @RequestParam(required = false) Integer petId,
	//             ...
	//             @RequestParam(required = false) String ownerCity,
	//             Pageable pageable) {
	//
	//         Specification<Visit> filtro = Ejercicio04CiudadPropietario.filterVisits(
	//                 petId, ownerLastName, petName, petType, description,
	//                 fromDate, toDate, ownerCity);
	//
	//         return new PagedModel<>(this.visitRepository.findAll(filtro, pageable)
	//                 .map(VisitResponse::from));
	//     }
	//
	// Probarlo:
	//     curl.exe -s "localhost:8080/api/v1/visits?ownerCity=Monona" | ConvertFrom-Json
	//
	// Las cuatro visitas de data.sql son de Jean Coleman, de Monona, asi que
	// Monona devuelve 4 y Madison 0. Es un dato comodo para la prueba.

	// -------------------------------------------------------------------------
	// 4. La prueba
	// -------------------------------------------------------------------------
	//
	//     @DataJpaTest
	//     class Ejercicio04Tests {
	//
	//         @Autowired
	//         private VisitRepository visitRepository;
	//
	//         @Test
	//         @DisplayName("Filtra por la ciudad del propietario, ignorando mayusculas")
	//         void filtraPorCiudadDelPropietario() {
	//             assertThat(this.visitRepository.findAll(
	//                     Ejercicio04CiudadPropietario.byOwnerCity("monona"))).hasSize(4);
	//             assertThat(this.visitRepository.findAll(
	//                     Ejercicio04CiudadPropietario.byOwnerCity("Madison"))).isEmpty();
	//         }
	//
	//         @Test
	//         @DisplayName("Un criterio nulo no anade predicado")
	//         void elCriterioNuloNoFiltra() {
	//             assertThat(this.visitRepository.findAll(
	//                     Ejercicio04CiudadPropietario.byOwnerCity(null))).hasSize(4);
	//         }
	//     }

	// -------------------------------------------------------------------------
	// POR QUE ESTO Y NO CONCATENAR CADENAS
	// -------------------------------------------------------------------------
	//
	// Es la pregunta del ejercicio, y la respuesta tiene tres partes:
	//
	//   1. INYECCION. Con "... WHERE city = '" + ciudad + "'" el cliente decide
	//      parte de la consulta. Con criterios, el valor SIEMPRE viaja como
	//      parametro preparado; no hay forma de que se interprete como SQL.
	//   2. COMPOSICION. Cada predicado se escribe y se prueba por separado, y se
	//      combinan sin tocarlos. Con cadenas hay que llevar la cuenta de los
	//      AND y de los espacios, que es de donde salen la mitad de los errores.
	//   3. TIPOS. cb.equal sobre una fecha no acepta un String. El compilador
	//      rechaza lo que con una cadena habria compilado sin decir nada.
	//
	// El precio, que tambien hay que decirlo: es mas verbosa y se lee peor que
	// la consulta equivalente. Para un filtro FIJO, @Query con JPQL gana.
	// La comparacion completa esta en laboratorio-criteria-api.md.

}
