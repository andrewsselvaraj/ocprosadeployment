package com.m2its.exam.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.m2its.common.jpa.Ids;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.web.ApiException;
import com.m2its.common.web.PageResponse;
import com.m2its.exam.api.ExamDtos.ExamResult;
import com.m2its.exam.api.ExamDtos.ResultItem;
import com.m2its.exam.api.ExamDtos.SelectedAnswer;
import com.m2its.exam.api.ExamDtos.SubmitExamRequest;
import com.m2its.exam.client.QuestionClient;
import com.m2its.exam.client.QuestionClient.AnswerRef;
import com.m2its.exam.client.QuestionClient.EvaluatedAnswer;
import com.m2its.exam.domain.ExamAnswer;
import com.m2its.exam.domain.ExamAnswerRepository;
import com.m2its.exam.domain.SessionSummary;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

/**
 * Note there is deliberately no @Transactional around submit(): the remote call to
 * question-service happens first, outside any DB transaction, so a slow dependency never
 * holds a database connection. saveAll() then runs in its own short transaction.
 */
@Service
public class ExamService {

    private final ExamAnswerRepository examAnswers;
    private final QuestionClient questionClient;
    private final MeterRegistry meterRegistry;

    public ExamService(ExamAnswerRepository examAnswers, QuestionClient questionClient, MeterRegistry meterRegistry) {
        this.examAnswers = examAnswers;
        this.questionClient = questionClient;
        this.meterRegistry = meterRegistry;
    }

    public ExamResult submit(AuthenticatedUser caller, SubmitExamRequest request) {
        String orgId = caller.resolveOrg(request.orgId());
        var questionIds = new HashSet<String>();
        for (SelectedAnswer a : request.answers()) {
            if (!questionIds.add(a.questionId())) {
                throw ApiException.badRequest("Question " + a.questionId() + " answered more than once");
            }
        }

        List<EvaluatedAnswer> evaluated = evaluate(request.answers().stream()
                .map(a -> new AnswerRef(a.questionId(), a.answerId())).toList(), caller.token());

        String sessionId = Ids.newId();
        List<ExamAnswer> rows = evaluated.stream()
                .map(e -> new ExamAnswer(Ids.newId(), sessionId, orgId, request.classId(), request.subjectId(),
                        e.questionId(), e.answerId(), caller.email()))
                .toList();
        examAnswers.saveAll(rows);

        meterRegistry.counter("m2its.exams.submitted", "org", orgId).increment();
        return toResult(sessionId, request.classId(), request.subjectId(), caller.email(), LocalDateTime.now(),
                evaluated);
    }

    /** Re-scores a past sitting through question-service (scores are not stored, as in the legacy app). */
    public ExamResult getResult(AuthenticatedUser caller, String sessionId) {
        List<ExamAnswer> rows = examAnswers.findBySessionIdAndStatusOrderById(sessionId, RecordStatus.ACTIVE);
        if (rows.isEmpty()) {
            throw ApiException.notFound("Exam session", sessionId);
        }
        ExamAnswer first = rows.get(0);
        caller.checkOrgAccess(first.getOrgId());
        if (!caller.canManage() && !Objects.equals(first.getTakenBy(), caller.email())) {
            throw ApiException.forbidden("Students may only view their own results");
        }
        List<EvaluatedAnswer> evaluated = evaluate(rows.stream()
                .map(r -> new AnswerRef(r.getQuestionId(), r.getSelectedAnswerId())).toList(), caller.token());
        return toResult(sessionId, first.getClassId(), first.getSubjectId(), first.getTakenBy(), first.getTakenAt(),
                evaluated);
    }

    /** Students always get their own sittings; admins see the organisation, optionally filtered. */
    public PageResponse<SessionSummary> listSessions(AuthenticatedUser caller, String orgId, String takenBy,
                                                     Pageable pageable) {
        String org = caller.resolveOrg(orgId);
        String filter = caller.canManage() ? takenBy : caller.email();
        return PageResponse.of(examAnswers.findSessions(org, filter, RecordStatus.ACTIVE, pageable), s -> s);
    }

    private List<EvaluatedAnswer> evaluate(List<AnswerRef> answers, String token) {
        try {
            return questionClient.evaluate(answers, token);
        } catch (ResourceAccessException e) {
            throw ApiException.serviceUnavailable("question-service is unreachable, try again later");
        }
    }

    private static ExamResult toResult(String sessionId, String classId, String subjectId, String takenBy,
                                       LocalDateTime takenAt, List<EvaluatedAnswer> evaluated) {
        List<ResultItem> items = evaluated.stream()
                .map(e -> new ResultItem(e.questionId(), e.answerId(), e.correct(), e.correctAnswerIds()))
                .toList();
        int correct = (int) items.stream().filter(ResultItem::correct).count();
        double percent = items.isEmpty() ? 0 : Math.round(correct * 10000.0 / items.size()) / 100.0;
        return new ExamResult(sessionId, classId, subjectId, takenBy, takenAt, items.size(), correct, percent, items);
    }
}
