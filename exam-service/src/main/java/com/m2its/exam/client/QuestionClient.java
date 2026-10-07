package com.m2its.exam.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import com.m2its.common.web.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for question-service's internal evaluation API.
 *
 * - Base URL comes from config: http://localhost:8083 locally, http://question-service:8080
 *   inside the cluster (Kubernetes Service DNS name).
 * - The caller's JWT is forwarded ("token relay") so question-service applies the same
 *   tenant checks.
 * - Explicit timeouts: a slow dependency must not tie up all our request threads.
 * - Network errors are retried (the call is read-only, hence safe to repeat); HTTP error
 *   responses are not.
 */
@Component
public class QuestionClient {

    private static final Logger log = LoggerFactory.getLogger(QuestionClient.class);

    private final RestClient restClient;

    public QuestionClient(RestClient.Builder builder,
                          @Value("${m2its.question-service.url}") String baseUrl,
                          @Value("${m2its.question-service.connect-timeout:2s}") Duration connectTimeout,
                          @Value("${m2its.question-service.read-timeout:5s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Retryable(includes = ResourceAccessException.class, maxRetries = 2, delay = 200, multiplier = 2)
    public List<EvaluatedAnswer> evaluate(List<AnswerRef> answers, String bearerToken) {
        try {
            EvaluateResponse response = restClient.post()
                    .uri("/internal/answers/evaluate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .body(new EvaluateRequest(answers))
                    .retrieve()
                    .body(EvaluateResponse.class);
            return response == null ? List.of() : response.results();
        } catch (HttpStatusCodeException e) {
            log.warn("question-service answered {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is4xxClientError()) {
                // e.g. unknown question id (404) or another organisation's question (403)
                throw new ApiException(HttpStatus.valueOf(e.getStatusCode().value()),
                        "Answers rejected by question-service");
            }
            throw ApiException.serviceUnavailable("question-service failed, try again later");
        }
    }

    public record AnswerRef(String questionId, String answerId) {
    }

    public record EvaluateRequest(List<AnswerRef> answers) {
    }

    public record EvaluatedAnswer(String questionId, String answerId, boolean correct, List<String> correctAnswerIds) {
    }

    public record EvaluateResponse(List<EvaluatedAnswer> results) {
    }
}
