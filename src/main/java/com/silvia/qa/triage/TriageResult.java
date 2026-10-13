package com.silvia.qa.triage;

public record TriageResult(
        TriageClassification classification,
        int confidence,
        String reason,
        String suggestedAction,
        boolean llmRequired
) {
}
