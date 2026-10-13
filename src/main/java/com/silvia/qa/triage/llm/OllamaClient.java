package com.silvia.qa.triage.llm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvia.qa.evidence.FailureEvidence;
import com.silvia.qa.triage.TriageClassification;
import com.silvia.qa.triage.TriageResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/** Synchronous local inference, with injectable HTTP client and endpoint for tests. */
public final class OllamaClient {
    private static final URI ENDPOINT = URI.create("http://localhost:11434/api/generate");
    private static final String MODEL = "qa-triage-hermes";
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final HttpClient httpClient;
    private final URI endpoint;

    public OllamaClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), ENDPOINT);
    }

    public OllamaClient(HttpClient httpClient, URI endpoint) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.endpoint = Objects.requireNonNull(endpoint);
    }

    public TriageResult triage(FailureEvidence evidence) throws IOException, InterruptedException {
        Objects.requireNonNull(evidence, "Failure evidence is required");
        OllamaRequest payload = new OllamaRequest(MODEL, mapper.writeValueAsString(evidence), false, "json");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Ollama returned HTTP " + response.statusCode());
        }
        OllamaResponse generated = mapper.readValue(response.body(), OllamaResponse.class);
        if (generated == null || !generated.done() || generated.response() == null
                || generated.response().isBlank()) {
            throw new IOException("Ollama did not return a completed response");
        }
        return parseResult(generated.response());
    }

    private TriageResult parseResult(String text) throws IOException {
        JsonNode result = mapper.readTree(text);
        if (result == null || !result.isObject() || result.size() != 4
                || !hasText(result, "classification") || !hasText(result, "reason")
                || !hasText(result, "suggestedAction")) {
            throw new IOException("Ollama triage output must contain exactly the four required fields");
        }
        JsonNode confidence = result.get("confidence");
        if (confidence == null || !confidence.isIntegralNumber() || !confidence.canConvertToInt()
                || confidence.intValue() < 0 || confidence.intValue() > 100) {
            throw new IOException("Ollama triage confidence must be an integer from 0 to 100");
        }
        TriageClassification classification;
        try {
            classification = TriageClassification.valueOf(result.get("classification").textValue());
        } catch (IllegalArgumentException invalidClassification) {
            throw new IOException("Ollama returned an invalid triage classification", invalidClassification);
        }
        // The model supplies four policy fields; routing is owned by Java.
        return new TriageResult(classification, confidence.intValue(),
                result.get("reason").textValue(), result.get("suggestedAction").textValue(), false);
    }

    private static boolean hasText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.textValue().isBlank();
    }
}
