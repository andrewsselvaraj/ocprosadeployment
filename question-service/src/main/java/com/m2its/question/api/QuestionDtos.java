package com.m2its.question.api;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.m2its.question.domain.AnswerOption;
import com.m2its.question.domain.Question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class QuestionDtos {

    private QuestionDtos() {
    }

    public record OptionRequest(@NotBlank @Size(max = 5000) String text, boolean correct) {
    }

    /** orgId is only used (and required) when a SUPERADMIN creates the question. */
    public record CreateQuestionRequest(
            String orgId,
            @NotBlank String classId,
            @NotBlank String subjectId,
            String chapterId,
            @NotBlank @Size(max = 10000) String text,
            @NotBlank @Pattern(regexp = "text|image") String type,
            @NotEmpty @Size(min = 2, max = 10) List<@Valid OptionRequest> options) {
    }

    public record UpdateQuestionRequest(
            String chapterId,
            @NotBlank @Size(max = 10000) String text,
            @NotBlank @Pattern(regexp = "text|image") String type,
            @NotEmpty @Size(min = 2, max = 10) List<@Valid OptionRequest> options) {
    }

    /** "correct" is only filled for content editors (ADMIN, DATA_ENTRY, SUPERADMIN); students never see it. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OptionView(String id, String text, Boolean correct) {
    }

    public record QuestionView(String id, String orgId, String classId, String subjectId, String chapterId,
                               String text, String type, List<OptionView> options, String updatedBy,
                               LocalDateTime updatedAt) {

        public static QuestionView from(Question q, boolean revealAnswers) {
            List<OptionView> options = q.getActiveOptions().stream()
                    .map(o -> new OptionView(o.getId(), o.getText(), revealAnswers ? o.isCorrect() : null))
                    .toList();
            String chapter = Question.NO_CHAPTER.equals(q.getChapterId()) ? null : q.getChapterId();
            return new QuestionView(q.getId(), q.getOrgId(), q.getClassId(), q.getSubjectId(), chapter, q.getText(),
                    q.getType(), options, q.getUpdatedBy(), q.getUpdatedAt());
        }
    }

    // ---- internal evaluation API used by exam-service ------------------------------------

    public record AnswerRef(@NotBlank String questionId, @NotBlank String answerId) {
    }

    public record EvaluateRequest(@NotEmpty @Size(max = 500) List<@Valid AnswerRef> answers) {
    }

    public record EvaluatedAnswer(String questionId, String answerId, boolean correct, List<String> correctAnswerIds) {

        public static EvaluatedAnswer of(Question q, String answerId) {
            List<String> correctIds = q.getAllOptions().stream()
                    .filter(AnswerOption::isCorrect)
                    .map(AnswerOption::getId)
                    .toList();
            return new EvaluatedAnswer(q.getId(), answerId, correctIds.contains(answerId), correctIds);
        }
    }

    public record EvaluateResponse(List<EvaluatedAnswer> results) {
    }
}
