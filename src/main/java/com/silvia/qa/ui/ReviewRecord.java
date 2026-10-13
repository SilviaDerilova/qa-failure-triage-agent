package com.silvia.qa.ui;

import com.silvia.qa.triage.TriageClassification;

import java.util.Objects;

public record ReviewRecord(
        String evidenceFileName,
        TriageClassification originalClassification,
        TriageClassification finalClassification,
        int confidence,
        String reason,
        String suggestedAction,
        String qaComment,
        boolean approved,
        String reviewedAt
) {
    public ReviewRecord {
        Objects.requireNonNull(originalClassification, "Original classification is required");
        Objects.requireNonNull(finalClassification, "Final classification is required");
        // Approval reflects the saved choice, including changes made after clicking Approve.
        approved = originalClassification == finalClassification;
    }
}
