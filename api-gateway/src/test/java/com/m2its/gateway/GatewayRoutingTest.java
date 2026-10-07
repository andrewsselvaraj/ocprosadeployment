package com.m2its.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the gateway on a random port with every route pointing at a stub backend that
 * echoes "<service>:<path>", then checks which service each URL reaches.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final HttpServer IDENTITY = stub("identity");
    private static final HttpServer ACADEMIC = stub("academic");
    private static final HttpServer QUESTION = stub("question");
    private static final HttpServer EXAM = stub("exam");

    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    Environment environment;

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        registry.add("IDENTITY_SERVICE_URL", () -> url(IDENTITY));
        registry.add("ACADEMIC_SERVICE_URL", () -> url(ACADEMIC));
        registry.add("QUESTION_SERVICE_URL", () -> url(QUESTION));
        registry.add("EXAM_SERVICE_URL", () -> url(EXAM));
    }

    @AfterAll
    static void stopStubs() {
        for (HttpServer s : new HttpServer[] {IDENTITY, ACADEMIC, QUESTION, EXAM}) {
            s.stop(0);
        }
    }

    @Test
    void routesEachPrefixToItsService() throws Exception {
        assertThat(call("/api/auth/login").body()).isEqualTo("identity:/api/auth/login");
        assertThat(call("/api/contact").body()).isEqualTo("identity:/api/contact");
        assertThat(call("/api/classes/c1/tree").body()).isEqualTo("academic:/api/classes/c1/tree");
        assertThat(call("/api/chapters/x").body()).isEqualTo("academic:/api/chapters/x");
        assertThat(call("/api/questions?subjectId=s1").body()).isEqualTo("question:/api/questions");
        assertThat(call("/api/exams/sessions").body()).isEqualTo("exam:/api/exams/sessions");
    }

    @Test
    void internalApisAreNotExposed() throws Exception {
        assertThat(call("/internal/answers/evaluate").statusCode()).isEqualTo(404);
    }

    @Test
    void healthIsUp() throws Exception {
        assertThat(call("/actuator/health/readiness").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> call(String path) throws Exception {
        String port = environment.getProperty("local.server.port");
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer stub(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = (name + ":" + exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String url(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
