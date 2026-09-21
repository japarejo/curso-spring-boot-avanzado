// =============================================================================
// Modulo 2 - Ejercicio 2: de entidad a DTO.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================
package org.springframework.samples.petclinic.web.api.dto;

import java.time.LocalDate;

import org.springframework.samples.petclinic.model.Pet;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representacion de una mascota en la API.
 *
 * Por que un DTO y no la entidad Pet:
 *
 *  1. CONTRATO ESTABLE. Renombrar un campo de la entidad deja de romper a los
 *     clientes. La entidad sirve al modelo de datos; el DTO, a la API. Son dos
 *     cosas con motivos distintos para cambiar.
 *
 *  2. NO SE FILTRA LO QUE NO TOCA. La entidad Pet navega a Owner y este a User.
 *     Serializar la entidad completa puede acabar exponiendo datos de la cuenta.
 *     Es el segundo riesgo del OWASP API Security Top 10.
 *
 *  3. SIN SORPRESAS DE CARGA PEREZOSA. Serializar una entidad con relaciones
 *     LAZY fuera de la transaccion lanza LazyInitializationException. El DTO se
 *     construye DENTRO de la transaccion, con lo que haga falta y nada mas.
 *
 *  4. SIN ANOTACIONES DE JACKSON EN EL DOMINIO. Los @JsonIgnore repartidos por
 *     las entidades son sintoma de estar devolviendo entidades. Con DTO, el
 *     modelo de dominio no sabe que existe JSON.
 *
 * Es un `record`: inmutable y sin getters escritos a mano.
 */
@Schema(description = "Mascota tal y como la expone la API")
public record PetResponse(

        @Schema(example = "1") Integer id,

        @Schema(example = "Leo") String name,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(example = "2010-09-07") LocalDate birthDate,

        @Schema(description = "Nombre del tipo, no el objeto entero", example = "cat")
        String type) {

    /**
     * Factoria desde la entidad. Se llama DENTRO de la transaccion, de modo que
     * getType() puede resolver la relacion perezosa sin problema.
     */
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getBirthDate(),
                pet.getType() != null ? pet.getType().getName() : null);
    }
}

// ---------- Cambio en PetRestController ----------
/*
    @GetMapping("/{id}")
    public PetResponse findById(@PathVariable("id") int id) {
        Pet pet = petService.findPetById(id);
        if (pet == null) {
            throw new ResourceNotFoundException("No existe la mascota " + id);
        }
        return PetResponse.from(pet);
    }

    @GetMapping
    public List<PetResponse> findAll() {
        return petService.findAll().stream().map(PetResponse::from).toList();
    }
*/

// ---------- Cambio en la prueba ----------
/*
    mockMvc.perform(get("/api/v1/pets/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Leo"))
        .andExpect(jsonPath("$.type").value("cat"))
        // Lo importante es esto: lo que NO sale.
        .andExpect(jsonPath("$.owner").doesNotExist())
        .andExpect(jsonPath("$.visits").doesNotExist());
*/
