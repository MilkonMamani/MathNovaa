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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.latex.VistaLatex;
import pe.mathnova.presentation.resolucion.FormulaAjustable;

class FactorizarYSimplificarPizarraTest {

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

    private record Paso(String titulo, List<String> formulas, List<String> textos, Node nodo) {
        boolean tiene(String latex) {
            return formulas.stream().anyMatch(f -> f != null && sinEspacios(f).contains(sinEspacios(latex)));
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
            ventana = Fx.ventana(raiz, 1280, 900);
        });
        Fx.esperar(() -> raiz.lookup(".editor-matematico") != null, "el editor no cargó");
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private List<Paso> resolver(String ejercicio) throws Exception {
        Object antes = enFx(() -> raiz.lookup(".pizarra-paso"));
        enFx(() -> controlador.resolver(ejercicio));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO && raiz.lookup(".pizarra-paso") != antes,
                "no se resolvió " + ejercicio);
        return enFx(() -> {
            List<Paso> pasos = new ArrayList<>();
            for (Node bloque : raiz.lookup(".pizarra-filas").lookupAll(".pizarra-paso")) {
                Label metodo = (Label) bloque.lookup(".pizarra-metodo");
                List<String> formulas = new ArrayList<>();
                List<String> textos = new ArrayList<>();
                juntar(bloque, formulas, textos);
                pasos.add(new Paso(metodo.getText(), formulas, textos, bloque));
            }
            return pasos;
        });
    }

    private static void juntar(Node n, List<String> formulas, List<String> textos) {
        if (n instanceof VistaLatex v) {
            formulas.add(v.getLatex());
        }
        if (n instanceof FormulaAjustable f) {
            formulas.add(f.getLatex());
        }
        if (n instanceof Labeled l) {
            textos.add(l.getText());
        }
        if (n instanceof Parent p) {
            for (Node h : p.getChildrenUnmodifiable()) {
                juntar(h, formulas, textos);
            }
        }
    }

    private static String sinEspacios(String s) {
        return s.replaceAll("\\s+", "");
    }

    private static boolean seVe(Node n) {
        for (Node x = n; x != null; x = x.getParent()) {
            if (!x.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static int primero(List<Paso> pasos, String operacion) {
        if (operacion.equals("límite")) {
            assertEquals("Evaluación directa", pasos.getFirst().titulo());
            assertTrue(pasos.getFirst().tiene("= \\frac{0}{0}"), () -> "sin 0/0: " + pasos.getFirst());
            return 1;
        }
        return 0;
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "lim(x->3, (x^2-9)/(x-3)) | límite   | \\lim_{x \\to 3}              | \\lim_{x \\to 3} \\left(x + 3\\right) = 3 + 3 = 6 | = 6",
            "d/dx((x^2-9)/(x-3))      | derivada | \\frac{d}{dx}                 | y' = 1                                           | = 1 \\qquad (x \\neq 3)",
            "int((x^2-9)/(x-3), x)    | integral | \\int                         | \\int x \\, dx = \\frac{x^{2}}{2}                 | = \\frac{x^{2}}{2} + 3x + C \\qquad (x \\neq 3)"
    })
    void casoCorto(String ejercicio, String operacion, String operador, String opera, String resultado)
            throws Exception {
        List<Paso> pasos = resolver(ejercicio);
        int k = primero(pasos, operacion);
        Paso factorizar = pasos.get(k);
        assertEquals("Factorizar el numerador por diferencia de cuadrados", factorizar.titulo());
        assertTrue(factorizar.tiene("x^{2} - 9 = \\left(x - 3\\right)\\left(x + 3\\right)"), factorizar::toString);

        Paso sustituir = pasos.get(k + 1);
        assertEquals("Sustituir en " + (operacion.equals("límite") ? "el límite"
                : operacion.equals("derivada") ? "la derivada" : "la integral"), sustituir.titulo());
        assertTrue(sustituir.tiene(operador), sustituir::toString);
        assertTrue(sustituir.tiene("\\frac{\\left(x - 3\\right)\\left(x + 3\\right)}{x - 3}"), sustituir::toString);
        assertTrue(sustituir.tiene("(x \\neq 3)"), sustituir::toString);
        assertTrue(sustituir.tiene(operacion.equals("integral") ? "\\int \\left(x + 3\\right) \\, dx"
                : operacion.equals("derivada") ? "= \\frac{d}{dx}\\left(x + 3\\right)" : "= \\lim_{x \\to 3} \\left(x + 3\\right)"),
                sustituir::toString);

        assertTrue(pasos.subList(k + 2, pasos.size()).stream().anyMatch(p -> p.tiene(opera)),
                () -> "no se opera sobre x + 3: " + pasos);
        assertFalse(pasos.stream().anyMatch(p -> p.titulo().contains("cociente") || p.titulo().contains("División")),
                () -> "no debe usar la regla del cociente ni la división: " + pasos);
        String r = enFx(() -> controlador.latexSolucion());
        String tarjeta = enFx(() -> ((FormulaAjustable) raiz.lookup(".pizarra-resultado-formula")).getLatex());
        assertNotNull(tarjeta);
        assertTrue(sinEspacios(tarjeta).endsWith(sinEspacios(resultado)), () -> "resultado: " + tarjeta + " / " + r);
        assertFalse(enFx(() -> raiz.lookup(".pizarra-comprobacion-titulo") != null
                && seVe(raiz.lookup(".pizarra-comprobacion-titulo"))
                && ((Labeled) raiz.lookup(".pizarra-comprobacion-titulo")).getText().contains("factoriz")));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "lim(x->2, (x^3-3*x-2)/(x-2)) | límite   | = \\lim_{x \\to 2} \\left(x + 1\\right)^{2} \\qquad (x \\neq 2) | = 9",
            "d/dx((x^3-3*x-2)/(x-2))      | derivada | = \\frac{d}{dx} \\left(x + 1\\right)^{2} \\qquad (x \\neq 2)    | = 2\\left(x + 1\\right) \\qquad (x \\neq 2)",
            "int((x^3-3*x-2)/(x-2), x)    | integral | = \\int \\left(x + 1\\right)^{2} \\, dx \\qquad (x \\neq 2)     | = \\frac{\\left(x + 1\\right)^{3}}{3} + C \\qquad (x \\neq 2)"
    })
    void casoLargoConRuffiniPlegado(String ejercicio, String operacion, String cancelacion, String resultado)
            throws Exception {
        List<Paso> pasos = resolver(ejercicio);
        int k = primero(pasos, operacion);
        Paso factorizar = pasos.get(k);
        assertTrue(factorizar.titulo().startsWith("Factorizar el numerador con Ruffini"), factorizar::toString);
        assertTrue(factorizar.tiene("x^{3} - 3x - 2 = \\left(x - 2\\right)\\left(x + 1\\right)^{2}"),
                factorizar::toString);
        assertTrue(factorizar.textos().contains("Se coloca 0 porque falta el término de grado 2."), factorizar::toString);
        assertTrue(factorizar.textos().stream().anyMatch(t -> t != null && t.startsWith("Residuo 0")), factorizar::toString);
        assertTrue(factorizar.tiene("x^{2} + 2x + 1 = \\left(x + 1\\right)^{2}"), factorizar::toString);
        enFx(() -> {
            ToggleButton ver = (ToggleButton) factorizar.nodo().lookup(".pizarra-ver-desarrollo");
            assertNotNull(ver, "sin Ver desarrollo");
            assertFalse(ver.isSelected(), "en Todos los pasos el desarrollo largo empieza cerrado");
            assertFalse(seVe(factorizar.nodo().lookup(".pizarra-desarrollo-aux")));
            assertTrue(seVe(factorizar.nodo().lookup(".pizarra-resumen")));
            assertTrue(seVe(pasos.get(k + 1).nodo()));
            ver.setSelected(true);
            assertTrue(seVe(factorizar.nodo().lookup(".pizarra-desarrollo-aux")));
            ver.setSelected(false);
            assertTrue(seVe(factorizar.nodo().lookup(".pizarra-resumen")));
        });

        Paso sustituir = pasos.get(k + 1);
        assertTrue(sustituir.titulo().startsWith("Sustituir en"), sustituir::toString);
        assertTrue(sustituir.tiene("\\frac{\\left(x - 2\\right)\\left(x + 1\\right)^{2}}{x - 2}"), sustituir::toString);
        assertTrue(sustituir.tiene(cancelacion), sustituir::toString);
        String tarjeta = enFx(() -> ((FormulaAjustable) raiz.lookup(".pizarra-resultado-formula")).getLatex());
        assertTrue(sinEspacios(tarjeta).endsWith(sinEspacios(resultado)), () -> "resultado: " + tarjeta);
    }

    @Test
    void enPasoAPasoElDesarrolloLargoSeAbreAlLlegarYUnEjercicioNuevoEmpiezaSegunElModo() throws Exception {
        List<Paso> pasos = resolver("d/dx((x^3-3*x-2)/(x-2))");
        enFx(() -> {
            Button modo = (Button) raiz.lookup(".pizarra-cabecera").lookupAll(".pizarra-control").stream()
                    .filter(n -> n instanceof Button b && "Paso a paso".equals(b.getText())).findFirst().orElseThrow();
            modo.fire();
            ToggleButton ver = (ToggleButton) pasos.getFirst().nodo().lookup(".pizarra-ver-desarrollo");
            assertTrue(ver.isSelected(), "al llegar al paso 1 en Paso a paso se abre el desarrollo");
        });
        List<Paso> otros = resolver("int((x^3-3*x-2)/(x-2), x)");
        boolean pasoAPaso = enFx(() -> raiz.lookup(".pizarra-cabecera").lookupAll(".pizarra-control").stream()
                .noneMatch(n -> n instanceof Button b && "Paso a paso".equals(b.getText())));
        boolean abierto = enFx(() -> ((ToggleButton) otros.getFirst().nodo().lookup(".pizarra-ver-desarrollo"))
                .isSelected());
        assertEquals(pasoAPaso, abierto, "el desplegable empieza según el modo de navegación");
    }

    @Test
    void cancelacionParcialYSinFactorComun() throws Exception {
        List<Paso> parcial = resolver("d/dx((x^2-9)/(x^2-x-6))");
        assertEquals("Factorizar el numerador por diferencia de cuadrados", parcial.get(0).titulo());
        assertEquals("Factorizar el denominador por trinomio simple", parcial.get(1).titulo());
        assertTrue(parcial.get(2).tiene("= \\frac{d}{dx}\\left(\\frac{x + 3}{x + 2}\\right) \\qquad (x \\neq -2,\\ x \\neq 3)"),
                () -> parcial.get(2).toString());

        List<Paso> sin = resolver("d/dx((x^2+1)/(x-3))");
        assertEquals("Regla del cociente", sin.getFirst().titulo());
        assertFalse(sin.stream().anyMatch(p -> p.titulo().startsWith("Factorizar") || p.titulo().startsWith("Sustituir")));
    }
}
