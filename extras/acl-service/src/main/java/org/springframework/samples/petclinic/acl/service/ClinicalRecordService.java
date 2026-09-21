package org.springframework.samples.petclinic.acl.service;

import java.util.List;

import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.samples.petclinic.acl.model.ClinicalRecord;
import org.springframework.samples.petclinic.acl.model.RecordForm;

public interface ClinicalRecordService {

	@PostFilter("hasPermission(filterObject, 'READ')")
	List<ClinicalRecord> findVisibleRecords();

	@PreAuthorize("hasPermission(#id, 'org.springframework.samples.petclinic.acl.model.ClinicalRecord', 'READ')")
	ClinicalRecord findReadableRecord(Long id);

	@PreAuthorize("hasPermission(#id, 'org.springframework.samples.petclinic.acl.model.ClinicalRecord', 'WRITE')")
	ClinicalRecord findWritableRecord(Long id);

	@PreAuthorize("hasPermission(#id, 'org.springframework.samples.petclinic.acl.model.ClinicalRecord', 'WRITE')")
	ClinicalRecord updateRecord(Long id, RecordForm form);

	@PreAuthorize("hasAuthority('ROLE_VET')")
	ClinicalRecord createRecord(RecordForm form, String ownerUsername);

	@PreAuthorize("hasPermission(#id, 'org.springframework.samples.petclinic.acl.model.ClinicalRecord', 'ADMINISTRATION')")
	void grantRead(Long id, String username);

	@PreAuthorize("hasPermission(#id, 'org.springframework.samples.petclinic.acl.model.ClinicalRecord', 'ADMINISTRATION')")
	void grantWrite(Long id, String username);

}
