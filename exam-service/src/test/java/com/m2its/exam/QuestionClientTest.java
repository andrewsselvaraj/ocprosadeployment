package com.m2its.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.m2its.common.web.ApiException;
import com.m2its.exam.client.QuestionClient;
import com.m2its.exam.client.QuestionClient.AnswerRef;
import com.m2its.exam.client.QuestionClient.EvaluatedAnswer;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/** Exercises the real HTTP client against a throw-away local HTTP server. */
class QuestionClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void forwardsTokenAndParsesResults() throws IOException {
        AtomicReference<String> authHeader = new AtomicReference<>();
        QuestionClient client = clientFor(200, """
                {"results":[{"questionId":"q1","answerId":"a1","correct":true,"correctAnswerIds":["a1"]}]}""",
                authHeader);

        List<EvaluatedAnswer> results = client.evaluate(List.of(new AnswerRef("q1", "a1")), "jwt-123");

        assertThat(authHeader.get()).isEqualTo("Bearer jwt-123");
        assertThat(results).singleElement().satisfies(r -> assertThat(r.correct()).isTrue());
    }

    @Test
    void clientErrorsArePassedOn() throws IOException {
        QuestionClient client = clientFor(404, "{\"detail\":\"Question q9 not found\"}", new AtomicReference<>());

        assertThatThrownBy(() -> client.evaluate(List.of(new AnswerRef("q9", "a1")), "jwt"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private QuestionClient clientFor(int status, String json, AtomicReference<String> authHeader) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/answers/evaluate", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new QuestionClient(RestClient.builder(), baseUrl, Duration.ofSeconds(1), Duration.ofSeconds(2));
    }
}
