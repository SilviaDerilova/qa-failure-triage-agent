package com.silvia.qa.triage;

import com.silvia.qa.evidence.FailureEvidence;
import com.silvia.qa.triage.llm.OllamaClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Requires local Ollama and qa-triage-hermes; enable only for manual validation.")
class HybridTriageOllamaIntegrationTest {

    @Test
    void shouldTriageAmbiguousServerErrorUsingLocalOllama() throws Exception {
        FailureEvidence evidence = new FailureEvidence(
                "ExampleApiTest",
                "shouldReturnSuccessfulResponse",
                "Expected successful API response",
                null,
                "java.lang.AssertionError",
                "Expected HTTP 200 but actual HTTP response was 500.",
                "GET",
                "https://example.test/api/products",
                500,
                "Internal Server Error",
                false,
                null);

        RuleBasedTriageEngine ruleEngine = new RuleBasedTriageEngine();
        assertThat(ruleEngine.triage(evidence).llmRequired()).isTrue();

        // The default client calls localhost:11434/api/generate using qa-triage-hermes.
        HybridTriageService service = new HybridTriageService(ruleEngine, new OllamaClient());
        TriageResult result = service.triage(evidence);

        assertThat(result).isNotNull();
        assertThat(result.classification()).isIn(List.of(TriageClassification.values()));
        assertThat(result.confidence()).isBetween(0, 100);
        assertThat(result.reason()).isNotBlank();
        assertThat(result.suggestedAction()).isNotBlank();
        assertThat(result.llmRequired()).isFalse();
    }
}
