package pe.mathnova.presentation.escritura;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.SimboloDudoso;
import pe.mathnova.domain.escritura.TablaConfusiones;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.infrastructure.escritura.ArchivoMuestras;

/**
 * La pestaña "Escribir": el estudiante escribe el ejercicio a mano y lo reconocido aparece, editable, en
 * el mismo campo de arriba donde se escribe con el teclado ({@link Destino}).
 *
 * <ol>
 *   <li>Tras una pausa sin escribir ({@link #ESPERA}, configurable) se leen los trazos en un Task, fuera
 *       del hilo de la interfaz, y aparece "Reconociendo…". El estudiante puede levantar el dedo entre
 *       símbolos: cada trazo nuevo cancela la lectura en curso y vuelve a esperar la pausa.</li>
 *   <li>Cada lectura va con la versión del lienzo. La respuesta solo se usa si el lienzo no cambió desde
 *       entonces (si se escribió "2x", se añadió "+1" y llega tarde la lectura de "2x", se descarta) y si
 *       el estudiante no editó el campo de arriba mientras tanto ({@link Destino#generacion}): una
 *       corrección suya no la pisa una lectura anterior.</li>
 *   <li>Borrar todo invalida las lecturas pendientes. Si la lectura falla, los trazos y lo que había en el
 *       campo se quedan, y "Reintentar" vuelve a leer.</li>
 *   <li>{@link #alTerminar}: "Resolver" justo después del último trazo espera la lectura de lo último
 *       escrito y recién entonces resuelve.</li>
 * </ol>
 */
public final class PanelEscritura extends VBox {

    /** Adónde va lo reconocido: el campo de arriba. */
    public interface Destino {

        /** Cambia cada vez que el estudiante edita el campo por su cuenta (teclado, borrar…). */
        long generacion();

        /**
         * Pone en el campo el LaTeX que leyó el reconocedor.
         *
         * @return null si quedó bien, o un aviso breve si MathNova no lo entiende del todo
         */
        String poner(String latex);
    }

    static final Duration ESPERA = Duration.millis(700);
    static final String LEYENDO = "Reconociendo…";
    static final String PREPARANDO = "Preparando el reconocedor… (solo la primera vez)";
    static final String LISTO = "Listo: revisa la expresión arriba y pulsa Resolver.";
    static final String REVISA = "Revisa la expresión";
    static final String NO_SE_PUDO = "No pude reconocer tu escritura. Tus trazos siguen aquí.";
    static final String SIN_LECTURA = "No reconocí nada. Escribe un poco más grande y vuelve a intentarlo.";
    static final String CONSERVA = "Se conserva tu corrección del campo de arriba.";

    private final ReconocedorEscritura reconocedor;
    private final Destino destino;

    private final LienzoEscritura lienzo = new LienzoEscritura();
    private final ToggleButton lapiz;
    private final ToggleButton borrador;
    private final Label estadoLectura = new Label();
    private final Button reintentar = new Button("Reintentar");
    private final HBox filaEstado;
    private final VBox preguntas = new VBox(6);
    private final PauseTransition espera;

    private Task<ResultadoReconocimiento> lectura;
    /** Cambia con cada cambio del lienzo: una lectura de trazos anteriores se descarta. */
    private long version;
    /** La versión del lienzo que ya está en el campo de arriba (o que ya se intentó leer y falló). */
    private long resuelta = -1;
    private boolean fallo;
    private ResultadoReconocimiento resultado;
    private boolean preparando;
    /** Lo que espera "Resolver": se corre cuando se lee la versión actual (o se descarta si falla). */
    private final List<Runnable> alLeer = new ArrayList<>();
    private final List<Runnable> alFallar = new ArrayList<>();
    /** Solo en desarrollo; null si no se pidió. */
    private final BarraMuestras muestras;
    /** Hay escritura que todavía no llegó al campo de arriba (esperando la pausa o leyendo). */
    private final javafx.beans.property.ReadOnlyBooleanWrapper pendiente =
            new javafx.beans.property.ReadOnlyBooleanWrapper(this, "pendiente", false);

    /** Un destino sin ediciones propias: recibe cada lectura (pruebas y herramientas de desarrollo). */
    public static Destino destino(java.util.function.Consumer<String> usar) {
        return new Destino() {
            @Override
            public long generacion() {
                return 0;
            }

            @Override
            public String poner(String latex) {
                usar.accept(latex);
                return null;
            }
        };
    }

    public PanelEscritura(ReconocedorEscritura reconocedor, java.util.function.Consumer<String> usar) {
        this(reconocedor, destino(usar));
    }

    public PanelEscritura(ReconocedorEscritura reconocedor, java.util.function.Consumer<String> usar,
                          Path carpetaMuestras) {
        this(reconocedor, destino(usar), ESPERA, carpetaMuestras);
    }

    /** Con {@code -Dmathnova.muestras=true} aparece además la barra de muestras para desarrollo. */
    public PanelEscritura(ReconocedorEscritura reconocedor, Destino destino) {
        this(reconocedor, destino, ESPERA,
                Boolean.getBoolean(BarraMuestras.PROPIEDAD) ? ArchivoMuestras.carpetaPorDefecto() : null);
    }

    /**
     * @param pausa           cuánto se espera sin escribir antes de leer
     * @param carpetaMuestras dónde guarda la barra de muestras de desarrollo; null, sin barra
     */
    public PanelEscritura(ReconocedorEscritura reconocedor, Destino destino, Duration pausa, Path carpetaMuestras) {
        this.reconocedor = reconocedor;
        this.destino = destino;
        this.espera = new PauseTransition(pausa == null ? ESPERA : pausa);
        getStyleClass().add("panel-escritura");
        setSpacing(12);

        Label indicacion = new Label("Escribe con el dedo, lápiz o mouse");
        indicacion.getStyleClass().add("subtitulo-tarjeta");

        ToggleGroup grupo = new ToggleGroup();
        lapiz = herramienta("M3 17 L4 13 L14 3 L17 6 L7 16 Z M12 5 L15 8", "Lápiz", grupo);
        borrador = herramienta("M7 17 L2 12 L11 3 L18 10 L11 17 Z M6 8 L13 15 M11 17 H18", "Borrador", grupo);
        lapiz.setSelected(true);
        grupo.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                antes.setSelected(true); // siempre hay una herramienta
                return;
            }
            lienzo.setHerramienta(ahora == lapiz ? LienzoEscritura.Herramienta.LAPIZ
                    : LienzoEscritura.Herramienta.BORRADOR);
        });
        Button deshacer = accion("M5 8 H13 A4 4 0 0 1 13 16 H8 M8 4 L4 8 L8 12", "Deshacer");
        deshacer.setOnAction(e -> {
            lienzo.deshacer();
            despuesDeCambiar();
        });
        Button borrarTodo = accion("M3 5 H17 M8 5 V3 H12 V5 M5 5 L6 17 H14 L15 5", "Borrar todo");
        borrarTodo.setOnAction(e -> {
            lienzo.borrarTodo();
            despuesDeCambiar();
        });
        HBox herramientas = new HBox(6, lapiz, borrador, deshacer, borrarTodo);
        herramientas.setAlignment(Pos.CENTER_LEFT);
        herramientas.getStyleClass().add("barra-escritura");

        lienzo.setOnEmpezar(this::cancelarLectura);
        lienzo.setOnTerminar(this::despuesDeCambiar);
        espera.setOnFinished(e -> leer());

        estadoLectura.getStyleClass().add("escritura-estado");
        estadoLectura.setWrapText(true);
        estadoLectura.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(estadoLectura, Priority.ALWAYS);
        reintentar.getStyleClass().addAll("boton-alternativa", "boton-reintentar-escritura");
        reintentar.setMinWidth(Region.USE_PREF_SIZE);
        reintentar.setOnAction(e -> reintentar());
        filaEstado = new HBox(8, estadoLectura, reintentar);
        filaEstado.setAlignment(Pos.CENTER_LEFT);
        mostrarNodo(reintentar, false);
        mostrarNodo(filaEstado, false);
        mostrarNodo(preguntas, false);

        getChildren().addAll(indicacion, herramientas, lienzo, filaEstado, preguntas);

        if (carpetaMuestras != null) {
            muestras = new BarraMuestras(lienzo, new ArchivoMuestras(carpetaMuestras), this::despuesDeCambiar);
            getChildren().add(muestras);
        } else {
            muestras = null;
        }
    }

    // ------------------------------------------------------------------ API

    /** "Revisa la expresión: …", con el motivo si lo hay. */
    public static String revisa(String motivo) {
        return motivo == null || motivo.isBlank() ? REVISA : REVISA + ": " + motivo;
    }

    /** Al abrir la pestaña: si el modelo no está cargado, se carga ya, en segundo plano. */
    public void alMostrar() {
        if (preparando || reconocedor.listo()) {
            return;
        }
        preparando = true;
        if (lectura == null && !fallo) {
            estado(PREPARANDO, false);
        }
        Thread hilo = new Thread(() -> {
            String error = null;
            try {
                reconocedor.preparar();
            } catch (RuntimeException e) {
                error = e.getMessage();
            }
            String fin = error;
            Platform.runLater(() -> {
                preparando = false;
                if (PREPARANDO.equals(estadoLectura.getText()) && lectura == null) {
                    if (fin == null) {
                        mostrarNodo(filaEstado, false);
                    } else {
                        estado(fin.startsWith(pe.mathnova.infrastructure.escritura.ReconocedorUniMERNet.FALTA)
                                ? fin : "No se pudo preparar el reconocedor. " + fin, true);
                        mostrarNodo(reintentar, true);
                    }
                }
            });
        }, "preparar-reconocedor");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Corre {@code listo} cuando lo que está en el lienzo ya está en el campo de arriba: enseguida si no
     * hay nada pendiente, o al terminar la lectura de lo último escrito (si estaba esperando la pausa, se
     * lee ya). Si esa lectura falla o el lienzo cambia antes, corre {@code siFalla}.
     */
    public void alTerminar(Runnable listo, Runnable siFalla) {
        boolean pendiente = espera.getStatus() == javafx.animation.Animation.Status.RUNNING || lectura != null
                || (!lienzo.isVacio() && resuelta != version);
        if (!pendiente) {
            listo.run();
            return;
        }
        alLeer.add(listo);
        alFallar.add(siFalla);
        if (lectura == null) {
            espera.stop();
            leer();
        }
    }

    /**
     * Pone en el lienzo los trazos de una muestra grabada, como si se acabaran de dibujar (se leen tras la
     * pausa). Lo usa la autoprueba para comprobar una instalación.
     */
    public void cargarMuestra(pe.mathnova.domain.escritura.Muestra m) {
        cancelarLectura();
        lienzo.cargar(m);
        despuesDeCambiar();
    }

    /** Lo último que devolvió el reconocedor, tal cual (antes de adaptarlo al editor); null si nada. */
    public String ultimaLectura() {
        return resultado == null ? null : resultado.latex();
    }

    /** true si hay una lectura en curso o esperando la pausa. */
    public boolean leyendo() {
        return lectura != null || espera.getStatus() == javafx.animation.Animation.Status.RUNNING;
    }

    /** true mientras haya escritura que todavía no llegó al campo de arriba ("Resolver" la espera). */
    public javafx.beans.property.ReadOnlyBooleanProperty pendienteProperty() {
        return pendiente.getReadOnlyProperty();
    }

    private void marcarPendiente() {
        pendiente.set(leyendo());
    }

    // ------------------------------------------------------------------ lectura

    /** Después de un trazo, de deshacer o de borrar: tras la pausa se lee lo que haya. */
    private void despuesDeCambiar() {
        cancelarLectura();
        if (lienzo.isVacio()) {
            resultado = null;
            fallo = false;
            resuelta = version;
            mostrarNodo(filaEstado, false);
            mostrarNodo(preguntas, false);
            fallaron(); // lo que esperaba la lectura ya no tiene qué leer
            marcarPendiente();
            return;
        }
        espera.playFromStart();
        marcarPendiente();
    }

    private void cancelarLectura() {
        espera.stop();
        version++;
        if (lectura != null) {
            lectura.cancel();
            lectura = null;
        }
        if (!preparando) {
            mostrarNodo(filaEstado, false);
        }
        marcarPendiente();
    }

    private void reintentar() {
        mostrarNodo(reintentar, false);
        fallo = false;
        if (lienzo.isVacio()) {
            mostrarNodo(filaEstado, false);
            alMostrar();
            return;
        }
        cancelarLectura();
        leer();
    }

    private void leer() {
        List<Trazo> trazos = List.copyOf(lienzo.trazos());
        if (trazos.isEmpty()) {
            return;
        }
        long esta = ++version;
        long generacion = destino.generacion();
        fallo = false;
        estado(reconocedor.listo() ? LEYENDO : PREPARANDO, false);
        Task<ResultadoReconocimiento> tarea = new Task<>() {
            @Override
            protected ResultadoReconocimiento call() {
                return reconocedor.reconocer(trazos);
            }
        };
        // Los manejadores del Task corren en el hilo de JavaFX.
        tarea.setOnSucceeded(e -> {
            if (esta != version) {
                return; // el lienzo cambió: lectura vieja
            }
            lectura = null;
            resuelta = esta;
            marcarPendiente();
            if (generacion != destino.generacion()) {
                estado(CONSERVA, false); // el estudiante editó el campo mientras se leía
                terminaron(); // "Resolver" resuelve lo que él dejó en el campo
                return;
            }
            mostrarResultado(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            if (esta != version) {
                return;
            }
            lectura = null;
            resuelta = esta;
            fallo = true;
            marcarPendiente();
            estado(NO_SE_PUDO + detalle(tarea.getException()), true);
            mostrarNodo(reintentar, true);
            fallaron();
        });
        lectura = tarea;
        marcarPendiente();
        Thread hilo = new Thread(tarea, "lectura-escritura");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarResultado(ResultadoReconocimiento r) {
        resultado = r;
        if (r == null || r.latex().isBlank()) {
            estado(SIN_LECTURA, true);
            mostrarNodo(preguntas, false);
            fallaron();
            return;
        }
        String aviso = destino.poner(r.latex());
        if (aviso == null) {
            estado(LISTO, false);
        } else {
            estado(aviso, true);
        }
        mostrarPreguntas(r);
        terminaron();
    }

    private void terminaron() {
        List<Runnable> listos = List.copyOf(alLeer);
        alLeer.clear();
        alFallar.clear();
        listos.forEach(Runnable::run);
    }

    /** El motivo, si dice algo útil para el estudiante (por ejemplo, que falta el reconocedor). */
    static String detalle(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String m = t.getMessage();
            if (m != null && m.startsWith(pe.mathnova.infrastructure.escritura.ReconocedorUniMERNet.FALTA)) {
                return " " + m;
            }
        }
        return "";
    }

    private void fallaron() {
        List<Runnable> fallos = List.copyOf(alFallar);
        alLeer.clear();
        alFallar.clear();
        fallos.forEach(Runnable::run);
    }

    /** Los símbolos dudosos, si el reconocedor los da: "¿El símbolo es x ×?". */
    private void mostrarPreguntas(ResultadoReconocimiento r) {
        preguntas.getChildren().clear();
        for (SimboloDudoso d : r.dudosos()) {
            FlowPane fila = new FlowPane(6, 6);
            fila.setAlignment(Pos.CENTER_LEFT);
            Label pregunta = new Label("¿El símbolo es");
            pregunta.getStyleClass().add("entendi-pregunta");
            fila.getChildren().add(pregunta);
            for (String alternativa : d.alternativas()) {
                Button b = new Button(TablaConfusiones.aLaVista(alternativa));
                b.getStyleClass().add("boton-alternativa-simbolo");
                b.setAccessibleText("El símbolo es " + TablaConfusiones.aLaVista(alternativa));
                b.setOnAction(e -> mostrarResultado(resultado.elegir(d, alternativa)));
                fila.getChildren().add(b);
            }
            Label cierre = new Label("?");
            cierre.getStyleClass().add("entendi-pregunta");
            fila.getChildren().add(cierre);
            preguntas.getChildren().add(fila);
        }
        mostrarNodo(preguntas, !r.dudosos().isEmpty());
    }

    private void estado(String texto, boolean error) {
        estadoLectura.setText(texto);
        estadoLectura.getStyleClass().remove("mensaje-error");
        if (error) {
            estadoLectura.getStyleClass().add("mensaje-error");
        }
        mostrarNodo(reintentar, false);
        mostrarNodo(filaEstado, true);
    }

    // ------------------------------------------------------------------ apoyo

    private static ToggleButton herramienta(String icono, String nombre, ToggleGroup grupo) {
        ToggleButton b = new ToggleButton(null, svg(icono));
        b.setToggleGroup(grupo);
        estilo(b, nombre);
        return b;
    }

    private static Button accion(String icono, String nombre) {
        Button b = new Button(null, svg(icono));
        estilo(b, nombre);
        return b;
    }

    private static void estilo(ButtonBase b, String nombre) {
        b.getStyleClass().add("herramienta-escritura");
        b.setAccessibleText(nombre);
        b.setTooltip(new Tooltip(nombre));
    }

    private static SVGPath svg(String contenido) {
        SVGPath s = new SVGPath();
        s.setContent(contenido);
        s.getStyleClass().add("herramienta-icono");
        return s;
    }

    private static void mostrarNodo(Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }

    // ------------------------------------------------------------------ para las pruebas

    LienzoEscritura lienzo() {
        return lienzo;
    }

    ResultadoReconocimiento resultado() {
        return resultado;
    }

    Label estadoLectura() {
        return estadoLectura;
    }

    Node filaEstado() {
        return filaEstado;
    }

    Button botonReintentar() {
        return reintentar;
    }

    BarraMuestras muestras() {
        return muestras;
    }
}
