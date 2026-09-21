// =============================================================================
// Modulo 1 - Ejercicio 4: anadir un bean y verlo en el contexto.
//
// Este fichero es SOLUCION DE REFERENCIA, no forma parte de la compilacion.
// Para probarlo, copia las dos clases a sus paquetes:
//   apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/service/
//   apps/petclinic-api/src/main/java/org/springframework/samples/petclinic/web/api/
// =============================================================================

// ---------- service/EstadisticasService.java ----------
package org.springframework.samples.petclinic.service;

import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstadisticasService {

    // Inyeccion por CONSTRUCTOR, que es la cuestion del modulo:
    //   - el campo puede ser final, asi que la clase es inmutable;
    //   - la clase declara explicitamente lo que necesita para funcionar;
    //   - se puede instanciar en una prueba sin Spring ni reflexion.
    private final PetRepository petRepository;

    // Desde Spring 4.3 no hace falta @Autowired si hay un unico constructor.
    public EstadisticasService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public long numeroDeMascotas() {
        return petRepository.count();
    }
}

// ---------- web/api/EstadisticasRestController.java ----------
/*
package org.springframework.samples.petclinic.web.api;

import java.util.Map;

import org.springframework.samples.petclinic.service.EstadisticasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estadisticas")
public class EstadisticasRestController {

    private final EstadisticasService estadisticas;

    public EstadisticasRestController(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping
    public Map<String, Long> resumen() {
        return Map.of("mascotas", estadisticas.numeroDeMascotas());
    }
}
*/

// Comprobacion:
//   curl -s localhost:8080/api/v1/estadisticas | jq
//   curl -s localhost:8080/actuator/beans \
//     | jq '.contexts.application.beans | keys | map(select(test("estadisticas";"i")))'
//
// Nota: PetRepository extiende Repository, que NO trae count(). Hay dos salidas:
//   a) declarar `long count();` en PetRepository, o
//   b) extender CrudRepository.
// Merece la pena comentar en clase por que el proyecto usa Repository y no
// CrudRepository: para exponer solo los metodos que de verdad se usan.
