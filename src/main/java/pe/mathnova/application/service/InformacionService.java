package pe.mathnova.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Configuración · Datos y Acerca de (Sprint 9, parte E): dónde está la base, las versiones de la app, del motor,
 * de Java, de JavaFX y de SQLite.
 */
@Service
public class InformacionService {

    private static final Logger LOG = LoggerFactory.getLogger(InformacionService.class);

    /** Una línea de "Acerca de". */
    public record Version(String nombre, String valor) {
    }

    private final JdbcTemplate jdbc;
    private final String url;
    private final String versionApp;
    private final String versionMotor;

    public InformacionService(JdbcTemplate jdbc, @Value("${spring.datasource.url:}") String url,
                              @Value("${mathnova.version:}") String versionApp,
                              @Value("${mathnova.version-motor:}") String versionMotor) {
        this.jdbc = jdbc;
        this.url = url;
        this.versionApp = versionApp;
        this.versionMotor = versionMotor;
    }

    /** "~/.mathnova/mathnova.db": la ruta del archivo, con la carpeta del usuario abreviada. */
    public String rutaBase() {
        return rutaCorta(url, System.getProperty("user.home"));
    }

    static String rutaCorta(String url, String casa) {
        String ruta = url == null ? "" : url.replaceFirst("^jdbc:sqlite:", "");
        int q = ruta.indexOf('?');
        if (q >= 0) {
            ruta = ruta.substring(0, q);
        }
        ruta = ruta.replace('\\', '/');
        String c = casa == null ? "" : casa.replace('\\', '/');
        if (!c.isEmpty() && ruta.regionMatches(true, 0, c, 0, c.length())) {
            ruta = "~" + ruta.substring(c.length());
        }
        return ruta;
    }

    public java.util.List<Version> versiones() {
        String app = versionApp == null || versionApp.isBlank() || versionApp.startsWith("@") ? "en desarrollo"
                : versionApp;
        String javafx = System.getProperty("javafx.runtime.version", System.getProperty("javafx.version", "21"));
        return java.util.List.of(
                new Version("MathNova", app),
                new Version("Motor de resolución", versionMotor == null || versionMotor.isBlank() ? "—" : versionMotor),
                new Version("Java", System.getProperty("java.version", String.valueOf(Runtime.version().feature()))),
                new Version("JavaFX", javafx),
                new Version("SQLite", sqlite()));
    }

    private String sqlite() {
        try {
            String v = jdbc.queryForObject("SELECT sqlite_version()", String.class);
            return v == null ? "—" : v;
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer la versión de SQLite", e);
            return "—";
        }
    }
}
