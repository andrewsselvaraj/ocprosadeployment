package com.m2its.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.m2its.common.web.ApiException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The caller, as described by the validated JWT. Every service uses this to enforce
 * multi-tenancy: an ADMIN or USER only sees data of their own organisation, while a
 * SUPERADMIN may work across organisations.
 */
public record AuthenticatedUser(String userId, String email, String orgId, Set<String> roles, String token) {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ORG_ID = "org";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_NAME = "name";

    public static AuthenticatedUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw ApiException.unauthorized("No authenticated user");
        }
        Jwt jwt = jwtAuth.getToken();
        Collection<String> roles = jwt.getClaimAsStringList(CLAIM_ROLES);
        return new AuthenticatedUser(
                jwt.getClaimAsString(CLAIM_USER_ID),
                jwt.getSubject(),
                jwt.getClaimAsString(CLAIM_ORG_ID),
                Set.copyOf(roles == null ? List.of() : roles),
                jwt.getTokenValue());
    }

    public boolean isSuperAdmin() {
        return roles.contains(Roles.SUPERADMIN);
    }

    public boolean canManage() {
        return isSuperAdmin() || roles.contains(Roles.ADMIN);
    }

    public boolean canEditContent() {
        return canManage() || roles.contains(Roles.DATA_ENTRY);
    }

    /**
     * Organisation to operate on. A SUPERADMIN must name one explicitly; everyone else is
     * pinned to the organisation in their token and may not name another.
     */
    public String resolveOrg(String requestedOrgId) {
        if (isSuperAdmin()) {
            if (requestedOrgId == null || requestedOrgId.isBlank()) {
                throw ApiException.badRequest("orgId is required for SUPERADMIN requests");
            }
            return requestedOrgId;
        }
        if (requestedOrgId != null && !requestedOrgId.isBlank() && !requestedOrgId.equals(orgId)) {
            throw ApiException.forbidden("Access to organisation " + requestedOrgId + " is not allowed");
        }
        return orgId;
    }

    /** Throws 403 unless the caller may access a record that belongs to {@code recordOrgId}. */
    public void checkOrgAccess(String recordOrgId) {
        if (!isSuperAdmin() && !Objects.equals(orgId, recordOrgId)) {
            throw ApiException.forbidden("Record belongs to another organisation");
        }
    }
}
