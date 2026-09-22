/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.samples.petclinic.model.BaseEntity;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.criteria.VisitCriteriaRepository;

import jakarta.persistence.LockModeType;

/**
 * Repository class for <code>Visit</code> domain objects All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data See here:
 * http://static.springsource.org/spring-data/jpa/docs/current/reference/html/jpa.repositories.html#jpa.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
public interface VisitRepository
		extends Repository<Visit, Integer>, JpaSpecificationExecutor<Visit>, VisitCriteriaRepository {

	// MODULO 6 - Esta interfaz es un buen resumen de las tres formas de consultar
	// que ofrece Spring Data, las tres disponibles a la vez por el mismo objeto:
	//
	//   1. Metodos DERIVADOS del nombre (findByPetId): Spring Data escribe la
	//      consulta. Gratis, pero solo llega hasta donde llega el nombre.
	//   2. JpaSpecificationExecutor: predicados componibles y reutilizables.
	//      Ver PetSpecification y VisitSpecification.
	//   3. VisitCriteriaRepository: fragmento implementado a mano cuando hacen
	//      falta agregaciones, subconsultas o proyecciones. Material adicional,
	//      ver modulos/06-spring-data-jpa/laboratorio-criteria-api.md


	/**
	 * Save a <code>Visit</code> to the data store, either inserting or updating it.
	 * @param visit the <code>Visit</code> to save
	 * @see BaseEntity#isNew
	 */
	void save(Visit visit) throws DataAccessException;

	List<Visit> findByPetId(Integer petId);
	
	Optional<Visit> findById(Integer id);

	List<Visit> findAll();

	// ------------------------------------------------------------------
	// LABORATORIO DE TRANSACCIONES AVANZADAS (modulo 6, material adicional)
	// modulos/06-spring-data-jpa/laboratorio-transacciones-avanzadas.md
	// ------------------------------------------------------------------

	/**
	 * Lectura con bloqueo pesimista: anade un SELECT ... FOR UPDATE, de modo que
	 * cualquier otra transaccion que intente leer la misma fila para escribirla
	 * espera hasta que esta termine.
	 *
	 * Exige transaccion activa: sin ella salta
	 * {@code TransactionRequiredException}, porque un bloqueo que se suelta
	 * inmediatamente no bloquea nada.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT visit FROM Visit visit WHERE visit.id = :id")
	Optional<Visit> findByIdBloqueando(@Param("id") Integer id);

	/**
	 * Actualizacion masiva en JPQL. Va DIRECTA a la base de datos:
	 *
	 *   - no pasa por el contexto de persistencia, asi que las entidades ya
	 *     cargadas se quedan obsoletas;
	 *   - no incrementa la columna `version`, asi que se salta el bloqueo
	 *     optimista por completo.
	 *
	 * Es rapidisima y por eso se usa, pero conviene saber a que se renuncia.
	 * Devuelve el numero de filas afectadas.
	 */
	@Modifying
	@Query("UPDATE Visit visit SET visit.description = :descripcion WHERE visit.id = :id")
	int actualizarDescripcionEnMasa(@Param("id") Integer id, @Param("descripcion") String descripcion);
}
