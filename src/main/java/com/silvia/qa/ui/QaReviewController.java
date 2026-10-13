package com.silvia.qa.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvia.qa.evidence.FailureEvidence;
import com.silvia.qa.triage.HybridTriageService;
import com.silvia.qa.triage.TriageClassification;
import com.silvia.qa.triage.TriageResult;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;

public final class QaReviewController {
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final HybridTriageService service = new HybridTriageService();
    private final ReviewRecordWriter writer = new ReviewRecordWriter();
    private final Button open = new Button("Open Evidence File");
    private final Button approve = new Button("Approve Suggestion");
    private final Button save = new Button("Save Review");
    private final Label status = new Label("Open a failure evidence JSON file to begin.");
    private final TextArea evidenceDetails = readOnlyArea(12);
    private final TextArea suggestionDetails = readOnlyArea(6);
    private final ComboBox<TriageClassification> finalClassification = new ComboBox<>();
    private final TextArea comment = new TextArea();
    private Path evidenceFile;
    private TriageResult suggestion;
    private Task<TriageResult> runningTask;

    public Parent createView(Window owner) {
        finalClassification.getItems().setAll(TriageClassification.values());
        comment.setPromptText("Add a QA comment");
        comment.setPrefRowCount(4);
        setReviewEnabled(false);
        open.setOnAction(event -> openEvidence(owner));
        approve.setOnAction(event -> {
            finalClassification.setValue(suggestion.classification());
            status.setText("Suggestion selected. Save Review to record your decision.");
        });
        save.setOnAction(event -> saveReview());

        VBox content = new VBox(10, open, status,
                new Label("Failure evidence"), evidenceDetails,
                new Label("Triage suggestion"), suggestionDetails,
                new Label("Final classification"), finalClassification,
                new Label("QA comment"), comment, approve, save);
        content.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void openEvidence(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Failure Evidence");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File selected = chooser.showOpenDialog(owner);
        if (selected == null) {
            return;
        }
        // Clear the previous decision so a failed load cannot save a stale review.
        suggestion = null;
        evidenceFile = null;
        evidenceDetails.clear();
        suggestionDetails.clear();
        finalClassification.setValue(null);
        comment.clear();
        setReviewEnabled(false);
        try {
            FailureEvidence evidence = mapper.readValue(selected, FailureEvidence.class);
            if (evidence == null || evidence.testClass() == null || evidence.testMethod() == null
                    || evidence.exceptionMessage() == null) {
                throw new IllegalArgumentException("Evidence must include test class, method, and exception message.");
            }
            evidenceFile = selected.toPath();
            evidenceDetails.setText(formatEvidence(evidence));
            runTriage(evidence);
        } catch (Exception failure) {
            showFailure("Could not open evidence", failure);
        }
    }

    private void runTriage(FailureEvidence evidence) {
        open.setDisable(true);
        status.setText("Running triage...");
        runningTask = new Task<>() {
            @Override
            protected TriageResult call() throws Exception {
                return service.triage(evidence);
            }
        };
        runningTask.setOnSucceeded(event -> {
            suggestion = runningTask.getValue();
            suggestionDetails.setText("Suggested classification: " + suggestion.classification()
                    + "\nConfidence: " + suggestion.confidence() + "/100"
                    + "\nReason: " + suggestion.reason()
                    + "\nSuggested action: " + suggestion.suggestedAction());
            finalClassification.setValue(suggestion.classification());
            setReviewEnabled(true);
            open.setDisable(false);
            status.setText("Review the suggestion, then save your decision.");
        });
        runningTask.setOnFailed(event -> {
            open.setDisable(false);
            showFailure("Triage failed", runningTask.getException());
        });
        Thread worker = new Thread(runningTask, "qa-review-triage");
        worker.setDaemon(true);
        worker.start();
    }

    private void saveReview() {
        if (suggestion == null || evidenceFile == null || finalClassification.getValue() == null) {
            status.setText("Select a final classification before saving.");
            return;
        }
        ReviewRecord review = new ReviewRecord(evidenceFile.getFileName().toString(),
                suggestion.classification(), finalClassification.getValue(),
                suggestion.confidence(), suggestion.reason(), suggestion.suggestedAction(),
                comment.getText(), finalClassification.getValue() == suggestion.classification(),
                Instant.now().toString());
        try {
            Path saved = writer.write(review);
            status.setText("Review saved: " + saved.toAbsolutePath());
        } catch (Exception failure) {
            showFailure("Could not save review", failure);
        }
    }

    private void setReviewEnabled(boolean enabled) {
        approve.setDisable(!enabled);
        save.setDisable(!enabled);
        finalClassification.setDisable(!enabled);
        comment.setDisable(!enabled);
    }

    private static String formatEvidence(FailureEvidence evidence) {
        String body = value(evidence.responseBody());
        boolean excerpt = body.length() > 3000 || evidence.responseBodyTruncated();
        if (body.length() > 3000) {
            body = body.substring(0, 3000);
        }
        return "Test class: " + value(evidence.testClass())
                + "\nTest method: " + value(evidence.testMethod())
                + "\nDisplay name: " + value(evidence.displayName())
                + "\nException message: " + value(evidence.exceptionMessage())
                + "\nHTTP method: " + value(evidence.httpMethod())
                + "\nRequest URL: " + value(evidence.requestUrl())
                + "\nResponse status: " + value(evidence.responseStatusCode())
                + "\nResponse body excerpt:\n" + body + (excerpt ? "\n[truncated]" : "");
    }

    private static String value(Object value) {
        return value == null ? "(not provided)" : value.toString();
    }

    private static TextArea readOnlyArea(int rows) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(rows);
        return area;
    }

    private void showFailure(String title, Throwable failure) {
        status.setText(title + ": " + value(failure.getMessage()));
    }

    public void close() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }
}
