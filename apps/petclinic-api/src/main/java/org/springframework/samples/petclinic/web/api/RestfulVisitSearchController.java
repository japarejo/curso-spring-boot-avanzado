package org.springframework.samples.petclinic.web.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Validated
@Tag(name = "Visits", description = "Visit retrieval REST API")
@RestController
@RequestMapping("/api/v1/visits")
public class RestfulVisitSearchController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 5;
	private static final int MAX_SIZE = 50;

	private static final Map<String, String> SORT_PROPERTIES = Map.of(
			"id", "id",
			"date", "date",
			"description", "description",
			"petName", "pet.name",
			"ownerLastName", "pet.owner.lastName",
			"petType", "pet.type.name");

	private final VisitService visitService;

	public RestfulVisitSearchController(VisitService visitService) {
		this.visitService = visitService;
	}

	@Operation(
			summary = "Search visits with filters and pagination",
			description = """
					Didactic REST example with optional filters, page metadata, navigation links and controlled sorting.
					Example: /api/v1/visits?ownerLastName=franklin&fromDate=2024-01-01&size=3&sort=date,desc
					Allowed sort fields: id, date, description, petName, ownerLastName, petType.
					""")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Visits returned successfully",
			content = @Content(mediaType = "application/json",
				schema = @Schema(implementation = PagedVisitsResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid pagination, filter or sort parameter",
			content = @Content)
	})
	@GetMapping
	public PagedVisitsResponse searchVisits(
			@Parameter(description = "Filter by pet identifier.", example = "7")
			@RequestParam(required = false) Integer petId,
			@Parameter(description = "Case-insensitive partial match against owner last name.", example = "Franklin")
			@RequestParam(required = false) String ownerLastName,
			@Parameter(description = "Case-insensitive partial match against pet name.", example = "Leo")
			@RequestParam(required = false) String petName,
			@Parameter(description = "Exact pet type match, case-insensitive.", example = "dog")
			@RequestParam(required = false) String petType,
			@Parameter(description = "Case-insensitive partial match against visit description.", example = "rabies")
			@RequestParam(required = false) String description,
			@Parameter(description = "Include visits on or after this date. ISO format: yyyy-MM-dd.", example = "2024-01-01")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@Parameter(description = "Include visits on or before this date. ISO format: yyyy-MM-dd.", example = "2024-12-31")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@ParameterObject
			@PageableDefault(page = DEFAULT_PAGE, size = DEFAULT_SIZE, sort = "date", direction = Sort.Direction.DESC)
			Pageable pageable,
			@Parameter(description = "Sort as field,direction. Direction can be asc or desc.", example = "date,desc")
			@RequestParam(defaultValue = "date,desc") String sort) {

		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BadRequestException("fromDate must be before or equal to toDate");
		}

		VisitFilters filters = new VisitFilters(
				petId,
				blankToNull(ownerLastName),
				blankToNull(petName),
				blankToNull(petType),
				blankToNull(description),
				fromDate,
				toDate);
		PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), parseSort(sort));
		Page<Visit> visits = visitService.search(filters.getPetId(), filters.getOwnerLastName(), filters.getPetName(),
				filters.getPetType(), filters.getDescription(), filters.getFromDate(), filters.getToDate(), pageRequest);

		return toResponse(visits, filters, sort);
	}

	private Sort parseSort(String sort) {
		String[] parts = sort.split(",", -1);
		String requestedField = parts[0].trim();
		String property = SORT_PROPERTIES.get(requestedField);

		if (property == null) {
			throw new BadRequestException("Unsupported sort field: " + requestedField);
		}
		if (parts.length > 2) {
			throw new BadRequestException("Sort must use the format field,direction");
		}

		Sort.Direction direction = Sort.Direction.DESC;
		if (parts.length == 2 && !parts[1].isBlank()) {
			try {
				direction = Sort.Direction.fromString(parts[1].trim());
			}
			catch (IllegalArgumentException ex) {
				throw new BadRequestException("Unsupported sort direction: " + parts[1].trim());
			}
		}

		return Sort.by(direction, property);
	}

	private PagedVisitsResponse toResponse(Page<Visit> visits, VisitFilters filters, String sort) {
		List<VisitSummary> content = visits.stream()
				.map(this::toSummary)
				.toList();

		PageMetadata page = new PageMetadata(
				visits.getNumber(),
				visits.getSize(),
				visits.getNumberOfElements(),
				visits.getTotalElements(),
				visits.getTotalPages(),
				visits.isFirst(),
				visits.isLast());

		PageLinks links = new PageLinks(
				pageLink(visits.getNumber()),
				pageLink(0),
				visits.hasPrevious() ? pageLink(visits.previousPageable().getPageNumber()) : null,
				visits.hasNext() ? pageLink(visits.nextPageable().getPageNumber()) : null,
				pageLink(Math.max(visits.getTotalPages() - 1, 0)));

		return new PagedVisitsResponse(content, page, filters, sort, links);
	}

	private VisitSummary toSummary(Visit visit) {
		Pet pet = visit.getPet();
		Owner owner = pet.getOwner();
		PetType type = pet.getType();

		return new VisitSummary(
				visit.getId(),
				visit.getDate(),
				visit.getDescription(),
				new PetSummary(pet.getId(), pet.getName(), type.getName()),
				new OwnerSummary(owner.getId(), owner.getFirstName(), owner.getLastName(), owner.getFullName()));
	}

	private URI pageLink(int page) {
		return ServletUriComponentsBuilder.fromCurrentRequest()
				.replaceQueryParam("page", page)
				.build()
				.toUri();
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	@Schema(description = "Response wrapper for a paginated visit search.")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PagedVisitsResponse {
		private List<VisitSummary> content;
		private PageMetadata page;
		private VisitFilters filters;
		private String sort;
		private PageLinks links;
	}

	@Schema(description = "Filters applied to the search. Null means the filter was not used.")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class VisitFilters {
		private Integer petId;
		private String ownerLastName;
		private String petName;
		private String petType;
		private String description;
		private LocalDate fromDate;
		private LocalDate toDate;
	}

	@Schema(description = "Page metadata that clients can use to render pagination controls.")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PageMetadata {
		private int number;
		private int size;
		private int numberOfElements;
		private long totalElements;
		private int totalPages;
		private boolean first;
		private boolean last;
	}

	@Schema(description = "Navigation links generated from the current request.")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PageLinks {
		private URI self;
		private URI first;
		private URI previous;
		private URI next;
		private URI last;
	}

	@Schema(description = "Visit row returned by the API without exposing the JPA entity graph.")
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class VisitSummary {
		private Integer id;
		private LocalDate date;
		private String description;
		private PetSummary pet;
		private OwnerSummary owner;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PetSummary {
		private Integer id;
		private String name;
		private String type;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OwnerSummary {
		private Integer id;
		private String firstName;
		private String lastName;
		private String fullName;
	}
}

