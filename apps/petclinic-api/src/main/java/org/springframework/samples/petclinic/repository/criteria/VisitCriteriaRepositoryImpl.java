package org.springframework.samples.petclinic.repository.criteria;

import java.util.ArrayList;
import java.util.List;

import org.springframework.samples.petclinic.model.NamedEntity_;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Owner_;
import org.springframework.samples.petclinic.model.Person_;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Pet_;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.model.Visit_;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * LABORATORIO DE CRITERIA API (modulo 6, material adicional).
 *
 * Implementacion del fragmento. El nombre NO es libre: tiene que ser el de la
 * interfaz mas el sufijo {@code Impl} para que Spring Data lo componga.
 *
 * Las cuatro consultas estan ordenadas de menor a mayor dificultad y cada una
 * introduce una pieza nueva de la API:
 *
 *   1. buscar                      predicados dinamicos y joins
 *   2. resumirPorTipoDeMascota     agregacion y proyeccion con construct
 *   3. mascotasSinVisitas          subconsulta correlacionada
 *   4. propietariosConAlMenos      groupBy con having
 */
public class VisitCriteriaRepositoryImpl implements VisitCriteriaRepository {

	@PersistenceContext
	private EntityManager em;

	// ------------------------------------------------------------------
	// 1. Predicados dinamicos
	// ------------------------------------------------------------------
	@Override
	@Transactional(readOnly = true)
	public List<Visit> buscar(FiltroVisitas filtro) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Visit> consulta = cb.createQuery(Visit.class);
		Root<Visit> visita = consulta.from(Visit.class);

		// Los joins se declaran UNA vez y se reutilizan. Declararlos dentro de
		// cada `if` produciria varios joins a la misma tabla, y el SQL saldria
		// con un producto cartesiano.
		Join<Visit, Pet> mascota = visita.join(Visit_.pet, JoinType.INNER);
		Join<Pet, PetType> tipo = mascota.join(Pet_.type, JoinType.LEFT);
		Join<Pet, Owner> propietario = mascota.join(Pet_.owner, JoinType.LEFT);

		List<Predicate> predicados = new ArrayList<>();

		if (tieneTexto(filtro.descripcion())) {
			// lower() en los dos lados: sin esto el resultado depende de la
			// intercalacion de la base de datos, y H2 y MySQL no coinciden.
			predicados.add(cb.like(cb.lower(visita.get(Visit_.description)),
					"%" + filtro.descripcion().toLowerCase() + "%"));
		}
		if (tieneTexto(filtro.nombreMascota())) {
			// `name` se declara en NamedEntity, no en Pet. El metamodelo lo
			// refleja: Pet_ hereda de NamedEntity_, asi que Pet_.name resuelve.
			predicados.add(cb.equal(cb.lower(mascota.get(NamedEntity_.name)),
					filtro.nombreMascota().toLowerCase()));
		}
		if (tieneTexto(filtro.tipoMascota())) {
			predicados.add(cb.equal(cb.lower(tipo.get(NamedEntity_.name)),
					filtro.tipoMascota().toLowerCase()));
		}
		if (tieneTexto(filtro.ciudadPropietario())) {
			predicados.add(cb.equal(cb.lower(propietario.get(Owner_.city)),
					filtro.ciudadPropietario().toLowerCase()));
		}
		if (filtro.desde() != null) {
			predicados.add(cb.greaterThanOrEqualTo(visita.get(Visit_.date), filtro.desde()));
		}
		if (filtro.hasta() != null) {
			predicados.add(cb.lessThanOrEqualTo(visita.get(Visit_.date), filtro.hasta()));
		}

		if (filtro.minimoVisitasDelPropietario() != null) {
			predicados.add(propietarioConAlMenos(cb, consulta, propietario,
					filtro.minimoVisitasDelPropietario()));
		}

		// and(...) sobre un array vacio devuelve una conjuncion siempre cierta,
		// asi que el filtro vacio devuelve todas las visitas sin casos especiales.
		consulta.where(cb.and(predicados.toArray(Predicate[]::new)));
		consulta.orderBy(cb.desc(visita.get(Visit_.date)));

		return em.createQuery(consulta).getResultList();
	}

	// ------------------------------------------------------------------
	// 2. Agregacion con proyeccion
	// ------------------------------------------------------------------
	@Override
	@Transactional(readOnly = true)
	public List<ResumenPorTipo> resumirPorTipoDeMascota() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ResumenPorTipo> consulta = cb.createQuery(ResumenPorTipo.class);
		Root<Visit> visita = consulta.from(Visit.class);
		Join<Visit, Pet> mascota = visita.join(Visit_.pet);
		Join<Pet, PetType> tipo = mascota.join(Pet_.type);

		// construct() hace que la consulta devuelva directamente el record.
		// La alternativa, multiselect() con Tuple, obliga a ir sacando los
		// valores por indice, que es fragil.
		consulta.select(cb.construct(ResumenPorTipo.class,
				tipo.get(NamedEntity_.name),
				cb.count(visita),
				cb.countDistinct(mascota)));

		consulta.groupBy(tipo.get(NamedEntity_.name));
		consulta.orderBy(cb.desc(cb.count(visita)));

		return em.createQuery(consulta).getResultList();
	}

	// ------------------------------------------------------------------
	// 3. Subconsulta correlacionada
	// ------------------------------------------------------------------
	@Override
	@Transactional(readOnly = true)
	public List<Pet> mascotasSinVisitas() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Pet> consulta = cb.createQuery(Pet.class);
		Root<Pet> mascota = consulta.from(Pet.class);

		Subquery<Integer> subconsulta = consulta.subquery(Integer.class);
		Root<Visit> visita = subconsulta.from(Visit.class);
		subconsulta.select(cb.literal(1));

		// correlate() es el paso que se olvida siempre. Marca que `mascota` es
		// la fila de la consulta EXTERNA, no una tabla nueva. Sin el, Hibernate
		// mete otro PET en el FROM de la subconsulta, el EXISTS se cumple en
		// cuanto haya una sola visita en toda la base de datos, y el metodo
		// devuelve la lista vacia. Es un fallo silencioso: no hay excepcion.
		Root<Pet> mascotaCorrelada = subconsulta.correlate(mascota);
		subconsulta.where(cb.equal(visita.get(Visit_.pet), mascotaCorrelada));

		consulta.where(cb.not(cb.exists(subconsulta)));
		consulta.orderBy(cb.asc(mascota.get(NamedEntity_.name)));

		return em.createQuery(consulta).getResultList();
	}

	// ------------------------------------------------------------------
	// 4. Agrupacion con HAVING
	// ------------------------------------------------------------------
	@Override
	@Transactional(readOnly = true)
	public List<String> propietariosConAlMenos(int minimo) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> consulta = cb.createQuery(String.class);
		Root<Visit> visita = consulta.from(Visit.class);
		Join<Visit, Pet> mascota = visita.join(Visit_.pet);
		Join<Pet, Owner> propietario = mascota.join(Pet_.owner);

		consulta.select(propietario.get(Person_.lastName));
		consulta.groupBy(propietario.get(Person_.lastName));
		// having filtra DESPUES de agrupar; where filtra ANTES. Poner esta
		// condicion en el where daria un error de sintaxis SQL, porque no se
		// puede usar una funcion de agregacion alli.
		consulta.having(cb.ge(cb.count(visita), (long) minimo));
		consulta.orderBy(cb.asc(propietario.get(Person_.lastName)));

		return em.createQuery(consulta).getResultList();
	}

	// ------------------------------------------------------------------
	// Auxiliares
	// ------------------------------------------------------------------

	/**
	 * Subconsulta reutilizada por el filtro: propietarios cuyas mascotas
	 * acumulan al menos N visitas.
	 */
	private Predicate propietarioConAlMenos(CriteriaBuilder cb, CriteriaQuery<?> consulta,
			Join<Pet, Owner> propietario, int minimo) {
		Subquery<Long> subconsulta = consulta.subquery(Long.class);
		Root<Visit> otraVisita = subconsulta.from(Visit.class);
		Join<Visit, Pet> otraMascota = otraVisita.join(Visit_.pet);
		subconsulta.select(cb.count(otraVisita));
		// Igual que en mascotasSinVisitas: hay que correlacionar el join externo.
		Join<Pet, Owner> propietarioCorrelado = subconsulta.correlate(propietario);
		subconsulta.where(cb.equal(otraMascota.get(Pet_.owner), propietarioCorrelado));
		return cb.ge(subconsulta, minimo);
	}

	private boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}
}
