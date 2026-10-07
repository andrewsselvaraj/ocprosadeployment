package com.m2its.exam.domain;

import java.time.LocalDateTime;

/** Read model filled directly by a JPQL constructor expression (see ExamAnswerRepository). */
public record SessionSummary(String sessionId, String classId, String subjectId, String takenBy,
                             LocalDateTime takenAt, long answeredQuestions) {
}
