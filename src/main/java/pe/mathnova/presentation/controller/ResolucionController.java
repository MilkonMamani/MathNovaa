package pe.mathnova.presentation.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.FaseFactorizacion;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.sugerencias.Sugerencia;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.MetodoNoAplicable;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.animacion.AnimadorResolucion;
import pe.mathnova.presentation.editor.EditorMatematico;
import pe.mathnova.presentation.editor.TecladoMatematico;
import pe.mathnova.presentation.grafica.GraficaFuncion;
import pe.mathnova.presentation.latex.ImagenLatex;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.resolucion.ContenidoEcuacion;
import pe.mathnova.presentation.resolucion.ContenidoFactorizacion;
import pe.mathnova.presentation.resolucion.ContenidoFraccion;
import pe.mathnova.presentation.resolucion.ContenidoDerivada;
import pe.mathnova.presentation.resolucion.ContenidoIntegral;
import pe.mathnova.presentation.resolucion.ContenidoLimite;
import pe.mathnova.presentation.resolucion.ContenidoPestanas;
import pe.mathnova.presentation.resolucion.FilaPaso;
import pe.mathnova.presentation.resolucion.PanelAnalisis;
import pe.mathnova.presentation.resolucion.PropiedadesFuncion;
import pe.mathnova.presentation.resolucion.VistaExploracion;
import pe.mathnova.presentation.resolucion.VistaPaso;
import pe.mathnova.presentation.resolucion.VistaResolucion;

/**
 * Pantalla "Resolver ejercicio" (HU04–HU09, HU18, HU33, HU36–HU38).
 *
 * <ul>
 *   <li>Arriba, el editor visual. "Resolver" decide qué hacer según lo que se escribió: factorizar,
 *       simplificar o resolver la ecuación; los límites, derivadas e integrales se reconocen pero todavía
 *       no se resuelven.</li>
 *   <li>Resultado en vivo (HU37, RF71): 400 ms después de la última tecla, si la expresión está
 *       completa, se calcula en un Task y se muestra en gris. Un cambio en la entrada descarta el
 *       cálculo anterior.</li>
 *   <li>A la izquierda, Teclado | Gráfica | Análisis | Exploración; al resolver se pasa a Gráfica.</li>
 * </ul>
 *
 * La resolución corre en un Task, fuera del hilo de la interfaz (RNF06); las animaciones las hace
 * {@link AnimadorResolucion}.
 */
@Component
@Scope("prototype")
public class ResolucionController {

    /** Por debajo de este ancho de ventana, el procedimiento pasa debajo del panel izquierdo. */
    static final double ANCHO_DOS_COLUMNAS = 1100;
    /** RF71. */
    static final Duration ESPERA_EN_VIVO = Duration.millis(400);

    private static final PseudoClass MAL = PseudoClass.getPseudoClass("mal");
    private static final PseudoClass NEUTRA = PseudoClass.getPseudoClass("neutra");
    private static final String PROXIMAMENTE = "Disponible próximamente";

    private static final String LEYENDO = "Leyendo la expresión…";
    private static final String PROBANDO = "Probando métodos…";
    private static final String VERIFICANDO = "Verificando el resultado…";
    private static final List<String> FASES = List.of(LEYENDO, PROBANDO, VERIFICANDO);

    /**
     * Lo que devuelve el Task, todo calculado fuera del hilo de la interfaz.
     *
     * @param contenido lo que muestran Gráfica, Análisis y Exploración, según el tipo de ejercicio
     * @param ans       LaTeX para la tecla "ans"; null si no hay un valor que reutilizar
     */
    private record Resultado(VistaResolucion vista, ContenidoPestanas contenido, String ans,
                             List<Sugerencia> sugerencias) {
    }

    /** Exploración sin métodos para el ejercicio que está en pantalla (el aviso inicial es otro). */
    static final String SIN_METODOS_EN_PANTALLA = "Este ejercicio se resolvió sin probar métodos.";

    private final FactorizacionService factorizacion;
    private final ResolverService resolverService;
    private final ConfiguracionService configuracion;
    private final AnimadorResolucion animador = new AnimadorResolucion();
    private final RenderizadorLatex latexSolucion = new RenderizadorLatex(28f);
    private final RenderizadorLatex latexPasos = new RenderizadorLatex(18f);
    private final RenderizadorLatex latexComparacion = new RenderizadorLatex(16f);
    private final RenderizadorLatex latexLeyenda = new RenderizadorLatex(14f);
    private final PauseTransition espera = new PauseTransition(ESPERA_EN_VIVO);
    private final ToggleGroup grupoPestanas = new ToggleGroup();
    /** Configuración "mostrar_codigos": si se ven F16, F04… junto al nombre del método. */
    private boolean codigos;

    private EditorMatematico editor;
    private TecladoMatematico teclado;

    @FXML
    private ScrollPane raiz;
    @FXML
    private StackPane zonaEditor;
    @FXML
    private ToggleButton campoQ;
    @FXML
    private ToggleButton campoR;
    @FXML
    private ToggleButton campoC;
    @FXML
    private StackPane envolturaR;
    @FXML
    private StackPane envolturaC;
    @FXML
    private Button botonResolver;
    @FXML
    private Label resultadoEnVivo;
    @FXML
    private Label avisoEntrada;
    @FXML
    private GridPane columnas;
    @FXML
    private VBox columnaIzquierda;
    @FXML
    private ToggleButton pestanaTeclado;
    @FXML
    private ToggleButton pestanaGrafica;
    @FXML
    private ToggleButton pestanaAnalisis;
    @FXML
    private ToggleButton pestanaExploracion;
    @FXML
    private VBox vistaTeclado;
    @FXML
    private StackPane zonaTeclado;
    @FXML
    private VBox vistaGrafica;
    @FXML
    private VBox vistaAnalisis;
    @FXML
    private VBox analisis;
    @FXML
    private Label sinAnalisis;
    @FXML
    private VBox vistaExploracion;
    @FXML
    private FlowPane tarjetasMetodos;
    @FXML
    private Label sinMetodos;
    @FXML
    private GridPane comparacion;
    @FXML
    private Label tituloComparacion;
    @FXML
    private VBox tarjetaSolucion;
    @FXML
    private VBox tarjetaGrafica;
    @FXML
    private StackPane zonaGrafica;
    @FXML
    private Label graficaNoDisponible;
    @FXML
    private Label ayudaGrafica;
    @FXML
    private TitledPane panelPropiedades;
    @FXML
    private VBox propiedades;
    @FXML
    private StackPane iconoSolucion;
    @FXML
    private Label subtituloSolucion;
    @FXML
    private VBox cuerpoSolucion;
    @FXML
    private ScrollPane marcoFormula;
    @FXML
    private ImageView formula;
    @FXML
    private Label etiquetaVerificado;
    @FXML
    private SVGPath marcaVerificado;
    @FXML
    private VBox irreducibles;
    @FXML
    private FlowPane alternativas;
    @FXML
    private VBox carga;
    @FXML
    private Label mensajeCarga;
    @FXML
    private Label errorSolucion;
    @FXML
    private VBox avisoNoAplica;
    @FXML
    private Label tituloNoAplica;
    @FXML
    private Label motivoNoAplica;
    @FXML
    private VBox tarjetaProcedimiento;
    @FXML
    private VBox pasos;
    @FXML
    private Label sinPasos;
    @FXML
    private VBox zonaSugerencias;
    @FXML
    private VBox sugerencias;
    @FXML
    private VBox secciones;
    @FXML
    private TitledPane panelVerificacion;
    @FXML
    private VBox lineasVerificacion;

    /** Lo último que decidió "Resolver" sobre la entrada. */
    private ResolverService.Plan plan = new ResolverService.Plan.Vacio();
    /** El Task está corriendo o se está esperando el mínimo del indicador. */
    private boolean calculando;
    /** Cambia con cada Resolver: los resultados de un ejercicio anterior se descartan. */
    private int ejecucion;
    /** Cambia con cada tecla: el resultado en vivo de una entrada anterior se descarta. */
    private int enVivo;
    private Task<Optional<String>> tareaEnVivo;
    /** Se pidió otro ejercicio mientras el Task corría: se resuelve apenas termine. */
    private boolean pendiente;
    /** HU08: el método con que empieza la factorización ("Probar" en Exploración); null es automático. */
    private String metodoForzado;
    /** Para la tecla "ans". */
    private String ultimaRespuesta;
    private Boolean dosColumnas;
    /** El campo de la barra (Q, R o C). */
    private Campo campo = Campo.Q;
    private final ToggleGroup grupoCampos = new ToggleGroup();
    /** El texto del resultado que está en pantalla; null si no hay uno. */
    private String textoResuelto;
    /** La tabla Q | R | C ya se pidió para el resultado en pantalla. */
    private boolean comparacionPedida;
    /** Cambia con cada comparación pedida: una comparación de un resultado anterior se descarta. */
    private int comparacionActual;
    /** La sugerencia "Asíntotas": al terminar de resolver se abre Análisis. */
    private boolean abrirAnalisis;
    /** Lo que se compara en Q, R y C para el resultado en pantalla; null si no hay comparación. */
    private String textoComparacion;
    /** El texto del sinMetodos inicial, del FXML. */
    private String sinMetodosInicial;

    public ResolucionController(FactorizacionService factorizacion, ResolverService resolverService,
                                ConfiguracionService configuracion) {
        this.factorizacion = factorizacion;
        this.resolverService = resolverService;
        this.configuracion = configuracion;
    }

    @FXML
    void initialize() {
        editor = new EditorMatematico();
        zonaEditor.getChildren().setAll(editor);
        editor.setOnCambio(e -> alCambiar());
        editor.setOnEnter(e -> resolver());

        teclado = new TecladoMatematico(editor);
        teclado.setRespuestaAnterior(() -> ultimaRespuesta);
        teclado.setOnConfiguracion(this::abrirConfiguracion);
        zonaTeclado.getChildren().setAll(teclado);

        espera.setOnFinished(e -> actualizarEntrada());

        prepararCampos();

        prepararPestanas();
        codigos = configuracion.mostrarCodigos();

        // Cada tarjeta mide lo que su contenido: ninguna se estira para igualar a la de al lado.
        GridPane.setValignment(columnaIzquierda, VPos.TOP);
        GridPane.setValignment(tarjetaProcedimiento, VPos.TOP);
        GridPane.setFillHeight(columnaIzquierda, false);
        GridPane.setFillHeight(tarjetaProcedimiento, false);
        raiz.sceneProperty().addListener((obs, antes, escena) -> seguirAncho(escena));
        acomodar(ANCHO_DOS_COLUMNAS);

        sinMetodosInicial = sinMetodos.getText();
        limpiarResultado();
        llenarComparacion(null);
        sinPasos.setText("Escribe un ejercicio y pulsa Resolver para ver el procedimiento.");
        mostrar(sinPasos, true);
        actualizarBoton();
    }

    // ---------------------------------------------------------------- API para el Navegador

    /** Pone el texto (sección 9) en el editor y lo resuelve en automático. */
    public void resolver(String texto) {
        metodoForzado = null;
        try {
            editor.setLatex(ImpresoraLatex.imprimir(Parser.parse(texto)));
        } catch (RuntimeException e) {
            // Texto mal escrito: se resuelve igual y se muestra el aviso.
        }
        resolverTexto(texto);
    }

    /** El texto (sección 9) de lo que hay en el editor. */
    public String texto() {
        try {
            return editor.getTexto();
        } catch (TraductorLatex.LatexNoValido e) {
            return "";
        }
    }

    public void enfocar() {
        editor.enfocar();
    }

    // ---------------------------------------------------------------- campo (HU20, HU21)

    /**
     * Q, R o C: siempre hay uno elegido. Si hay un resultado en pantalla, se vuelve a resolver en el
     * campo nuevo con la animación.
     */
    private void prepararCampos() {
        Map<ToggleButton, Campo> botones = Map.of(campoQ, Campo.Q, campoR, Campo.R, campoC, Campo.C);
        botones.keySet().forEach(b -> b.setToggleGroup(grupoCampos));
        campoQ.setSelected(true);
        grupoCampos.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                antes.setSelected(true); // un clic en el elegido no lo deja sin campo
                return;
            }
            Campo nuevo = botones.get((ToggleButton) ahora);
            if (nuevo == campo) {
                return;
            }
            campo = nuevo;
            if (textoResuelto != null) {
                resolverTexto(textoResuelto);
            } else {
                espera.playFromStart(); // el resultado en vivo cambia con el campo
            }
            editor.enfocar();
        });
    }

    /** Para las pruebas y el Navegador: el campo elegido. */
    public Campo campo() {
        return campo;
    }

    // ---------------------------------------------------------------- pestañas

    private void prepararPestanas() {
        List<ToggleButton> botones = List.of(pestanaTeclado, pestanaGrafica, pestanaAnalisis, pestanaExploracion);
        List<Node> vistas = List.of(vistaTeclado, vistaGrafica, vistaAnalisis, vistaExploracion);
        for (int i = 0; i < botones.size(); i++) {
            ToggleButton b = botones.get(i);
            Node vista = vistas.get(i);
            b.setToggleGroup(grupoPestanas);
            b.setOnAction(e -> {
                if (!b.isSelected()) {
                    b.setSelected(true);
                }
                for (Node v : vistas) {
                    mostrar(v, v == vista);
                }
                if (b == pestanaExploracion) {
                    pedirComparacion(); // la tabla Q | R | C se calcula solo cuando se abre
                }
                editor.enfocar();
            });
        }
        pestanaTeclado.fire();
    }

    private void abrirConfiguracion() {
        MenuItem item = new MenuItem("Configuración: " + PROXIMAMENTE.toLowerCase());
        item.setDisable(true);
        ContextMenu menu = new ContextMenu(item);
        menu.show(teclado, Side.TOP, teclado.getWidth() - 220, 30);
    }

    // ---------------------------------------------------------------- entrada y resultado en vivo

    /** Cada tecla: se reinicia la espera de 400 ms y se descarta el cálculo en vivo anterior. */
    private void alCambiar() {
        metodoForzado = null;
        enVivo++;
        if (tareaEnVivo != null) {
            tareaEnVivo.cancel();
            tareaEnVivo = null;
        }
        if (!editor.isCompleto()) {
            // El editor ya avisa con ⚠: no se repite el error del parser.
            mostrar(resultadoEnVivo, false);
            mostrar(avisoEntrada, false);
        }
        espera.playFromStart();
    }

    /** Pasados los 400 ms: qué hará Resolver, el aviso y el resultado en vivo. */
    private void actualizarEntrada() {
        String texto;
        try {
            texto = editor.getTexto();
            plan = resolverService.decidir(texto);
        } catch (TraductorLatex.LatexNoValido e) {
            texto = null;
            plan = new ResolverService.Plan.NoSoportado(e.getMessage());
        }
        boolean completo = editor.isCompleto();
        String aviso = switch (plan) {
            case ResolverService.Plan.Invalido inv -> completo ? inv.mensaje() : "";
            case ResolverService.Plan.Operacion op -> op.mensaje();
            case ResolverService.Plan.NoSoportado no -> no.mensaje();
            default -> "";
        };
        avisoEntrada.setText(aviso);
        mostrar(avisoEntrada, !aviso.isEmpty());
        actualizarBoton();

        mostrar(resultadoEnVivo, false);
        if (completo && plan.resoluble() && texto != null) {
            calcularEnVivo(texto);
        }
    }

    private void calcularEnVivo(String texto) {
        int este = enVivo;
        Campo enCampo = campo;
        Task<Optional<String>> tarea = new Task<>() {
            @Override
            protected Optional<String> call() {
                return resolverService.enVivo(texto, enCampo);
            }
        };
        tarea.setOnSucceeded(e -> {
            if (este == enVivo && !tarea.isCancelled()) {
                Optional<String> r = tarea.getValue();
                resultadoEnVivo.setText(r.map(FormateadorTexto::pizarraConNegativos).orElse(""));
                mostrar(resultadoEnVivo, r.isPresent());
            }
        });
        tareaEnVivo = tarea;
        Thread hilo = new Thread(tarea, "resultado-en-vivo");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Descarta el resultado en vivo que haya y lo calcula para {@code texto}. */
    private void refrescarEnVivo(String texto) {
        enVivo++;
        if (tareaEnVivo != null) {
            tareaEnVivo.cancel();
            tareaEnVivo = null;
        }
        mostrar(resultadoEnVivo, false);
        if (plan.resoluble()) {
            calcularEnVivo(texto);
        }
    }

    private void actualizarBoton() {
        botonResolver.setDisable(calculando || !plan.resoluble());
    }

    // ---------------------------------------------------------------- Resolver

    /**
     * HU04. Si había una animación en curso, se cancela y empieza el ejercicio nuevo. Mientras el
     * Task corre, el botón queda deshabilitado; se habilita otra vez cuando empieza la cascada.
     */
    @FXML
    void resolver() {
        String texto;
        try {
            texto = editor.getTexto();
        } catch (TraductorLatex.LatexNoValido e) {
            avisoEntrada.setText(e.getMessage());
            mostrar(avisoEntrada, true);
            return;
        }
        resolverTexto(texto);
    }

    private void resolverTexto(String texto) {
        espera.stop();
        plan = resolverService.decidir(texto);
        // El texto pudo llegar sin teclear (una sugerencia, el historial, Enter antes de los 400 ms): el
        // resultado en vivo que había era del ejercicio anterior.
        refrescarEnVivo(texto);
        if (!plan.resoluble()) {
            actualizarBotonYAviso();
            return;
        }
        if (calculando) {
            pendiente = true;
            return;
        }
        ResolverService.Plan esteplan = plan;
        String forzado = esteplan instanceof ResolverService.Plan.Factorizar
                || esteplan instanceof ResolverService.Plan.ResolverEcuacion
                || esteplan instanceof ResolverService.Plan.CalcularLimite
                || esteplan instanceof ResolverService.Plan.Derivar
                || esteplan instanceof ResolverService.Plan.Integrar
                || esteplan instanceof ResolverService.Plan.IntegralMultiple ? metodoForzado : null;
        int esta = ++ejecucion;
        animador.cancelar();
        animador.setActivas(configuracion.animacionesActivas());
        codigos = configuracion.mostrarCodigos();
        boolean conCodigos = codigos;
        Campo enCampo = campo;
        textoResuelto = null;
        comparacionPedida = false;
        comparacionActual++;
        llenarComparacion(null);

        limpiarResultado();
        mostrar(avisoEntrada, false);
        pestanaGrafica.fire();
        mostrarCarga(true);
        calculando = true;
        actualizarBoton();

        long inicio = System.nanoTime();
        Task<Resultado> tarea = new Task<>() {
            @Override
            protected Resultado call() {
                return calcular(texto, esteplan, forzado, enCampo, conCodigos, fase -> updateMessage(mensaje(fase)));
            }

        };
        animador.seguirFases(mensajeCarga, tarea.messageProperty(), FASES);
        tarea.setOnSucceeded(e -> {
            Resultado resultado = tarea.getValue();
            animador.trasCargaMinima(inicio, () -> {
                if (esta == ejecucion && !reintentarPendiente()) {
                    mostrarResultado(resultado);
                    textoResuelto = texto;
                    if (vistaExploracion.isVisible()) {
                        pedirComparacion();
                    }
                }
            });
        });
        tarea.setOnFailed(e -> {
            Throwable causa = tarea.getException();
            animador.trasCargaMinima(inicio, () -> {
                if (esta != ejecucion || reintentarPendiente()) {
                    return;
                }
                if (causa instanceof MetodoNoAplicable noAplica) {
                    mostrarNoAplica(noAplica);
                } else {
                    String motivo = causa == null || causa.getMessage() == null ? "error desconocido" : causa.getMessage();
                    mostrarError("No se pudo resolver: " + motivo);
                }
            });
        });
        Thread hilo = new Thread(tarea, "resolucion");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Fuera del hilo de la interfaz. */
    private Resultado calcular(String texto, ResolverService.Plan p, String forzado, Campo enCampo, boolean conCodigos,
                               java.util.function.Consumer<FaseFactorizacion> avance) {
        return switch (p) {
            case ResolverService.Plan.FactorizarFunciones f -> {
                ResolucionFunciones r = factorizacion.factorizarFunciones(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, factorizacion.verificar(r), conCodigos);
                yield new Resultado(v, ContenidoFactorizacion.de(r, v, texto, conCodigos), v.latexFormula(),
                        factorizacion.sugerencias(r));
            }
            case ResolverService.Plan.Simplificar s -> {
                Simplificacion simp = factorizacion.simplificar(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(simp, factorizacion.verificar(simp), conCodigos);
                yield new Resultado(v, ContenidoFraccion.de(simp, v, factorizacion.asintotas(simp), texto, conCodigos),
                        v.latexFormula(), factorizacion.sugerencias(simp));
            }
            case ResolverService.Plan.CalcularLimite l -> {
                ResolucionLimite r = resolverService.resolverLimite(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.resultado() instanceof ResultadoLimite.Finito f ? f.latex() : null;
                yield new Resultado(v, ContenidoLimite.de(r, resolverService.limites(), conCodigos), ans, List.of());
            }
            case ResolverService.Plan.Derivar d -> {
                pe.mathnova.domain.calculo.derivadas.ResolucionDerivada r =
                        resolverService.resolverDerivada(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.esImplicita() ? null : ImpresoraLatex.imprimir(r.resultado());
                yield new Resultado(v, ContenidoDerivada.de(r, resolverService.caminos(r), conCodigos), ans, List.of());
            }
            case ResolverService.Plan.Integrar i -> {
                pe.mathnova.domain.calculo.integrales.ResolucionIntegral r =
                        resolverService.resolverIntegral(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.definida() && r.resuelta() ? ImpresoraLatex.imprimir(r.valor()) : null;
                yield new Resultado(v, ContenidoIntegral.de(r, resolverService.caminos(r), conCodigos), ans, List.of());
            }
            case ResolverService.Plan.Parciales pp -> {
                pe.mathnova.domain.calculo.parciales.ResolucionParcial r =
                        resolverService.resolverParciales(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.resultado() == null ? null : ImpresoraLatex.imprimir(r.resultado());
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoParciales.de(r), ans,
                        pe.mathnova.domain.sugerencias.MotorSugerencias.paraVariasVariables(r.funcion()));
            }
            case ResolverService.Plan.LimiteDoble ld -> {
                pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble r =
                        resolverService.resolverLimiteDoble(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.existe() ? ImpresoraLatex.imprimir(r.valor()) : null;
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoLimiteDoble.de(r), ans, List.of());
            }
            case ResolverService.Plan.Extremos ex -> {
                pe.mathnova.domain.calculo.extremos.ResolucionExtremos r =
                        resolverService.resolverExtremos(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoExtremos.de(r), null, List.of());
            }
            case ResolverService.Plan.IntegralMultiple im -> {
                pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple r =
                        resolverService.resolverIntegralMultiple(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.resuelta() ? ImpresoraLatex.imprimir(r.valor()) : null;
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoIntegralMultiple.de(r,
                        resolverService.caminos(r), conCodigos), ans, List.of());
            }
            case ResolverService.Plan.ResolverEcuacion ec -> {
                ResolucionEcuacion r = resolverService.resolverEcuacion(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                String ans = r.soluciones().size() == 1
                        ? ImpresoraLatex.imprimir(new pe.mathnova.domain.expresion.Constante(r.soluciones().getFirst().valor()))
                        : null;
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                yield new Resultado(v, ContenidoEcuacion.de(r, v, conCodigos), ans, List.of());
            }
            default -> {
                Resolucion r = factorizacion.factorizar(texto, forzado, enCampo, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, factorizacion.verificar(r), conCodigos);
                yield new Resultado(v, ContenidoFactorizacion.de(r, v, texto, conCodigos), v.latexFormula(),
                        factorizacion.sugerencias(r));
            }
        };
    }

    private void actualizarBotonYAviso() {
        String aviso = switch (plan) {
            case ResolverService.Plan.Invalido inv -> inv.mensaje();
            case ResolverService.Plan.Operacion op -> op.mensaje();
            case ResolverService.Plan.NoSoportado no -> no.mensaje();
            default -> "";
        };
        avisoEntrada.setText(aviso);
        mostrar(avisoEntrada, !aviso.isEmpty());
        actualizarBoton();
    }

    /** Si llegó otro ejercicio durante el cálculo, se descarta este resultado y se resuelve el nuevo. */
    private boolean reintentarPendiente() {
        boolean hay = pendiente;
        pendiente = false;
        if (!hay) {
            return false;
        }
        animador.detenerFases();
        terminarCalculo();
        resolver();
        return true;
    }

    private static String mensaje(FaseFactorizacion fase) {
        return switch (fase) {
            case LECTURA -> LEYENDO;
            case METODOS -> PROBANDO;
            case VERIFICACION -> VERIFICANDO;
        };
    }

    /** "Probar" en Exploración: vuelve a resolver empezando por ese método; la cascada se repite. */
    private void resolverCon(String codigo) {
        String texto = texto();
        metodoForzado = codigo;
        resolverTexto(texto);
    }

    // ---------------------------------------------------------------- resultado

    private void mostrarResultado(Resultado resultado) {
        VistaResolucion v = resultado.vista();
        ContenidoPestanas contenido = resultado.contenido();
        animador.detenerFases();
        mostrarCarga(false);
        terminarCalculo();
        ultimaRespuesta = resultado.ans();
        textoComparacion = contenido.comparacion().orElse(null);

        subtituloSolucion.setText(v.subtitulo());
        formula.setImage(ImagenLatex.de(latexSolucion, contenido.latexSolucion()));
        marcoFormula.setHvalue(0);

        // "Verificado" solo con pasos y comprobación correcta; si no, gris.
        VistaResolucion.Etiqueta e = v.etiqueta();
        etiquetaVerificado.setText(e.texto());
        etiquetaVerificado.pseudoClassStateChanged(MAL, e == VistaResolucion.Etiqueta.NO_COINCIDE);
        etiquetaVerificado.pseudoClassStateChanged(NEUTRA, e == VistaResolucion.Etiqueta.IRREDUCIBLE
                || e == VistaResolucion.Etiqueta.SIN_FACTORIZACION || e == VistaResolucion.Etiqueta.SIN_SOLUCION
                || e == VistaResolucion.Etiqueta.IRREDUCIBLE_R || e == VistaResolucion.Etiqueta.APROXIMADO
                || e == VistaResolucion.Etiqueta.NO_FACTORIZABLE || e == VistaResolucion.Etiqueta.NO_EXISTE
                || e == VistaResolucion.Etiqueta.SIN_CALCULAR);
        etiquetaVerificado.setGraphic(e == VistaResolucion.Etiqueta.VERIFICADO ? marcaVerificado : null);
        // Parte E: si la verificación fue numérica (radicales anidados), el tooltip lo dice.
        etiquetaVerificado.setTooltip(v.ayudaEtiqueta() == null ? null : new Tooltip(v.ayudaEtiqueta()));
        mostrar(etiquetaVerificado, true);
        mostrar(iconoSolucion, true);

        for (String nota : v.notas()) {
            irreducibles.getChildren().add(nota(nota, "nota-irreducible"));
        }

        // Otros caminos, como antes; también están en Exploración.
        if (!v.alternativas().isEmpty() && contenido.exploracion().probables()) {
            Label rotulo = new Label("También se puede resolver con:");
            rotulo.getStyleClass().add("alternativas-rotulo");
            alternativas.getChildren().add(rotulo);
            for (VistaResolucion.Alternativa a : v.alternativas()) {
                Button enlace = new Button(a.nombre());
                enlace.getStyleClass().add("boton-alternativa");
                enlace.setTooltip(new Tooltip("Resolver otra vez empezando por: " + a.ayuda()));
                enlace.setOnAction(ev -> resolverCon(a.codigo()));
                alternativas.getChildren().add(enlace);
            }
        }
        mostrar(alternativas, !alternativas.getChildren().isEmpty());

        List<FilaPaso> filas = new ArrayList<>();
        for (VistaPaso p : v.pasos()) {
            FilaPaso fila = new FilaPaso(p, latexPasos, animador);
            filas.add(fila);
            pasos.getChildren().add(fila.nodo());
        }
        if (v.sinPasos() != null) {
            sinPasos.setText(v.sinPasos());
            mostrar(sinPasos, true);
        }

        // HU18
        for (String linea : v.verificacion()) {
            lineasVerificacion.getChildren().add(nota(linea, "linea-verificacion"));
        }
        if (!v.verificacion().isEmpty()) {
            lineasVerificacion.getChildren().add(nota(v.coincide() ? "✓ coincide con la entrada"
                    : "✗ NO coincide con la entrada", v.coincide() ? "coincide" : "no-coincide"));
        }
        mostrar(panelVerificacion, !v.verificacion().isEmpty());
        mostrar(secciones, true);

        // Análisis: la tabla de valores (límites), las propiedades (RF66, también en el panel derecho) y
        // las asíntotas (RF26).
        contenido.tabla().ifPresent(t -> analisis.getChildren().add(PanelAnalisis.tabla(t)));
        contenido.propiedades().ifPresent(pr -> {
            propiedades.getChildren().setAll(new PropiedadesFuncion(pr));
            analisis.getChildren().add(new PropiedadesFuncion(pr));
        });
        contenido.secciones().forEach(sec -> analisis.getChildren().add(PanelAnalisis.seccion(sec)));
        contenido.asintotas().ifPresent(a -> analisis.getChildren().add(PanelAnalisis.asintotas(a)));
        mostrar(panelPropiedades, contenido.propiedades().isPresent());
        mostrar(sinAnalisis, analisis.getChildren().isEmpty());

        // HU10: sugerencias debajo de los pasos.
        sugerencias.getChildren().clear();
        for (Sugerencia s : resultado.sugerencias()) {
            sugerencias.getChildren().add(tarjetaSugerencia(s));
        }
        mostrar(zonaSugerencias, !resultado.sugerencias().isEmpty());
        if (abrirAnalisis) {
            abrirAnalisis = false;
            pestanaAnalisis.fire();
        }

        llenarExploracion(contenido.exploracion());
        boolean conComparacion = textoComparacion != null;
        mostrar(tituloComparacion, conComparacion);
        mostrar(comparacion, conComparacion);

        // HU35: la gráfica, o el aviso si no es de una variable; Sprint 8: las curvas de nivel de f(x, y)
        GraficaFuncion grafica = contenido.grafica().map(d -> new GraficaFuncion(d, latexLeyenda)).orElse(null);
        pe.mathnova.presentation.grafica.CurvasNivel nivel = contenido.curvasNivel()
                .map(pe.mathnova.presentation.grafica.CurvasNivel::new).orElse(null);
        if (nivel != null) {
            grafica = null;
            zonaGrafica.getChildren().setAll(nivel);
        } else if (grafica != null) {
            zonaGrafica.getChildren().setAll(grafica);
        }
        boolean hayGrafica = grafica != null || nivel != null;
        mostrar(zonaGrafica, hayGrafica);
        mostrar(ayudaGrafica, hayGrafica);
        mostrar(graficaNoDisponible, !hayGrafica);
        mostrar(tarjetaGrafica, true);
        tarjetaGrafica.setOpacity(animador.activas() ? 0 : 1);

        panelPropiedades.setAnimated(animador.activas());
        panelVerificacion.setAnimated(animador.activas());

        GraficaFuncion graficaFinal = grafica;
        List<Node> solucion = new ArrayList<>(List.of(iconoSolucion, subtituloSolucion, marcoFormula, irreducibles,
                alternativas, secciones));
        if (v.sinPasos() != null) {
            solucion.add(sinPasos);
        }
        if (!resultado.sugerencias().isEmpty()) {
            solucion.add(zonaSugerencias);
        }
        animador.presentar(filas.stream().map(FilaPaso::nodo).toList(), filas.stream().map(FilaPaso::numero).toList(),
                solucion, etiquetaVerificado, () -> animador.presentarGrafica(tarjetaGrafica,
                        graficaFinal == null ? null : graficaFinal.progresoProperty()));
    }

    /**
     * HU10: ícono, título, la justificación en una línea (completa en el tooltip) y "Resolver". Las que
     * todavía no se resuelven (RN20) quedan desactivadas con "Disponible en una próxima versión"; el
     * tooltip va en la tarjeta porque un botón desactivado no muestra el suyo.
     */
    private Node tarjetaSugerencia(Sugerencia s) {
        SVGPath icono = new SVGPath();
        icono.setContent(switch (s.operacion()) {
            case "LIMITE" -> "M1 12 C5 12 6 3 9 3 M9 3 L15 3 M12 0 L12 15";
            case "ASINTOTAS" -> "M1 8 H15 M8 1 V15 M2 14 C6 9 7 9 7 1";
            case "INTEGRAR" -> "M9 1 C6 1 7 6 7 8 C7 10 8 15 5 15";
            default -> "M1 14 L5 6 L9 10 L15 2";
        });
        icono.getStyleClass().add("sugerencia-icono");
        StackPane marcoIcono = new StackPane(icono);
        marcoIcono.setMinSize(20, 20);

        Label titulo = new Label(s.titulo());
        titulo.getStyleClass().add("sugerencia-titulo");
        Label justificacion = new Label(FormateadorTexto.pizarraConNegativos(s.justificacion()));
        justificacion.getStyleClass().add("sugerencia-justificacion");
        justificacion.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        justificacion.setMaxWidth(Double.MAX_VALUE);
        VBox textos = new VBox(2, titulo, justificacion);
        javafx.scene.layout.HBox.setHgrow(textos, javafx.scene.layout.Priority.ALWAYS);
        textos.setMinWidth(0);

        Button resolver = new Button("Resolver");
        resolver.getStyleClass().add("boton-alternativa");
        resolver.setDisable(!s.disponible());
        resolver.setOnAction(e -> resolverSugerencia(s));

        HBox tarjeta = new HBox(10, marcoIcono, textos, resolver);
        tarjeta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        tarjeta.getStyleClass().add("tarjeta-sugerencia");
        if (!s.disponible()) {
            tarjeta.setOpacity(0.65);
        }
        Tooltip.install(tarjeta, new Tooltip(s.disponible()
                ? FormateadorTexto.pizarraConNegativos(s.justificacion())
                : Sugerencia.PROXIMA_VERSION + ". " + FormateadorTexto.pizarraConNegativos(s.justificacion())));
        return tarjeta;
    }

    /** La plantilla va al editor y se resuelve con la cascada; las asíntotas abren Análisis. */
    private void resolverSugerencia(Sugerencia s) {
        if (!s.disponible()) {
            return;
        }
        if (s.operacion().equals("ASINTOTAS")) {
            if (s.plantilla().equals(textoResuelto)) {
                pestanaAnalisis.fire();
                return;
            }
            abrirAnalisis = true;
        }
        resolver(s.plantilla());
    }

    /** Exploración: una tarjeta por método, "Usado", "Probar" o "No aplica". */
    private void llenarExploracion(VistaExploracion x) {
        tarjetasMetodos.getChildren().clear();
        for (VistaExploracion.Tarjeta t : x.tarjetas()) {
            Label nombre = new Label(t.nombre());
            nombre.getStyleClass().add("metodo-nombre");
            nombre.setWrapText(true);
            Label estado = new Label(t.estado().texto());
            estado.getStyleClass().addAll("metodo-estado", "metodo-" + t.estado().name().toLowerCase().replace('_', '-'));
            VBox tarjeta = new VBox(6, estado, nombre);
            tarjeta.getStyleClass().add("tarjeta-metodo");
            tarjeta.setPrefWidth(170);
            Tooltip.install(tarjeta, new Tooltip(t.ayuda()));
            if (t.motivo() != null) {
                Label motivo = new Label(t.motivo());
                motivo.getStyleClass().add("metodo-motivo");
                motivo.setWrapText(true);
                tarjeta.getChildren().add(motivo);
            }
            if (t.vistaPrevia() != null) {
                Label vista = new Label(t.vistaPrevia());
                vista.getStyleClass().add("metodo-vista-previa");
                vista.setWrapText(true);
                tarjeta.getChildren().add(vista);
            }
            if (t.estado() == VistaExploracion.Estado.PROBAR && x.probables()) {
                Button probar = new Button("Probar");
                probar.getStyleClass().add("boton-alternativa");
                probar.setOnAction(e -> {
                    switch (t.accion()) {
                        case ABRIR_ANALISIS -> pestanaAnalisis.fire();
                        case RESOLVER_CON -> resolverCon(t.codigo());
                    }
                });
                tarjeta.getChildren().add(probar);
            }
            tarjetasMetodos.getChildren().add(tarjeta);
        }
        // El aviso "Resuelve un ejercicio…" es solo para cuando no hay nada en pantalla.
        sinMetodos.setText(SIN_METODOS_EN_PANTALLA);
        mostrar(sinMetodos, x.tarjetas().isEmpty());
    }

    /**
     * La tabla Q | R | C para el resultado en pantalla, en un Task y solo cuando se abre Exploración
     * (o ya está abierta al terminar de resolver). Se calcula una vez por resultado.
     */
    private void pedirComparacion() {
        if (textoResuelto == null || textoComparacion == null || comparacionPedida) {
            return;
        }
        comparacionPedida = true;
        int esta = ++comparacionActual;
        String texto = textoComparacion;
        mostrarComparacionCalculando();
        Task<FactorizacionService.Comparacion> tarea = new Task<>() {
            @Override
            protected FactorizacionService.Comparacion call() {
                return factorizacion.comparar(texto);
            }
        };
        tarea.setOnSucceeded(e -> {
            if (esta == comparacionActual) {
                llenarComparacion(tarea.getValue());
            }
        });
        tarea.setOnFailed(e -> {
            if (esta == comparacionActual) {
                llenarComparacion(null);
            }
        });
        Thread hilo = new Thread(tarea, "comparacion-campos");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarComparacionCalculando() {
        comparacion.getChildren().clear();
        Campo[] campos = Campo.values();
        for (int i = 0; i < campos.length; i++) {
            comparacion.add(rotuloCampo(campos[i]), 0, i);
            Label l = new Label("Calculando…");
            l.getStyleClass().add("comparacion-pendiente");
            comparacion.add(l, 1, i);
        }
    }

    /**
     * Q, R y C con su resultado en LaTeX. Un campo sin resultado no tiene fila; si los tres coinciden se
     * muestra una sola vez con la nota. null: todavía no hay un resultado.
     */
    private void llenarComparacion(FactorizacionService.Comparacion c) {
        comparacion.getChildren().clear();
        if (c == null) {
            Label l = new Label("Todavía no hay un resultado");
            l.getStyleClass().add("comparacion-pendiente");
            comparacion.add(l, 0, 0, 2, 1);
            return;
        }
        if (c.iguales()) {
            Label rotulo = new Label("Q = R = C");
            rotulo.getStyleClass().addAll("comparacion-campo", "comparacion-campo-elegido");
            comparacion.add(rotulo, 0, 0);
            comparacion.add(imagenComparacion(c.latex().get(Campo.Q)), 1, 0);
            Label nota = new Label(c.nota());
            nota.getStyleClass().add("comparacion-nota");
            nota.setWrapText(true);
            comparacion.add(nota, 0, 1, 2, 1);
            return;
        }
        int fila = 0;
        for (Campo cada : Campo.values()) {
            String latex = c.latex().get(cada);
            if (latex == null) {
                continue; // la operación no tiene sentido o no hay resultado en ese campo
            }
            Label rotulo = rotuloCampo(cada);
            if (cada == campo) {
                rotulo.getStyleClass().add("comparacion-campo-elegido");
            }
            comparacion.add(rotulo, 0, fila);
            comparacion.add(imagenComparacion(latex), 1, fila);
            fila++;
        }
    }

    private ImageView imagenComparacion(String latex) {
        ImageView img = new ImageView(ImagenLatex.de(latexComparacion, latex));
        img.setPreserveRatio(true);
        return img;
    }

    private static Label rotuloCampo(Campo c) {
        Label l = new Label(c.name());
        l.getStyleClass().add("comparacion-campo");
        return l;
    }

    /** HU08: el método elegido no aplica. No es un error: se muestra el motivo. */
    private void mostrarNoAplica(MetodoNoAplicable e) {
        animador.detenerFases();
        mostrarCarga(false);
        terminarCalculo();
        mostrar(cuerpoSolucion, false);
        subtituloSolucion.setText("El método elegido no aplica a esta expresión");
        tituloNoAplica.setText(VistaResolucion.nombre(e.codigo(), codigos) + " no aplica");
        motivoNoAplica.setText(FormateadorTexto.pizarra(codigos ? e.motivo() : FormateadorTexto.sinCodigos(e.motivo())));
        mostrar(avisoNoAplica, true);
        sinPasos.setText("Prueba otro método en Exploración o vuelve a resolver en automático.");
        mostrar(sinPasos, true);
        metodoForzado = null;
    }

    private void mostrarError(String mensaje) {
        animador.detenerFases();
        mostrarCarga(false);
        terminarCalculo();
        mostrar(cuerpoSolucion, false);
        subtituloSolucion.setText("No se pudo resolver");
        errorSolucion.setText(mensaje);
        mostrar(errorSolucion, true);
    }

    private void terminarCalculo() {
        calculando = false;
        actualizarBoton();
    }

    private void limpiarResultado() {
        pasos.getChildren().clear();
        lineasVerificacion.getChildren().clear();
        irreducibles.getChildren().clear();
        alternativas.getChildren().clear();
        formula.setImage(null);
        subtituloSolucion.setText("Pulsa Resolver para ver la solución.");
        propiedades.getChildren().clear();
        analisis.getChildren().clear();
        mostrar(sinAnalisis, true);
        tarjetasMetodos.getChildren().clear();
        sinMetodos.setText(sinMetodosInicial);
        mostrar(sinMetodos, true);
        textoComparacion = null;
        mostrar(tituloComparacion, true);
        mostrar(comparacion, true);
        sugerencias.getChildren().clear();
        mostrar(zonaSugerencias, false);
        zonaGrafica.getChildren().clear();
        mostrar(tarjetaGrafica, false);
        panelPropiedades.setExpanded(false);
        panelVerificacion.setExpanded(false);
        mostrar(iconoSolucion, false);
        mostrar(etiquetaVerificado, false);
        mostrar(alternativas, false);
        mostrar(sinPasos, false);
        mostrar(secciones, false);
        mostrar(errorSolucion, false);
        mostrar(avisoNoAplica, false);
        mostrar(cuerpoSolucion, true);
    }

    private void mostrarCarga(boolean si) {
        mostrar(carga, si);
        mostrar(cuerpoSolucion, !si);
        if (!si) {
            mensajeCarga.setText("");
        }
    }

    private static Label nota(String texto, String clase) {
        Label l = new Label(texto);
        l.setWrapText(true);
        l.getStyleClass().add(clase);
        return l;
    }

    private static void mostrar(Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }

    // ---------------------------------------------------------------- ancho de la ventana

    private void seguirAncho(Scene escena) {
        if (escena != null) {
            escena.widthProperty().addListener((obs, antes, ancho) -> acomodar(ancho.doubleValue()));
            acomodar(escena.getWidth());
        }
    }

    /** Dos columnas desde 1100 px de ancho; por debajo, el procedimiento pasa debajo del panel izquierdo. */
    private void acomodar(double anchoVentana) {
        boolean dos = anchoVentana >= ANCHO_DOS_COLUMNAS;
        if (dosColumnas != null && dosColumnas == dos) {
            return;
        }
        dosColumnas = dos;
        if (dos) {
            columnas.getColumnConstraints().setAll(columna(55), columna(45));
            GridPane.setConstraints(columnaIzquierda, 0, 0);
            GridPane.setConstraints(tarjetaProcedimiento, 1, 0);
        } else {
            columnas.getColumnConstraints().setAll(columna(100));
            GridPane.setConstraints(columnaIzquierda, 0, 0);
            GridPane.setConstraints(tarjetaProcedimiento, 0, 1);
        }
    }

    private static ColumnConstraints columna(double porcentaje) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(porcentaje);
        c.setFillWidth(true);
        return c;
    }
}
