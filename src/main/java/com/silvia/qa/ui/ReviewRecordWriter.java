package com.silvia.qa.ui;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ReviewRecordWriter {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path directory;

    public ReviewRecordWriter() {
        this(Path.of("target", "qa-reviews"));
    }

    public ReviewRecordWriter(Path directory) {
        this.directory = Objects.requireNonNull(directory);
    }

    public Path write(ReviewRecord review) throws IOException {
        Objects.requireNonNull(review);
        Files.createDirectories(directory);
        Path file = Files.createTempFile(directory, "review-", ".json");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), review);
            return file;
        } catch (IOException failure) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }
}
