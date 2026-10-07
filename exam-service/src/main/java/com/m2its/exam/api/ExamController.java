package com.m2its.exam.api;

import static com.m2its.common.security.AuthenticatedUser.current;

import com.m2its.common.web.PageResponse;
import com.m2its.exam.api.ExamDtos.ExamResult;
import com.m2its.exam.api.ExamDtos.SubmitExamRequest;
import com.m2its.exam.domain.SessionSummary;
import com.m2its.exam.service.ExamService;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResult submit(@Valid @RequestBody SubmitExamRequest request) {
        return service.submit(current(), request);
    }

    @GetMapping("/sessions")
    public PageResponse<SessionSummary> sessions(@RequestParam(required = false) String orgId,
                                                 @RequestParam(required = false) String takenBy,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        // Sorting is fixed by the query (newest first), so only page/size are accepted
        return service.listSessions(current(), orgId, takenBy, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/sessions/{sessionId}")
    public ExamResult result(@PathVariable String sessionId) {
        return service.getResult(current(), sessionId);
    }
}
