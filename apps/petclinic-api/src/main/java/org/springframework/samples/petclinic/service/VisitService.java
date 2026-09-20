package org.springframework.samples.petclinic.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.samples.petclinic.repository.VisitSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@Service
@Validated
public class VisitService {

	
	@Autowired
	VisitRepository visitRepository;

	@Transactional
	public Visit create(@Valid Visit visit) {
		visitRepository.save(visit);
		return visit;
	}

	public Optional<Visit> findById(int visitId){
		return visitRepository.findById(visitId);
	}

	/**
	 * Search visits using JPA Criteria API with dynamic filtering and pagination.
	 * 
	 * @param petId Filter by pet identifier
	 * @param ownerLastName Filter by owner last name (case-insensitive, partial match)
	 * @param petName Filter by pet name (case-insensitive, partial match)
	 * @param petType Filter by pet type (case-insensitive, exact match)
	 * @param description Filter by description (case-insensitive, partial match)
	 * @param fromDate Filter visits from this date (inclusive)
	 * @param toDate Filter visits until this date (inclusive)
	 * @param pageable Pagination and sorting parameters
	 * @return A page of visits matching the criteria
	 */
    public Page<Visit> search(Integer petId, String ownerLastName, String petName, String petType,
            String description, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return visitRepository.findAll(
        	VisitSpecification.filterVisits(petId, ownerLastName, petName, petType, description, fromDate, toDate),
        	pageable
        );
    }
}
