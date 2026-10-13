package com.silvia.qa.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class QaReviewApplication extends Application {
    private QaReviewController controller;

    @Override
    public void start(Stage stage) {
        controller = new QaReviewController();
        stage.setTitle("QA Failure Review");
        stage.setScene(new Scene(controller.createView(stage), 900, 850));
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
