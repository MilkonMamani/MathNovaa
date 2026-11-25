package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.resolucion.FormulaAjustable;
import pe.mathnova.presentation.resolucion.TablaAnalisis;

/**
 * La pestaña Análisis rediseñada, desde "Resolver" y en una sola sesión: la factorización con sus propiedades, la
 * frase de raíces repetidas y el conjunto solución con su selector; después un límite, una derivada y una integral,
 * que muestran sus propios datos y nada del conjunto solución anterior.
 */
class AnalisisEnResolverTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

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
            return false;
        }

        @Override
        public void guardarMostrarExplicaciones(boolean si) {
        }
    }

    private Stage ventana;
    private Parent raiz;
    private ResolucionController controlador;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        enFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER,
                        new Preferencias(), new ReconocedorSimulado(0)));
                raiz = loader.load();
                controlador = loader.getController();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            ventana = Fx.ventana(raiz, 1400, 950);
        });
        Fx.esperar(() -> raiz.lookup(".editor-matematico") != null, "el editor no cargó");
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private void resolver(String ejercicio) throws Exception {
        Object antes = enFx(() -> raiz.lookup(".pizarra-paso") == null ? raiz.lookup(".pizarra-enunciado")
                : raiz.lookup(".pizarra-paso"));
        enFx(() -> controlador.resolver(ejercicio));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO
                && (raiz.lookup(".pizarra-paso") == null || raiz.lookup(".pizarra-paso") != antes),
                "no se resolvió " + ejercicio);
        enFx(() -> {
            ((ToggleButton) raiz.lookup("#pestanaAnalisis")).fire();
            raiz.applyCss();
            raiz.layout();
        });
    }

    private Node panel() {
        return raiz.lookup("#analisis");
    }

    /** Las etiquetas de la tabla y lo que dice cada valor (LaTeX o texto). */
    private List<String> filas() throws Exception {
        return enFx(() -> {
            List<String> r = new ArrayList<>();
            juntar(panel(), r);
            return r;
        });
    }

    private static void juntar(Node n, List<String> out) {
        if (n instanceof FormulaAjustable f && f.getLatex() != null) {
            out.add(f.getLatex());
        } else if (n instanceof Labeled l && l.getText() != null) {
            out.add(l.getText());
        }
        if (n instanceof Parent p && !(n instanceof FormulaAjustable)) {
            for (Node h : p.getChildrenUnmodifiable()) {
                juntar(h, out);
            }
        }
    }

    private static boolean seVe(Node n) {
        for (Node x = n; x != null; x = x.getParent()) {
            if (!x.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private String conjunto() throws Exception {
        return enFx(() -> ((FormulaAjustable) panel().lookup(".conjunto-solucion")).getLatex());
    }

    private void elegir(String condicion) throws Exception {
        enFx(() -> {
            ToggleButton b = panel().lookupAll(".condicion").stream().map(n -> (ToggleButton) n)
                    .filter(t -> t.getText().equals(condicion)).findFirst().orElseThrow();
            b.fire();
        });
    }

    /** Nada del conjunto solución: ni título, ni selector, ni recta, ni C.S. */
    private void sinConjuntoSolucion(String ejercicio) throws Exception {
        enFx(() -> {
            assertTrue(panel().lookupAll(".selector-condicion").isEmpty(), ejercicio + ": quedó el selector");
            assertTrue(panel().lookupAll(".recta-solucion").isEmpty(), ejercicio + ": quedó la recta");
            assertTrue(panel().lookupAll(".conjunto-solucion").isEmpty(), ejercicio + ": quedó el C.S.");
        });
        List<String> textos = filas();
        assertFalse(textos.stream().anyMatch(t -> t.contains("Conjunto solución") || t.contains("C.S.")
                || t.contains("No aplica")), () -> ejercicio + ": " + textos);
    }

    @Test
    void factorizacionConConjuntoSolucionYDespuesLasOtrasOperaciones() throws Exception {
        resolver("2*x^8-12*x^6+18*x^4-8*x^2");
        List<String> textos = filas();
        assertEquals("Análisis de la función", enFx(() -> ((Label) raiz.lookup("#vistaAnalisis")
                .lookup(".titulo-tarjeta")).getText()));
        for (String esperado : List.of("Grado", "8", "Coeficiente principal", "2", "Corte con el eje y",
                "\\left(0, 0\\right)", "Raíces de f(x) = 0", "x \\in \\left\\{-2, -1, 0, 1, 2\\right\\}",
                "Las raíces −1, 0 y 1 aparecen dos veces cada una en la factorización.", "Conjunto solución")) {
            assertTrue(textos.contains(esperado), () -> "falta " + esperado + " en " + textos);
        }
        assertFalse(textos.stream().anyMatch(t -> t.contains("doble") || t.contains("Signo de la función")),
                textos::toString);

        // f(x) > 0 al empezar, y las cinco condiciones cambian la recta y el C.S. sin tocar la Pizarra
        enFx(() -> {
            List<ToggleButton> bs = panel().lookupAll(".condicion").stream().map(n -> (ToggleButton) n).toList();
            assertEquals(List.of("f(x) > 0", "f(x) ≥ 0", "f(x) < 0", "f(x) ≤ 0", "f(x) = 0"),
                    bs.stream().map(ToggleButton::getText).toList());
            assertTrue(bs.getFirst().isSelected());
            // Paso a paso, en el paso 2: cambiar la condición no mueve la Pizarra
            Button modo = (Button) raiz.lookup(".pizarra-cabecera").lookupAll(".pizarra-control").stream()
                    .filter(n -> n instanceof Button b && "Paso a paso".equals(b.getText())).findFirst()
                    .orElseThrow();
            modo.fire();
        });
        String pasoAntes = enFx(() -> ((Label) raiz.lookup(".pizarra-etiqueta-pasos")).getText());
        Object pizarraAntes = enFx(() -> raiz.lookup(".pizarra-paso"));
        String[][] casos = {
                {"f(x) > 0", "\\left(-\\infty, -2\\right) \\cup \\left(2, +\\infty\\right)"},
                {"f(x) ≥ 0", "\\left(-\\infty, -2\\right] \\cup \\left\\{-1, 0, 1\\right\\} \\cup \\left[2, +\\infty\\right)"},
                {"f(x) < 0", "\\left(-2, -1\\right) \\cup \\left(-1, 0\\right) \\cup \\left(0, 1\\right) \\cup \\left(1, 2\\right)"},
                {"f(x) ≤ 0", "\\left[-2, 2\\right]"},
                {"f(x) = 0", "\\left\\{-2, -1, 0, 1, 2\\right\\}"}};
        for (String[] c : casos) {
            elegir(c[0]);
            assertEquals("\\text{C.S.} = " + c[1], conjunto(), c[0]);
        }
        assertEquals(pasoAntes, enFx(() -> ((Label) raiz.lookup(".pizarra-etiqueta-pasos")).getText()));
        assertTrue(pizarraAntes == enFx(() -> raiz.lookup(".pizarra-paso")), "la Pizarra no se rehízo");
        assertEquals(EstadoResolucion.RESUELTO, enFx(() -> controlador.estado()));

        // La Pizarra de una factorización termina en Resultado y Comprobación: sin "Propiedades de la función"
        enFx(() -> {
            Node desplegable = raiz.lookup("#panelPropiedades");
            assertFalse(seVe(desplegable) && desplegable.isManaged(), "sigue el desplegable de propiedades");
            Node secciones = raiz.lookup("#secciones");
            assertFalse(secciones.isManaged(), "sigue el espacio de la sección");
            assertNotNull(raiz.lookup(".pizarra-comprobacion-titulo"));
        });

        // x² + 1 con f(x) < 0: no hay solución, C.S. = ∅
        resolver("x^2 + 1");
        elegir("f(x) < 0");
        assertEquals("\\text{C.S.} = \\varnothing", conjunto());

        // Límite: la tabla de valores; nada del conjunto solución
        resolver("lim(x->3, (x^2-9)/(x-3))");
        sinConjuntoSolucion("límite");
        List<String> limite = filas();
        assertTrue(limite.contains("Tabla de valores"), limite::toString);
        assertTrue(limite.stream().anyMatch(t -> t.contains("se acercan a 6")), limite::toString);

        // Derivada: crecimiento, puntos críticos
        resolver("d/dx(x^3)");
        sinConjuntoSolucion("derivada");
        List<String> derivada = filas();
        assertTrue(derivada.contains("Crecimiento") && derivada.contains("Crece en"), derivada::toString);
        assertTrue(derivada.stream().anyMatch(t -> t.contains("\\cup")), "el intervalo va como fórmula: " + derivada);

        // Integral: la verificación del motor
        resolver("int(2*x, x)");
        sinConjuntoSolucion("integral");
        List<String> integral = filas();
        assertTrue(integral.contains("Derivada del resultado"), integral::toString);
        assertTrue(integral.contains("x^{2} + C"), integral::toString);
        // Fuera de la factorización, el panel de la Pizarra no cambia: un límite no tiene propiedades
        enFx(() -> assertFalse(raiz.lookup("#secciones").isManaged()));
    }

    @Test
    void conPocoAnchoCadaValorPasaDebajoDeSuEtiqueta() throws Exception {
        enFx(() -> {
            TablaAnalisis t = new TablaAnalisis();
            t.formula("Coeficiente principal", "2").formula("Raíces de f(x) = 0", "x \\in \\{-2, -1, 0, 1, 2\\}");
            javafx.scene.layout.Pane caja = new javafx.scene.layout.Pane(t);
            Stage s = Fx.ventana(caja, 520, 300);
            caja.applyCss();
            t.resize(480, t.prefHeight(480));
            t.layout();
            Node valor = t.getChildrenUnmodifiable().get(1);
            Node etiqueta = t.getChildrenUnmodifiable().get(0);
            assertEquals(etiqueta.getBoundsInParent().getMinY(), valor.getBoundsInParent().getMinY(), 12,
                    "ancho: al lado");
            t.resize(200, t.prefHeight(200));
            t.layout();
            assertTrue(valor.getBoundsInParent().getMinY() >= etiqueta.getBoundsInParent().getMaxY() - 1,
                    "angosto: debajo");
            assertTrue(t.prefHeight(200) > t.prefHeight(480), "apilada ocupa más alto");
            s.close();
        });
    }
}
