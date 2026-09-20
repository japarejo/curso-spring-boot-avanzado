package org.springframework.samples.petclinic.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;

/**
 * JPA Criteria API specifications for Pet queries.
 *
 * This class deliberately exposes two variants of the same visit-description filter
 * to illustrate how LEFT JOIN behaves differently depending on whether the WHERE
 * predicate includes an OR-null guard:
 *
 *   withVisitDescriptionLeftJoin  → true LEFT JOIN: all pets are returned.
 *   withVisitDescriptionNoOrGuard → effective INNER JOIN: only pets with a
 *                                   matching visit survive the WHERE clause.
 */
public class PetSpecification {

    private PetSpecification() {}

    public static Specification<Pet> byPetName(String name) {
        return (root, query, cb) -> name == null ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Pet> byOwnerLastName(String lastName) {
        return (root, query, cb) -> lastName == null ? null
                : cb.like(cb.lower(root.get("owner").get("lastName")),
                        "%" + lastName.toLowerCase() + "%");
    }

    /**
     * TRUE LEFT JOIN — pets without any matching visit are preserved.
     *
     * The generated SQL looks roughly like:
     *
     *   SELECT DISTINCT p.*
     *   FROM pets p
     *   LEFT JOIN visits v ON v.pet_id = p.id
     *   WHERE (v.description LIKE '%keyword%' OR v.id IS NULL)
     *
     * The OR v.id IS NULL guard keeps rows where the LEFT JOIN produced no
     * matching visit (all visit columns are NULL).  Without this guard those
     * rows would be eliminated by the WHERE clause, turning the LEFT JOIN
     * into an effective INNER JOIN (see {@link #withVisitDescriptionNoOrGuard}).
     */
    public static Specification<Pet> withVisitDescriptionLeftJoin(String description) {
        return (root, query, cb) -> {
            if (description == null) return null;
            if (query != null) query.distinct(true);
            Join<Pet, Visit> visitJoin = root.join("visits", JoinType.LEFT);
            return cb.or(
                cb.isNull(visitJoin.get("id")),
                cb.like(cb.lower(visitJoin.get("description")),
                        "%" + description.toLowerCase() + "%")
            );
        };
    }

    /**
     * EFFECTIVE INNER JOIN — identical LEFT JOIN declaration but WITHOUT the
     * OR-null guard in the WHERE predicate.
     *
     * The generated SQL looks roughly like:
     *
     *   SELECT DISTINCT p.*
     *   FROM pets p
     *   LEFT JOIN visits v ON v.pet_id = p.id
     *   WHERE v.description LIKE '%keyword%'
     *
     * When v.description is NULL (pet has no matching visit) the LIKE predicate
     * evaluates to FALSE/UNKNOWN, so those rows are discarded.  The result set
     * contains only pets that have at least one visit matching the keyword —
     * exactly the same outcome as an INNER JOIN would produce.
     */
    public static Specification<Pet> withVisitDescriptionNoOrGuard(String description) {
        return (root, query, cb) -> {
            if (description == null) return null;
            if (query != null) query.distinct(true);
            Join<Pet, Visit> visitJoin = root.join("visits", JoinType.LEFT);
            // No OR guard: NULL visit rows are filtered out → effective INNER JOIN
            return cb.like(cb.lower(visitJoin.get("description")),
                    "%" + description.toLowerCase() + "%");
        };
    }

    public static Specification<Pet> filterPets(String petName, String ownerLastName,
            String visitDescription, boolean trueLeftJoin) {
        Specification<Pet> spec = Specification
                .where(byPetName(petName))
                .and(byOwnerLastName(ownerLastName));

        if (visitDescription != null) {
            spec = spec.and(trueLeftJoin
                    ? withVisitDescriptionLeftJoin(visitDescription)
                    : withVisitDescriptionNoOrGuard(visitDescription));
        }
        return spec;
    }
}
