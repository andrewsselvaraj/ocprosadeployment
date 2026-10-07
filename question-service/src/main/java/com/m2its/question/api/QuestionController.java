package com.m2its.question.api;

import static com.m2its.common.security.AuthenticatedUser.current;

import com.m2its.common.security.Roles;
import com.m2its.common.web.PageResponse;
import com.m2its.question.api.QuestionDtos.CreateQuestionRequest;
import com.m2its.question.api.QuestionDtos.QuestionView;
import com.m2its.question.api.QuestionDtos.UpdateQuestionRequest;
import com.m2its.question.service.QuestionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<QuestionView> search(@RequestParam String subjectId,
                                             @RequestParam(required = false) String chapterId,
                                             @RequestParam(required = false) String orgId,
                                             @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
                                             Pageable pageable) {
        return service.search(current(), orgId, subjectId, chapterId, pageable);
    }

    @GetMapping("/{id}")
    public QuestionView get(@PathVariable String id) {
        return service.get(current(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public QuestionView create(@Valid @RequestBody CreateQuestionRequest request) {
        return service.create(current(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public QuestionView update(@PathVariable String id, @Valid @RequestBody UpdateQuestionRequest request) {
        return service.update(current(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(current(), id);
    }
}
