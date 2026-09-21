package org.springframework.samples.petclinic.acl.service;

import java.util.List;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.samples.petclinic.acl.model.ClinicalRecord;

@Service
public class AclPermissionService {

	private final AclService aclService;
	private final SidRetrievalStrategyImpl sidRetrievalStrategy = new SidRetrievalStrategyImpl();

	public AclPermissionService(AclService aclService) {
		this.aclService = aclService;
	}

	public boolean canWrite(Authentication authentication, ClinicalRecord record) {
		return hasPermission(authentication, record, BasePermission.WRITE);
	}

	public boolean canAdmin(Authentication authentication, ClinicalRecord record) {
		return hasPermission(authentication, record, BasePermission.ADMINISTRATION);
	}

	private boolean hasPermission(Authentication authentication, ClinicalRecord record, Permission permission) {
		try {
			Acl acl = aclService.readAclById(new ObjectIdentityImpl(ClinicalRecord.class, record.getId()));
			List<Sid> sids = sidRetrievalStrategy.getSids(authentication);
			return acl.isGranted(List.of(permission), sids, false);
		}
		catch (NotFoundException ex) {
			return false;
		}
	}

}
