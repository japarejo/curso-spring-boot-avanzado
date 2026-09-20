package org.springframework.samples.petclinic.web.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag(name = "LEFT JOIN demo", description = "Ilustra el efecto del OR en un LEFT JOIN con la Criteria API")
@RestController
@RequestMapping("/api/v2/pets")
public class PetVisitSearchController {

    private final PetService petService;

    public PetVisitSearchController(PetService petService) {
        this.petService = petService;
    }

    @Operation(
        summary = "Buscar mascotas demostrando LEFT JOIN vs INNER JOIN implícito",
        description = """
            Demuestra la diferencia entre un LEFT JOIN "verdadero" y un LEFT JOIN \
            que se convierte en un INNER JOIN implícito al omitir el OR en la condición WHERE.

            ## Modo `joinMode=left` — LEFT JOIN verdadero

            La Specification genera (aproximadamente):

            ```sql
            SELECT DISTINCT p.*
            FROM pets p
            LEFT JOIN visits v ON v.pet_id = p.id
            WHERE (v.description LIKE '%keyword%' OR v.id IS NULL)
            ```

            El predicado `OR v.id IS NULL` conserva las filas donde el LEFT JOIN \
            no encontró ninguna visita coincidente (todas las columnas de `v` son NULL). \
            **Resultado: aparecen TODAS las mascotas**, incluidas las que no tienen \
            ninguna visita que contenga el término buscado.

            ## Modo `joinMode=inner` — INNER JOIN implícito (sin OR)

            La Specification genera (aproximadamente):

            ```sql
            SELECT DISTINCT p.*
            FROM pets p
            LEFT JOIN visits v ON v.pet_id = p.id
            WHERE v.description LIKE '%keyword%'
            ```

            Al eliminar el `OR v.id IS NULL`, las filas con `v.description = NULL` \
            son descartadas por la cláusula WHERE. **Resultado: solo aparecen las \
            mascotas que tienen al menos una visita que contiene el término buscado**, \
            exactamente igual que si hubiésemos declarado un INNER JOIN.

            ## Cómo probarlo

            1. Llama con `visitDescription=rabies&joinMode=left` → observa cuántas mascotas devuelve.
            2. Llama con `visitDescription=rabies&joinMode=inner` → observa cómo el número se reduce.
            3. El campo `hasMatchingVisit` indica si la mascota tiene alguna visita que coincide.
            4. Las mascotas con `hasMatchingVisit=false` **solo aparecen en el modo `left`**.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultado de la búsqueda",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PetSearchResponse.class))),
        @ApiResponse(responseCode = "400", description = "Parámetro joinMode inválido",
            content = @Content)
    })
    @GetMapping
    public PetSearchResponse searchPets(
            @Parameter(description = "Coincidencia parcial (case-insensitive) en el nombre de la mascota.",
                example = "Leo")
            @RequestParam(required = false) String petName,

            @Parameter(description = "Coincidencia parcial (case-insensitive) en el apellido del dueño.",
                example = "Franklin")
            @RequestParam(required = false) String ownerLastName,

            @Parameter(description = "Palabra clave a buscar en las descripciones de las visitas. "
                + "Es el parámetro que activa el JOIN con la tabla visits.",
                example = "rabies")
            @RequestParam(required = false) String visitDescription,

            @Parameter(description = """
                Modo de join:
                - `left` (por defecto): LEFT JOIN con OR-null guard. Devuelve TODAS las mascotas.
                - `inner`: LEFT JOIN SIN OR guard → INNER JOIN implícito. Solo mascotas con visita coincidente.
                """,
                example = "left")
            @RequestParam(defaultValue = "left") String joinMode) {

        boolean trueLeftJoin = switch (joinMode.toLowerCase()) {
            case "left"  -> true;
            case "inner" -> false;
            default -> throw new BadRequestException(
                "joinMode debe ser 'left' o 'inner', valor recibido: " + joinMode);
        };

        List<Pet> pets = petService.searchPetsWithVisits(
                blankToNull(petName),
                blankToNull(ownerLastName),
                blankToNull(visitDescription),
                trueLeftJoin);

        List<PetSummary> summaries = pets.stream()
                .map(p -> toSummary(p, visitDescription))
                .toList();

        String sqlExplanation = buildSqlExplanation(visitDescription, trueLeftJoin);

        return new PetSearchResponse(joinMode.toLowerCase(), visitDescription,
                sqlExplanation, summaries.size(), summaries);
    }

    private PetSummary toSummary(Pet pet, String visitDescription) {
        Owner owner = pet.getOwner();
        boolean hasMatch = visitDescription != null && pet.getVisits().stream()
                .anyMatch(v -> v.getDescription() != null
                        && v.getDescription().toLowerCase()
                                .contains(visitDescription.toLowerCase()));

        List<VisitSummary> visits = pet.getVisits().stream()
                .map(this::toVisitSummary)
                .toList();

        return new PetSummary(
                pet.getId(),
                pet.getName(),
                pet.getType() != null ? pet.getType().getName() : null,
                owner != null ? owner.getFullName() : null,
                hasMatch,
                visits);
    }

    private VisitSummary toVisitSummary(Visit v) {
        return new VisitSummary(v.getId(), v.getDate(), v.getDescription());
    }

    private String buildSqlExplanation(String visitDescription, boolean trueLeftJoin) {
        String keyword = visitDescription != null ? visitDescription : "<ninguno>";
        if (trueLeftJoin) {
            return "SELECT DISTINCT p.* FROM pets p "
                + "LEFT JOIN visits v ON v.pet_id = p.id "
                + "WHERE (LOWER(v.description) LIKE '%" + keyword.toLowerCase() + "%' OR v.id IS NULL)"
                + " — El OR preserva las filas NULL del LEFT JOIN (mascotas sin visita coincidente).";
        } else {
            return "SELECT DISTINCT p.* FROM pets p "
                + "LEFT JOIN visits v ON v.pet_id = p.id "
                + "WHERE LOWER(v.description) LIKE '%" + keyword.toLowerCase() + "%'"
                + " — Sin OR: las filas NULL son descartadas → INNER JOIN implícito.";
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    @Schema(description = "Resultado de la búsqueda de mascotas con demostración de LEFT JOIN.")
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PetSearchResponse {
        @Schema(description = "Modo de join utilizado: 'left' o 'inner'.", example = "left")
        private String joinMode;

        @Schema(description = "Filtro aplicado a la descripción de las visitas.", example = "rabies")
        private String visitDescriptionFilter;

        @Schema(description = "SQL aproximado que genera la Specification (con valores interpolados para ilustración).")
        private String sqlApproximado;

        @Schema(description = "Número de mascotas devueltas.")
        private int resultCount;

        @Schema(description = "Lista de mascotas encontradas.")
        private List<PetSummary> pets;
    }

    @Schema(description = "Resumen de una mascota con sus visitas y si coincide con el filtro.")
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PetSummary {
        @Schema(example = "7")
        private Integer petId;
        @Schema(example = "Samantha")
        private String petName;
        @Schema(example = "cat")
        private String petType;
        @Schema(example = "Jean Coleman")
        private String ownerFullName;
        @Schema(description = "true si la mascota tiene al menos una visita cuya descripción contiene el término buscado.")
        private boolean hasMatchingVisit;
        @Schema(description = "Todas las visitas de la mascota (cargadas de forma EAGER).")
        private List<VisitSummary> visits;
    }

    @Schema(description = "Resumen de una visita.")
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class VisitSummary {
        @Schema(example = "1")
        private Integer visitId;
        @Schema(example = "2024-01-15")
        private LocalDate date;
        @Schema(example = "annual rabies shot")
        private String description;
    }
}
