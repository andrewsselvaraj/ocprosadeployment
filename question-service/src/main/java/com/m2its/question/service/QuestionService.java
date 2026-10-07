package com.m2its.question.service;

import static com.m2its.common.jpa.RecordStatus.ACTIVE;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.m2its.common.jpa.Ids;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.web.ApiException;
import com.m2its.common.web.PageResponse;
import com.m2its.question.api.QuestionDtos.AnswerRef;
import com.m2its.question.api.QuestionDtos.CreateQuestionRequest;
import com.m2its.question.api.QuestionDtos.EvaluateResponse;
import com.m2its.question.api.QuestionDtos.EvaluatedAnswer;
import com.m2its.question.api.QuestionDtos.OptionRequest;
import com.m2its.question.api.QuestionDtos.QuestionView;
import com.m2its.question.api.QuestionDtos.UpdateQuestionRequest;
import com.m2its.question.domain.Question;
import com.m2its.question.domain.QuestionRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questions;

    public QuestionService(QuestionRepository questions) {
        this.questions = questions;
    }

    public PageResponse<QuestionView> search(AuthenticatedUser caller, String orgId, String subjectId,
                                             String chapterId, Pageable pageable) {
        String org = caller.resolveOrg(orgId);
        boolean reveal = caller.canEditContent();
        return PageResponse.of(questions.search(org, subjectId, chapterId, ACTIVE, pageable),
                q -> QuestionView.from(q, reveal));
    }

    public QuestionView get(AuthenticatedUser caller, String id) {
        return QuestionView.from(findActive(caller, id), caller.canEditContent());
    }

    @Transactional
    public QuestionView create(AuthenticatedUser caller, CreateQuestionRequest request) {
        validateOptions(request.options());
        Question q = new Question(Ids.newId(), caller.resolveOrg(request.orgId()), request.classId(),
                request.subjectId(), request.chapterId(), request.text(), request.type(), caller.email());
        request.options().forEach(o -> q.addOption(Ids.newId(), o.text(), o.correct()));
        return QuestionView.from(questions.save(q), true);
    }

    /** Replaces the text and options. Old options are retired, not deleted (exam history). */
    @Transactional
    public QuestionView update(AuthenticatedUser caller, String id, UpdateQuestionRequest request) {
        validateOptions(request.options());
        Question q = findActive(caller, id);
        q.setText(request.text());
        q.setType(request.type());
        q.setChapterId(request.chapterId());
        q.setUpdatedBy(caller.email());
        q.retireOptions();
        request.options().forEach(o -> q.addOption(Ids.newId(), o.text(), o.correct()));
        return QuestionView.from(q, true);
    }

    @Transactional
    public void delete(AuthenticatedUser caller, String id) {
        Question q = findActive(caller, id);
        q.setStatus(RecordStatus.DELETED);
        q.setUpdatedBy(caller.email());
    }

    /**
     * Scores submitted answers. Includes retired options and deleted questions on purpose, so
     * re-scoring an old exam gives the same result it had when it was taken.
     */
    public EvaluateResponse evaluate(AuthenticatedUser caller, List<AnswerRef> answers) {
        List<String> ids = answers.stream().map(AnswerRef::questionId).distinct().toList();
        Map<String, Question> byId = questions.findWithOptionsByIdIn(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<EvaluatedAnswer> results = answers.stream().map(a -> {
            Question q = byId.get(a.questionId());
            if (q == null) {
                throw ApiException.notFound("Question", a.questionId());
            }
            caller.checkOrgAccess(q.getOrgId());
            return EvaluatedAnswer.of(q, a.answerId());
        }).toList();
        return new EvaluateResponse(results);
    }

    private Question findActive(AuthenticatedUser caller, String id) {
        Question q = questions.findByIdAndStatus(id, ACTIVE).orElseThrow(() -> ApiException.notFound("Question", id));
        caller.checkOrgAccess(q.getOrgId());
        return q;
    }

    private static void validateOptions(List<OptionRequest> options) {
        if (options.stream().noneMatch(OptionRequest::correct)) {
            throw ApiException.badRequest("At least one option must be marked correct");
        }
    }
}
