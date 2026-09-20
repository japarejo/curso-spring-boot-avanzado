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

import java.util.Collection;

import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.samples.petclinic.model.BaseEntity;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.repository.OwnerRepository;

/**
 * Spring Data JPA OwnerRepository interface
 *
 * @author Michael Isvy
 * @since 15.1.2013
 */
public interface OwnerRepository extends Repository<Owner, Integer> {

	/**
	 * Save an <code>Owner</code> to the data store, either inserting or updating it.
	 * @param owner the <code>Owner</code> to save
	 * @see BaseEntity#isNew
	 */
	void save(Owner owner) throws DataAccessException;

	/**
	 * Retrieve <code>Owner</code>s from the data store by last name, returning all owners
	 * whose last name <i>starts</i> with the given name.
	 * @param lastName Value to search for
	 * @return a <code>Collection</code> of matching <code>Owner</code>s (or an empty
	 * <code>Collection</code> if none found)
	 *
	 * MODULO 6 - Punto de partida del ejercicio de consultas N+1.
	 *
	 * El listado de propietarios muestra las mascotas de cada uno. Sin este
	 * @EntityGraph, Hibernate ejecuta 1 consulta para los propietarios y N mas,
	 * una por propietario, al recorrer owner.pets: el problema N+1.
	 *
	 * Ejercicio (ver modulos/06-spring-data-jpa):
	 *   1. Comenta la linea @EntityGraph y arranca con el perfil `nplus1`,
	 *      que activa open-in-view y el registro de SQL. Cuenta las consultas
	 *      que provoca GET /owners.
	 *   2. Arranca sin ese perfil: ahora falla con LazyInitializationException.
	 *      Es el mismo defecto, que open-in-view se limitaba a ocultar.
	 *   3. Vuelve a activar el @EntityGraph: una sola consulta con LEFT JOIN.
	 */
	@EntityGraph(attributePaths = "pets")
	@Query("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName%")
	public Collection<Owner> findByLastName(@Param("lastName") String lastName);


	/**
	 * Retrieve an <code>Owner</code> from the data store by id.
	 * @param id the id to search for
	 * @return the <code>Owner</code> if found
	 * @throws org.springframework.dao.DataRetrievalFailureException if not found
	 */
	/**
	 * MODULO 6 - Grafo de carga.
	 *
	 * La vista de detalle de un propietario SIEMPRE muestra sus mascotas, asi que
	 * traerlas en la misma consulta no es una optimizacion prematura: es lo correcto.
	 *
	 * Sin este @EntityGraph y con spring.jpa.open-in-view=false (nuestro caso),
	 * renderizar la vista lanza LazyInitializationException, porque la sesion de
	 * Hibernate ya se ha cerrado cuando la JSP recorre owner.pets. Con OSIV activado
	 * el fallo no aparece, pero a cambio se ejecuta una consulta extra por mascota
	 * fuera de la transaccion: el problema seguia ahi, solo que invisible.
	 *
	 * Ojo: esto NO es lo mismo que poner FetchType.EAGER en la entidad. El grafo se
	 * aplica solo a esta consulta; el resto siguen cargando perezosamente.
	 */
	@EntityGraph(attributePaths = "pets")
	public Owner findById(@Param("id") int id);

	@Query("SELECT owner FROM Owner owner WHERE owner.user.username =:username")
	public Owner findByUserName(@Param("username") String username);

	/**
	 * MODULO 6 - Fetch join explicito.
	 *
	 * Misma finalidad que el @EntityGraph de findById, escrita a mano en JPQL.
	 * Tener las dos versiones permite compararlas en clase:
	 *   - @EntityGraph es declarativo y se lee mejor;
	 *   - LEFT JOIN FETCH da control total sobre el tipo de join.
	 *
	 * El DISTINCT evita que el propietario aparezca repetido una vez por mascota:
	 * el join multiplica las filas, y sin el la coleccion llegaria duplicada.
	 *
	 * Lo usa la vista de detalle del propietario, que recorre owner.pets DESPUES
	 * de que la transaccion haya terminado. Sin esta consulta, la peticion muere
	 * con LazyInitializationException (spring.jpa.open-in-view esta a false).
	 */
	@Query("SELECT DISTINCT owner FROM Owner owner LEFT JOIN FETCH owner.pets WHERE owner.id = :id")
	public Owner findByIdWithPets(@Param("id") int id);

}
