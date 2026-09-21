// =============================================================================
// Modulo 5 - Ejercicio 3: configuracion tipada y validada.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================
package org.springframework.samples.petclinic.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Agrupa las propiedades `petclinic.*` en un objeto inmutable y validado.
 *
 * Se descubre solo gracias al @ConfigurationPropertiesScan de
 * PetclinicApplication; no hace falta @EnableConfigurationProperties.
 *
 * Frente a repartir @Value("${...}") por el codigo:
 *
 *   1. FALLA AL ARRANCAR si falta una propiedad obligatoria o si un valor
 *      incumple una restriccion, y el mensaje dice exactamente cual. Con
 *      @Value el fallo aparece mas tarde, normalmente en produccion.
 *   2. AUTOCOMPLETADO en el IDE, porque el tipo se conoce.
 *   3. CONVERSION AUTOMATICA: "30m" se convierte en Duration sin escribir nada.
 *   4. AGRUPACION: quien lee la clase ve de un vistazo toda la configuracion
 *      del area, en lugar de tener que buscar @Value por todo el proyecto.
 */
@Validated
@ConfigurationProperties(prefix = "petclinic")
public record PetclinicProperties(

        @NotBlank(message = "petclinic.nombre-clinica es obligatoria")
        String nombreClinica,

        @Min(value = 1, message = "debe permitirse al menos una mascota")
        @Max(value = 10, message = "mas de 10 mascotas por propietario no esta soportado")
        @DefaultValue("5")
        int maximoMascotasPorPropietario,

        @DefaultValue("30m")
        Duration tiempoEsperaCita) {
}

// ---------- application.properties ----------
/*
petclinic.nombre-clinica=Clinica Veterinaria del Curso
petclinic.maximo-mascotas-por-propietario=5
petclinic.tiempo-espera-cita=30m
*/

// ---------- Uso ----------
/*
@Service
public class PetService {

    private final PetclinicProperties propiedades;
    private final PetRepository petRepository;

    public PetService(PetclinicProperties propiedades, PetRepository petRepository) {
        this.propiedades = propiedades;
        this.petRepository = petRepository;
    }

    public void savePet(Pet pet) throws DuplicatedPetNameException {
        Owner propietario = pet.getOwner();
        if (propietario.getPets().size() >= propiedades.maximoMascotasPorPropietario()) {
            throw new BadRequestException(
                "Un propietario no puede tener mas de "
                + propiedades.maximoMascotasPorPropietario() + " mascotas");
        }
        petRepository.save(pet);
    }
}
*/

// ---------- Comprobaciones ----------
/*
  # 1. Quitar petclinic.nombre-clinica y arrancar:
  #    la aplicacion NO arranca y el error nombra la propiedad.
  #
  # 2. Poner maximo-mascotas-por-propietario=20 y arrancar:
  #    tampoco arranca, y el mensaje es el de la anotacion @Max.
  #
  # 3. Ver los valores efectivos en ejecucion:
  curl -s localhost:8080/actuator/configprops \
    | jq '.contexts.application.beans | to_entries
          | map(select(.key | test("petclinic";"i")))'
*/
