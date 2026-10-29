package org.springframework.samples.petclinic.repository.criteria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.samples.petclinic.model.BaseEntity_;
import org.springframework.samples.petclinic.model.Diagnose;
import org.springframework.samples.petclinic.model.Diagnose_;
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
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * ESCENARIO DE CRITERIA API: busqueda avanzada de pacientes (modulo 6).
 *
 * Cada criterio de la pantalla se traduce en UNA pieza de la API, y por eso el
 * escenario sirve para repasarla entera:
 *
 *   nombre                         like + lower
 *   tipos, ciudades                in
 *   nacidaDesde / nacidaHasta      greaterThanOrEqualTo / lessThanOrEqualTo
 *   apellidoPropietario            like 'texto%'
 *   minVisitas / maxVisitas        subconsulta correlacionada con COUNT
 *   visitadaDesde / visitadaHasta  EXISTS
 *   sinVisitasDesde                NOT EXISTS
 *   enfermedades, veterinario      EXISTS con joins dentro de la subconsulta
 *   numeroVisitas, ultimaVisita    LEFT JOIN + GROUP BY + COUNT / MAX
 *   ordenacion y paginacion        orderBy dinamico, setFirstResult, consulta de total
 *
 * Tres decisiones de diseno que conviene discutir en clase:
 *
 *  1. Los filtros de visitas van en SUBCONSULTAS, no en el join de visitas. El
 *     LEFT JOIN de visitas existe solo para contar y calcular la ultima visita.
 *     Si se filtrara sobre el, "visitada en enero" haria que numeroVisitas
 *     contara solo las de enero, y el LEFT JOIN pasaria a comportarse como INNER.
 *
 *  2. minVisitas/maxVisitas podrian ir en el HAVING de la consulta principal,
 *     pero entonces la consulta del TOTAL (que no agrupa) no sabria aplicarlos y
 *     el paginador mostraria un total incorrecto. Con una subconsulta, el mismo
 *     predicado vale para las dos consultas.
 *
 *  3. Los predicados NO se pueden reutilizar entre dos CriteriaQuery: estan
 *     atados a la raiz con la que se crearon. Por eso se construyen con un
 *     metodo (predicados(...)) que se llama una vez por consulta.
 */
public class BusquedaMascotasRepositoryImpl implements BusquedaMascotasRepository {

	@PersistenceContext
	private EntityManager em;

	@Override
	@Transactional(readOnly = true)
	public Page<FichaMascota> buscarMascotas(FiltroMascotas filtro, Pageable pagina) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<FichaMascota> consulta = cb.createQuery(FichaMascota.class);

		Root<Pet> mascota = consulta.from(Pet.class);
		Join<Pet, PetType> tipo = mascota.join(Pet_.type, JoinType.LEFT);
		Join<Pet, Owner> propietario = mascota.join(Pet_.owner);
		// Solo para las columnas calculadas. LEFT: las mascotas sin visitas
		// tambien salen, con numeroVisitas = 0 y ultimaVisita = null.
		Join<Pet, Visit> visita = mascota.join(Pet_.visits, JoinType.LEFT);

		Expression<Long> numeroVisitas = cb.count(visita);
		Expression<LocalDate> ultimaVisita = cb.greatest(visita.get(Visit_.date));

		consulta.select(cb.construct(FichaMascota.class,
				mascota.get(BaseEntity_.id),
				mascota.get(NamedEntity_.name),
				tipo.get(NamedEntity_.name),
				mascota.get(Pet_.birthDate),
				propietario.get(Person_.firstName),
				propietario.get(Person_.lastName),
				propietario.get(Owner_.city),
				numeroVisitas,
				ultimaVisita));

		consulta.where(predicados(cb, consulta, mascota, tipo, propietario, filtro));

		// Con agregados en el SELECT, todo lo que no es agregado va al GROUP BY.
		// MySQL (ONLY_FULL_GROUP_BY) rechaza la consulta si falta alguno.
		consulta.groupBy(mascota.get(BaseEntity_.id), mascota.get(NamedEntity_.name), tipo.get(NamedEntity_.name),
				mascota.get(Pet_.birthDate), propietario.get(Person_.firstName),
				propietario.get(Person_.lastName), propietario.get(Owner_.city));

		consulta.orderBy(orden(cb, pagina.getSort(), mascota, tipo, propietario, numeroVisitas, ultimaVisita));

		TypedQuery<FichaMascota> query = em.createQuery(consulta);
		if (pagina.isPaged()) {
			query.setFirstResult((int) pagina.getOffset());
			query.setMaxResults(pagina.getPageSize());
		}
		List<FichaMascota> contenido = query.getResultList();

		// Solo lanza la consulta del total si no se puede deducir: por ejemplo,
		// en la primera pagina con menos resultados que el tamano no hace falta.
		return PageableExecutionUtils.getPage(contenido, pagina, () -> contar(filtro));
	}

	/**
	 * Consulta del total para el paginador. Mismos predicados, otra raiz, y sin
	 * el join de visitas: contar mascotas no necesita agrupar.
	 */
	private long contar(FiltroMascotas filtro) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> consulta = cb.createQuery(Long.class);
		Root<Pet> mascota = consulta.from(Pet.class);
		Join<Pet, PetType> tipo = mascota.join(Pet_.type, JoinType.LEFT);
		Join<Pet, Owner> propietario = mascota.join(Pet_.owner);

		consulta.select(cb.count(mascota));
		consulta.where(predicados(cb, consulta, mascota, tipo, propietario, filtro));
		return em.createQuery(consulta).getSingleResult();
	}

	// ------------------------------------------------------------------
	// Un predicado por cada criterio activado en la pantalla
	// ------------------------------------------------------------------
	private Predicate[] predicados(CriteriaBuilder cb, AbstractQuery<?> consulta, Root<Pet> mascota,
			Join<Pet, PetType> tipo, Join<Pet, Owner> propietario, FiltroMascotas f) {
		List<Predicate> p = new ArrayList<>();

		// --- Mascota ---------------------------------------------------
		if (tieneTexto(f.nombre())) {
			p.add(cb.like(cb.lower(mascota.get(NamedEntity_.name)), "%" + f.nombre().trim().toLowerCase() + "%"));
		}
		if (tieneElementos(f.tipos())) {
			p.add(cb.lower(tipo.get(NamedEntity_.name)).in(f.tipos().stream().map(String::toLowerCase).toList()));
		}
		if (f.nacidaDesde() != null) {
			p.add(cb.greaterThanOrEqualTo(mascota.get(Pet_.birthDate), f.nacidaDesde()));
		}
		if (f.nacidaHasta() != null) {
			p.add(cb.lessThanOrEqualTo(mascota.get(Pet_.birthDate), f.nacidaHasta()));
		}

		// --- Propietario ----------------------------------------------
		if (tieneTexto(f.apellidoPropietario())) {
			p.add(cb.like(cb.lower(propietario.get(Person_.lastName)),
					f.apellidoPropietario().trim().toLowerCase() + "%"));
		}
		if (tieneElementos(f.ciudades())) {
			p.add(propietario.get(Owner_.city).in(f.ciudades()));
		}

		// --- Historial de visitas -------------------------------------
		if (f.minVisitas() != null) {
			p.add(cb.ge(numeroDeVisitas(cb, consulta, mascota), f.minVisitas()));
		}
		if (f.maxVisitas() != null) {
			p.add(cb.le(numeroDeVisitas(cb, consulta, mascota), f.maxVisitas()));
		}
		if (f.visitadaDesde() != null || f.visitadaHasta() != null) {
			p.add(cb.exists(visitasEntre(cb, consulta, mascota, f.visitadaDesde(), f.visitadaHasta())));
		}
		if (f.sinVisitasDesde() != null) {
			p.add(cb.not(cb.exists(visitasEntre(cb, consulta, mascota, f.sinVisitasDesde(), null))));
		}

		// --- Diagnosticos ---------------------------------------------
		if (tieneElementos(f.enfermedades()) || f.veterinario() != null) {
			p.add(cb.exists(diagnosticos(cb, consulta, mascota, f.enfermedades(), f.veterinario())));
		}

		return p.toArray(Predicate[]::new);
	}

	/** (select count(v) from Visit v where v.pet = mascota) */
	private Subquery<Long> numeroDeVisitas(CriteriaBuilder cb, AbstractQuery<?> consulta, Root<Pet> mascota) {
		Subquery<Long> sub = consulta.subquery(Long.class);
		Root<Visit> v = sub.from(Visit.class);
		// correlate(): "mascota" es la fila de la consulta EXTERNA. Sin esto la
		// subconsulta contaria las visitas de TODAS las mascotas.
		Root<Pet> esta = sub.correlate(mascota);
		sub.select(cb.count(v)).where(cb.equal(v.get(Visit_.pet), esta));
		return sub;
	}

	/** (select 1 from Visit v where v.pet = mascota and v.date between desde and hasta) */
	private Subquery<Integer> visitasEntre(CriteriaBuilder cb, AbstractQuery<?> consulta, Root<Pet> mascota,
			LocalDate desde, LocalDate hasta) {
		Subquery<Integer> sub = consulta.subquery(Integer.class);
		Root<Visit> v = sub.from(Visit.class);
		List<Predicate> condiciones = new ArrayList<>();
		condiciones.add(cb.equal(v.get(Visit_.pet), sub.correlate(mascota)));
		if (desde != null) {
			condiciones.add(cb.greaterThanOrEqualTo(v.get(Visit_.date), desde));
		}
		if (hasta != null) {
			condiciones.add(cb.lessThanOrEqualTo(v.get(Visit_.date), hasta));
		}
		return sub.select(cb.literal(1)).where(condiciones.toArray(Predicate[]::new));
	}

	/**
	 * (select 1 from Diagnose d join d.visit v where v.pet = mascota
	 *    and d.disease.id in (...) and d.vet.id = ...)
	 *
	 * Enfermedad y veterinario van en la MISMA subconsulta: "diagnosticada de
	 * diabetes por la Dra. Leary" exige que sea el mismo diagnostico. Con dos
	 * EXISTS separados valdria una diabetes de otro veterinario y cualquier otro
	 * diagnostico de Leary. Las dos lecturas son legitimas; esta es la que se ha
	 * elegido, y la pantalla lo dice.
	 */
	private Subquery<Integer> diagnosticos(CriteriaBuilder cb, AbstractQuery<?> consulta, Root<Pet> mascota,
			Collection<Integer> enfermedades, Integer veterinario) {
		Subquery<Integer> sub = consulta.subquery(Integer.class);
		Root<Diagnose> d = sub.from(Diagnose.class);
		Join<Diagnose, Visit> v = d.join(Diagnose_.visit);
		List<Predicate> condiciones = new ArrayList<>();
		condiciones.add(cb.equal(v.get(Visit_.pet), sub.correlate(mascota)));
		if (tieneElementos(enfermedades)) {
			condiciones.add(d.get(Diagnose_.disease).get(BaseEntity_.id).in(enfermedades));
		}
		if (veterinario != null) {
			condiciones.add(cb.equal(d.get(Diagnose_.vet).get(BaseEntity_.id), veterinario));
		}
		return sub.select(cb.literal(1)).where(condiciones.toArray(Predicate[]::new));
	}

	// ------------------------------------------------------------------
	// Ordenacion dinamica
	// ------------------------------------------------------------------
	private List<Order> orden(CriteriaBuilder cb, Sort sort, Root<Pet> mascota, Join<Pet, PetType> tipo,
			Join<Pet, Owner> propietario, Expression<Long> numeroVisitas, Expression<LocalDate> ultimaVisita) {
		List<Order> orden = new ArrayList<>();
		for (Sort.Order o : sort) {
			Expression<?> columna = switch (o.getProperty()) {
				case "nombre" -> mascota.get(NamedEntity_.name);
				case "tipo" -> tipo.get(NamedEntity_.name);
				case "fechaNacimiento" -> mascota.get(Pet_.birthDate);
				case "propietario" -> propietario.get(Person_.lastName);
				case "ciudad" -> propietario.get(Owner_.city);
				// Se puede ordenar por un agregado: el ORDER BY se evalua despues del GROUP BY.
				case "numeroVisitas" -> numeroVisitas;
				case "ultimaVisita" -> ultimaVisita;
				// Nunca se traduce el nombre recibido a un atributo con get(String):
				// el usuario podria ordenar por cualquier columna, incluidas las que no ve.
				default -> throw new IllegalArgumentException("No se puede ordenar por '" + o.getProperty()
						+ "'. Valores admitidos: " + CRITERIOS_DE_ORDEN);
			};
			orden.add(o.isAscending() ? cb.asc(columna) : cb.desc(columna));
		}
		// Desempate por id: sin un orden total, la misma mascota puede aparecer
		// en dos paginas (o en ninguna) cuando hay empates en la columna elegida.
		orden.add(cb.asc(mascota.get(BaseEntity_.id)));
		return orden;
	}

	// ------------------------------------------------------------------
	// Opciones para la pantalla
	// ------------------------------------------------------------------
	@Override
	@Transactional(readOnly = true)
	public List<String> ciudadesDePropietarios() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> consulta = cb.createQuery(String.class);
		Root<Owner> propietario = consulta.from(Owner.class);
		consulta.select(propietario.get(Owner_.city)).distinct(true).orderBy(cb.asc(propietario.get(Owner_.city)));
		return em.createQuery(consulta).getResultList();
	}

	private static boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}

	private static boolean tieneElementos(Collection<?> valores) {
		return valores != null && !valores.isEmpty();
	}

}
