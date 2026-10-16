package com.liceo.campustrack;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;

/**
 * CampusTrack as a desktop app: starts the Spring Boot server inside this process and
 * shows its pages in a JavaFX WebView window. Phones can still connect over HTTPS while
 * the window is open.
 */
public class DesktopAppLauncher extends Application {

    private ConfigurableApplicationContext springContext;
    private Throwable startupError;
    private Stage stage;

    @Override
    public void init() {
        // Starts the Spring Boot web app silently in the background
        try {
            springContext = new SpringApplicationBuilder(CampusTrackApplication.class)
                    .run(getParameters().getRaw().toArray(new String[0]));
        } catch (Throwable e) {
            startupError = e;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        if (startupError != null) {
            Throwable cause = startupError;
            while (cause.getCause() != null) cause = cause.getCause();
            dialog(Alert.AlertType.ERROR, "CampusTrack could not start.\n\n" + cause.getMessage()
                    + "\n\nIf another copy is already running, close it first.");
            Platform.exit();
            return;
        }

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // A WebView ignores alert() and answers "no" to confirm() unless we show the dialogs ourselves
        engine.setOnAlert(event -> dialog(Alert.AlertType.INFORMATION, event.getData()));
        engine.setConfirmHandler(message -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
            alert.initOwner(stage);
            alert.setHeaderText(null);
            alert.setTitle("CampusTrack");
            return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
        });

        // A WebView cannot download files, so CSV exports are saved through a "Save As" window
        engine.locationProperty().addListener((observable, previousUrl, url) -> {
            if (url != null && isExport(url)) {
                Platform.runLater(() -> {
                    engine.getLoadWorker().cancel();
                    if (previousUrl != null) engine.load(previousUrl);
                });
                saveExport(url);
            }
        });

        // Loads the HTML UI through the local-only HTTP port (campustrack.http-port)
        String port = springContext.getEnvironment().getProperty("campustrack.http-port", "8080");
        engine.load("http://localhost:" + port);

        Scene scene = new Scene(webView, 1280, 800);

        primaryStage.setTitle("CampusTrack System");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private static boolean isExport(String url) {
        String path = URI.create(url).getPath();
        return path != null && path.endsWith("/export");
    }

    /** Fetches the CSV with the WebView's own login cookie, then asks where to save it. */
    private void saveExport(String url) {
        Thread worker = new Thread(() -> {
            try {
                // The WebView's cookie store is the JVM default, so this request is already signed in
                HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setInstanceFollowRedirects(false);
                String type = connection.getContentType();
                if (connection.getResponseCode() != 200 || type == null || !type.startsWith("text/csv")) {
                    throw new IllegalStateException("The export is not available. Sign in again and retry.");
                }
                String disposition = connection.getHeaderField("Content-Disposition");
                String fileName = disposition != null && disposition.contains("filename=\"")
                        ? disposition.substring(disposition.indexOf("filename=\"") + 10, disposition.lastIndexOf('"'))
                        : "campustrack-export.csv";
                byte[] content;
                try (InputStream in = connection.getInputStream()) {
                    content = in.readAllBytes();
                }
                Platform.runLater(() -> chooseAndWrite(fileName, content));
            } catch (Exception e) {
                Platform.runLater(() -> dialog(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()));
            }
        }, "csv-export");
        worker.setDaemon(true);
        worker.start();
    }

    private void chooseAndWrite(String fileName, byte[] content) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save attendance export");
        chooser.setInitialFileName(fileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        File target = chooser.showSaveDialog(stage);
        if (target == null) return;
        try {
            Files.write(target.toPath(), content);
            dialog(Alert.AlertType.INFORMATION, "Saved to " + target.getAbsolutePath());
        } catch (Exception e) {
            dialog(Alert.AlertType.ERROR, "Could not save the file: " + e.getMessage());
        }
    }

    private void dialog(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        if (stage != null && stage.isShowing()) alert.initOwner(stage);
        alert.setHeaderText(null);
        alert.setTitle("CampusTrack");
        alert.showAndWait();
    }

    @Override
    public void stop() {
        // Shuts down the backend server when the window is closed
        if (springContext != null) {
            springContext.close();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
