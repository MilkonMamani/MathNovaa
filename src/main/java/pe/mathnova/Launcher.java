package pe.mathnova;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Application;
import pe.mathnova.presentation.MathNovaFxApp;

/**
 * Punto de entrada. Es una clase aparte que NO extiende Application:
 * así IntelliJ y el jar arrancan sin el error "JavaFX runtime components are missing".
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) throws IOException {
        // SQLite no crea carpetas; la base vive en ~/.mathnova/mathnova.db
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".mathnova"));
        Application.launch(MathNovaFxApp.class, args);
    }
}
