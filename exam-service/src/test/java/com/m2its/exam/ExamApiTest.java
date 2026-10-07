package com.m2its.exam;

import static com.m2its.common.testsupport.JwtTestSupport.orgAdmin;
import static com.m2its.common.testsupport.JwtTestSupport.student;
import static com.m2its.common.testsupport.JwtTestSupport.token;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.m2its.exam.client.QuestionClient;
import com.m2its.exam.client.QuestionClient.EvaluatedAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

/** question-service is replaced by a Mockito mock; the HTTP client itself is tested in QuestionClientTest. */
@SpringBootTest
@AutoConfigureMockMvc
class ExamApiTest {

    private static final String SUBMISSION = """
            {"classId":"demo-class-10","subjectId":"demo-subj-cloud",
             "answers":[{"questionId":"q1","answerId":"q1-a"},{"questionId":"q2","answerId":"q2-a"}]}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    QuestionClient questionClient;

    @BeforeEach
    void scoring() {
        given(questionClient.evaluate(anyList(), anyString())).willReturn(List.of(
                new EvaluatedAnswer("q1", "q1-a", true, List.of("q1-a")),
                new EvaluatedAnswer("q2", "q2-a", false, List.of("q2-b"))));
    }

    @Test
    void submitScoresAndStoresTheSitting() throws Exception {
        String body = mvc.perform(post("/api/exams/submit").with(student("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content(SUBMISSION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.scorePercent").value(50.0))
                .andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(body, "$.sessionId");

        mvc.perform(get("/api/exams/sessions/" + sessionId).with(student("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].correctAnswerIds[0]").value("q2-b"));

        mvc.perform(get("/api/exams/sessions").with(student("demo-org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$.content[0].answeredQuestions").value(2));

        // Another student of the same org cannot read it; the org admin can
        mvc.perform(get("/api/exams/sessions/" + sessionId).with(token("someone-else", "demo-org", "USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/exams/sessions/" + sessionId).with(orgAdmin("demo-org")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/exams/sessions/" + sessionId).with(orgAdmin("other-org")))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateQuestionIsRejected() throws Exception {
        mvc.perform(post("/api/exams/submit").with(student("demo-org")).contentType(MediaType.APPLICATION_JSON)
                        .content(SUBMISSION.replace("\"q2\"", "\"q1\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unreachableQuestionServiceGives503() throws Exception {
        given(questionClient.evaluate(anyList(), anyString())).willThrow(new ResourceAccessException("down"));
        mvc.perform(post("/api/exams/submit").with(student("demo-org"))
                        .contentType(MediaType.APPLICATION_JSON).content(SUBMISSION))
                .andExpect(status().isServiceUnavailable());
    }
}
