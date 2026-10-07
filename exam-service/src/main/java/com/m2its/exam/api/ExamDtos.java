package com.m2its.exam.api;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public final class ExamDtos {

    private ExamDtos() {
    }

    public record SelectedAnswer(@NotBlank String questionId, @NotBlank String answerId) {
    }

    /** orgId is only used (and required) when a SUPERADMIN submits. */
    public record SubmitExamRequest(
            String orgId,
            @NotBlank String classId,
            @NotBlank String subjectId,
            @NotEmpty @Size(max = 500) List<@Valid SelectedAnswer> answers) {
    }

    public record ResultItem(String questionId, String selectedAnswerId, boolean correct,
                             List<String> correctAnswerIds) {
    }

    public record ExamResult(String sessionId, String classId, String subjectId, String takenBy,
                             LocalDateTime takenAt, int totalQuestions, int correctAnswers, double scorePercent,
                             List<ResultItem> items) {
    }
}
