package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.mathnova.application.service.HistorialService;
import pe.mathnova.application.service.PracticaService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.Navegador;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
class PracticaPantallaTest {

    private static final PseudoClass DESCARTADA = PseudoClass.getPseudoClass("descartada");

    @Autowired
    private PracticaService servicio;
    @Autowired
    private HistorialService historial;
    @Autowired
    private ResolverService resolver;
    @Autowired
    private JdbcTemplate jdbc;

    private PracticaController controlador;
    private Parent raiz;
    private Stage ventana;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        vaciar();
        Fx.enFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/practica.fxml"));
            loader.setControllerFactory(c -> new PracticaController(servicio));
            try {
                raiz = loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            controlador = loader.getController();
            ventana = Fx.ventana(raiz, 1280, 860);
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        Fx.enFx(() -> ventana.close());
        vaciar();
    }

    private void vaciar() {
        jdbc.update("DELETE FROM resolucion");
        jdbc.update("DELETE FROM ejercicio");
    }

    private Node buscar(String id) {
        Node n = raiz.lookup(id);
        assertNotNull(n, id);
        return n;
    }

    private String texto(String id) {
        return ((Label) buscar(id)).getText();
    }

    private void capturar(String nombre) throws Exception {
        Fx.enFx(() -> {
            raiz.applyCss();
            raiz.layout();
            try {
                File carpeta = new File("target/capturas");
                carpeta.mkdirs();
                ImageIO.write(SwingFXUtils.fromFXImage(raiz.snapshot(null, null), null), "png",
                        new File(carpeta, nombre));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }

    private Button opcion(String texto) throws Exception {
        return Fx.enFx(() -> controlador.botones().values().stream().filter(b -> b.getText().equals(texto))
                .findFirst().orElseThrow(() -> new AssertionError("falta la opción " + texto)));
    }

    private Button otraQue(String texto) throws Exception {
        return Fx.enFx(() -> controlador.botones().values().stream().filter(b -> !b.getText().equals(texto))
                .findFirst().orElseThrow());
    }

    private int pasoActual() throws Exception {
        return Fx.enFx(() -> controlador.pregunta() == null ? 0 : controlador.pregunta().numero());
    }

    @Test
    void sinEjercicioSeEligeEntreTarjetas() throws Exception {
        Fx.esperar(() -> !raiz.lookupAll(".practica-tarjeta").isEmpty(), "no aparecieron los ejercicios");
        assertTrue(Fx.enFx(() -> buscar("#vistaElegir").isVisible()));
        assertFalse(Fx.enFx(() -> buscar("#vistaPractica").isVisible()));
        List<String> temas = Fx.enFx(() -> raiz.lookupAll(".practica-tarjeta-tema").stream()
                .map(n -> ((Label) n).getText()).toList());
        assertTrue(temas.contains("Factorización en Q · 3 pasos"), temas.toString());
        assertTrue(temas.stream().anyMatch(t -> t.startsWith("Límites · ")), temas.toString());
        capturar("practica-elegir.png");

        Fx.enFx(() -> ((Button) raiz.lookupAll(".practica-tarjeta").stream()
                .filter(t -> ((PracticaService.Propuesta) t.getUserData()).texto().equals("2x^3 - 3x^2 - 3x + 2"))
                .findFirst().orElseThrow().lookup(".boton-primario")).fire());
        Fx.esperar(() -> controlador.practica() != null, "no empezó");
        assertTrue(Fx.enFx(() -> buscar("#vistaPractica").isVisible()));
        assertEquals("Paso 1 de 3", Fx.enFx(() -> texto("#pasoActual")));
    }

    @Test
    void unaPracticaCompletaConUnError() throws Exception {
        Fx.enFx(() -> controlador.empezar("x^3 - 2x^2 - x + 2", null));
        Fx.esperar(() -> controlador.practica() != null, "no empezó");
        assertEquals("Factorizar", Fx.enFx(() -> texto("#tituloEjercicio")));
        assertEquals("Paso 1 de 3", Fx.enFx(() -> texto("#pasoActual")));
        assertEquals(0.0, Fx.enFx(() -> ((javafx.scene.control.ProgressBar) buscar("#progreso")).getProgress()));

        Button mal = otraQue("Agrupación de términos");
        Fx.enFx(mal::fire);
        Fx.enFx(() -> {
            assertTrue(mal.isDisabled(), "no se puede volver a pulsar");
            assertTrue(mal.getPseudoClassStates().contains(DESCARTADA), "tachada y gris");
            assertTrue(buscar("#avisoNoAplica").isVisible());
            assertFalse(texto("#motivo").isBlank());
            assertEquals("1", texto("#fallidos"));
            assertEquals("0", texto("#aciertos"));
        });
        Fx.enFx(() -> ((Button) buscar("#verPista")).fire());
        Fx.enFx(() -> {
            assertTrue(buscar("#cajaPista").isVisible());
            assertEquals("Mira si al juntar los términos de dos en dos aparece algo que se repite.", texto("#pista"));
        });
        capturar("practica-error-y-pista.png");

        Fx.enFx(opcion("Agrupación de términos")::fire);
        Fx.enFx(() -> {
            assertEquals("Paso 2 de 3", texto("#pasoActual"));
            assertEquals(1, raiz.lookupAll(".historial-paso").size(), "el paso acertado se muestra");
            assertFalse(buscar("#avisoNoAplica").isVisible(), "el aviso es del paso anterior");
            assertFalse(buscar("#cajaPista").isVisible());
        });
        assertEquals(2, pasoActual());

        Fx.enFx(opcion("Diferencia de cuadrados")::fire);
        Fx.enFx(() -> assertEquals("Paso 3 de 3", texto("#pasoActual")));
        assertEquals(PracticaService.TERMINASTE, Fx.enFx(() -> controlador.pregunta().pregunta()));
        capturar("practica-terminaste.png");
        Fx.enFx(opcion(PracticaService.SI_TERMINE)::fire);

        Fx.enFx(() -> {
            assertTrue(buscar("#cajaFinal").isVisible());
            assertFalse(buscar("#cajaPregunta").isVisible());
            assertEquals("Terminaste. Acertaste 2 de 3 pasos a la primera.", texto("#resumen"));
            assertEquals("2", texto("#aciertos"));
            assertEquals(1.0, ((javafx.scene.control.ProgressBar) buscar("#progreso")).getProgress());
        });
        capturar("practica-final.png");
        int sesion = Fx.enFx(() -> controlador.practica().sesionId());
        assertEquals(2, jdbc.queryForObject("SELECT aciertos FROM sesion_practica WHERE id = ?", Integer.class, sesion));

        Fx.enFx(() -> ((Button) buscar("#repetir")).fire());
        Fx.esperar(() -> controlador.practica() != null && controlador.practica().sesionId() != sesion, "no repitió");
        assertEquals("Paso 1 de 3", Fx.enFx(() -> texto("#pasoActual")));
        Fx.enFx(() -> ((Button) buscar("#elegirOtro")).fire());
        assertTrue(Fx.enFx(() -> buscar("#vistaElegir").isVisible()));
    }

    @Test
    void unEjercicioQueNoSePracticaAvisa() throws Exception {
        Fx.enFx(() -> controlador.empezar("d/dx(x^2)", null));
        Fx.esperar(() -> buscar("#error").isVisible(), "no avisó");
        assertTrue(Fx.enFx(() -> texto("#error")).startsWith("No se puede practicar este ejercicio"));
        assertTrue(Fx.enFx(() -> buscar("#vistaElegir").isVisible()));
    }

    @Test
    void practicarDesdeLaHojaDelHistorial() throws Exception {
        resolver.resolverYGuardar("lim(x→2, (x^2 - 4)/(x - 2))", Campo.Q, null);
        List<String> abiertos = new ArrayList<>();
        Navegador navegador = new Navegador(null) {
            @Override
            public boolean sePuedePracticar(String texto, Campo campo) {
                return servicio.sePuedePracticar(texto, campo);
            }

            @Override
            public void abrirEnPractica(String texto, Campo campo) {
                abiertos.add(texto + " | " + campo);
            }
        };
        Parent[] h = new Parent[1];
        HistorialController[] c = new HistorialController[1];
        Fx.enFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/historial.fxml"));
            loader.setControllerFactory(k -> new HistorialController(historial, navegador));
            try {
                h[0] = loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            c[0] = loader.getController();
            c[0].setEjecutor(javafx.concurrent.Task::run);
            ventana.getScene().setRoot(h[0]);
            h[0].applyCss();
            h[0].layout();
        });
        Fx.enFx(() -> {
            h[0].lookupAll(".boton-icono-historial").stream().map(Button.class::cast)
                    .filter(b -> "Ver la hoja con todos los pasos".equals(b.getAccessibleText())).findFirst()
                    .orElseThrow().fire();
        });
        Button practicar = Fx.enFx(() -> (Button) h[0].lookup("#practicar"));
        Fx.esperar(() -> !practicar.isDisabled(), "Practicar no se activó");
        Fx.enFx(practicar::fire);
        assertEquals(List.of("lim(x→2, (x^2 - 4)/(x - 2)) | null"), abiertos);
        assertFalse(Fx.enFx(c[0]::hojaVisible), "la hoja se cierra");
    }
}
