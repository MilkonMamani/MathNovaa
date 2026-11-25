package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.latex.VistaLatex;
import pe.mathnova.presentation.resolucion.FormulaAjustable;

class SignosEnLaPizarraTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));
    private static final Pattern DOBLE = Pattern.compile("[+\\-−]\\s*[\\-−]");

    private static final class Preferencias extends ConfiguracionService {
        Preferencias() {
            super(null);
        }

        @Override
        public boolean animacionesActivas() {
            return false;
        }

        @Override
        public boolean mostrarCodigos() {
            return false;
        }

        @Override
        public boolean mostrarExplicaciones() {
            return true;
        }

        @Override
        public void guardarMostrarExplicaciones(boolean si) {
        }
    }

    private Stage ventana;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @AfterEach
    void cerrar() throws Exception {
        if (ventana != null) {
            enFx(() -> ventana.close());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "∫((5*x-3)/(x^2-2*x-3), x)",
            "∫((x - 3)/(x^2 + 2x - 3), x)",
            "∫(-2x + 1, x)",
            "x^2-2*x-3",
            "x^3 - 7x - 6",
            "lim(x->3, (x^2-2*x-3)/(x-3))",
            "lim(x->-1, (x^2 - 2x - 3)/(x + 1))",
            "d/dx(-3x^2 + 2x - 1)",
            "d/dx((x-3)/(x+1))",
            "x^2 + x - 1 = 0"
    })
    void ningunaFormulaEscribeDosSignosSeguidos(String ejercicio) throws Exception {
        Parent[] raiz = new Parent[1];
        ResolucionController[] controlador = new ResolucionController[1];
        enFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER,
                        new Preferencias(), new ReconocedorSimulado(0)));
                raiz[0] = loader.load();
                controlador[0] = loader.getController();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            ventana = Fx.ventana(raiz[0], 1280, 900);
        });
        Fx.esperar(() -> raiz[0].lookup(".editor-matematico") != null, "el editor no cargó");
        enFx(() -> controlador[0].resolver(ejercicio));
        Fx.esperar(() -> controlador[0].estado() == EstadoResolucion.RESUELTO, "no se resolvió " + ejercicio);

        List<String> textos = new ArrayList<>();
        enFx(() -> {
            for (Node n : raiz[0].lookupAll(".titled-pane")) {
                ((TitledPane) n).setExpanded(true);
            }
            juntar(raiz[0], textos);
            for (Node b : raiz[0].lookupAll(".toggle-button")) {
                if (b instanceof ToggleButton t) {
                    t.fire();
                    raiz[0].applyCss();
                    raiz[0].layout();
                    juntar(raiz[0], textos);
                }
            }
        });
        List<String> malos = textos.stream().filter(t -> t != null && DOBLE.matcher(t).find()).distinct().toList();
        assertTrue(malos.isEmpty(), () -> ejercicio + " escribe dos signos seguidos en: " + malos);
        if (ejercicio.startsWith("∫((5")) {
            assertTrue(textos.contains("\\int \\frac{5x - 3}{x^{2} - 2x - 3} \\, dx = "
                    + "3\\ln\\left|x - 3\\right| + 2\\ln\\left|x + 1\\right| + C"), () -> "sin el resultado: " + textos);
        }
    }

    private static void juntar(Node n, List<String> out) {
        if (n instanceof VistaLatex v) {
            out.add(v.getLatex());
        }
        if (n instanceof FormulaAjustable f) {
            out.add(f.getLatex());
        }
        if (n instanceof Labeled l) {
            out.add(l.getText());
        }
        if (n instanceof javafx.scene.text.Text t) {
            out.add(t.getText());
        }
        if (n instanceof TitledPane tp && tp.getContent() != null) {
            juntar(tp.getContent(), out);
        }
        if (n instanceof ScrollPane sp && sp.getContent() != null) {
            juntar(sp.getContent(), out);
        }
        if (n instanceof Parent p) {
            for (Node h : p.getChildrenUnmodifiable()) {
                juntar(h, out);
            }
        }
    }
}
