// =============================================================================
// Modulo 6 - Ejercicio 3: proyecciones de Spring Data.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
//
// Para probarla: pegar las interfaces y los metodos en OwnerRepository, activar
// el registro de SQL y comparar la consulta que sale en cada caso.
//
//     spring.jpa.show-sql=true
//     spring.jpa.properties.hibernate.format_sql=true
// =============================================================================
package org.springframework.samples.petclinic.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Query;
import org.springframework.samples.petclinic.model.Owner;

/**
 * Las CUATRO formas de proyectar que ofrece Spring Data, de menos a mas
 * flexible, con el SQL que genera cada una.
 *
 * Todas responden a la misma pregunta: "necesito tres columnas, no las doce de
 * la entidad, y no quiero cargar las mascotas".
 */
public interface Ejercicio03Proyecciones {

	// -------------------------------------------------------------------------
	// 1. PROYECCION CERRADA con interfaz
	// -------------------------------------------------------------------------
	//
	// Solo getters. Spring Data crea un proxy al vuelo y, lo importante, deduce
	// de los getters que la consulta debe traer SOLO esas columnas:
	//
	//     select o1_0.id, o1_0.first_name, o1_0.last_name from owners o1_0
	//         where o1_0.city = ?
	//
	// Es la opcion por defecto y casi siempre la correcta: menos datos por el
	// cable, menos memoria y ni una relacion perezosa que pueda estallar fuera
	// de la transaccion.
	interface ResumenOwner {

		Integer getId();

		String getFirstName();

		String getLastName();

	}

	List<ResumenOwner> findByCity(String city);

	// -------------------------------------------------------------------------
	// 2. PROYECCION ABIERTA con @Value
	// -------------------------------------------------------------------------
	//
	// En cuanto un getter lleva una expresion SpEL, la proyeccion pasa a ser
	// ABIERTA, y eso tiene una consecuencia que conviene saber: Spring Data ya
	// no puede deducir que columnas hacen falta, asi que carga la ENTIDAD
	// COMPLETA y evalua la expresion despues, en memoria.
	//
	// El SQL vuelve a ser el de siempre, con sus doce columnas. Se gana
	// comodidad en la respuesta y se pierde toda la optimizacion. Merece la pena
	// medirlo en clase, porque es un coste invisible.
	interface FichaOwner {

		@Value("#{target.firstName + ' ' + target.lastName}")
		String getNombreCompleto();

		String getCity();

		// Esto tambien se puede hacer, y aqui ya se esta pagando el precio
		// completo: cargar la coleccion de mascotas para contarla.
		@Value("#{target.pets.size()}")
		int getNumeroDeMascotas();

	}

	List<FichaOwner> findByLastName(String lastName);

	// -------------------------------------------------------------------------
	// 3. PROYECCION CON RECORD (DTO)
	// -------------------------------------------------------------------------
	//
	// Un record con un constructor que encaja con los nombres de las
	// propiedades. Genera el mismo SQL ajustado que la proyeccion cerrada, pero
	// devuelve un objeto de verdad en lugar de un proxy: se puede serializar, se
	// puede comparar por valor y se puede pasar fuera de la capa de datos sin
	// arrastrar nada de Spring Data.
	//
	// Es lo que conviene cuando el resultado va a salir del repositorio hacia
	// arriba. La diferencia con la proyeccion cerrada es practica, no de
	// rendimiento.
	record ResumenOwnerDto(Integer id, String firstName, String lastName) {
	}

	List<ResumenOwnerDto> findByTelephone(String telephone);

	// -------------------------------------------------------------------------
	// 4. PROYECCION DINAMICA
	// -------------------------------------------------------------------------
	//
	// El tipo se decide en la llamada. UN metodo de repositorio sirve para la
	// entidad completa, para el resumen y para la ficha:
	//
	//     ownerRepository.findByAddress(dir, Owner.class);
	//     ownerRepository.findByAddress(dir, ResumenOwner.class);
	//     ownerRepository.findByAddress(dir, ResumenOwnerDto.class);
	//
	// Evita la coleccion de findByXConResumen, findByXConFicha, findByXCompleto
	// que acaba apareciendo en cualquier repositorio que lleve tiempo vivo.
	<T> List<T> findByAddress(String address, Class<T> tipo);

	// -------------------------------------------------------------------------
	// 5. Y CON @Query, para cuando la proyeccion no cabe en un nombre
	// -------------------------------------------------------------------------
	//
	// Dos formas. Con `new`, que necesita el nombre completo del paquete y no lo
	// comprueba nadie hasta que se ejecuta:
	@Query("""
			SELECT new org.springframework.samples.petclinic.repository.Ejercicio03Proyecciones$ResumenOwnerDto(
			           owner.id, owner.firstName, owner.lastName)
			FROM Owner owner WHERE owner.city = :ciudad
			""")
	List<ResumenOwnerDto> resumenPorCiudad(String ciudad);

	// O seleccionando los campos con alias, que Spring Data enlaza con la
	// interfaz por nombre. Se lee mejor y no lleva rutas de paquete dentro de
	// una cadena:
	@Query("""
			SELECT owner.id AS id, owner.firstName AS firstName, owner.lastName AS lastName
			FROM Owner owner WHERE owner.city = :ciudad
			""")
	List<ResumenOwner> resumenPorCiudadConAlias(String ciudad);

	// -------------------------------------------------------------------------
	// PROYECCION FRENTE A DTO DEL MODULO 2
	// -------------------------------------------------------------------------
	//
	// Es la pregunta del ejercicio y la respuesta corta es que no compiten:
	//
	//   - La PROYECCION condiciona la CONSULTA. Decide que columnas viajan de la
	//     base de datos a la aplicacion. Su motivo es el rendimiento.
	//   - El DTO condiciona la RESPUESTA. Decide que campos viajan de la
	//     aplicacion al cliente. Su motivo es el contrato y no exponer datos de
	//     mas (modulo 2, "exposicion excesiva de datos" del OWASP API Top 10).
	//
	// Lo habitual en un proyecto serio es usar los dos: proyectar lo que se
	// necesita y mapearlo a un DTO estable. Y hay un caso en que el DTO se puede
	// usar directamente como proyeccion, que es el punto 3 de arriba: cuando el
	// record ya tiene exactamente los campos que hacen falta.
	//
	// -------------------------------------------------------------------------
	// LO QUE NO FUNCIONA, Y CONVIENE PROBARLO
	// -------------------------------------------------------------------------
	//
	// 1. Una proyeccion cerrada con un getter que no existe en la entidad falla
	//    al ARRANCAR, no al ejecutar. Es lo correcto: renombrar un campo de la
	//    entidad rompe el arranque en lugar de romper una peticion.
	//
	// 2. Las proyecciones anidadas funcionan (getPets() devolviendo otra
	//    proyeccion), pero generan un join y ya no son gratis. Medirlo antes de
	//    darlo por bueno.
	//
	// 3. Una proyeccion NO es una entidad gestionada: no tiene contexto de
	//    persistencia, no se puede modificar y no se puede guardar. Si hace
	//    falta escribir, hay que cargar la entidad.

	Collection<Owner> findAll();

}
