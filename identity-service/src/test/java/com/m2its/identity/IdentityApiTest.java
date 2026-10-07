package com.m2its.identity;

import static com.m2its.common.testsupport.JwtTestSupport.orgAdmin;
import static com.m2its.common.testsupport.JwtTestSupport.superAdmin;
import static com.m2its.common.testsupport.JwtTestSupport.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.m2its.identity.user.AppUserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Runs against H2 with the Flyway migrations + demo seed, exactly like a local run. */
@SpringBootTest
@AutoConfigureMockMvc
class IdentityApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AppUserRepository users;

    @Test
    void loginIssuesJwtThatOtherEndpointsAccept() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@demo.local","password":"Demo@123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        // Real signature validation path (no mocked authentication here)
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@demo.local"))
                .andExpect(jsonPath("$.orgId").value("demo-org"));

        // The legacy {noop} password was upgraded to BCrypt on login
        assertThat(users.findById("demo-student").orElseThrow().getPasswordHash()).startsWith("{bcrypt}");
    }

    @Test
    void superAdminLoginHasNoOrg() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"superadmin@m2its.local","password":"Demo@123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("SUPERADMIN"))
                .andExpect(jsonPath("$.user.orgId").doesNotExist());
    }

    @Test
    void legacyDataEntryOperatorRoleIsSupported() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"author@demo.local","password":"Demo@123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("DATA_ENTRY"));
        // Data entry operators edit content but do not manage users
        mvc.perform(get("/api/users").with(token(
                        "demo-author", "demo-org", "DATA_ENTRY")))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@demo.local","password":"nope"}"""))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@demo.local","password":"nope"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/orgs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orgs").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlySuperAdminCreatesOrganizations() throws Exception {
        String org = """
                {"name":"New School","email":"office@new.local","password":"Secret#123"}""";
        mvc.perform(post("/api/orgs").with(orgAdmin("demo-org")).contentType(MediaType.APPLICATION_JSON).content(org))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/orgs").with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content(org))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void orgAdminManagesOnlyOwnOrganization() throws Exception {
        mvc.perform(post("/api/users").with(orgAdmin("demo-org")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pupil","email":"pupil@demo.local","password":"Secret#123","role":"USER"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orgId").value("demo-org"));

        mvc.perform(get("/api/users").with(orgAdmin("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber());

        mvc.perform(get("/api/users").param("orgId", "other-org").with(orgAdmin("demo-org")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPayloadReturnsProblemDetail() throws Exception {
        mvc.perform(post("/api/users").with(orgAdmin("demo-org")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"x","role":"USER"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void contactFormIsPublic() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Visitor","email":"v@x.local","subject":"Hi","message":"Hello"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void healthAndMetricsEndpointsArePublic() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }
}
