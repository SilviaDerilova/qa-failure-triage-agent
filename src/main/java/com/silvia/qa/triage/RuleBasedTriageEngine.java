package com.silvia.qa.triage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvia.qa.evidence.FailureEvidence;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.silvia.qa.triage.TriageClassification.*;

/** Small evidence rules; unmatched cases remain available for later LLM review. */
public final class RuleBasedTriageEngine {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern MISSING_INPUT = Pattern.compile(
            "required test (?:data|input)\\s+[^;\\r\\n]+\\s+is missing\\b");
    private static final Pattern EXPECTED_SUCCESS = Pattern.compile(
            "expected(?: http(?: status)?)?\\s*:?\\s*(?:<|\\[)?2\\d{2}\\b");

    public TriageResult triage(FailureEvidence evidence) {
        if (evidence == null) {
            return unknown();
        }
        String message = normalize(evidence.exceptionMessage());
        Integer status = evidence.responseStatusCode();
        // A server error conflicts with the simple missing-input explanation.
        if (MISSING_INPUT.matcher(message).find() && !isServerError(status)) {
            return new TriageResult(TEST_DATA_ISSUE, 95,
                    "The failure explicitly identifies missing required test input.",
                    "Supply the missing required value in the test fixture.", false);
        }

        boolean assertion = evidence.exceptionType() != null
                && evidence.exceptionType().endsWith("AssertionError");
        if (assertion && status != null && status >= 200 && status < 300
                && !evidence.responseBodyTruncated() && hasSuccessfulApplicationResponse(evidence.responseBody())) {
            boolean wrongExpectation = message.contains("wrong expected status code")
                    || message.contains("wrong expected result");
            boolean nonexistentField = message.contains("intentionally nonexistent")
                    && message.contains("field");
            if (wrongExpectation || nonexistentField) {
                return new TriageResult(AUTOMATION_DEFECT, 95,
                        "The successful API response conflicts with an explicitly incorrect test expectation.",
                        "Correct the assertion to match the documented API contract.", false);
            }
        }

        if (assertion && isServerError(status) && EXPECTED_SUCCESS.matcher(message).find()
                && !evidence.responseBodyTruncated() && isGenericServerError(evidence.responseBody())) {
            return new TriageResult(UNKNOWN, 55,
                    "A generic HTTP 5xx response does not identify an application or environment cause.",
                    "Retrieve server diagnostics for the failed request.", true);
        }
        return unknown();
    }

    private static boolean hasSuccessfulApplicationResponse(String body) {
        try {
            JsonNode root = JSON.readTree(body == null ? "" : body);
            if (root == null || !root.isObject()) {
                return false;
            }
            JsonNode code = root.get("responseCode");
            return code != null && code.isIntegralNumber() && code.canConvertToInt()
                    && code.intValue() >= 200 && code.intValue() < 300;
        } catch (Exception invalidJson) {
            return false;
        }
    }

    // Exact generic bodies prevent a diagnostic detail from being mistaken for a generic error.
    private static boolean isGenericServerError(String body) {
        String text = normalize(body).trim();
        if (text.equals("internal server error") || text.equals("server error")) {
            return true;
        }
        try {
            JsonNode root = JSON.readTree(body == null ? "" : body);
            if (root == null || !root.isObject() || root.size() != 2) {
                return false;
            }
            JsonNode code = root.get("responseCode");
            JsonNode message = root.get("message");
            return code != null && code.isIntegralNumber() && code.canConvertToInt()
                    && isServerError(code.intValue()) && message != null && message.isTextual()
                    && (normalize(message.textValue()).equals("internal server error")
                    || normalize(message.textValue()).equals("server error"));
        } catch (Exception invalidJson) {
            return false;
        }
    }

    private static boolean isServerError(Integer status) {
        return status != null && status >= 500 && status < 600;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static TriageResult unknown() {
        return new TriageResult(UNKNOWN, 30,
                "The evidence does not match a rule that reliably identifies the cause.",
                "Compare the failure evidence with the documented scenario requirements.", true);
    }
}
