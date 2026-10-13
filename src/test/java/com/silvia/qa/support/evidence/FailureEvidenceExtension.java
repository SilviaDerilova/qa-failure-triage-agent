package com.silvia.qa.support.evidence;

import com.silvia.qa.evidence.FailureEvidence;
import com.silvia.qa.evidence.FailureEvidenceWriter;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.time.Instant;

/** Register as an instance field and attach filter() to each API request. */
public final class FailureEvidenceExtension implements BeforeEachCallback, TestWatcher {
    private final ApiEvidenceFilter filter = new ApiEvidenceFilter();
    private final FailureEvidenceWriter writer = new FailureEvidenceWriter();

    public ApiEvidenceFilter filter() {
        return filter;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        filter.reset();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        try {
            writer.write(new FailureEvidence(
                    context.getRequiredTestClass().getName(),
                    context.getRequiredTestMethod().getName(),
                    context.getDisplayName(),
                    Instant.now().toString(),
                    cause.getClass().getName(),
                    cause.getMessage(),
                    filter.httpMethod,
                    filter.requestUrl,
                    filter.responseStatusCode,
                    filter.responseBody,
                    filter.responseBodyTruncated,
                    filter.responseTimeMs));
        } catch (Exception writingFailure) {
            // Preserve the original test failure if evidence cannot be saved.
            cause.addSuppressed(writingFailure);
        } finally {
            filter.reset();
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        filter.reset();
    }
}
