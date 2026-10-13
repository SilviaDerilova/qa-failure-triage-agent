package com.silvia.qa.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvia.qa.triage.TriageClassification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static com.silvia.qa.triage.TriageClassification.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewRecordWriterTest {
    @TempDir
    Path temporaryDirectory;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void writesAllReviewFieldsAndCreatesDirectory() throws Exception {
        Path directory = temporaryDirectory.resolve("qa-reviews");
        ReviewRecord review = review(UNKNOWN, false);
        Path file = new ReviewRecordWriter(directory).write(review);

        assertThat(file.getParent()).isEqualTo(directory);
        assertThat(file.getFileName().toString()).startsWith("review-").endsWith(".json");
        JsonNode json = mapper.readTree(file.toFile());
        assertThat(json.size()).isEqualTo(9);
        assertThat(json.get("evidenceFileName").textValue()).isEqualTo("failure-example.json");
        assertThat(json.get("originalClassification").textValue()).isEqualTo("UNKNOWN");
        assertThat(json.get("finalClassification").textValue()).isEqualTo("UNKNOWN");
        assertThat(json.get("confidence").intValue()).isEqualTo(55);
        assertThat(json.get("reason").textValue()).isEqualTo("The cause is ambiguous.");
        assertThat(json.get("suggestedAction").textValue()).isEqualTo("Retrieve server diagnostics.");
        assertThat(json.get("qaComment").textValue()).isEqualTo("QA reviewed: \"needs investigation\"\nFollow up.");
        assertThat(json.get("approved").booleanValue()).isTrue();
        assertThat(Instant.parse(json.get("reviewedAt").textValue()))
                .isEqualTo(Instant.parse("2026-10-06T12:00:00Z"));
        assertThat(mapper.readValue(file.toFile(), ReviewRecord.class)).isEqualTo(review);
    }

    @Test
    void changedClassificationIsNotApprovedEvenIfApprovedWasPassedAsTrue() throws Exception {
        Path file = new ReviewRecordWriter(temporaryDirectory).write(review(ENVIRONMENT_ISSUE, true));
        JsonNode json = mapper.readTree(file.toFile());

        assertThat(json.get("originalClassification").textValue()).isEqualTo("UNKNOWN");
        assertThat(json.get("finalClassification").textValue()).isEqualTo("ENVIRONMENT_ISSUE");
        assertThat(json.get("approved").booleanValue()).isFalse();
    }

    @Test
    void repeatedReviewsProduceSeparateFilesWithoutOverwriting() throws Exception {
        ReviewRecordWriter writer = new ReviewRecordWriter(temporaryDirectory);
        ReviewRecord original = review(UNKNOWN, true);
        Path first = writer.write(original);
        Path second = writer.write(review(PRODUCT_DEFECT, false));

        assertThat(first).isNotEqualTo(second);
        assertThat(mapper.readValue(first.toFile(), ReviewRecord.class)).isEqualTo(original);
        assertThat(mapper.readValue(second.toFile(), ReviewRecord.class).finalClassification())
                .isEqualTo(PRODUCT_DEFECT);
    }

    @Test
    void reportsAnUnwritableOutputLocation() throws Exception {
        Path file = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(file, "preserve this file");

        assertThatThrownBy(() -> new ReviewRecordWriter(file).write(review(UNKNOWN, true)))
                .isInstanceOf(IOException.class);
        assertThat(Files.readString(file)).isEqualTo("preserve this file");
    }

    private static ReviewRecord review(TriageClassification finalClassification, boolean approved) {
        return new ReviewRecord("failure-example.json", UNKNOWN, finalClassification, 55,
                "The cause is ambiguous.", "Retrieve server diagnostics.",
                "QA reviewed: \"needs investigation\"\nFollow up.", approved, "2026-10-06T12:00:00Z");
    }
}
