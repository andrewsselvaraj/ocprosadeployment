package com.m2its.academic;

import static com.m2its.common.testsupport.JwtTestSupport.orgAdmin;
import static com.m2its.common.testsupport.JwtTestSupport.student;
import static com.m2its.common.testsupport.JwtTestSupport.superAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AcademicApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void studentReadsSeededCatalogue() throws Exception {
        mvc.perform(get("/api/classes").with(student("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("demo-class-10"));

        mvc.perform(get("/api/classes/demo-class-10/tree").with(student("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.subjects[?(@.id=='demo-subj-sci')].chapters[0].name").value("Physics Basics"));
    }

    @Test
    void adminBuildsHierarchy() throws Exception {
        String classJson = mvc.perform(post("/api/classes").with(orgAdmin("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Class 11"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orgId").value("demo-org"))
                .andReturn().getResponse().getContentAsString();
        String classId = JsonPath.read(classJson, "$.id");

        String subjectJson = mvc.perform(post("/api/subjects").with(orgAdmin("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"classId":"%s","name":"Maths"}""".formatted(classId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classId").value(classId))
                .andReturn().getResponse().getContentAsString();
        String subjectId = JsonPath.read(subjectJson, "$.id");

        mvc.perform(post("/api/chapters").with(orgAdmin("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"subjectId":"%s","name":"Algebra"}""".formatted(subjectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classId").value(classId))
                .andExpect(jsonPath("$.orgId").value("demo-org"));

        // A class with active subjects cannot be deleted
        mvc.perform(delete("/api/classes/" + classId).with(orgAdmin("demo-org")))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/subjects/" + subjectId).with(orgAdmin("demo-org")))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/classes/" + classId).with(orgAdmin("demo-org")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/classes/" + classId).with(orgAdmin("demo-org")))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentsCannotWrite() throws Exception {
        mvc.perform(post("/api/classes").with(student("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Hack"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherOrganisationsCannotSeeData() throws Exception {
        mvc.perform(get("/api/classes/demo-class-10").with(orgAdmin("other-org")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/classes").with(orgAdmin("other-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void superAdminMustChooseOrganisation() throws Exception {
        mvc.perform(get("/api/classes").with(superAdmin())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/classes").param("orgId", "demo-org").with(superAdmin()))
                .andExpect(status().isOk());
    }
}
