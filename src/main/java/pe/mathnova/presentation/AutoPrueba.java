package pe.mathnova.presentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pe.mathnova.domain.escritura.Muestra;
import pe.mathnova.infrastructure.escritura.ArchivoMuestras;
import pe.mathnova.presentation.controller.EstadoResolucion;
import pe.mathnova.presentation.controller.ResolucionController;

/**
 * Comprobación de una instalación: con {@code --mathnova.autoprueba=<muestra.json | carpeta>} MathNova abre
 * "Escribir", pone en el lienzo los trazos grabados de cada muestra, pulsa "Resolver" enseguida (el mismo
 * camino que el botón: primero se reconoce lo escrito y se pone en el campo de arriba) y anota qué pasó en
 * {@code --mathnova.autoprueba.salida} (por defecto ~/.mathnova/autoprueba.txt). Con
 * {@code --mathnova.autoprueba.salir=true} se cierra al terminar.
 *
 * <p>Las muestras son archivos de {@link ArchivoMuestras}: trazos (x, y, t), no imágenes.
 */
final class AutoPrueba {

    private static final Logger LOG = LoggerFactory.getLogger(AutoPrueba.class);
    private static final long LIMITE_MS = 240_000;

    private final ResolucionController pantalla;
    private final List<Path> muestras;
    private final Path salida;
    private final boolean salir;
    private final List<String> lineas = new ArrayList<>();
    private final String pid;
    private int actual = -1;
    private long inicio;
    private long campoLleno;

    AutoPrueba(ResolucionController pantalla, String rutas, Path salida, boolean salir, String pid) {
        this.pantalla = pantalla;
        this.muestras = listar(rutas);
        this.salida = salida;
        this.salir = salir;
        this.pid = pid;
    }

    static List<Path> listar(String rutas) {
        List<Path> r = new ArrayList<>();
        for (String s : rutas.split(";")) {
            if (s.isBlank()) {
                continue;
            }
            Path p = Path.of(s.strip()).toAbsolutePath();
            if (Files.isDirectory(p)) {
                try (Stream<Path> l = Files.list(p)) {
                    l.filter(x -> x.toString().endsWith(".json")).sorted().forEach(r::add);
                } catch (IOException e) {
                    LOG.warn("No se pudo leer {}", p, e);
                }
            } else {
                r.add(p);
            }
        }
        return r;
    }

    void iniciar() {
        lineas.add("MathNova autoprueba " + LocalDateTime.now());
        lineas.add("java pid " + ProcessHandle.current().pid() + "; reconocedor " + pid);
        lineas.add("muestra\tetiqueta\tlectura del modelo\tcampo superior\ttexto MathNova\tsolución\taviso\tms hasta el campo\tms hasta resolver");
        pantalla.abrirEscribir();
        siguiente();
    }

    private void siguiente() {
        actual++;
        if (actual >= muestras.size()) {
            terminar();
            return;
        }
        Path p = muestras.get(actual);
        Muestra m;
        try {
            m = ArchivoMuestras.leer(p);
        } catch (IOException e) {
            lineas.add(p.getFileName() + "\tERROR al leer: " + e.getMessage());
            siguiente();
            return;
        }
        inicio = System.nanoTime();
        campoLleno = -1;
        String antes = pantalla.latexEditor();
        pantalla.panelEscritura().cargarMuestra(m);
        // El estudiante pulsa Resolver justo después de escribir.
        pantalla.pulsarResolver();
        esperar(p, m, antes);
    }

    private void esperar(Path p, Muestra m, String antes) {
        PauseTransition t = new PauseTransition(Duration.millis(100));
        t.setOnFinished(e -> {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            if (campoLleno < 0 && !pantalla.panelEscritura().leyendo()) {
                campoLleno = ms;
            }
            EstadoResolucion estado = pantalla.estado();
            boolean listo = campoLleno >= 0 && (estado == EstadoResolucion.RESUELTO || estado == EstadoResolucion.ERROR
                    || estado == EstadoResolucion.INCOMPLETO);
            if (listo || ms > LIMITE_MS) {
                String lectura = pantalla.panelEscritura().ultimaLectura();
                lineas.add(String.join("\t", p.getFileName().toString(), m.etiqueta(),
                        lectura == null ? "" : lectura, pantalla.latexEditor(), pantalla.texto(),
                        estado == EstadoResolucion.RESUELTO ? String.valueOf(pantalla.latexSolucion()) : estado.name(),
                        pantalla.aviso(), String.valueOf(campoLleno), listo ? String.valueOf(ms) : "LIMITE"));
                escribir();
                siguiente();
            } else {
                esperar(p, m, antes);
            }
        });
        t.play();
    }

    private void escribir() {
        try {
            Files.createDirectories(salida.toAbsolutePath().getParent());
            Files.write(salida, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("No se pudo escribir {}", salida, e);
        }
    }

    private void terminar() {
        lineas.add("fin " + LocalDateTime.now());
        escribir();
        LOG.info("Autoprueba terminada: {}", salida);
        if (salir) {
            Platform.exit();
        }
    }
}
