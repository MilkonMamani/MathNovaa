package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
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
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.Navegador;

/**
 * HU15: la pantalla Historial sobre la base de prueba de target/. La pantalla escribe en su propio hilo
 * (sin la transacción de la prueba), así que cada prueba vacía el historial antes y después.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
class HistorialPantallaTest {

    private static final PseudoClass MARCADO = PseudoClass.getPseudoClass("marcado");

    @Autowired
    private HistorialService historial;
    @Autowired
    private ResolverService resolver;
    @Autowired
    private JdbcTemplate jdbc;

    /** Navegador de prueba: anota "Abrir en Resolver". */
    private static final class NavegadorPrueba extends Navegador {
        final List<String> abiertos = new ArrayList<>();

        NavegadorPrueba() {
            super(null);
        }

        @Override
        public void abrirEnResolver(String texto, Campo campo, String forzado) {
            abiertos.add(texto + " | " + campo + " | " + forzado);
        }
    }

    private final NavegadorPrueba navegador = new NavegadorPrueba();
    private HistorialController controlador;
    private Parent raiz;
    private Stage ventana;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        vaciar();
        resolver.resolverYGuardar("x^3 - 2x^2 - x + 2", Campo.Q, null);
        resolver.resolverYGuardar("d/dx(x^3 + 2x)", Campo.Q, null);
        Fx.enFx(() -> {
            FXMLLoader loader = new FXMLLoader(HistorialPantallaTest.class.getResource("/fxml/historial.fxml"));
            loader.setControllerFactory(c -> new HistorialController(historial, navegador));
            try {
                loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            raiz = loader.getRoot();
            controlador = loader.getController();
            // la hoja se carga en el mismo hilo: la prueba no tiene que esperar otro
            controlador.setEjecutor(javafx.concurrent.Task::run);
            ventana = Fx.ventana(raiz, 1280, 800);
            raiz.applyCss();
            raiz.layout();
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

    @SuppressWarnings("unchecked")
    private <T extends Node> T buscar(String id) {
        T n = (T) raiz.lookup(id);
        assertNotNull(n, id);
        return n;
    }

    private List<Button> botones(String ayuda) {
        return raiz.lookupAll(".boton-icono-historial").stream().map(Button.class::cast)
                .filter(b -> ayuda.equals(b.getAccessibleText())).toList();
    }

    private void capturar(String nombre) throws Exception {
        Fx.enFx(() -> {
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

    @Test
    void laTablaMuestraLoResueltoDelMasRecienteAlMasAntiguoConSuConteo() throws Exception {
        Fx.enFx(() -> {
            List<HistorialService.Fila> filas = controlador.filasVisibles();
            assertEquals(List.of("Derivar", "Factorizar"), filas.stream().map(HistorialService.Fila::operacion).toList());
            assertEquals("2 ejercicios", ((Label) buscar("#conteo")).getText());
            assertEquals(List.of("Todos", "Fundamentos de Matemática", "Cálculo 1", "Cálculo 2", "Cálculo 3"),
                    ((ComboBox<?>) buscar("#selectorCurso")).getItems());
            assertEquals("Cualquier fecha", ((ComboBox<HistorialService.Periodo>) buscar("#selectorFecha"))
                    .getConverter().toString(HistorialService.Periodo.CUALQUIERA));
            // botones de 44 px como mínimo
            for (Node n : raiz.lookupAll(".boton-icono-historial")) {
                assertTrue(((Button) n).getHeight() >= 44, "alto del botón de ícono");
            }
            assertTrue(((ToggleButton) buscar("#soloFavoritos")).getHeight() >= 44);
        });
        capturar("historial-tabla.png");
    }

    @Test
    void elBuscadorFiltraYSinCoincidenciasLoDice() throws Exception {
        Fx.enFx(() -> {
            ((TextField) buscar("#buscador")).setText("deriv");
            controlador.recargar();
            assertEquals(List.of("Derivar"), controlador.filasVisibles().stream().map(HistorialService.Fila::operacion).toList());
            assertEquals("1 ejercicio", ((Label) buscar("#conteo")).getText());
            ((TextField) buscar("#buscador")).setText("coseno hiperbólico");
            controlador.recargar();
            Label vacio = buscar("#sinResultados");
            assertTrue(vacio.isVisible());
            assertEquals("No hay ejercicios que coincidan con la búsqueda.", vacio.getText());
        });
    }

    @Test
    void laEstrellaMarcaElFavoritoYSoloFavoritosSePintaAzul() throws Exception {
        Fx.enFx(() -> {
            Button estrella = botones("Marcar como favorito").getFirst();
            estrella.fire();
            assertTrue(estrella.getPseudoClassStates().contains(MARCADO));
            assertEquals("Quitar de favoritos", estrella.getAccessibleText());

            ToggleButton favoritos = buscar("#soloFavoritos");
            favoritos.fire();
            assertTrue(favoritos.getPseudoClassStates().contains(MARCADO));
            assertEquals(1, controlador.filasVisibles().size());
            favoritos.fire();
            assertFalse(favoritos.getPseudoClassStates().contains(MARCADO));
            assertEquals(2, controlador.filasVisibles().size());
        });
    }

    @Test
    void elOjoAbreLaHojaConLosPasosYElResultado() throws Exception {
        Fx.enFx(() -> {
            botones("Ver la hoja con todos los pasos").getLast().fire(); // la factorización
            assertTrue(controlador.hojaVisible());
            assertTrue(buscar("#velo").isVisible());
        });
        Fx.esperar(() -> !raiz.lookupAll(".historial-paso").isEmpty(), "La hoja no se llenó");
        Fx.enFx(() -> {
            assertEquals("Factorizar", ((Label) buscar("#verboHoja")).getText());
            assertFalse(raiz.lookupAll(".historial-paso").isEmpty());
            assertFalse(raiz.lookupAll(".pizarra-nota").isEmpty());
            assertNotNull(raiz.lookup(".pizarra-resultado-final"));
            assertTrue(buscar("#exportarPdf").isDisabled(), "sin el servicio de PDF, Exportar PDF queda desactivado");
            assertFalse(buscar("#abrirEnResolver").isDisabled());
            raiz.applyCss();
            raiz.layout();
        });
        capturar("historial-hoja.png");
        Fx.enFx(() -> {
            ((Button) buscar("#abrirEnResolver")).fire();
            assertEquals(List.of("x^3 - 2x^2 - x + 2 | Q | null"), navegador.abiertos);
            assertFalse(controlador.hojaVisible());
        });
    }

    @Test
    void cerrarQuitaLaHojaYElVelo() throws Exception {
        Fx.enFx(() -> {
            botones("Ver la hoja con todos los pasos").getFirst().fire();
            ((Button) buscar("#cerrarHoja")).fire();
            assertFalse(controlador.hojaVisible());
            assertFalse(buscar("#velo").isVisible());
        });
    }

    @Test
    void laPapeleraPreguntaYBorraEnCascada() throws Exception {
        Fx.enFx(() -> {
            botones("Eliminar este ejercicio").getFirst().fire();
            assertTrue(controlador.dialogoVisible());
            Set<String> textos = new java.util.HashSet<>();
            raiz.lookupAll(".label").forEach(n -> textos.add(((Label) n).getText()));
            assertTrue(textos.contains("¿Eliminar este ejercicio?"));
            assertTrue(textos.contains("Se borran también sus pasos. Esto no se puede deshacer."));
            raiz.applyCss();
            raiz.layout();
        });
        capturar("historial-eliminar.png");
        Fx.enFx(() -> {
            ((Button) buscar("#cancelarEliminar")).fire();
            assertFalse(controlador.dialogoVisible());
            assertEquals(2, controlador.filasVisibles().size());

            botones("Eliminar este ejercicio").getFirst().fire();
            ((Button) buscar("#confirmarEliminar")).fire();
            assertEquals(List.of("Factorizar"), controlador.filasVisibles().stream()
                    .map(HistorialService.Fila::operacion).toList());
            assertEquals("1 ejercicio", ((Label) buscar("#conteo")).getText());
        });
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM resolucion", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id NOT IN "
                + "(SELECT id FROM resolucion)", Integer.class));
    }
}
