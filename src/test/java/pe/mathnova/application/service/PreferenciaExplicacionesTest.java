package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.mathnova.MathNovaApplication;

class PreferenciaExplicacionesTest {

    private static final Path BASE = Path.of("target", "prueba-explicaciones.db");

    private static ConfigurableApplicationContext arrancar() {
        return new SpringApplicationBuilder(MathNovaApplication.class)
                .run("--spring.datasource.url=jdbc:sqlite:" + BASE.toString().replace('\\', '/'));
    }

    @Test
    void laPreferenciaSeGuardaYSeRecuperaAlReiniciar() throws Exception {
        Files.deleteIfExists(BASE);
        try (ConfigurableApplicationContext app = arrancar()) {
            ConfiguracionService c = app.getBean(ConfiguracionService.class);
            assertFalse(c.mostrarExplicaciones(), "por defecto las explicaciones están ocultas");
            c.guardarMostrarExplicaciones(true);
            assertTrue(c.mostrarExplicaciones());
        }
        try (ConfigurableApplicationContext app = arrancar()) {
            ConfiguracionService c = app.getBean(ConfiguracionService.class);
            assertTrue(c.mostrarExplicaciones(), "se perdió al reiniciar");
            c.guardarMostrarExplicaciones(false);
        }
        try (ConfigurableApplicationContext app = arrancar()) {
            assertFalse(app.getBean(ConfiguracionService.class).mostrarExplicaciones());
        }
    }
}
