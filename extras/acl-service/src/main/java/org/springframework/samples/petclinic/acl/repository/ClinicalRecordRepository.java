package org.springframework.samples.petclinic.acl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.samples.petclinic.acl.model.ClinicalRecord;

public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecord, Long> {
}
