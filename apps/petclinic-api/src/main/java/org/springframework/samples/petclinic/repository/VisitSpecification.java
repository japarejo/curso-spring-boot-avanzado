package org.springframework.samples.petclinic.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.samples.petclinic.model.Visit;

/**
 * JPA Specification for building dynamic queries to search visits.
 * Provides static factory methods for creating Specifications with various filter criteria.
 */
public class VisitSpecification {

	private VisitSpecification() {
		// Utility class
	}

	/**
	 * Filter visits by pet ID.
	 */
	public static Specification<Visit> byPetId(Integer petId) {
		return (root, query, cb) -> petId == null ? null : cb.equal(root.get("pet").get("id"), petId);
	}

	/**
	 * Filter visits by owner's last name (case-insensitive, partial match).
	 */
	public static Specification<Visit> byOwnerLastName(String ownerLastName) {
		return (root, query, cb) -> ownerLastName == null ? null
				: cb.like(cb.lower(root.get("pet").get("owner").get("lastName")),
						"%" + ownerLastName.toLowerCase() + "%");
	}

	/**
	 * Filter visits by pet name (case-insensitive, partial match).
	 */
	public static Specification<Visit> byPetName(String petName) {
		return (root, query, cb) -> petName == null ? null
				: cb.like(cb.lower(root.get("pet").get("name")), "%" + petName.toLowerCase() + "%");
	}

	/**
	 * Filter visits by pet type name (case-insensitive, exact match).
	 */
	public static Specification<Visit> byPetType(String petType) {
		return (root, query, cb) -> petType == null ? null
				: cb.equal(cb.lower(root.get("pet").get("type").get("name")), petType.toLowerCase());
	}

	/**
	 * Filter visits by description (case-insensitive, partial match).
	 */
	public static Specification<Visit> byDescription(String description) {
		return (root, query, cb) -> description == null ? null
				: cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
	}

	/**
	 * Filter visits on or after the given date.
	 */
	public static Specification<Visit> fromDate(LocalDate fromDate) {
		return (root, query, cb) -> fromDate == null ? null : cb.greaterThanOrEqualTo(root.get("date"), fromDate);
	}

	/**
	 * Filter visits on or before the given date.
	 */
	public static Specification<Visit> toDate(LocalDate toDate) {
		return (root, query, cb) -> toDate == null ? null : cb.lessThanOrEqualTo(root.get("date"), toDate);
	}

	/**
	 * Combine multiple specifications with AND logic.
	 */
	public static Specification<Visit> filterVisits(Integer petId, String ownerLastName, String petName,
			String petType, String description, LocalDate fromDate, LocalDate toDate) {
		return Specification.where(byPetId(petId))
				.and(byOwnerLastName(ownerLastName))
				.and(byPetName(petName))
				.and(byPetType(petType))
				.and(byDescription(description))
				.and(fromDate(fromDate))
				.and(toDate(toDate));
	}
}
