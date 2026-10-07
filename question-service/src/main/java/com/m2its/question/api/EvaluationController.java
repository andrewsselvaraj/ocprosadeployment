package com.m2its.question.api;

import static com.m2its.common.security.AuthenticatedUser.current;

import com.m2its.question.api.QuestionDtos.EvaluateRequest;
import com.m2its.question.api.QuestionDtos.EvaluateResponse;
import com.m2its.question.service.QuestionService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API for exam-service. It reveals correct answers, so it is protected
 * three ways: the api-gateway has no route for /internal/** (so it is unreachable from the
 * internet), NetworkPolicies only admit traffic from the gateway and exam-service pods, and
 * the caller's JWT is still required and tenant-checked.
 */
@RestController
@RequestMapping("/internal/answers")
public class EvaluationController {

    private final QuestionService service;

    public EvaluationController(QuestionService service) {
        this.service = service;
    }

    /** Read-only and idempotent, so the caller may safely retry it. */
    @PostMapping("/evaluate")
    public EvaluateResponse evaluate(@Valid @RequestBody EvaluateRequest request) {
        return service.evaluate(current(), request.answers());
    }
}
