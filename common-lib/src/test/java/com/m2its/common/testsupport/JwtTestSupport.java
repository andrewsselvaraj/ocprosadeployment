package com.m2its.common.testsupport;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;

import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.security.Roles;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * MockMvc helpers that authenticate a request with a JWT carrying our custom claims.
 * Shipped in common-lib's test-jar so every service's tests can use it.
 */
public final class JwtTestSupport {

    private JwtTestSupport() {
    }

    public static RequestPostProcessor superAdmin() {
        return token("sa-1", null, Roles.SUPERADMIN);
    }

    public static RequestPostProcessor orgAdmin(String orgId) {
        return token("admin-" + orgId, orgId, Roles.ADMIN);
    }

    public static RequestPostProcessor student(String orgId) {
        return token("student-" + orgId, orgId, Roles.USER);
    }

    public static RequestPostProcessor token(String userId, String orgId, String role) {
        return jwt()
                .jwt(j -> {
                    j.subject(userId + "@test.local")
                            .claim(AuthenticatedUser.CLAIM_USER_ID, userId)
                            .claim(AuthenticatedUser.CLAIM_ROLES, List.of(role));
                    if (orgId != null) {
                        j.claim(AuthenticatedUser.CLAIM_ORG_ID, orgId);
                    }
                })
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
