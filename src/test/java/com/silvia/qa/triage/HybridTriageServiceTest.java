package com.silvia.qa.triage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvia.qa.evidence.FailureEvidence;
import com.silvia.qa.triage.llm.OllamaClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridTriageServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> responseBody = new AtomicReference<>();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private HttpServer server;
    private HybridTriageService service;

    @BeforeEach
    void startStubOllama() throws IOException {
        responseBody.set(envelope(validModelOutput(), true));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/generate", exchange -> {
            try {
                requests.incrementAndGet();
                requestMethod.set(exchange.getRequestMethod());
                contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus.get(), body.length);
                exchange.getResponseBody().write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/generate");
        service = new HybridTriageService(new RuleBasedTriageEngine(),
                new OllamaClient(HttpClient.newHttpClient(), endpoint));
    }

    @AfterEach
    void stopStubOllama() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void clearMissingInputReturnsRuleResultWithoutCallingOllama() throws Exception {
        FailureEvidence evidence = evidence(
                "Required test data 'search_product' is missing; scenario cannot continue.",
                200, "{\"responseCode\":400}");

        assertThat(service.triage(evidence)).isEqualTo(new RuleBasedTriageEngine().triage(evidence));
        assertThat(requests.get()).isZero();
    }

    @Test
    void ambiguousServerErrorCallsOllamaAndReturnsParsedModelResult() throws Exception {
        FailureEvidence evidence = ambiguousEvidence();
        TriageResult result = service.triage(evidence);

        assertThat(requests.get()).isEqualTo(1);
        assertThat(requestMethod.get()).isEqualTo("POST");
        assertThat(contentType.get()).isEqualTo("application/json");
        JsonNode payload = mapper.readTree(requestBody.get());
        assertThat(payload.size()).isEqualTo(4);
        assertThat(payload.get("model").textValue()).isEqualTo("qa-triage-hermes");
        assertThat(payload.get("stream").booleanValue()).isFalse();
        assertThat(payload.get("format").textValue()).isEqualTo("json");
        assertThat(mapper.readValue(payload.get("prompt").textValue(), FailureEvidence.class)).isEqualTo(evidence);
        assertThat(result).isEqualTo(new TriageResult(TriageClassification.UNKNOWN, 60,
                "The generic 5xx response does not identify the cause.",
                "Retrieve server diagnostics for the failed request.", false));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not JSON",
            "{\"classification\":\"OTHER\",\"confidence\":55,\"reason\":\"Ambiguous\",\"suggestedAction\":\"Inspect diagnostics\"}",
            "{\"classification\":\"UNKNOWN\",\"confidence\":101,\"reason\":\"Ambiguous\",\"suggestedAction\":\"Inspect diagnostics\"}",
            "{\"classification\":\"UNKNOWN\",\"confidence\":55.5,\"reason\":\"Ambiguous\",\"suggestedAction\":\"Inspect diagnostics\"}",
            "{\"classification\":\"UNKNOWN\",\"reason\":\"Ambiguous\",\"suggestedAction\":\"Inspect diagnostics\"}",
            "{\"classification\":\"UNKNOWN\",\"confidence\":55,\"reason\":\"\",\"suggestedAction\":\"Inspect diagnostics\"}",
            "{\"classification\":\"UNKNOWN\",\"confidence\":55,\"reason\":\"Ambiguous\",\"suggestedAction\":\"Inspect diagnostics\",\"llmRequired\":true}"
    })
    void malformedModelOutputIsRejected(String output) throws Exception {
        responseBody.set(envelope(output, true));
        assertThatThrownBy(() -> service.triage(ambiguousEvidence())).isInstanceOf(IOException.class);
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void incompleteGenerationIsRejected() throws Exception {
        responseBody.set(envelope(validModelOutput(), false));
        assertThatThrownBy(() -> service.triage(ambiguousEvidence()))
                .isInstanceOf(IOException.class).hasMessageContaining("completed response");
    }

    @Test
    void httpFailureIsReportedWithoutRetry() {
        responseStatus.set(503);
        responseBody.set("{\"error\":\"model unavailable\"}");
        assertThatThrownBy(() -> service.triage(ambiguousEvidence()))
                .isInstanceOf(IOException.class).hasMessage("Ollama returned HTTP 503");
        assertThat(requests.get()).isEqualTo(1);
    }

    private String envelope(String output, boolean done) throws IOException {
        return mapper.writeValueAsString(Map.of("model", "qa-triage-hermes", "response", output,
                "done", done, "total_duration", 123));
    }

    private static String validModelOutput() {
        return """
                {"classification":"UNKNOWN","confidence":60,
                 "reason":"The generic 5xx response does not identify the cause.",
                 "suggestedAction":"Retrieve server diagnostics for the failed request."}
                """;
    }

    private static FailureEvidence ambiguousEvidence() {
        return evidence("expected: 200 but was: 500", 500, "Internal Server Error");
    }

    private static FailureEvidence evidence(String message, int status, String body) {
        return new FailureEvidence("ExampleTest", "search", "Search succeeds", null,
                "java.lang.AssertionError", message, "POST",
                "https://automationexercise.com/api/searchProduct", status, body, false, 100L);
    }
}
