package com.silvia.qa.triage;

import com.silvia.qa.evidence.FailureEvidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.silvia.qa.triage.TriageClassification.*;
import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedTriageEngineTest {
    private final RuleBasedTriageEngine engine = new RuleBasedTriageEngine();

    @Test
    void missingRequiredFixtureValueIsTestDataIssue() {
        TriageResult result = engine.triage(evidence(
                "Required test data 'search_product' is missing; scenario cannot continue.",
                200, "{\"responseCode\":400,\"message\":\"search_product parameter is missing\"}", false));

        assertThat(result).isEqualTo(new TriageResult(TEST_DATA_ISSUE, 95,
                "The failure explicitly identifies missing required test input.",
                "Supply the missing required value in the test fixture.", false));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "The automated test contained the wrong expected status code: expected 201, actual 200.",
            "Response must contain the intentionally nonexistent demo field"
    })
    void explicitIncorrectExpectationWithSuccessfulResponseIsAutomationDefect(String message) {
        TriageResult result = engine.triage(evidence(message, 200,
                "{\"responseCode\":200,\"products\":[]}", false));

        assertThat(result.classification()).isEqualTo(AUTOMATION_DEFECT);
        assertThat(result.confidence()).isEqualTo(95);
        assertThat(result.llmRequired()).isFalse();
        assertThat(result.reason()).contains("incorrect test expectation");
        assertThat(result.suggestedAction()).contains("documented API contract");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Internal Server Error",
            "{\"responseCode\":500,\"message\":\"Internal Server Error\"}"
    })
    void genericServerErrorWithExpectedSuccessRemainsAmbiguous(String body) {
        TriageResult result = engine.triage(evidence("expected: 200 but was: 500", 500, body, false));

        assertThat(result.classification()).isEqualTo(UNKNOWN);
        assertThat(result.confidence()).isEqualTo(55);
        assertThat(result.llmRequired()).isTrue();
        assertThat(result.reason()).contains("does not identify");
        assertThat(result.suggestedAction()).isEqualTo("Retrieve server diagnostics for the failed request.");
    }

    @Test
    void bareStatusMismatchDoesNotProveTheTestExpectationIsWrong() {
        assertFallback(evidence("expected: 201 but was: 200", 200, "{\"responseCode\":200}", false));
    }

    @Test
    void missingInputRejectionAloneDoesNotProveAFixtureProblem() {
        assertFallback(evidence("expected: 200 but was: 400", 200,
                "{\"responseCode\":400,\"message\":\"search_product parameter is missing\"}", false));
    }

    @Test
    void applicationErrorContradictsSuccessfulHttpStatus() {
        assertFallback(evidence("wrong expected status code", 200, "{\"responseCode\":400}", false));
    }

    @Test
    void serverErrorConflictsWithMissingInputExplanation() {
        assertFallback(evidence("Required test input 'search_product' is missing", 500,
                "Internal Server Error", false));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Dependency connection timed out",
            "{\"responseCode\":500,\"message\":\"Internal Server Error\",\"detail\":\"Database unavailable\"}",
            "<html>Internal Server Error</html>"
    })
    void nonGenericServerResponsesAreLeftForFurtherReview(String body) {
        assertFallback(evidence("expected: 200 but was: 500", 500, body, false));
    }

    @Test
    void truncatedResponseCannotEstablishSuccessOrGenericError() {
        assertFallback(evidence("wrong expected status code", 200, "{\"responseCode\":200}", true));
        assertFallback(evidence("expected: 200 but was: 500", 500, "Internal Server Error", true));
    }

    @Test
    void insufficientEvidenceAndNamesContainingLabelsDoNotTriggerRules() {
        assertFallback(null);
        assertFallback(evidence(null, null, null, false));
        assertFallback(evidence("AUTOMATION_DEFECT", 200, "not JSON", false));
    }

    private void assertFallback(FailureEvidence evidence) {
        TriageResult result = engine.triage(evidence);
        assertThat(result.classification()).isEqualTo(UNKNOWN);
        assertThat(result.confidence()).isEqualTo(30);
        assertThat(result.llmRequired()).isTrue();
    }

    private static FailureEvidence evidence(String message, Integer status, String body, boolean truncated) {
        return new FailureEvidence("TestDataIssueFailureDemoTest", "test", "AUTOMATION_DEFECT",
                null, "java.lang.AssertionError", message, "POST", "/api/searchProduct",
                status, body, truncated, 100L);
    }
}
