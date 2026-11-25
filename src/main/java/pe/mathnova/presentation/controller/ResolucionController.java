package pe.mathnova.presentation.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.application.editor.LatexReconocido;
import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.FaseFactorizacion;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.sugerencias.Sugerencia;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.escritura.ReconocedorEscritura;
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
import pe.mathnova.presentation.escritura.PanelEscritura;
import pe.mathnova.presentation.grafica.GraficaFuncion;
import pe.mathnova.application.service.Verificacion;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.resolucion.ContenidoEcuacion;
import pe.mathnova.presentation.resolucion.ContenidoFactorizacion;
import pe.mathnova.presentation.resolucion.ContenidoFraccion;
import pe.mathnova.presentation.resolucion.ContenidoDerivada;
import pe.mathnova.presentation.resolucion.ContenidoIntegral;
import pe.mathnova.presentation.resolucion.ContenidoLimite;
import pe.mathnova.presentation.resolucion.ContenidoPestanas;
import pe.mathnova.presentation.resolucion.FormulaAjustable;
import pe.mathnova.presentation.resolucion.PanelAnalisis;
import pe.mathnova.presentation.resolucion.PizarraResolucion;
import pe.mathnova.presentation.resolucion.PropiedadesFuncion;
import pe.mathnova.presentation.resolucion.TablaAnalisis;
import pe.mathnova.presentation.resolucion.VistaExploracion;
import pe.mathnova.presentation.resolucion.VistaResolucion;

@Component
@Scope("prototype")
public class ResolucionController {

    static final double ANCHO_BARRA_UNA_FILA = 760;
    static final double ANCHO_HERRAMIENTAS_MIN = 480;
    static final double ANCHO_PIZARRA_MIN = 480;
    static final double PROPORCION_HERRAMIENTAS = 0.4;
    static final double OPACIDAD_ATENUADA = 0.4;
    static final String CAMPO_NO_IMPORTA = "El campo no cambia este cálculo";
    static final Duration ESPERA_ERROR = Duration.millis(400);

    private static final PseudoClass MAL = PseudoClass.getPseudoClass("mal");
    private static final PseudoClass ARRASTRANDO = PseudoClass.getPseudoClass("arrastrando");
    private static final PseudoClass NEUTRA = PseudoClass.getPseudoClass("neutra");
    private static final String PROXIMAMENTE = "Disponible próximamente";

    private static final String LEYENDO = "Leyendo la expresión…";
    private static final String PROBANDO = "Probando métodos…";
    private static final String VERIFICANDO = "Verificando el resultado…";
    private static final List<String> FASES = List.of(LEYENDO, PROBANDO, VERIFICANDO);

    private record Resultado(VistaResolucion vista, ContenidoPestanas contenido, String ans,
                             List<Sugerencia> sugerencias, Object resolucion, String latexEntrada,
                             String rotulo) {

        Resultado(VistaResolucion vista, ContenidoPestanas contenido, String ans, List<Sugerencia> sugerencias,
                  Object resolucion) {
            this(vista, contenido, ans, sugerencias, resolucion, null, null);
        }

        Resultado conEntrada(String latex, String rotuloPizarra) {
            return new Resultado(vista, contenido, ans, sugerencias, resolucion, latex, rotuloPizarra);
        }
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ResolucionController.class);

    private static final ReconocedorEscritura SIN_RECONOCEDOR = trazos -> {
        throw new UnsupportedOperationException("No hay un reconocedor de escritura configurado");
    };

    static final String SIN_METODOS_EN_PANTALLA = "Este ejercicio se resolvió sin probar métodos.";

    private final FactorizacionService factorizacion;
    private final ResolverService resolverService;
    private final ConfiguracionService configuracion;
    private final ReconocedorEscritura reconocedor;
    private final AnimadorResolucion animador = new AnimadorResolucion();
    private final FormulaAjustable formulaSolucion = new FormulaAjustable(28, 18);
    private final PantallaResolucion pantalla = new PantallaResolucion();
    private final HBox filaAcciones = new HBox(8);
    private Tooltip ayudaCampo;
    private final RenderizadorLatex latexPasos = new RenderizadorLatex(18f);
    private final RenderizadorLatex latexLeyenda = new RenderizadorLatex(14f).siempreNegra();
    private final PauseTransition espera = new PauseTransition(ESPERA_ERROR);
    private final ToggleGroup grupoPestanas = new ToggleGroup();
    private boolean codigos;

    private EditorMatematico editor;
    private TecladoMatematico teclado;
    private PizarraResolucion pizarra;
    private Runnable onConfiguracion = this::configuracionNoDisponible;
    private String latexEscrito;
    private boolean ecoEscritura;
    private long generacionEditor;
    private String ultimoLatexEditor = "";
    private PanelEscritura panelEscritura;
    private javafx.util.Duration pausaEscritura;

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
    private Label etiquetaEscritoAMano;
    @FXML
    private Label avisoEntrada;
    @FXML
    private Label notaActualizar;
    @FXML
    private VBox zonaEntrada;
    @FXML
    private HBox barraEntrada;
    @FXML
    private HBox selectorCampo;
    @FXML
    private SplitPane division;
    @FXML
    private VBox panelHerramientas;
    @FXML
    private VBox panelPizarra;
    @FXML
    private VBox columnaIzquierda;
    @FXML
    private StackPane contenidoIzquierda;
    @FXML
    private ToggleButton pestanaTeclado;
    @FXML
    private ToggleButton pestanaEscribir;
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
    private VBox vistaEscritura;
    @FXML
    private StackPane zonaEscritura;
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
    private VBox comparacion;
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
    private StackPane recuadroFormula;
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
    private StackPane zonaPizarra;
    @FXML
    private Label sinPasos;
    @FXML
    private VBox zonaSugerencias;
    @FXML
    private VBox sugerencias;
    @FXML
    private VBox secciones;

    private int ejecucion;
    private boolean pendiente;
    private String metodoForzado;
    private String ultimaRespuesta;
    private boolean divisionInicial;
    private Campo campo = Campo.Q;
    private final ToggleGroup grupoCampos = new ToggleGroup();
    private String textoResuelto;
    private boolean comparacionPedida;
    private int comparacionActual;
    private boolean abrirAnalisis;
    private String textoComparacion;
    private String sinMetodosInicial;
    private Object ultimaResolucion;
    private ExportarPdf exportarPdf;
    private EnPizarra enPizarra;
    private java.util.function.Supplier<javafx.scene.Parent> fabricaGrafica;

    record EnPizarra(String texto, Campo campo, String forzado) {
    }

    public ResolucionController(FactorizacionService factorizacion, ResolverService resolverService,
                                ConfiguracionService configuracion) {
        this(factorizacion, resolverService, configuracion, SIN_RECONOCEDOR);
    }

    @Autowired
    public ResolucionController(FactorizacionService factorizacion, ResolverService resolverService,
                                ConfiguracionService configuracion, ReconocedorEscritura reconocedor) {
        this.factorizacion = factorizacion;
        this.resolverService = resolverService;
        this.configuracion = configuracion;
        this.reconocedor = reconocedor;
    }

    @FXML
    void initialize() {
        editor = new EditorMatematico();
        zonaEditor.getChildren().setAll(editor);
        editor.setOnCambio(e -> alCambiar(e.getLatex()));
        editor.setOnEnter(e -> resolver());

        teclado = new TecladoMatematico(editor);
        teclado.setRespuestaAnterior(() -> ultimaRespuesta);
        teclado.setOnConfiguracion(() -> onConfiguracion.run());
        zonaTeclado.getChildren().setAll(teclado);

        PanelEscritura.Destino destino = new PanelEscritura.Destino() {
            @Override
            public long generacion() {
                return generacionEditor;
            }

            @Override
            public String poner(String latex) {
                return ponerEscritura(latex);
            }
        };
        panelEscritura = pausaEscritura == null ? new PanelEscritura(reconocedor, destino)
                : new PanelEscritura(reconocedor, destino, pausaEscritura,
                        Boolean.getBoolean("mathnova.muestras")
                                ? pe.mathnova.infrastructure.escritura.ArchivoMuestras.carpetaPorDefecto() : null);
        zonaEscritura.getChildren().setAll(panelEscritura);
        panelEscritura.pendienteProperty().addListener((obs, antes, ahora) -> pintar());

        pizarra = new PizarraResolucion(latexPasos, animador, configuracion.mostrarExplicaciones());
        if (exportarPdf != null) {
            pizarra.setOnExportar(this::exportarPdf);
        }
        pizarra.setOnExplicaciones(configuracion::guardarMostrarExplicaciones);
        escucharConfiguracion();
        zonaPizarra.getChildren().setAll(pizarra);

        espera.setOnFinished(e -> actualizarEntrada());

        prepararCampos();

        prepararPestanas();
        codigos = configuracion.mostrarCodigos();

        formulaSolucion.setLateral(etiquetaVerificado);
        recuadroFormula.getChildren().setAll(formulaSolucion);
        vistaGrafica.getChildren().remove(tarjetaSolucion);
        filaAcciones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        filaAcciones.getStyleClass().add("fila-acciones");
        botonResolver.setMinWidth(120);
        notaActualizar.setText(PantallaResolucion.PULSA_RESOLVER);

        prepararDivision();
        raiz.sceneProperty().addListener((obs, antes, escena) -> seguirAncho(escena));
        acomodarBarra(ANCHO_BARRA_UNA_FILA);

        sinMetodosInicial = sinMetodos.getText();
        limpiarResultado();
        llenarComparacion(null);
        sinPasos.setText("Escribe un ejercicio y pulsa Resolver para verlo resuelto en la pizarra.");
        mostrar(sinPasos, true);
        pintar();
    }

    public void resolver(String texto) {
        resolver(texto, null, null);
    }

    public void resolver(String texto, Campo enCampo, String forzado) {
        if (enCampo != null && enCampo != campo) {
            campo = enCampo;
            (enCampo == Campo.R ? campoR : enCampo == Campo.C ? campoC : campoQ).setSelected(true);
        }
        metodoForzado = forzado;
        try {
            editor.setLatex(ImpresoraLatex.imprimir(Parser.parse(texto)));
        } catch (RuntimeException e) {
        }
        quitarEscritoAMano();
        pantalla.cambiar(PantallaResolucion.analizarTexto(texto, resolverService));
        resolverActual();
    }

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

    public EstadoResolucion estado() {
        return pantalla.estado();
    }

    public void setOnConfiguracion(Runnable accion) {
        onConfiguracion = accion == null ? this::configuracionNoDisponible : accion;
    }

    public void actualizarPreferencias() {
        pizarra.setExplicaciones(configuracion.mostrarExplicaciones());
    }

    public Object ultimaResolucion() {
        return pantalla.hayResultado() ? ultimaResolucion : null;
    }

    private void prepararCampos() {
        Map<ToggleButton, Campo> botones = Map.of(campoQ, Campo.Q, campoR, Campo.R, campoC, Campo.C);
        botones.keySet().forEach(b -> b.setToggleGroup(grupoCampos));
        Campo inicial = configuracion.campoProperty().get();
        campo = inicial;
        (inicial == Campo.R ? campoR : inicial == Campo.C ? campoC : campoQ).setSelected(true);
        ayudaCampo = new Tooltip(CAMPO_NO_IMPORTA);
        grupoCampos.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                antes.setSelected(true);
                return;
            }
            Campo nuevo = botones.get((ToggleButton) ahora);
            if (nuevo == campo) {
                return;
            }
            campo = nuevo;
            boolean estabaResuelto = pantalla.estado() == EstadoResolucion.RESUELTO;
            pantalla.invalidar();
            if (estabaResuelto) {
                resolverActual();
            } else {
                espera.playFromStart();
                pintar();
            }
            editor.enfocar();
        });
    }

    static boolean campoImporta(ResolverService.Plan p) {
        return p == null || p instanceof ResolverService.Plan.Vacio || p instanceof ResolverService.Plan.Factorizar
                || p instanceof ResolverService.Plan.FactorizarFunciones || p instanceof ResolverService.Plan.Simplificar
                || p instanceof ResolverService.Plan.ResolverEcuacion || p instanceof ResolverService.Plan.Invalido
                || p instanceof ResolverService.Plan.NoSoportado;
    }

    public Campo campo() {
        return campo;
    }

    private void prepararPestanas() {
        List<ToggleButton> botones = List.of(pestanaTeclado, pestanaEscribir, pestanaGrafica, pestanaAnalisis,
                pestanaExploracion);
        List<Node> vistas = List.of(vistaTeclado, vistaEscritura, vistaGrafica, vistaAnalisis, vistaExploracion);
        for (int i = 0; i < botones.size(); i++) {
            ToggleButton b = botones.get(i);
            Node vista = vistas.get(i);
            b.setToggleGroup(grupoPestanas);
            b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            b.setOnAction(e -> {
                if (!b.isSelected()) {
                    b.setSelected(true);
                }
                for (Node v : vistas) {
                    mostrar(v, v == vista);
                }
                if (b == pestanaExploracion) {
                    pedirComparacion();
                }
                if (b != pestanaEscribir) {
                    editor.enfocar();
                } else {
                    panelEscritura.alMostrar();
                }
            });
        }
        pestanaTeclado.fire();
    }

    private void configuracionNoDisponible() {
        MenuItem item = new MenuItem("Configuración: " + PROXIMAMENTE.toLowerCase());
        item.setDisable(true);
        ContextMenu menu = new ContextMenu(item);
        menu.show(teclado, Side.TOP, teclado.getWidth() - 220, 30);
    }

    String ponerEscritura(String reconocido) {
        String latex = LatexReconocido.adaptar(reconocido);
        LOG.info("Escritura a mano: modelo «{}» → editor «{}»", reconocido, latex);
        latexEscrito = null;
        ecoEscritura = true;
        mostrar(etiquetaEscritoAMano, true);
        editor.setLatex(latex);
        PantallaResolucion.Analisis a = PantallaResolucion.analizar(latex, resolverService);
        if (a.tipo() == PantallaResolucion.Tipo.ERROR || a.tipo() == PantallaResolucion.Tipo.INCOMPLETO) {
            return PanelEscritura.revisa(a.mensaje());
        }
        return null;
    }

    private void escucharConfiguracion() {
        pizarra.setModoInicial(modo(configuracion.modoProperty().get()));
        configuracion.modoProperty().addListener((obs, antes, ahora) -> pizarra.setModoInicial(modo(ahora)));
        configuracion.explicacionesProperty().addListener((obs, antes, si) -> pizarra.setExplicaciones(si));
        configuracion.campoProperty().addListener((obs, antes, c) ->
                (c == Campo.R ? campoR : c == Campo.C ? campoC : campoQ).setSelected(true));
        configuracion.codigosProperty().addListener((obs, antes, si) -> {
            if (pantalla.estado() == EstadoResolucion.RESUELTO) {
                resolverActual();
            }
        });
    }

    private static pe.mathnova.presentation.resolucion.EstadoPizarra.Modo modo(ConfiguracionService.ModoPizarra m) {
        return m == ConfiguracionService.ModoPizarra.PASO_A_PASO
                ? pe.mathnova.presentation.resolucion.EstadoPizarra.Modo.PASO_A_PASO
                : pe.mathnova.presentation.resolucion.EstadoPizarra.Modo.TODOS;
    }

    @Autowired(required = false)
    public void setExportacion(pe.mathnova.application.service.ExportacionPdfService servicio) {
        exportarPdf = servicio == null ? null : new ExportarPdf(servicio);
        if (pizarra != null) {
            pizarra.setOnExportar(exportarPdf == null ? null : this::exportarPdf);
        }
    }

    void exportarPdf() {
        EnPizarra e = enPizarra;
        if (exportarPdf == null || e == null) {
            return;
        }
        java.util.function.Supplier<javafx.scene.Parent> fabrica = fabricaGrafica;
        java.util.List<String> estilos = raiz.getScene() == null ? java.util.List.of()
                : java.util.List.copyOf(raiz.getScene().getStylesheets());
        exportarPdf.exportar(pizarra.botonExportar(),
                hoy -> exportarPdf.servicio().desdeResolver(e.texto(), e.campo(), e.forzado(), hoy),
                fabrica == null ? null : () -> ExportarPdf.png(fabrica.get(), 900, 540, estilos));
    }

    ExportarPdf exportador() {
        return exportarPdf;
    }

    @Autowired(required = false)
    void setPausaEscritura(@org.springframework.beans.factory.annotation.Value("${mathnova.escritura.pausa-ms:700}")
                           long ms) {
        pausaEscritura = javafx.util.Duration.millis(Math.max(150, ms));
    }

    public PanelEscritura panelEscritura() {
        return panelEscritura;
    }

    public void abrirEscribir() {
        pestanaEscribir.fire();
    }

    public void pulsarResolver() {
        resolver();
    }

    public String latexEditor() {
        return editor.getLatex();
    }

    public String aviso() {
        return avisoEntrada.isVisible() ? avisoEntrada.getText() : "";
    }

    public String latexSolucion() {
        return formulaSolucion.getLatex();
    }

    private void quitarEscritoAMano() {
        latexEscrito = null;
        ecoEscritura = false;
        mostrar(etiquetaEscritoAMano, false);
    }

    boolean escritoAMano() {
        return etiquetaEscritoAMano.isVisible();
    }

    private void pintar() {
        EstadoResolucion e = pantalla.estado();

        String mensaje = pantalla.mensaje();
        avisoEntrada.setText(mensaje == null ? "" : mensaje);
        mostrar(avisoEntrada, mensaje != null);
        editor.marcarFalta(e == EstadoResolucion.INCOMPLETO);

        botonResolver.setDisable(!pantalla.botonActivo()
                && !(panelEscritura != null && panelEscritura.pendienteProperty().get()));

        boolean importa = campoImporta(pantalla.analisis().plan());
        for (ToggleButton b : List.of(campoQ, campoR, campoC)) {
            b.setDisable(!importa);
        }
        if (importa) {
            Tooltip.uninstall(selectorCampo, ayudaCampo);
        } else {
            Tooltip.install(selectorCampo, ayudaCampo);
        }

        boolean atenuado = pantalla.atenuado();
        double opacidad = atenuado ? OPACIDAD_ATENUADA : 1;
        for (Node n : List.of(tarjetaSolucion, zonaGrafica, graficaNoDisponible, ayudaGrafica, analisis, sinAnalisis,
                tarjetasMetodos, comparacion, tituloComparacion, tarjetaProcedimiento)) {
            n.setOpacity(opacidad);
        }
        mostrar(notaActualizar, atenuado);

        boolean etiqueta = pantalla.etiquetaVisible() && etiquetaVerificado.getText() != null
                && !etiquetaVerificado.getText().isEmpty();
        mostrar(etiquetaVerificado, etiqueta);
        mostrar(iconoSolucion, pantalla.etiquetaVisible());
    }

    private void alCambiar(String latex) {
        if (ecoEscritura) {
            ecoEscritura = false;
            latexEscrito = latex;
        } else {
            if (latexEscrito != null && !latexEscrito.equals(latex)) {
                quitarEscritoAMano();
            }
            if (!latex.equals(ultimoLatexEditor)) {
                generacionEditor++;
            }
        }
        ultimoLatexEditor = latex;
        boolean cambio = pantalla.cambiar(PantallaResolucion.analizar(latex, resolverService));
        if (cambio) {
            metodoForzado = null;
            espera.playFromStart();
        }
        pintar();
    }

    private void actualizarEntrada() {
        pantalla.confirmar(pantalla.version());
        pintar();
    }

    @FXML
    void resolver() {
        if (panelEscritura != null && panelEscritura.leyendo()) {
            panelEscritura.alTerminar(this::resolverEditor, this::pintar);
            return;
        }
        resolverEditor();
    }

    private void resolverEditor() {
        espera.stop();
        alCambiar(editor.getLatex());
        PantallaResolucion.Analisis a = pantalla.analisis();
        pantalla.confirmar(pantalla.version());
        if (a.tipo() == PantallaResolucion.Tipo.INCOMPLETO) {
            if (a.casilla() >= 0) {
                editor.resaltarCasilla(a.casilla());
            }
            pintar();
            return;
        }
        resolverActual();
    }

    private void resolverActual() {
        espera.stop();
        int version = pantalla.version();
        pantalla.confirmar(version);
        PantallaResolucion.Analisis a = pantalla.analisis();
        if (!a.resoluble()) {
            pintar();
            return;
        }
        String texto = a.texto();
        if (pantalla.calculando()) {
            pendiente = true;
            pintar();
            return;
        }
        ResolverService.Plan esteplan = a.plan();
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
        enPizarra = null;
        fabricaGrafica = null;
        comparacionPedida = false;
        comparacionActual++;
        llenarComparacion(null);

        limpiarResultado();
        pestanaGrafica.fire();
        mostrarCarga(true);
        pantalla.empezarCalculo();
        pintar();

        long inicio = System.nanoTime();
        Task<Resultado> tarea = new Task<>() {
            @Override
            protected Resultado call() {
                Resultado r = calcular(texto, esteplan, forzado, enCampo, conCodigos, fase -> updateMessage(mensaje(fase)));
                guardarEnHistorial(texto, r, forzado);
                return r.conEntrada(latexEntrada(texto), rotulo(esteplan));
            }

        };
        animador.seguirFases(mensajeCarga, tarea.messageProperty(), FASES);
        tarea.setOnSucceeded(e -> {
            Resultado resultado = tarea.getValue();
            animador.trasCargaMinima(inicio, () -> {
                if (esta == ejecucion && !reintentarPendiente()) {
                    pantalla.resultado(version, resultado.vista().etiqueta() == VistaResolucion.Etiqueta.VERIFICADO);
                    mostrarResultado(resultado);
                    textoResuelto = texto;
                    enPizarra = new EnPizarra(texto, enCampo, forzado);
                    if (vistaExploracion.isVisible()) {
                        pedirComparacion();
                    }
                    pintar();
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
                    pantalla.resultado(version, false);
                    mostrarNoAplica(noAplica);
                } else {
                    String motivo = causa == null || causa.getMessage() == null ? "error desconocido" : causa.getMessage();
                    pantalla.fallo(version, "No se pudo resolver: " + motivo);
                    mostrarError("No se pudo resolver: " + motivo);
                }
                pintar();
            });
        });
        Thread hilo = new Thread(tarea, "resolucion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private Resultado calcular(String texto, ResolverService.Plan p, String forzado, Campo enCampo, boolean conCodigos,
                               java.util.function.Consumer<FaseFactorizacion> avance) {
        return switch (p) {
            case ResolverService.Plan.FactorizarFunciones f -> {
                ResolucionFunciones r = factorizacion.factorizarFunciones(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, factorizacion.verificar(r), conCodigos);
                yield new Resultado(v, ContenidoFactorizacion.de(r, v, texto, conCodigos), v.latexFormula(),
                        factorizacion.sugerencias(r), r);
            }
            case ResolverService.Plan.Simplificar s -> {
                Simplificacion simp = factorizacion.simplificar(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(simp, factorizacion.verificar(simp), conCodigos);
                yield new Resultado(v, ContenidoFraccion.de(simp, v, factorizacion.asintotas(simp), texto, conCodigos),
                        v.latexFormula(), factorizacion.sugerencias(simp), simp);
            }
            case ResolverService.Plan.CalcularLimite l -> {
                ResolucionLimite r = resolverService.resolverLimite(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.resultado() instanceof ResultadoLimite.Finito f ? f.latex() : null;
                yield new Resultado(v, ContenidoLimite.de(r, resolverService.limites(), conCodigos), ans, List.of(), r);
            }
            case ResolverService.Plan.Derivar d -> {
                pe.mathnova.domain.calculo.derivadas.ResolucionDerivada r =
                        resolverService.resolverDerivada(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.esImplicita() ? null : ImpresoraLatex.imprimir(r.resultado());
                yield new Resultado(v, ContenidoDerivada.de(r, resolverService.caminos(r), conCodigos), ans, List.of(), r);
            }
            case ResolverService.Plan.Integrar i -> {
                pe.mathnova.domain.calculo.integrales.ResolucionIntegral r =
                        resolverService.resolverIntegral(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.definida() && r.resuelta() ? ImpresoraLatex.imprimir(r.valor()) : null;
                yield new Resultado(v, ContenidoIntegral.de(r, resolverService.caminos(r), conCodigos), ans, List.of(), r);
            }
            case ResolverService.Plan.Parciales pp -> {
                pe.mathnova.domain.calculo.parciales.ResolucionParcial r =
                        resolverService.resolverParciales(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.resultado() == null ? null : ImpresoraLatex.imprimir(r.resultado());
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoParciales.de(r), ans,
                        pe.mathnova.domain.sugerencias.MotorSugerencias.paraVariasVariables(r.funcion()), r);
            }
            case ResolverService.Plan.LimiteDoble ld -> {
                pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble r =
                        resolverService.resolverLimiteDoble(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.existe() ? ImpresoraLatex.imprimir(r.valor()) : null;
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoLimiteDoble.de(r), ans, List.of(), r);
            }
            case ResolverService.Plan.Extremos ex -> {
                pe.mathnova.domain.calculo.extremos.ResolucionExtremos r =
                        resolverService.resolverExtremos(texto, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoExtremos.de(r), null, List.of(), r);
            }
            case ResolverService.Plan.IntegralMultiple im -> {
                pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple r =
                        resolverService.resolverIntegralMultiple(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                String ans = r.resuelta() ? ImpresoraLatex.imprimir(r.valor()) : null;
                yield new Resultado(v, pe.mathnova.presentation.resolucion.ContenidoIntegralMultiple.de(r,
                        resolverService.caminos(r), conCodigos), ans, List.of(), r);
            }
            case ResolverService.Plan.ResolverEcuacion ec -> {
                ResolucionEcuacion r = resolverService.resolverEcuacion(texto, forzado, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                String ans = r.soluciones().size() == 1
                        ? ImpresoraLatex.imprimir(new pe.mathnova.domain.expresion.Constante(r.soluciones().getFirst().valor()))
                        : null;
                VistaResolucion v = VistaResolucion.de(r, conCodigos);
                yield new Resultado(v, ContenidoEcuacion.de(r, v, conCodigos), ans, List.of(), r);
            }
            default -> {
                Resolucion r = factorizacion.factorizar(texto, forzado, enCampo, avance);
                avance.accept(FaseFactorizacion.VERIFICACION);
                VistaResolucion v = VistaResolucion.de(r, factorizacion.verificar(r), conCodigos);
                yield new Resultado(v, ContenidoFactorizacion.de(r, v, texto, conCodigos), v.latexFormula(),
                        factorizacion.sugerencias(r), r);
            }
        };
    }

    private void guardarEnHistorial(String texto, Resultado r, String forzado) {
        try {
            resolverService.guardar(texto, r.resolucion(), forzado, r.sugerencias());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo guardar {} en el historial", texto, e);
        }
    }

    private boolean reintentarPendiente() {
        boolean hay = pendiente;
        pendiente = false;
        if (!hay) {
            return false;
        }
        animador.detenerFases();
        pantalla.cancelarCalculo();
        resolverActual();
        return true;
    }

    private static String mensaje(FaseFactorizacion fase) {
        return switch (fase) {
            case LECTURA -> LEYENDO;
            case METODOS -> PROBANDO;
            case VERIFICACION -> VERIFICANDO;
        };
    }

    private void resolverCon(String codigo) {
        metodoForzado = codigo;
        resolverActual();
    }

    private void mostrarResultado(Resultado resultado) {
        VistaResolucion v = resultado.vista();
        ContenidoPestanas contenido = resultado.contenido();
        animador.detenerFases();
        mostrarCarga(false);
        ultimaRespuesta = resultado.ans();
        ultimaResolucion = resultado.resolucion();
        textoComparacion = contenido.comparacion().orElse(null);

        subtituloSolucion.setText(v.subtitulo());
        formulaSolucion.setLatex(contenido.latexSolucion());

        VistaResolucion.Etiqueta e = v.etiqueta();
        etiquetaVerificado.setText(e.texto());
        etiquetaVerificado.pseudoClassStateChanged(MAL, e == VistaResolucion.Etiqueta.NO_COINCIDE);
        etiquetaVerificado.pseudoClassStateChanged(NEUTRA, e == VistaResolucion.Etiqueta.IRREDUCIBLE
                || e == VistaResolucion.Etiqueta.SIN_FACTORIZACION || e == VistaResolucion.Etiqueta.SIN_SOLUCION
                || e == VistaResolucion.Etiqueta.IRREDUCIBLE_R || e == VistaResolucion.Etiqueta.APROXIMADO
                || e == VistaResolucion.Etiqueta.NO_FACTORIZABLE || e == VistaResolucion.Etiqueta.NO_EXISTE
                || e == VistaResolucion.Etiqueta.SIN_CALCULAR);
        etiquetaVerificado.setGraphic(e == VistaResolucion.Etiqueta.VERIFICADO ? marcaVerificado : null);
        etiquetaVerificado.setTooltip(v.ayudaEtiqueta() == null ? null : new Tooltip(v.ayudaEtiqueta()));
        mostrar(etiquetaVerificado, true);
        mostrar(iconoSolucion, true);

        for (String nota : v.notas()) {
            irreducibles.getChildren().add(nota(nota, "nota-irreducible"));
        }

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

        java.util.Optional<pe.mathnova.presentation.resolucion.FormaLegible> legible =
                resultado.resolucion() instanceof pe.mathnova.domain.factorizacion.Resolucion res
                        ? pe.mathnova.presentation.resolucion.FormaLegible.de(res) : java.util.Optional.empty();
        pizarra.mostrar(new PizarraResolucion.Contenido(resultado.rotulo(), resultado.latexEntrada(),
                pasosPizarra(v, resultado.resolucion()), resultadoPizarra(v, contenido, resultado.resolucion()),
                esFactorizacion(resultado.rotulo()) ? comprobacion(v, resultado.rotulo(), resultado.resolucion()) : null,
                legible.map(pe.mathnova.presentation.resolucion.FormaLegible::pie).orElse(List.of())));
        if (v.sinPasos() != null) {
            sinPasos.setText(v.sinPasos());
            mostrar(sinPasos, true);
        }

        mostrar(secciones, true);

        analisis.getChildren().clear();
        propiedades.getChildren().clear();
        TablaAnalisis tablaAnalisis = new TablaAnalisis();
        contenido.tabla().ifPresent(t -> PanelAnalisis.tabla(tablaAnalisis, t));
        java.util.Optional<pe.mathnova.presentation.resolucion.AnalisisFactorizacion> deFactorizacion =
                contenido.analisisFactorizacion();
        if (deFactorizacion.isPresent()) {
            PanelAnalisis.factorizacion(tablaAnalisis, deFactorizacion.get());
        } else {
            contenido.propiedades().ifPresent(pr -> {
                propiedades.getChildren().setAll(new PropiedadesFuncion(pr));
                PanelAnalisis.propiedades(tablaAnalisis, pr);
            });
        }
        contenido.secciones().forEach(sec -> PanelAnalisis.seccion(tablaAnalisis, sec));
        contenido.asintotas().ifPresent(a -> PanelAnalisis.asintotas(tablaAnalisis, a));
        if (!tablaAnalisis.vacia()) {
            analisis.getChildren().add(tablaAnalisis);
        }
        boolean desplegable = deFactorizacion.isEmpty() && contenido.propiedades().isPresent();
        mostrar(panelPropiedades, desplegable);
        mostrar(secciones, desplegable);
        mostrar(sinAnalisis, analisis.getChildren().isEmpty());

        sugerencias.getChildren().clear();
        for (Sugerencia s : sugerenciasFinales(resultado)) {
            sugerencias.getChildren().add(tarjetaSugerencia(s));
        }
        mostrar(zonaSugerencias, !sugerenciasFinales(resultado).isEmpty());
        if (abrirAnalisis) {
            abrirAnalisis = false;
            pestanaAnalisis.fire();
        }

        llenarExploracion(contenido.exploracion());
        boolean conComparacion = textoComparacion != null;
        mostrar(tituloComparacion, conComparacion);
        mostrar(comparacion, conComparacion);

        GraficaFuncion grafica = contenido.grafica().map(d -> new GraficaFuncion(d, latexLeyenda)).orElse(null);
        pe.mathnova.presentation.grafica.CurvasNivel nivel = contenido.curvasNivel()
                .map(pe.mathnova.presentation.grafica.CurvasNivel::new).orElse(null);
        if (nivel != null) {
            grafica = null;
            zonaGrafica.getChildren().setAll(nivel);
            var datos = contenido.curvasNivel().get();
            fabricaGrafica = () -> new pe.mathnova.presentation.grafica.CurvasNivel(datos);
        } else if (grafica != null) {
            zonaGrafica.getChildren().setAll(grafica);
            var datos = contenido.grafica().get();
            fabricaGrafica = () -> new GraficaFuncion(datos, latexLeyenda);
        } else {
            fabricaGrafica = null;
        }
        boolean hayGrafica = grafica != null || nivel != null;
        mostrar(zonaGrafica, hayGrafica);
        mostrar(ayudaGrafica, hayGrafica);
        mostrar(graficaNoDisponible, !hayGrafica);
        mostrar(tarjetaGrafica, true);
        tarjetaGrafica.setOpacity(1);

        panelPropiedades.setAnimated(animador.activas());

        List<Node> solucion = new ArrayList<>(List.of(iconoSolucion, subtituloSolucion, formulaSolucion, irreducibles,
                alternativas, secciones));
        if (v.sinPasos() != null) {
            solucion.add(sinPasos);
        }
        if (!sugerenciasFinales(resultado).isEmpty()) {
            solucion.add(zonaSugerencias);
        }
        solucion.addAll(pizarra.cierre());
        animador.presentar(pizarra.filasPasos(), pizarra.numeros(), solucion, etiquetaVerificado, () -> {});
    }

    static boolean esFactorizacion(String rotulo) {
        return "FACTORIZA".equals(rotulo);
    }

    private static List<Sugerencia> sugerenciasFinales(Resultado resultado) {
        return esFactorizacion(resultado.rotulo()) ? List.of() : resultado.sugerencias();
    }

    static List<pe.mathnova.presentation.resolucion.VistaPaso> pasosPizarra(VistaResolucion v, Object resolucion) {
        if (resolucion instanceof pe.mathnova.domain.calculo.derivadas.ResolucionDerivada) {
            return v.pasos().stream()
                    .filter(p -> !pe.mathnova.domain.calculo.derivadas.MotorDerivadas.PUNTOS_CRITICOS.equals(p.codigo()))
                    .toList();
        }
        return v.pasos();
    }

    static String resultadoPizarra(VistaResolucion v, ContenidoPestanas contenido, Object resolucion) {
        if (resolucion instanceof pe.mathnova.domain.calculo.limites.ResolucionLimite && v.latexFormula() != null) {
            return v.latexFormula();
        }
        List<String> restricciones = switch (resolucion) {
            case pe.mathnova.domain.calculo.derivadas.ResolucionDerivada d ->
                    pe.mathnova.domain.calculo.FactorComun.restricciones(d.pasos());
            case pe.mathnova.domain.calculo.integrales.ResolucionIntegral i when !i.definida() ->
                    pe.mathnova.domain.calculo.FactorComun.restricciones(i.pasos());
            case null, default -> List.of();
        };
        String latex = contenido.latexSolucion();
        if (latex == null || restricciones.isEmpty()) {
            return latex;
        }
        return latex + " \\qquad (" + String.join(",\\ ", restricciones).replace("≠", "\\neq").replace("−", "-")
                + ")";
    }

    static String rotulo(ResolverService.Plan p) {
        return switch (p) {
            case ResolverService.Plan.Factorizar f -> "FACTORIZA";
            case ResolverService.Plan.FactorizarFunciones f -> "FACTORIZA";
            case ResolverService.Plan.Simplificar s -> "SIMPLIFICA";
            case ResolverService.Plan.ResolverEcuacion e -> "RESUELVE";
            case ResolverService.Plan.CalcularLimite l -> "CALCULA EL LÍMITE";
            case ResolverService.Plan.LimiteDoble l -> "CALCULA EL LÍMITE";
            case ResolverService.Plan.Derivar d -> "DERIVA";
            case ResolverService.Plan.Parciales d -> "DERIVA";
            case ResolverService.Plan.Integrar i -> "INTEGRA";
            case ResolverService.Plan.IntegralMultiple i -> "INTEGRA";
            case ResolverService.Plan.Extremos e -> "HALLA LOS EXTREMOS";
            case null, default -> null;
        };
    }

    private static String latexEntrada(String texto) {
        try {
            return ImpresoraLatex.imprimir(Parser.parse(texto));
        } catch (RuntimeException e) {
            return null;
        }
    }

    static PizarraResolucion.Comprobacion comprobacion(VistaResolucion v, String rotulo) {
        return comprobacion(v, rotulo, null);
    }

    static PizarraResolucion.Comprobacion comprobacion(VistaResolucion v, String rotulo, Object resolucion) {
        if ("FACTORIZA".equals(rotulo) && resolucion instanceof pe.mathnova.domain.factorizacion.Resolucion r) {
            PizarraResolucion.Comprobacion c = comprobacionFactorizacion(v, r);
            if (c != null) {
                return c;
            }
        }
        if (resolucion instanceof pe.mathnova.domain.calculo.integrales.ResolucionIntegral ri) {
            PizarraResolucion.Comprobacion c = comprobacionIntegral(ri);
            if (c != null) {
                return c;
            }
        }
        if (v.verificacion().isEmpty()) {
            return v.ayudaEtiqueta() == null ? null
                    : new PizarraResolucion.Comprobacion(null, null, List.of(),
                            v.etiqueta() == VistaResolucion.Etiqueta.VERIFICADO
                                    ? PizarraResolucion.Comprobacion.Estado.COINCIDE
                                    : PizarraResolucion.Comprobacion.Estado.SIN_COMPROBAR,
                            v.ayudaEtiqueta());
        }
        boolean numerica = v.verificacion().getFirst().startsWith("Se evalúan")
                || Verificacion.AYUDA_NUMERICA.equals(v.ayudaEtiqueta());
        String metodo;
        String accion;
        String bien;
        String mal;
        switch (rotulo == null ? "" : rotulo) {
            case "SIMPLIFICA" -> {
                metodo = "Por productos cruzados";
                accion = "Ver productos";
                bien = "Coincide con la fracción original";
                mal = "No coincide con la fracción original";
            }
            case "RESUELVE" -> {
                metodo = "Sustituyendo las soluciones";
                accion = "Ver sustitución";
                bien = "Las soluciones cumplen la ecuación";
                mal = "Alguna solución no cumple la ecuación";
            }
            case "INTEGRA" -> {
                metodo = "Derivando el resultado";
                accion = "Ver derivación";
                bien = "La derivada coincide con el integrando";
                mal = "La derivada no coincide con el integrando";
            }
            case "FACTORIZA" -> {
                metodo = numerica ? "Evaluando con " + pe.mathnova.domain.numeros.EvaluacionNumerica.CIFRAS
                        + " cifras" : "Por expansión";
                accion = numerica ? "Ver evaluación" : "Ver expansión";
                bien = numerica ? coincidenciaNumerica(v.verificacion()) : "Coincide con la entrada";
                mal = "No coincide con la entrada";
            }
            default -> {
                metodo = null;
                accion = "Ver detalle";
                bien = "Comprobado";
                mal = "No coincide";
            }
        }
        return new PizarraResolucion.Comprobacion(metodo, accion, v.verificacion(),
                v.coincide() ? PizarraResolucion.Comprobacion.Estado.COINCIDE
                        : PizarraResolucion.Comprobacion.Estado.NO_COINCIDE,
                v.coincide() ? bien : mal);
    }

    static PizarraResolucion.Comprobacion comprobacionIntegral(
            pe.mathnova.domain.calculo.integrales.ResolucionIntegral r) {
        var ver = r.verificacion();
        if (ver == null || !r.resuelta()) {
            return null;
        }
        char x = r.variable();
        List<pe.mathnova.presentation.resolucion.CadenaIgualdades.Eslabon> cadena = new ArrayList<>();
        cadena.add(new pe.mathnova.presentation.resolucion.CadenaIgualdades.Eslabon(
                "\\frac{d}{d" + x + "}\\left(" + ImpresoraLatex.imprimir(r.antiderivada()) + "\\right)"));
        String derivada = ImpresoraLatex.imprimir(ver.derivada());
        cadena.add(new pe.mathnova.presentation.resolucion.CadenaIgualdades.Eslabon(derivada));
        String ultimo = derivada;
        try {
            pe.mathnova.domain.expresion.Expresion repartida = repartir(ver.derivada());
            String r2 = ImpresoraLatex.imprimir(repartida);
            if (!r2.equals(ultimo)) {
                cadena.add(new pe.mathnova.presentation.resolucion.CadenaIgualdades.Eslabon(r2));
                ultimo = r2;
            }
            String simple = ImpresoraLatex.imprimir(
                    pe.mathnova.domain.calculo.derivadas.FormaFinal.de(repartida, x));
            if (!simple.equals(ultimo)) {
                cadena.add(new pe.mathnova.presentation.resolucion.CadenaIgualdades.Eslabon(simple));
                ultimo = simple;
            }
        } catch (RuntimeException e) {
        }
        boolean uno = r.coeficiente().equals(pe.mathnova.domain.algebra.Racional.UNO);
        String integrando = ImpresoraLatex.imprimir(r.integral().cuerpo());
        if (ver.coincide() && ver.exacta() && uno && !integrando.equals(ultimo)) {
            cadena.add(new pe.mathnova.presentation.resolucion.CadenaIgualdades.Eslabon(integrando));
        }
        boolean ok = ver.coincide();
        String mensaje = !ok ? "La derivada no coincide con el integrando"
                : ver.exacta() ? "La derivada coincide con el integrando"
                : "La derivada coincide con el integrando en cinco puntos";
        String extra = uno ? null : "Integrando: " + pe.mathnova.domain.expresion.ImpresoraPizarra.imprimir(
                r.integral().cuerpo()) + " (por " + pe.mathnova.presentation.FormateadorTexto.numero(r.coeficiente()) + ")";
        return new PizarraResolucion.Comprobacion("Derivando el resultado", "Ver derivación", List.of(),
                ok ? PizarraResolucion.Comprobacion.Estado.COINCIDE : PizarraResolucion.Comprobacion.Estado.NO_COINCIDE,
                mensaje, cadena, extra == null ? null : "Comparación", extra == null ? List.of() : List.of(extra));
    }

    static pe.mathnova.domain.expresion.Expresion repartir(pe.mathnova.domain.expresion.Expresion e) {
        if (!(e instanceof pe.mathnova.domain.expresion.Suma s)) {
            List<pe.mathnova.domain.expresion.Expresion> t = repartidos(e);
            return t.size() == 1 ? e : new pe.mathnova.domain.expresion.Suma(t);
        }
        List<pe.mathnova.domain.expresion.Expresion> t = new ArrayList<>();
        s.terminos().forEach(x -> t.addAll(repartidos(x)));
        return new pe.mathnova.domain.expresion.Suma(t);
    }

    private static List<pe.mathnova.domain.expresion.Expresion> repartidos(pe.mathnova.domain.expresion.Expresion t) {
        if (t instanceof pe.mathnova.domain.expresion.Cociente c
                && c.numerador() instanceof pe.mathnova.domain.expresion.Suma s
                && pe.mathnova.domain.expresion.Expresiones.variables(c.denominador()).isEmpty()) {
            return s.terminos().stream().map(u -> (pe.mathnova.domain.expresion.Expresion)
                    new pe.mathnova.domain.expresion.Cociente(u, c.denominador())).toList();
        }
        if (t instanceof pe.mathnova.domain.expresion.Negacion n) {
            List<pe.mathnova.domain.expresion.Expresion> a = repartidos(n.argumento());
            return a.size() == 1 ? List.of(t) : a.stream().map(u -> (pe.mathnova.domain.expresion.Expresion)
                    new pe.mathnova.domain.expresion.Negacion(u)).toList();
        }
        return List.of(t);
    }

    static String coincidenciaNumerica(List<String> lineas) {
        long puntos = lineas.stream().filter(l -> l.strip().endsWith("✓")).count();
        return "Coincidencia numérica en " + puntos + (puntos == 1 ? " punto" : " puntos");
    }

    private static List<String> puntos(List<String> lineas) {
        return lineas.stream().filter(l -> l.contains("≈") && (l.strip().endsWith("✓") || l.strip().endsWith("✗")))
                .toList();
    }

    private static PizarraResolucion.Comprobacion comprobacionFactorizacion(VistaResolucion v,
            pe.mathnova.domain.factorizacion.Resolucion r) {
        var entrada = pe.mathnova.domain.algebra.Polinomio.desde(r.entrada());
        var ext = r.extension();
        var legible = pe.mathnova.presentation.resolucion.FormaLegible.de(r);
        if (legible.isPresent() && !legible.get().cadena().isEmpty()) {
            var l = legible.get();
            boolean algebra = l.algebraExacta();
            boolean raiz = l.raizComprobada();
            List<String> numericos = puntos(v.verificacion());
            String metodo = "Expansión exacta con r"
                    + (raiz ? " · P(r) ≈ 0 con " + pe.mathnova.domain.numeros.EvaluacionNumerica.CIFRAS + " cifras" : "");
            boolean ok = algebra && raiz;
            return new PizarraResolucion.Comprobacion(metodo, "Ver expansión", List.of(),
                    ok ? PizarraResolucion.Comprobacion.Estado.COINCIDE : PizarraResolucion.Comprobacion.Estado.NO_COINCIDE,
                    ok ? "Coincide con la entrada (usando P(r) = 0)" : "No coincide con la entrada", l.cadena(),
                    numericos.isEmpty() ? null : coincidenciaNumerica(numericos)
                            + " (valor de Cardano de r, " + pe.mathnova.domain.numeros.EvaluacionNumerica.CIFRAS + " cifras)",
                    numericos);
        }
        if (v.latexFormula() == null || v.verificacion().isEmpty()) {
            return null;
        }
        pe.mathnova.presentation.resolucion.CadenaExpansion.Resultado cadena;
        if (ext == null || ext.pasos().isEmpty()) {
            cadena = pe.mathnova.presentation.resolucion.CadenaExpansion.de(r.resultado(), entrada, v.latexFormula());
        } else {
            cadena = pe.mathnova.presentation.resolucion.CadenaExpansion.de(ext.resultado(), entrada, v.latexFormula())
                    .orElse(null);
        }
        if (cadena == null) {
            return null;
        }
        if (cadena.expresiones().size() < 2) {
            return new PizarraResolucion.Comprobacion("Por expansión", null, List.of(),
                    cadena.coincide() ? PizarraResolucion.Comprobacion.Estado.COINCIDE
                            : PizarraResolucion.Comprobacion.Estado.NO_COINCIDE,
                    cadena.coincide() ? "Coincide con la entrada" : "No coincide con la entrada");
        }
        boolean ok = cadena.coincide();
        return new PizarraResolucion.Comprobacion("Por expansión", "Ver expansión", List.of(),
                ok ? PizarraResolucion.Comprobacion.Estado.COINCIDE : PizarraResolucion.Comprobacion.Estado.NO_COINCIDE,
                ok ? "Coincide con la entrada" : "No coincide con la entrada", cadena.eslabones(), null, List.of());
    }

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
        resolver.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
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
            tarjeta.setMinWidth(0);
            tarjeta.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> Math.max(120, Math.min(170, vistaExploracion.getWidth() - 48)), vistaExploracion.widthProperty()));
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
        sinMetodos.setText(SIN_METODOS_EN_PANTALLA);
        mostrar(sinMetodos, x.tarjetas().isEmpty());
    }

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
        comparacion.getChildren().setAll(pe.mathnova.presentation.resolucion.ComparacionCampos.calculando());
    }

    private void llenarComparacion(FactorizacionService.Comparacion c) {
        comparacion.getChildren().clear();
        if (c == null) {
            Label l = new Label("Todavía no hay un resultado");
            l.getStyleClass().add("comparacion-pendiente");
            comparacion.getChildren().add(l);
            return;
        }
        comparacion.getChildren().add(new pe.mathnova.presentation.resolucion.ComparacionCampos(
                pe.mathnova.presentation.resolucion.ComparacionCampos.filas(c), campo));
    }

    private void mostrarNoAplica(MetodoNoAplicable e) {
        animador.detenerFases();
        mostrarCarga(false);
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
        mostrar(cuerpoSolucion, false);
        subtituloSolucion.setText("No se pudo resolver");
        errorSolucion.setText(mensaje);
        mostrar(errorSolucion, true);
        pizarra.limpiar();
        sinPasos.setText(mensaje);
        mostrar(sinPasos, true);
    }

    private void limpiarResultado() {
        pizarra.limpiar();
        irreducibles.getChildren().clear();
        alternativas.getChildren().clear();
        formulaSolucion.setLatex(null);
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

    private void seguirAncho(Scene escena) {
        if (escena != null) {
            escena.widthProperty().addListener((obs, antes, ancho) -> acomodarBarra(ancho.doubleValue()));
            acomodarBarra(escena.getWidth());
        }
    }

    private void prepararDivision() {
        panelHerramientas.setMinWidth(ANCHO_HERRAMIENTAS_MIN);
        panelPizarra.setMinWidth(ANCHO_PIZARRA_MIN);
        for (VBox panel : List.of(panelHerramientas, panelPizarra)) {
            panel.setMaxWidth(Double.MAX_VALUE);
            panel.setFillWidth(true);
        }
        SplitPane.setResizableWithParent(panelHerramientas, false);
        columnaIzquierda.setMinWidth(0);
        contenidoIzquierda.setMinWidth(0);
        tarjetaProcedimiento.setMinWidth(0);

        division.setDividerPositions(PROPORCION_HERRAMIENTAS);
        division.widthProperty().addListener((obs, antes, ancho) -> posicionInicial(ancho.doubleValue()));

        division.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getTarget() instanceof Node n && enSeparador(n)) {
                division.pseudoClassStateChanged(ARRASTRANDO, true);
            }
        });
        division.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> division.pseudoClassStateChanged(ARRASTRANDO, false));
    }

    private void posicionInicial(double ancho) {
        if (divisionInicial || ancho < ANCHO_HERRAMIENTAS_MIN + ANCHO_PIZARRA_MIN) {
            return;
        }
        divisionInicial = true;
        division.setDividerPositions(PROPORCION_HERRAMIENTAS);
    }

    private boolean enSeparador(Node n) {
        for (Node x = n; x != null && x != division; x = x.getParent()) {
            if (x.getStyleClass().contains("split-pane-divider")) {
                return true;
            }
        }
        return false;
    }

    double posicionSeparador() {
        return division.getDividers().get(0).getPosition();
    }

    private void acomodarBarra(double anchoVentana) {
        boolean unaFila = anchoVentana <= 0 || anchoVentana >= ANCHO_BARRA_UNA_FILA;
        boolean ahora = barraEntrada.getChildren().contains(botonResolver);
        if (unaFila == ahora) {
            return;
        }
        if (unaFila) {
            filaAcciones.getChildren().clear();
            barraEntrada.getChildren().setAll(zonaEditor, etiquetaEscritoAMano, selectorCampo, botonResolver);
            zonaEntrada.getChildren().remove(filaAcciones);
        } else {
            barraEntrada.getChildren().setAll(zonaEditor, etiquetaEscritoAMano);
            filaAcciones.getChildren().setAll(selectorCampo, botonResolver);
            zonaEntrada.getChildren().add(zonaEntrada.getChildren().indexOf(barraEntrada) + 1, filaAcciones);
        }
    }

    boolean barraEnDosFilas() {
        return !barraEntrada.getChildren().contains(botonResolver);
    }
}
