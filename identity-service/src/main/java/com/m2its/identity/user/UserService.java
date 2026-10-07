package com.m2its.identity.user;

import com.m2its.common.jpa.Ids;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.web.ApiException;
import com.m2its.common.web.PageResponse;
import com.m2its.identity.org.OrganizationRepository;
import com.m2its.identity.user.UserDtos.CreateUserRequest;
import com.m2its.identity.user.UserDtos.UpdateUserRequest;
import com.m2its.identity.user.UserDtos.UserResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository users;
    private final OrganizationRepository organizations;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, OrganizationRepository organizations, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<UserResponse> list(AuthenticatedUser caller, String orgId, Pageable pageable) {
        String org = caller.resolveOrg(orgId);
        return PageResponse.of(users.findByOrgIdAndStatus(org, RecordStatus.ACTIVE, pageable), UserResponse::from);
    }

    public UserResponse get(AuthenticatedUser caller, String id) {
        AppUser user = findActive(id);
        if (!caller.canManage() && !user.getId().equals(caller.userId())) {
            throw ApiException.forbidden("Users may only view their own profile");
        }
        caller.checkOrgAccess(user.getOrgId());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse create(AuthenticatedUser caller, CreateUserRequest request) {
        String orgId = caller.resolveOrg(request.orgId());
        organizations.findByIdAndStatus(orgId, RecordStatus.ACTIVE)
                .orElseThrow(() -> ApiException.badRequest("Organization " + orgId + " does not exist"));
        if (users.existsByEmailAndStatus(request.email(), RecordStatus.ACTIVE)) {
            throw ApiException.conflict("A user with email " + request.email() + " already exists");
        }
        AppUser user = new AppUser(Ids.newId(), orgId, request.name(), request.email(),
                passwordEncoder.encode(request.password()), request.role(), caller.email());
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public UserResponse update(AuthenticatedUser caller, String id, UpdateUserRequest request) {
        AppUser user = findActive(id);
        caller.checkOrgAccess(user.getOrgId());
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setUpdatedBy(caller.email());
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(AuthenticatedUser caller, String id) {
        AppUser user = findActive(id);
        caller.checkOrgAccess(user.getOrgId());
        user.setStatus(RecordStatus.DELETED);
        user.setUpdatedBy(caller.email());
    }

    private AppUser findActive(String id) {
        return users.findByIdAndStatus(id, RecordStatus.ACTIVE).orElseThrow(() -> ApiException.notFound("User", id));
    }
}
