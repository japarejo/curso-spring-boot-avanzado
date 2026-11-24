package org.springframework.samples.petclinic.repository;

import java.time.LocalDate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.samples.petclinic.model.Visit;

/**
 * Filtros dinámicos de visitas con la API de criterios.
 *
 * <p>Una {@code Specification<Visit>} es una función que recibe tres cosas y devuelve una
 * condición del WHERE:
 *
 * <pre>
 *   (root, query, cb) -&gt; Predicate
 *
 *   root  = la entidad de la consulta (FROM Visit v). root.get("date") es v.date
 *   query = la consulta completa (para distinct, orderBy...). Aquí no se usa
 *   cb    = CriteriaBuilder, la fábrica de condiciones: equal, like, lower...
 * </pre>
 *
 * <p>La regla que hace que todo encaje: <b>si el filtro no viene, se devuelve {@code null}</b>.
 * Spring Data descarta los predicados nulos, así que el WHERE final solo contiene los filtros
 * que el usuario ha rellenado, sin escribir un solo {@code if} al combinarlos.
 *
 * <p>Los predicados van de menos a más complejo:
 * <ol>
 *   <li>Igualdad sobre un campo propio: {@link #byPetId}.</li>
 *   <li>Navegar relaciones (genera JOIN): {@link #byPetType}.</li>
 *   <li>Búsqueda parcial sin mayúsculas: {@link #byPetName}, {@link #byOwnerLastName},
 *       {@link #byDescription}.</li>
 *   <li>Rangos: {@link #fromDate}, {@link #toDate}.</li>
 * </ol>
 */
public final class VisitSpecification {

	private VisitSpecification() {
		// Solo métodos estáticos
	}

	// ---------------------------------------------------------------------------------
	// 1. Igualdad: WHERE v.pet.id = ?
	// ---------------------------------------------------------------------------------

	public static Specification<Visit> byPetId(Integer petId) {
		return (root, query, cb) -> {
			if (petId == null) {
				return null; // sin filtro
			}
			return cb.equal(root.get("pet").get("id"), petId);
		};
	}

	// ---------------------------------------------------------------------------------
	// 2. Navegar relaciones: cada get() sobre una relación es un JOIN
	//    WHERE lower(v.pet.type.name) = ?
	// ---------------------------------------------------------------------------------

	public static Specification<Visit> byPetType(String petType) {
		return (root, query, cb) -> {
			if (sinValor(petType)) {
				return null;
			}
			Expression<String> tipo = root.get("pet").get("type").get("name");
			return cb.equal(cb.lower(tipo), petType.toLowerCase());
		};
	}

	// ---------------------------------------------------------------------------------
	// 3. Búsqueda parcial: WHERE lower(campo) LIKE '%texto%'
	//    Las tres comparten la misma forma; solo cambia el campo.
	// ---------------------------------------------------------------------------------

	public static Specification<Visit> byPetName(String petName) {
		return (root, query, cb) -> sinValor(petName) ? null
				: contiene(cb, root.get("pet").get("name"), petName);
	}

	public static Specification<Visit> byOwnerLastName(String ownerLastName) {
		return (root, query, cb) -> sinValor(ownerLastName) ? null
				: contiene(cb, root.get("pet").get("owner").get("lastName"), ownerLastName);
	}

	public static Specification<Visit> byDescription(String description) {
		return (root, query, cb) -> sinValor(description) ? null
				: contiene(cb, root.get("description"), description);
	}

	// ---------------------------------------------------------------------------------
	// 4. Rangos: WHERE v.date >= ? / WHERE v.date <= ?
	//    Dos predicados independientes: se puede dar solo uno de los extremos.
	// ---------------------------------------------------------------------------------

	public static Specification<Visit> fromDate(LocalDate fromDate) {
		return (root, query, cb) -> fromDate == null ? null
				: cb.greaterThanOrEqualTo(root.get("date"), fromDate);
	}

	public static Specification<Visit> toDate(LocalDate toDate) {
		return (root, query, cb) -> toDate == null ? null
				: cb.lessThanOrEqualTo(root.get("date"), toDate);
	}

	// ---------------------------------------------------------------------------------
	// Combinar: allOf une con AND. Los que devuelven null desaparecen del WHERE.
	//
	//   filterVisits(null, null, "leo", null, null, null, null)
	//     -> WHERE lower(p.name) LIKE '%leo%'
	// ---------------------------------------------------------------------------------

	public static Specification<Visit> filterVisits(Integer petId, String ownerLastName, String petName,
			String petType, String description, LocalDate fromDate, LocalDate toDate) {
		return Specification.allOf(
				byPetId(petId),
				byOwnerLastName(ownerLastName),
				byPetName(petName),
				byPetType(petType),
				byDescription(description),
				fromDate(fromDate),
				toDate(toDate));
	}

	// ---------------------------------------------------------------------------------
	// Ayudantes
	// ---------------------------------------------------------------------------------

	/** Un texto nulo o vacío no filtra (un formulario envía "" en los campos en blanco). */
	private static boolean sinValor(String texto) {
		return texto == null || texto.isBlank();
	}

	/**
	 * lower(campo) LIKE '%texto%'. El valor viaja como parámetro de la consulta preparada,
	 * nunca concatenado en el SQL: por eso no hay inyección posible.
	 */
	private static Predicate contiene(CriteriaBuilder cb, Expression<String> campo, String texto) {
		return cb.like(cb.lower(campo), "%" + texto.toLowerCase() + "%");
	}
}
