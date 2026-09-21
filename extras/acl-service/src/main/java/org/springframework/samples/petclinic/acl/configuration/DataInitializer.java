package org.springframework.samples.petclinic.acl.configuration;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.samples.petclinic.acl.model.ClinicalRecord;
import org.springframework.samples.petclinic.acl.repository.ClinicalRecordRepository;

@Component
public class DataInitializer implements ApplicationRunner {

	private final ClinicalRecordRepository recordRepository;
	private final MutableAclService aclService;

	public DataInitializer(ClinicalRecordRepository recordRepository, MutableAclService aclService) {
		this.recordRepository = recordRepository;
		this.aclService = aclService;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (recordRepository.count() > 0) {
			return;
		}
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"admin",
				"system",
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		try {
			ClinicalRecord luna = createRecord(
					"Luna",
					"Otitis externa",
					"Limpiar oido, analgesia y revision en 7 dias.",
					"ana");
			ClinicalRecord max = createRecord(
					"Max",
					"Control postoperatorio",
					"Reposo, collar isabelino y control de sutura.",
					"bruno");
			grant(luna, "bruno", BasePermission.READ);
			grant(luna, "ROLE_ASSISTANT", BasePermission.READ, false);
			grant(max, "ROLE_ASSISTANT", BasePermission.READ, false);
			grant(luna,"japarejo",BasePermission.READ);
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

	private ClinicalRecord createRecord(String petName, String diagnosis, String treatmentPlan, String ownerUsername) {
		ClinicalRecord record = new ClinicalRecord();
		record.setPetName(petName);
		record.setDiagnosis(diagnosis);
		record.setTreatmentPlan(treatmentPlan);
		record.setOwnerUsername(ownerUsername);
		ClinicalRecord saved = recordRepository.saveAndFlush(record);

		MutableAcl acl = aclService.createAcl(identity(saved));
		PrincipalSid owner = new PrincipalSid(ownerUsername);
		acl.setOwner(owner);
		acl.insertAce(acl.getEntries().size(), BasePermission.READ, owner, true);
		acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, owner, true);
		acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, owner, true);
		acl.insertAce(acl.getEntries().size(), BasePermission.READ, new GrantedAuthoritySid("ROLE_ADMIN"), true);
		acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, new GrantedAuthoritySid("ROLE_ADMIN"), true);
		acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, new GrantedAuthoritySid("ROLE_ADMIN"), true);
		aclService.updateAcl(acl);
		return saved;
	}

	private void grant(ClinicalRecord record, String sid, org.springframework.security.acls.model.Permission permission) {
		grant(record, sid, permission, true);
	}

	private void grant(
			ClinicalRecord record,
			String sid,
			org.springframework.security.acls.model.Permission permission,
			boolean principal) {
		MutableAcl acl = (MutableAcl) aclService.readAclById(identity(record));
		acl.insertAce(
				acl.getEntries().size(),
				permission,
				principal ? new PrincipalSid(sid) : new GrantedAuthoritySid(sid),
				true);
		aclService.updateAcl(acl);
	}

	private ObjectIdentity identity(ClinicalRecord record) {
		return new ObjectIdentityImpl(ClinicalRecord.class, record.getId());
	}

}
