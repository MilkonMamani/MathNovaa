package pe.mathnova.infrastructure.escritura;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import pe.mathnova.domain.escritura.Muestra;

public final class ArchivoMuestras {

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path carpeta;

    public ArchivoMuestras(Path carpeta) {
        this.carpeta = carpeta;
    }

    public static Path carpetaPorDefecto() {
        return Path.of(System.getProperty("user.home"), ".mathnova", "muestras");
    }

    public Path carpeta() {
        return carpeta;
    }

    public Path guardar(Muestra m) throws IOException {
        Files.createDirectories(carpeta);
        Path archivo = carpeta.resolve(nombre(m.etiqueta()) + "_" + LocalDateTime.now().format(HORA) + ".json");
        try (OutputStream out = Files.newOutputStream(archivo)) {
            JSON.writeValue(out, m);
        } catch (JacksonException e) {
            throw new IOException("No se pudo escribir la muestra: " + e.getOriginalMessage(), e);
        }
        return archivo;
    }

    public static Muestra leer(Path archivo) throws IOException {
        try (InputStream in = Files.newInputStream(archivo)) {
            return JSON.readValue(in, Muestra.class);
        } catch (JacksonException e) {
            throw new IOException("No es una muestra válida: " + e.getOriginalMessage(), e);
        }
    }

    static String nombre(String etiqueta) {
        String limpio = etiqueta.strip().replaceAll("[^A-Za-z0-9_-]+", "_");
        return limpio.isEmpty() ? "muestra" : limpio;
    }
}
