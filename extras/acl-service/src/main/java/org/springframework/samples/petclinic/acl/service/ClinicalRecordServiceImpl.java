package org.springframework.samples.petclinic.acl.service;

import java.util.List;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.samples.petclinic.acl.model.ClinicalRecord;
import org.springframework.samples.petclinic.acl.model.RecordForm;
import org.springframework.samples.petclinic.acl.repository.ClinicalRecordRepository;

@Service
public class ClinicalRecordServiceImpl implements ClinicalRecordService {

	private final ClinicalRecordRepository recordRepository;
	private final MutableAclService aclService;

	public ClinicalRecordServiceImpl(ClinicalRecordRepository recordRepository, MutableAclService aclService) {
		this.recordRepository = recordRepository;
		this.aclService = aclService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClinicalRecord> findVisibleRecords() {
		return recordRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public ClinicalRecord findReadableRecord(Long id) {
		return recordRepository.findById(id).orElseThrow();
	}

	@Override
	@Transactional(readOnly = true)
	public ClinicalRecord findWritableRecord(Long id) {
		return recordRepository.findById(id).orElseThrow();
	}

	@Override
	@Transactional
	public ClinicalRecord updateRecord(Long id, RecordForm form) {
		ClinicalRecord record = recordRepository.findById(id).orElseThrow();
		applyForm(record, form);
		return record;
	}

	@Override
	@Transactional
	public ClinicalRecord createRecord(RecordForm form, String ownerUsername) {
		ClinicalRecord record = new ClinicalRecord();
		record.setOwnerUsername(ownerUsername);
		applyForm(record, form);
		ClinicalRecord saved = recordRepository.saveAndFlush(record);
		createOwnerAcl(saved, ownerUsername);
		return saved;
	}

	@Override
	@Transactional
	public void grantRead(Long id, String username) {
		grant(id, username, BasePermission.READ);
	}

	@Override
	@Transactional
	public void grantWrite(Long id, String username) {
		grant(id, username, BasePermission.WRITE);
	}

	private void applyForm(ClinicalRecord record, RecordForm form) {
		record.setPetName(form.getPetName());
		record.setDiagnosis(form.getDiagnosis());
		record.setTreatmentPlan(form.getTreatmentPlan());
	}

	private void createOwnerAcl(ClinicalRecord record, String ownerUsername) {
		MutableAcl acl = aclService.createAcl(identity(record.getId()));
		PrincipalSid owner = new PrincipalSid(ownerUsername);
		acl.setOwner(owner);
		acl.insertAce(acl.getEntries().size(), BasePermission.READ, owner, true);
		acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, owner, true);
		acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, owner, true);
		acl.insertAce(acl.getEntries().size(), BasePermission.READ, new GrantedAuthoritySid("ROLE_ADMIN"), true);
		acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, new GrantedAuthoritySid("ROLE_ADMIN"), true);
		acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, new GrantedAuthoritySid("ROLE_ADMIN"), true);
		aclService.updateAcl(acl);
	}

	private void grant(Long id, String username, org.springframework.security.acls.model.Permission permission) {
		MutableAcl acl = readOrCreateAcl(id);
		acl.insertAce(acl.getEntries().size(), permission, new PrincipalSid(username), true);
		aclService.updateAcl(acl);
	}

	private MutableAcl readOrCreateAcl(Long id) {
		ObjectIdentity identity = identity(id);
		try {
			return (MutableAcl) aclService.readAclById(identity);
		}
		catch (NotFoundException ex) {
			return aclService.createAcl(identity);
		}
	}

	private ObjectIdentity identity(Long id) {
		return new ObjectIdentityImpl(ClinicalRecord.class, id);
	}

}
