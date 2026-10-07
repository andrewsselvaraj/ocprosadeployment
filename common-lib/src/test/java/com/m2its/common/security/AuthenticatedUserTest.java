package com.m2its.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import com.m2its.common.web.ApiException;

import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

    private final AuthenticatedUser admin = new AuthenticatedUser("u1", "a@x", "org-1", Set.of(Roles.ADMIN), "t");
    private final AuthenticatedUser superAdmin = new AuthenticatedUser("s1", "s@x", null, Set.of(Roles.SUPERADMIN), "t");

    @Test
    void orgUserIsPinnedToOwnOrg() {
        assertThat(admin.resolveOrg(null)).isEqualTo("org-1");
        assertThat(admin.resolveOrg("org-1")).isEqualTo("org-1");
        assertThatThrownBy(() -> admin.resolveOrg("org-2")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> admin.checkOrgAccess("org-2")).isInstanceOf(ApiException.class);
    }

    @Test
    void superAdminMustNameAnOrg() {
        assertThat(superAdmin.resolveOrg("org-2")).isEqualTo("org-2");
        assertThatThrownBy(() -> superAdmin.resolveOrg(null)).isInstanceOf(ApiException.class);
        superAdmin.checkOrgAccess("any-org");
    }
}
