package pe.mathnova.presentation.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.parser.Parser;

/**
 * De extremo a extremo: se teclea el texto en el editor (MathLive en el WebView, con KeyEvents de
 * JavaFX como el teclado físico), el traductor lo pasa al texto del parser y el orquestador lo
 * factoriza. Los nombres de función tecleados deben volverse funciones (RN45: "sen" gana sobre s·e·n).
 * La ventana es propia de la prueba; no se abre la aplicación.
 */
class EscrituraFuncionesTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static Stage ventana;
    private static EditorMatematico editor;

    @BeforeAll
    static void abrirEditor() throws Exception {
        CountDownLatch arranque = new CountDownLatch(1);
        try {
            Platform.startup(arranque::countDown);
        } catch (IllegalStateException yaArrancado) {
            arranque.countDown();
        } catch (UnsupportedOperationException | Error sinPantalla) {
            Assumptions.abort("Sin pantalla para JavaFX: " + sinPantalla);
        }
        assertTrue(arranque.await(30, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
        enFx(() -> {
            editor = new EditorMatematico();
            ventana = new Stage();
            ventana.setScene(new Scene(editor, 640, 160));
            ventana.setOpacity(0);
            ventana.show();
            return null;
        });
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (!enFx(() -> editor.estaListo() && Boolean.TRUE.equals(
                editor.webView().getEngine().executeScript("!!(window.editor && window.fuentesListas)")))) {
            assertTrue(System.nanoTime() < limite, "El editor no cargó");
            Thread.sleep(100);
        }
        // Las primeras teclas, mientras la ventana todavía toma el foco, se pierden: se teclea "x"
        // hasta que aparezca.
        while (!"x".equals(teclearLatex("x"))) {
            assertTrue(System.nanoTime() < limite, "El editor no recibe las teclas");
            Thread.sleep(200);
        }
    }

    @AfterAll
    static void cerrar() throws Exception {
        if (ventana != null) {
            enFx(() -> {
                ventana.close();
                return null;
            });
        }
    }

    // ------------------------------------------------------------------ casos

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "2sen^2x - senx - 1  | (2sen(x) + 1)(sen(x) - 1)  | ",
            "e^(2x) - 3e^x + 2   | (e^x - 1)(e^x - 2)         | ",
            "ln^2 x - 4          | (ln(x) - 2)(ln(x) + 2)     | x > 0",
            "tan^2 x - 1         | (tan(x) - 1)(tan(x) + 1)   | x ≠ π/2 + kπ",
            "cos(2x) + cos x     | (cos(x) + 1)(2cos(x) - 1)  | "
    })
    void tecleadoSeFactoriza(String tecleado, String esperado, String restriccion) throws Exception {
        String texto = teclear(tecleado);
        assertInstanceOf(ResolverService.Plan.FactorizarFunciones.class, RESOLVER.decidir(texto), texto);
        ResolucionFunciones r = FACTORIZACION.factorizarFunciones(texto, fase -> { });
        assertTrue(r.factorizable(), () -> texto + ": " + r.mensaje());
        assertEquals(Polinomios.mismosFactores(esperado), Polinomios.mismosFactores(r.formula()),
                () -> texto + " dio " + r.formula());
        assertEquals(restriccion == null ? List.of() : List.of(restriccion), r.restricciones());
        // RN27: con R o C elegidos, las funciones se factorizan igual, primero en Q.
        for (Campo campo : Campo.values()) {
            assertEquals(Optional.of("= " + r.formula()), RESOLVER.enVivo(texto, campo), campo::name);
        }
        // "Qué más puedes hacer" también aparece después de una función.
        assertFalse(FACTORIZACION.sugerencias(r).isEmpty(), texto);
    }

    @Test
    void sumaDeXYSenoNoEsPolinomicaEnUnaFuncion() throws Exception {
        String texto = teclear("x + sen x");
        assertEquals(Parser.parse("x + sen(x)"), Parser.parse(texto));
        ResolucionFunciones r = FACTORIZACION.factorizarFunciones(texto, fase -> { });
        assertFalse(r.factorizable());
        assertEquals("No se puede factorizar: no es polinómica en una sola función (RN32)", r.mensaje());
        assertTrue(FACTORIZACION.sugerencias(r).isEmpty());
    }

    @Test
    void conEspaciosSonLetrasSueltas() throws Exception {
        // RN45: "s e n" con espacios sigue siendo el producto s·e·n.
        assertEquals("senx", teclearLatex("s e n x"));
        assertEquals(Parser.parse("s·e·n·x"), Parser.parse(TraductorLatex.traducir("senx")));
        assertEquals("lnx", teclearLatex("l n x"));
    }

    @Test
    void elNombreMasLargoGana() throws Exception {
        assertEquals(Parser.parse("arcsen(x)"), Parser.parse(teclear("arcsenx")));
        assertEquals(Parser.parse("csc(x)"), Parser.parse(teclear("cosec x")));
        assertEquals(Parser.parse("arctan(x)"), Parser.parse(teclear("arctan x")));
        assertEquals(Parser.parse("arccos(x)"), Parser.parse(teclear("arccos x")));
        assertEquals(Parser.parse("exp(x) + log(x)"), Parser.parse(teclear("exp x + log x")));
        assertEquals(Parser.parse("cot(x) + sec(x) + csc(x)"), Parser.parse(teclear("cot x + sec x + csc x")));
    }

    @Test
    void funcionesEnLetraRectaYExponenteSobreElNombre() throws Exception {
        assertEquals("\\operatorname{\\mathrm{sen}}x+\\cos x", teclearLatex("sen x + cos x"));
        // La cifra del exponente de una función no se lleva la x: sen^2x es (sen x)^2.
        assertEquals("\\operatorname{\\mathrm{sen}}^2x", teclearLatex("sen^2x"));
        assertEquals(Parser.parse("sen^2(x)"), Parser.parse(teclear("sen^2 x")));
        assertEquals(Parser.parse("(sen(x))^2"), Parser.parse(teclear("sen^2x")));
        // Sin paréntesis el argumento es lo que sigue, como en la gramática.
        assertEquals(Parser.parse("sen(x)"), Parser.parse(teclear("senx")));
    }

    // ------------------------------------------------------------------ teclado

    /** Limpia el editor, teclea letra por letra y devuelve el texto para el parser. */
    private static String teclear(String s) throws Exception {
        return TraductorLatex.traducir(teclearLatex(s));
    }

    /** Limpia el editor, teclea letra por letra y devuelve el LaTeX de MathLive. */
    private static String teclearLatex(String s) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!enFx(() -> {
            ventana.toFront();
            editor.limpiar();
            editor.enfocar();
            return Boolean.TRUE.equals(js("document.getElementById('mf').hasFocus() && window.editor.valor() === ''"));
        })) {
            assertTrue(System.nanoTime() < limite, "El editor no tomó el foco");
            Thread.sleep(100);
        }
        for (char c : s.toCharArray()) {
            enFx(() -> {
                pulsar(editor.webView(), c);
                return null;
            });
        }
        return enFx(editor::getLatex);
    }

    private static Object js(String script) {
        return editor.webView().getEngine().executeScript(script);
    }

    private static void pulsar(WebView web, char c) {
        KeyCode code = codigo(c);
        boolean shift = Character.isUpperCase(c) || "^()+".indexOf(c) >= 0;
        String tecla = String.valueOf(c);
        Event.fireEvent(web, new KeyEvent(KeyEvent.KEY_PRESSED, "", tecla, code, shift, false, false, false));
        Event.fireEvent(web, new KeyEvent(KeyEvent.KEY_TYPED, tecla, "", KeyCode.UNDEFINED, shift, false, false, false));
        Event.fireEvent(web, new KeyEvent(KeyEvent.KEY_RELEASED, "", tecla, code, shift, false, false, false));
    }

    private static KeyCode codigo(char c) {
        if (Character.isLetter(c)) {
            return KeyCode.getKeyCode(String.valueOf(Character.toUpperCase(c)));
        }
        if (Character.isDigit(c)) {
            return KeyCode.getKeyCode(String.valueOf(c));
        }
        return switch (c) {
            case ' ' -> KeyCode.SPACE;
            case '^' -> KeyCode.CIRCUMFLEX;
            case '(' -> KeyCode.LEFT_PARENTHESIS;
            case ')' -> KeyCode.RIGHT_PARENTHESIS;
            case '+' -> KeyCode.PLUS;
            case '-' -> KeyCode.MINUS;
            default -> KeyCode.UNDEFINED;
        };
    }

    private static <T> T enFx(Supplier<T> accion) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return accion.get();
        }
        FutureTask<T> tarea = new FutureTask<>(accion::get);
        Platform.runLater(tarea);
        return tarea.get(30, TimeUnit.SECONDS);
    }

    /** Compara factorizaciones sin importar el orden de los factores. */
    private static final class Polinomios {
        static java.util.Set<String> mismosFactores(String formula) {
            String sinPuntos = formula.replace("·", "");
            java.util.Set<String> f = new java.util.TreeSet<>();
            int nivel = 0;
            int desde = 0;
            for (int i = 0; i < sinPuntos.length(); i++) {
                char c = sinPuntos.charAt(i);
                if (c == '(') {
                    if (nivel == 0) {
                        desde = i;
                    }
                    nivel++;
                } else if (c == ')') {
                    nivel--;
                    if (nivel == 0) {
                        f.add(Parser.parse(sinPuntos.substring(desde, i + 1)).toString());
                    }
                }
            }
            return f;
        }
    }
}
