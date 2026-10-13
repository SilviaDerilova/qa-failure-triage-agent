package com.silvia.qa.evidence;

/** Response time is in milliseconds; absent responses have null data and a false truncation flag. */
public record FailureEvidence(
        String testClass,
        String testMethod,
        String displayName,
        String timestamp,
        String exceptionType,
        String exceptionMessage,
        String httpMethod,
        String requestUrl,
        Integer responseStatusCode,
        String responseBody,
        boolean responseBodyTruncated,
        Long responseTimeMs
) {
}
