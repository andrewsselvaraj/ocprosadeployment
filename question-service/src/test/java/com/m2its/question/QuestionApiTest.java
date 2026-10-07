package com.m2its.question;

import static com.m2its.common.testsupport.JwtTestSupport.orgAdmin;
import static com.m2its.common.testsupport.JwtTestSupport.student;
import static com.m2its.common.testsupport.JwtTestSupport.token;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionApiTest {

    private static final String NEW_QUESTION = """
            {"classId":"demo-class-10","subjectId":"demo-subj-sci","text":"2 + 2 = ?","type":"text",
             "options":[{"text":"3","correct":false},{"text":"4","correct":true},{"text":"5","correct":false}]}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void studentsDoNotSeeCorrectAnswers() throws Exception {
        mvc.perform(get("/api/questions").param("subjectId", "demo-subj-cloud").with(student("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].options.length()").value(4))
                .andExpect(jsonPath("$.content[0].options[0].correct").doesNotExist());

        mvc.perform(get("/api/questions/demo-q1").with(orgAdmin("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].correct").value(true));
    }

    @Test
    void adminCreatesAndUpdatesQuestionKeepingOptionOrder() throws Exception {
        String created = mvc.perform(post("/api/questions").with(orgAdmin("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_QUESTION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options[*].text", contains("3", "4", "5")))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        List<String> oldOptionIds = JsonPath.read(created, "$.options[*].id");

        mvc.perform(put("/api/questions/" + id).with(orgAdmin("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"text":"2 + 3 = ?","type":"text",
                                 "options":[{"text":"5","correct":true},{"text":"6","correct":false}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[*].text", contains("5", "6")));

        // Retired options still evaluate (old exams keep their score)
        mvc.perform(post("/internal/answers/evaluate").with(student("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"answers":[{"questionId":"%s","answerId":"%s"}]}""".formatted(id, oldOptionIds.get(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].correct").value(true));
    }

    @Test
    void questionNeedsACorrectOption() throws Exception {
        mvc.perform(post("/api/questions").with(orgAdmin("demo-org")).contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_QUESTION.replace("\"correct\":true", "\"correct\":false")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateScoresAnswers() throws Exception {
        mvc.perform(post("/internal/answers/evaluate").with(student("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"answers":[{"questionId":"demo-q1","answerId":"demo-q1-a"},
                                            {"questionId":"demo-q2","answerId":"demo-q2-a"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].correct").value(true))
                .andExpect(jsonPath("$.results[1].correct").value(false))
                .andExpect(jsonPath("$.results[1].correctAnswerIds[0]").value("demo-q2-b"));
    }

    @Test
    void dataEntryOperatorAuthorsQuestions() throws Exception {
        mvc.perform(post("/api/questions").with(token("author", "demo-org", "DATA_ENTRY"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_QUESTION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options[1].correct").value(true));
    }

    @Test
    void legacyDelStatusRowsDoNotBreakLoading() throws Exception {
        // The legacy dump contains answer rows with status 'DEL' (besides 'D')
        jdbc.update("""
                INSERT INTO answer_info_v_two (pk_answer_id, fk_org_id, fk_class_id, fk_subject_id, fk_chapter_id,
                  fk_question_id, answers, correct_answer, answer_updated_by, answer_status)
                VALUES ('legacy-del', 'demo-org', 'demo-class-10', 'demo-subj-cloud', '', 'demo-q3', 'Old', 'No',
                  'legacy', 'DEL')""");
        mvc.perform(get("/api/questions/demo-q3").with(student("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options.length()").value(4));
        mvc.perform(post("/internal/answers/evaluate").with(student("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"answers":[{"questionId":"demo-q3","answerId":"demo-q3-a"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].correct").value(true));
    }

    @Test
    void otherOrganisationCannotEvaluateOrRead() throws Exception {
        mvc.perform(get("/api/questions/demo-q1").with(student("other-org"))).andExpect(status().isForbidden());
        mvc.perform(post("/internal/answers/evaluate").with(student("other-org"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"answers":[{"questionId":"demo-q1","answerId":"demo-q1-a"}]}"""))
                .andExpect(status().isForbidden());
    }
}
