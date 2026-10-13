package com.silvia.qa.triage;

import com.silvia.qa.evidence.FailureEvidence;
import com.silvia.qa.triage.llm.OllamaClient;

import java.io.IOException;
import java.util.Objects;

public final class HybridTriageService {
    private final RuleBasedTriageEngine ruleEngine;
    private final OllamaClient ollamaClient;

    public HybridTriageService() {
        this(new RuleBasedTriageEngine(), new OllamaClient());
    }

    public HybridTriageService(RuleBasedTriageEngine ruleEngine, OllamaClient ollamaClient) {
        this.ruleEngine = Objects.requireNonNull(ruleEngine);
        this.ollamaClient = Objects.requireNonNull(ollamaClient);
    }

    public TriageResult triage(FailureEvidence evidence) throws IOException, InterruptedException {
        Objects.requireNonNull(evidence, "Failure evidence is required");
        TriageResult result = ruleEngine.triage(evidence);
        return result.llmRequired() ? ollamaClient.triage(evidence) : result;
    }
}
