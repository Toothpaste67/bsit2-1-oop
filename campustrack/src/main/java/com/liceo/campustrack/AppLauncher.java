package com.liceo.campustrack;

import javafx.application.Application;

public class AppLauncher {

    public static void main(String[] args) {
        // Bypasses the JDK JavaFX module check and launches the desktop app cleanly
        Application.launch(DesktopAppLauncher.class, args);
    }
}
