package pe.mathnova;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Application;
import pe.mathnova.presentation.MathNovaFxApp;

public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".mathnova"));
        Application.launch(MathNovaFxApp.class, args);
    }
}
