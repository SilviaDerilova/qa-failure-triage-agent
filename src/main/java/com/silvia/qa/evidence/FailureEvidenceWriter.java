package com.silvia.qa.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FailureEvidenceWriter {
    private final ObjectMapper mapper = new ObjectMapper();

    public Path write(FailureEvidence evidence) throws IOException {
        Path directory = Path.of("target", "failure-evidence");
        Files.createDirectories(directory);
        Path file = Files.createTempFile(directory, "failure-", ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), evidence);
        return file;
    }
}
