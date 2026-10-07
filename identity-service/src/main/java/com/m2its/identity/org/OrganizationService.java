package com.m2its.identity.org;

import java.util.List;

import com.m2its.common.jpa.Ids;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.web.ApiException;
import com.m2its.identity.org.OrganizationDtos.CreateOrganizationRequest;
import com.m2its.identity.org.OrganizationDtos.OrganizationResponse;
import com.m2its.identity.org.OrganizationDtos.UpdateOrganizationRequest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final PasswordEncoder passwordEncoder;

    public OrganizationService(OrganizationRepository organizations, PasswordEncoder passwordEncoder) {
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
    }

    public List<OrganizationResponse> list(AuthenticatedUser caller) {
        if (caller.isSuperAdmin()) {
            return organizations.findByStatusOrderByName(RecordStatus.ACTIVE).stream()
                    .map(OrganizationResponse::from).toList();
        }
        return List.of(get(caller, caller.orgId()));
    }

    public OrganizationResponse get(AuthenticatedUser caller, String id) {
        caller.checkOrgAccess(id);
        return OrganizationResponse.from(findActive(id));
    }

    @Transactional
    public OrganizationResponse create(AuthenticatedUser caller, CreateOrganizationRequest request) {
        Organization org = new Organization(Ids.newId(), request.name(), request.email(),
                passwordEncoder.encode(request.password()), caller.email());
        return OrganizationResponse.from(organizations.save(org));
    }

    @Transactional
    public OrganizationResponse update(AuthenticatedUser caller, String id, UpdateOrganizationRequest request) {
        Organization org = findActive(id);
        org.setName(request.name());
        org.setEmail(request.email());
        org.setUpdatedBy(caller.email());
        // No save() call needed: the entity is managed, Hibernate flushes changes on commit (dirty checking)
        return OrganizationResponse.from(org);
    }

    @Transactional
    public void delete(AuthenticatedUser caller, String id) {
        Organization org = findActive(id);
        org.setStatus(RecordStatus.DELETED);
        org.setUpdatedBy(caller.email());
    }

    Organization findActive(String id) {
        return organizations.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> ApiException.notFound("Organization", id));
    }
}
