package pe.mathnova.presentation.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.presentation.controller.EstadoResolucion;
import pe.mathnova.presentation.controller.ResolucionController;

/**
 * Prueba de humo de extremo a extremo (Parte F). Se abre la pantalla "Resolver ejercicio" de verdad
 * (resolucion.fxml con su controlador, el editor MathLive en el WebView y el teclado en pantalla) en
 * una ventana propia e invisible, y cada ejercicio se entra como lo haría el estudiante: pulsando las
 * teclas del teclado en pantalla y escribiendo el resto con el teclado físico (KeyEvents de JavaFX).
 * Después se pulsa "Resolver" y se comprueba que no hubo error, que el resultado es el esperado (por
 * estructura o numéricamente) y que la pantalla terminó en RESUELTO.
 *
 * <p>En los pasos, "[etiqueta]" es la tecla del teclado en pantalla con esa etiqueta ("[→]" es la
 * flecha); el resto se escribe con el teclado físico, letra por letra.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PruebaHumoTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    /** Mientras vale true, factorizar falla: así se ve dónde aparece un error del motor. */
    private static volatile boolean fallarFactorizacion;

    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR) {
        @Override
        public pe.mathnova.domain.factorizacion.Resolucion factorizar(String texto, String forzado,
                pe.mathnova.domain.factorizacion.Campo campo,
                java.util.function.Consumer<pe.mathnova.application.service.FaseFactorizacion> avance) {
            if (fallarFactorizacion) {
                throw new IllegalStateException("fallo de prueba en el motor");
            }
            return super.factorizar(texto, forzado, campo, avance);
        }
    };
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    /** Sin base de datos ni animaciones: el resultado aparece de una vez. */
    private static final class SinAnimaciones extends ConfiguracionService {
        SinAnimaciones() {
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
    }

    private static Stage ventana;
    private static Parent raiz;
    private static ResolucionController controlador;
    private static EditorMatematico editor;
    private static TecladoMatematico teclado;

    @BeforeAll
    static void abrirPantalla() throws Exception {
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
            try {
                FXMLLoader loader = new FXMLLoader(PruebaHumoTest.class.getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER, new SinAnimaciones()));
                raiz = loader.load();
                controlador = loader.getController();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            Scene escena = new Scene(raiz, 1280, 900);
            var css = PruebaHumoTest.class.getResource("/css/mathnova.css");
            if (css != null) {
                escena.getStylesheets().add(css.toExternalForm());
            }
            ventana = new Stage();
            ventana.setScene(escena);
            ventana.setOpacity(0);
            ventana.show();
            editor = (EditorMatematico) raiz.lookup(".editor-matematico");
            teclado = (TecladoMatematico) raiz.lookup(".teclado-mn");
            return null;
        });
        assertNotNull(editor);
        assertNotNull(teclado);
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (!enFx(() -> editor.estaListo() && Boolean.TRUE.equals(
                editor.webView().getEngine().executeScript("!!(window.editor && window.fuentesListas)")))) {
            assertTrue(System.nanoTime() < limite, "El editor no cargó");
            Thread.sleep(100);
        }
        // Las primeras teclas, mientras la ventana toma el foco, se pierden: se teclea "x" hasta que aparezca.
        while (!"x".equals(limpiarYEscribir("x"))) {
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

    // ------------------------------------------------------------------ el guion de demostración

    /** Con -Dmathnova.capturar=true, el LaTeX que arman las teclas se guarda como fixture en vez de compararse. */
    private static final boolean CAPTURAR = Boolean.getBoolean("mathnova.capturar");

    /** Caso, LaTeX recibido, texto traducido, resultado y estado, para el informe de target/guion-demostracion.md. */
    private static final List<String> INFORME = java.util.Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> guion() {
        return Guion.casos().stream().map(Arguments::of);
    }

    /** PASO 0: las teclas del guion dan el LaTeX guardado (o, al capturar, lo guardan). */
    @Order(1)
    @ParameterizedTest(name = "{0}")
    @MethodSource("guion")
    void elTecladoArmaElLatexDelGuion(Guion.Caso c) throws Exception {
        String latex = entrar(c.pasos());
        if (CAPTURAR) {
            c.guardar(latex);
        } else {
            assertNotNull(c.latex(), c.id() + ": falta el fixture; corre con -Dmathnova.capturar=true");
            assertEquals(c.latex(), latex, c + ": las teclas ya no dan el LaTeX guardado");
        }
        if (c.latexCon() != null) {
            // MathLive guarda \operatorname{x} como \operatorname{\mathrm{x}}: las dos formas valen.
            String plano = latex.replaceAll("\\\\operatorname\\{\\\\mathrm\\{([A-Za-z]+)\\}\\}", "\\\\operatorname{$1}");
            assertTrue(plano.contains(c.latexCon()), () -> c + ": falta " + c.latexCon() + " en " + latex);
        }
    }

    /** El LaTeX del guion se pone en el editor, se pulsa Resolver y se mira lo que queda en la pantalla. */
    @Order(2)
    @ParameterizedTest(name = "{0}")
    @MethodSource("guion")
    void seResuelveDesdeElLatexDelEditor(Guion.Caso c) throws Exception {
        String latex = c.latex();
        assertNotNull(latex, c.id() + ": falta el fixture; corre con -Dmathnova.capturar=true");
        String texto = "";
        String resultado = "";
        boolean bien = false;
        try {
            limpiarYEscribir("");
            elegirCampo(c.campo());
            enFx(() -> {
                editor.setLatex(latex);
                return null;
            });
            esperar(() -> latex.equals(editor.getLatex()) || !editor.getLatex().isEmpty(), c + ": el editor no tomó el LaTeX");
            try {
                texto = TraductorLatex.traducir(latex);
            } catch (TraductorLatex.LatexNoValido e) {
                resultado = "LatexNoValido: " + e.getMessage();
                fail(c + ": el LaTeX " + latex + " no se traduce: " + e.getMessage());
            }
            String contexto = c + " (" + latex + " → " + texto + ")";
            assertEquals(c.texto(), texto, contexto);
            if (c.aviso() != null) {
                esperar(() -> visible("#avisoEntrada"), contexto + ": no apareció el aviso");
                resultado = enFx(() -> textoDe("#avisoEntrada"));
                String aviso = resultado;
                assertTrue(aviso.contains(c.aviso()), () -> contexto + ": el aviso dice " + aviso);
                assertTrue(enFx(() -> raiz.lookup("#botonResolver").isDisabled()), contexto + ": Resolver activo");
                bien = true;
                return;
            }
            resolverYEsperar(contexto);
            String error = enFx(() -> visible("#errorSolucion") ? textoDe("#errorSolucion") : null);
            resultado = error != null ? error : resumen(enFx(controlador::ultimaResolucion));
            assertFalse(enFx(() -> visible("#avisoEntrada")), () -> contexto + ": aviso " + textoDe("#avisoEntrada"));
            assertNull(error, contexto + ": " + error);
            assertEquals(EstadoResolucion.RESUELTO, enFx(controlador::estado), contexto);
            Object r = enFx(controlador::ultimaResolucion);
            assertNotNull(r, contexto);
            Guion.Salida salida = enFx(() -> new Guion.Salida(r, pantalla(), curvasDibujadas()));
            try {
                c.chequeo().comprobar(salida);
            } catch (AssertionError e) {
                throw new AssertionError(contexto + ": " + e.getMessage(), e);
            }
            bien = true;
        } finally {
            INFORME.add("| " + c + " | `" + latex + "` | `" + texto + "` | " + resultado.replace("|", "\\|").replace("\n", " ")
                    + " | " + (bien ? "✔" : "✘") + " |");
            elegirCampo("Q");
        }
    }

    @AfterAll
    static void escribirInforme() throws Exception {
        if (INFORME.isEmpty()) {
            return;
        }
        List<String> filas = new ArrayList<>(List.of("| Caso | LaTeX recibido | Texto traducido | Resultado | Estado |",
                "|---|---|---|---|---|"));
        filas.addAll(INFORME);
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target"));
        java.nio.file.Files.write(java.nio.file.Path.of("target", "guion-demostracion.md"), filas,
                java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Lo que dice la resolución en una línea. */
    private static String resumen(Object r) {
        if (r == null) {
            return "";
        }
        if (r instanceof pe.mathnova.domain.calculo.limites.ResolucionLimite l) {
            return l.resultado() instanceof pe.mathnova.domain.calculo.limites.ResultadoLimite.NoExiste n
                    ? "No existe: " + n.motivo() : l.resultado().texto();
        }
        if (r instanceof pe.mathnova.domain.ecuacion.ResolucionEcuacion e) {
            return String.join(", ", e.soluciones().stream().map(s -> "x = " + s.valor()).toList());
        }
        try {
            return String.valueOf(r.getClass().getMethod("texto").invoke(r));
        } catch (ReflectiveOperationException e) {
            return r.getClass().getSimpleName();
        }
    }

    /** Todo el texto de las tarjetas Solución, Procedimiento y Análisis (las fórmulas son imágenes). */
    private static String pantalla() {
        StringBuilder sb = new StringBuilder();
        for (String id : List.of("#tarjetaSolucion", "#tarjetaProcedimiento", "#vistaAnalisis")) {
            Node n = raiz.lookup(id);
            if (n != null) {
                textos(n, sb);
            }
        }
        return sb.toString();
    }

    private static void textos(Node n, StringBuilder sb) {
        if (!n.isVisible()) {
            return;
        }
        if (n instanceof javafx.scene.control.Labeled l && l.getText() != null) {
            sb.append(l.getText()).append('\n');
        } else if (n instanceof javafx.scene.text.Text t && t.getText() != null) {
            sb.append(t.getText()).append('\n');
        }
        if (n.getAccessibleText() != null) {
            sb.append(n.getAccessibleText()).append('\n');
        }
        if (n instanceof javafx.scene.control.TitledPane tp && tp.getContent() != null) {
            textos(tp.getContent(), sb);
        }
        if (n instanceof Parent p) {
            for (Node h : p.getChildrenUnmodifiable()) {
                textos(h, sb);
            }
        }
    }

    private static List<pe.mathnova.presentation.grafica.DatosCurvasNivel> curvasDibujadas() {
        List<pe.mathnova.presentation.grafica.DatosCurvasNivel> r = new ArrayList<>();
        for (Node n : raiz.lookup("#zonaGrafica") instanceof Parent p ? p.getChildrenUnmodifiable() : List.<Node>of()) {
            if (n instanceof pe.mathnova.presentation.grafica.CurvasNivel cn) {
                r.add(cn.datos());
            }
        }
        return r;
    }

    /** Si el motor falla, el error se ve en la tarjeta Solución y también en el panel de Procedimiento. */
    @Test
    void unErrorDelMotorSeVeEnElProcedimiento() throws Exception {
        try {
            fallarFactorizacion = true;
            entrar(List.of("x^2-1"));
            resolverYEsperar("x^2 - 1 con el motor fallando");
            assertTrue(enFx(() -> visible("#errorSolucion")), "falta el error en la tarjeta Solución");
            String error = enFx(() -> textoDe("#errorSolucion"));
            assertTrue(error.contains("fallo de prueba en el motor"), error);
            assertTrue(enFx(() -> visible("#sinPasos")), "el panel de Procedimiento no dice nada");
            assertEquals(error, enFx(() -> textoDe("#sinPasos")));
        } finally {
            fallarFactorizacion = false;
        }
    }

    /** Resolver, borrar y escribir algo con error: nada del ejercicio anterior queda como actual. */
    @Test
    void trasUnErrorNoQuedaNadaDelEjercicioAnterior() throws Exception {
        entrar(List.of("x^3-2x^2-x+2"));
        resolverYEsperar("x^3 - 2x^2 - x + 2");
        assertEquals(EstadoResolucion.RESUELTO, enFx(controlador::estado));
        assertTrue(enFx(() -> visible("#etiquetaVerificado")));
        // RF71 retirado: ya no hay línea en vivo que esperar bajo el editor.

        entrar(List.of("x^2+*3"));
        // Más que los 400 ms del resultado en vivo.
        Thread.sleep(900);
        enFx(() -> null);
        assertEquals(EstadoResolucion.ERROR, enFx(controlador::estado));
        // RF71 retirado: antes se comprobaba que no quedara la línea en vivo del ejercicio anterior.
        assertFalse(enFx(() -> visible("#etiquetaVerificado")), "sigue \"Verificado\" del ejercicio anterior");
        assertTrue(enFx(() -> visible("#avisoEntrada")), "falta el mensaje de error");
        assertTrue(enFx(() -> raiz.lookup("#botonResolver").isDisabled()), "Resolver debe quedar desactivado");
        assertEquals(0.4, enFx(() -> raiz.lookup("#tarjetaSolucion").getOpacity()), 1e-9,
                "la tarjeta Solución debe verse atenuada");
    }

    /** Parte 1: √, sqrt y raiz escritos con el teclado físico insertan la plantilla, nunca \surd. */
    @Test
    void laRaizEscritaAManoEsPlantilla() throws Exception {
        for (String atajo : List.of("√", "sqrt", "raiz")) {
            String latex = limpiarYEscribir(atajo + "2");
            assertFalse(latex.contains("surd"), atajo + " → " + latex);
            assertEquals("√2", TraductorLatex.traducir(latex), atajo + " → " + latex);
        }
    }

    /** Parte 3: la casilla que falta se nombra, se marca en naranja y el cursor va ahí. */
    @Test
    void laCasillaQueFaltaSeResaltaYElCursorVaAhi() throws Exception {
        entrar(List.of("[∫]", "1"));
        assertEquals(EstadoResolucion.INCOMPLETO, enFx(controlador::estado));
        assertEquals(TraductorLatex.OTRO_LIMITE, enFx(() -> textoDe("#avisoEntrada")));
        assertTrue(enFx(() -> visible("#avisoEntrada")));
        assertTrue(enFx(() -> raiz.lookup("#botonResolver").isDisabled()));
        enFx(() -> {
            teclado.pulsar(tecla("Enter"));
            return null;
        });
        assertEquals(Boolean.TRUE, enFx(() -> editor.webView().getEngine().executeScript(
                "document.getElementById('mf').classList.contains('falta')")), "la casilla no quedó marcada");
        escribirAMano("2");
        String latex = enFx(editor::getLatex);
        assertTrue(latex.startsWith("\\int_1^2") || latex.startsWith("\\int_{1}^{2}"), latex);
        // MathLive avisa el cambio con un setTimeout: se espera el mensaje nuevo.
        esperar(() -> "Completa el integrando".equals(textoDe("#avisoEntrada")),
                "el mensaje no pasó a la casilla siguiente: " + latex);
    }

    /** Parte 5: en una ventana angosta la barra pasa a dos filas y Resolver no se encoge a "…". */
    @Test
    void enUnaVentanaAngostaLaBarraPasaADosFilas() throws Exception {
        try {
            anchoDeVentana(700);
            Node boton = enFx(() -> raiz.lookup("#botonResolver"));
            assertTrue(enFx(() -> boton.getParent() != raiz.lookup("#barraEntrada")), "el botón sigue en la barra");
            assertTrue(enFx(boton::getLayoutBounds).getWidth() >= 120, "el botón mide menos de 120 px");
            assertTrue(enFx(() -> raiz.lookup("#selectorCampo").getParent() == boton.getParent()));
        } finally {
            anchoDeVentana(1280);
        }
        assertTrue(enFx(() -> raiz.lookup("#botonResolver").getParent() == raiz.lookup("#barraEntrada")));
        assertTrue(enFx(() -> raiz.lookup("#botonResolver").getLayoutBounds().getWidth()) >= 120);
    }

    /** Parte 5: Q/R/C solo se activa cuando el campo importa. */
    @Test
    void elSelectorDeCampoSoloSeActivaCuandoImporta() throws Exception {
        entrar(List.of("[d/dx]", "x^2"));
        assertTrue(enFx(() -> raiz.lookup("#campoR").isDisabled()), "en una derivada el campo no importa");
        entrar(List.of("x^4+1"));
        assertFalse(enFx(() -> raiz.lookup("#campoR").isDisabled()), "al factorizar el campo importa");
    }

    /** Parte 4: en una tarjeta angosta la fórmula se reduce o se parte, sin barras ni cortes. */
    @Test
    void laFormulaDeLaSolucionNoSeCorta() throws Exception {
        try {
            anchoDeVentana(700);
            entrar(List.of("[∫]", "[→]", "[→]", "[□/□]", "3x+5", "[→]", "(x-1)(x+2)"));
            resolverYEsperar("integral larga");
            assertEquals(EstadoResolucion.RESUELTO, enFx(controlador::estado));
            Thread.sleep(200);
            enFx(() -> {
                Node tarjeta = raiz.lookup("#tarjetaSolucion");
                tarjeta.applyCss();
                assertTrue(tarjeta.lookupAll(".scroll-pane").isEmpty(), "hay un ScrollPane en la tarjeta Solución");
                pe.mathnova.presentation.resolucion.FormulaAjustable f =
                        (pe.mathnova.presentation.resolucion.FormulaAjustable) tarjeta.lookup(".formula-ajustable");
                assertNotNull(f);
                for (Node n : f.getChildrenUnmodifiable()) {
                    if (n.isVisible() && n.isManaged()) {
                        assertTrue(n.getBoundsInParent().getMaxX() <= f.getWidth() + 1,
                                () -> "se sale: " + n + " " + n.getBoundsInParent() + " en " + f.getWidth());
                    }
                }
                return null;
            });
        } finally {
            anchoDeVentana(1280);
        }
    }

    private static void anchoDeVentana(double ancho) throws Exception {
        enFx(() -> {
            ventana.setWidth(ancho);
            return null;
        });
        Thread.sleep(300);
        enFx(() -> {
            raiz.applyCss();
            raiz.layout();
            return null;
        });
    }

    // ------------------------------------------------------------------ cómo entra el estudiante

    /** Limpia el editor y hace los pasos; devuelve el LaTeX del editor. */
    private static String entrar(List<String> pasos) throws Exception {
        limpiarYEscribir("");
        for (String paso : pasos) {
            if (paso.startsWith("[") && paso.endsWith("]") && paso.length() > 2) {
                Teclas.Tecla t = tecla(paso.substring(1, paso.length() - 1));
                enFx(() -> {
                    teclado.pulsar(t);
                    return null;
                });
            } else {
                escribirAMano(paso);
            }
        }
        return enFx(editor::getLatex);
    }

    private static Teclas.Tecla tecla(String etiqueta) {
        if (etiqueta.equals("→")) {
            return Teclas.DERECHA;
        }
        if (etiqueta.equals("⌫")) {
            return Teclas.BORRAR;
        }
        return Stream.concat(Teclas.PESTANAS.stream(), Stream.of(Teclas.GRIEGAS))
                .flatMap(p -> Stream.of(p.izquierda(), p.derecha()))
                .filter(Objects::nonNull)
                .flatMap(b -> b.filas().stream())
                .flatMap(List::stream)
                .filter(t -> t.etiqueta().equals(etiqueta) && t.accion() != Teclas.Accion.NADA)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay tecla " + etiqueta));
    }

    private static String limpiarYEscribir(String s) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!enFx(() -> {
            ventana.toFront();
            editor.limpiar();
            editor.enfocar();
            return Boolean.TRUE.equals(editor.webView().getEngine().executeScript(
                    "document.getElementById('mf').hasFocus() && window.editor.valor() === ''"));
        })) {
            assertTrue(System.nanoTime() < limite, "El editor no tomó el foco");
            Thread.sleep(100);
        }
        escribirAMano(s);
        return enFx(editor::getLatex);
    }

    private static void escribirAMano(String s) throws Exception {
        for (char c : s.toCharArray()) {
            enFx(() -> {
                pulsarTecla(c);
                return null;
            });
        }
    }

    private static void pulsarTecla(char c) {
        KeyCode code = codigo(c);
        boolean shift = Character.isUpperCase(c) || "^()+*;=".indexOf(c) >= 0;
        String t = String.valueOf(c);
        Event.fireEvent(editor.webView(), new KeyEvent(KeyEvent.KEY_PRESSED, "", t, code, shift, false, false, false));
        Event.fireEvent(editor.webView(), new KeyEvent(KeyEvent.KEY_TYPED, t, "", KeyCode.UNDEFINED, shift, false, false, false));
        Event.fireEvent(editor.webView(), new KeyEvent(KeyEvent.KEY_RELEASED, "", t, code, shift, false, false, false));
    }

    private static KeyCode codigo(char c) {
        if (Character.isLetter(c) && c < 128) {
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
            case '*' -> KeyCode.ASTERISK;
            case '=' -> KeyCode.EQUALS;
            case ',' -> KeyCode.COMMA;
            case ';' -> KeyCode.SEMICOLON;
            default -> KeyCode.UNDEFINED;
        };
    }

    private static void elegirCampo(String campo) throws Exception {
        enFx(() -> {
            ToggleButton b = (ToggleButton) raiz.lookup("#campo" + campo);
            if (!b.isSelected()) {
                b.fire();
            }
            return null;
        });
    }

    /** Espera a que "Resolver" se active (los 400 ms), lo pulsa y espera a que termine. */
    private static void resolverYEsperar(String contexto) throws Exception {
        esperar(() -> !raiz.lookup("#botonResolver").isDisabled() || visible("#avisoEntrada"),
                contexto + ": Resolver no se activó");
        String aviso = enFx(() -> visible("#avisoEntrada") ? textoDe("#avisoEntrada") : null);
        if (aviso != null && enFx(() -> raiz.lookup("#botonResolver").isDisabled())) {
            fail(contexto + ": " + aviso + " [estado " + enFx(controlador::estado) + "]");
        }
        enFx(() -> {
            ((Button) raiz.lookup("#botonResolver")).fire();
            return null;
        });
        // La tarjeta Solución (con #carga) ya no está en la pantalla: la solución va en la pizarra.
        esperar(() -> controlador.estado() != EstadoResolucion.CALCULANDO && !visible("#carga"),
                contexto + ": no terminó de calcular");
    }

    private static void esperar(Supplier<Boolean> condicion, String mensaje) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        Thread.sleep(50);
        while (!enFx(condicion)) {
            assertTrue(System.nanoTime() < limite, mensaje);
            Thread.sleep(50);
        }
    }

    private static boolean visible(String id) {
        Node n = raiz.lookup(id);
        return n != null && n.isVisible() && n.isManaged();
    }

    private static String textoDe(String id) {
        Node n = raiz.lookup(id);
        return n instanceof Label l ? l.getText() : String.valueOf(n);
    }

    private static <T> T enFx(Supplier<T> accion) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return accion.get();
        }
        FutureTask<T> tarea = new FutureTask<>(accion::get);
        Platform.runLater(tarea);
        return tarea.get(60, TimeUnit.SECONDS);
    }
}
