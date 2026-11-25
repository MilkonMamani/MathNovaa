package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.animacion.AnimadorResolucion;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.pizarra.ModeloRuffini;
import pe.mathnova.presentation.pizarra.Pizarra;

public final class PizarraResolucion extends VBox {

    static final String VER_EXPLICACION = "Ver explicación";
    static final double TAMANO = Pizarra.TAMANO;
    static final double TAMANO_MINIMO = Pizarra.TAMANO_MINIMO;
    static final double TAMANO_RESULTADO = 22;

    private static final PseudoClass PENDIENTE = PseudoClass.getPseudoClass("pendiente");
    private static final PseudoClass ENCENDIDO = PseudoClass.getPseudoClass("encendido");
    private static final PseudoClass MAL = PseudoClass.getPseudoClass("mal");
    private static final PseudoClass NEUTRA = PseudoClass.getPseudoClass("neutra");
    private static final String ICONO_FOCO =
            "M5 7 A5 5 0 1 1 11 11.5 V13 H5 V11.5 A5 5 0 0 1 5 7 Z M6 15.5 H10";
    private static final String ICONO_AYUDA =
            "M10 1 A9 9 0 1 1 9.99 1 Z M7.5 7.5 A2.5 2.5 0 1 1 10 10 V11.5 M10 14.4 V14.6";
    static final String ICONO_DESCARGA = "M10 2 V13 M5.5 8.5 L10 13 L14.5 8.5 M3 13.5 V17.5 H17 V13.5";
    static final String EXPORTAR = "Exportar PDF";
    static final String EXPORTAR_SIN_RESULTADO = "Exportar PDF: resuelve un ejercicio primero";
    private static final String FLECHA = "M0 6 H15 M10 1 L15 6 L10 11";

    public record Contenido(String rotulo, String latexEntrada, List<VistaPaso> pasos, String latexResultado,
                            Comprobacion comprobacion, List<Desarrollo.Renglon> pieResultado) {

        public Contenido {
            pieResultado = pieResultado == null ? List.of() : List.copyOf(pieResultado);
        }

        public Contenido(String rotulo, String latexEntrada, List<VistaPaso> pasos, String latexResultado,
                         Comprobacion comprobacion) {
            this(rotulo, latexEntrada, pasos, latexResultado, comprobacion, List.of());
        }

        public Contenido(String rotulo, String latexEntrada, List<VistaPaso> pasos, String latexResultado,
                         String comprobacion) {
            this(rotulo, latexEntrada, pasos, latexResultado,
                    comprobacion == null ? null : Comprobacion.mensaje(comprobacion));
        }
    }

    public record Comprobacion(String metodo, String accion, List<String> lineas, Estado estado, String mensaje,
                               List<CadenaIgualdades.Eslabon> cadena, String tituloExtra, List<String> lineasExtra) {

        public enum Estado { COINCIDE, NO_COINCIDE, SIN_COMPROBAR }

        public Comprobacion {
            lineas = lineas == null ? List.of() : List.copyOf(lineas);
            cadena = cadena == null ? List.of() : List.copyOf(cadena);
            lineasExtra = lineasExtra == null ? List.of() : List.copyOf(lineasExtra);
        }

        public Comprobacion(String metodo, String accion, List<String> lineas, Estado estado, String mensaje) {
            this(metodo, accion, lineas, estado, mensaje, List.of(), null, List.of());
        }

        boolean conDetalle() {
            return accion != null && (!lineas.isEmpty() || !cadena.isEmpty() || !lineasExtra.isEmpty());
        }

        public static Comprobacion mensaje(String texto) {
            return new Comprobacion(null, null, List.of(), Estado.SIN_COMPROBAR, texto);
        }
    }

    private final RenderizadorLatex renderizador;
    private final AnimadorResolucion animador;
    private final EstadoPizarra estado;
    private Consumer<Boolean> onExplicaciones = si -> { };

    private final Button anterior = new Button("‹");
    private final Label etiquetaPasos = new Label();
    private final Button siguiente = new Button("›");
    private final Button botonModo = new Button();
    private final ToggleButton interruptor = new ToggleButton();

    private final VBox cuerpo = new VBox();
    private final Button exportar = new Button();
    private final Tooltip ayudaExportar = new Tooltip();
    private Runnable alExportar;
    private EstadoPizarra.Modo modoInicial = EstadoPizarra.Modo.TODOS;
    private boolean hayResultado;
    private boolean comprobacionPermitida = true;
    private final Label rotulo = new Label();
    private final FormulaAjustable enunciado = new FormulaAjustable(TAMANO_RESULTADO, TAMANO_MINIMO);
    private final VBox filas = new VBox();
    private final VBox bloqueResultado = new VBox();
    private final StackPane superficieResultado = new StackPane();
    private final FormulaAjustable formulaResultado = new FormulaAjustable(TAMANO_RESULTADO, TAMANO_MINIMO + 2);
    private final VBox pieResultado = new VBox(4);

    private final VBox seccionComprobacion = new VBox();
    private final Label metodoComprobacion = new Label();
    private final Label estadoComprobacion = new Label();
    private final ToggleButton verDetalle = new ToggleButton();
    private final VBox detalleComprobacion = new VBox();

    private final List<Fila> pasos = new ArrayList<>();
    private final List<Node> notasSubpasos = new ArrayList<>();
    private RenglonesPaso.Marco marco = RenglonesPaso.Marco.GENERAL;

    public PizarraResolucion(RenderizadorLatex renderizador, AnimadorResolucion animador, boolean explicaciones) {
        this.renderizador = renderizador;
        this.animador = animador;
        this.estado = new EstadoPizarra(explicaciones);
        getStyleClass().add("pizarra-resolucion");
        getStyleClass().add(pe.mathnova.presentation.latex.Apariencia.CLASE_ESCALABLE);

        Label titulo = new Label("Pizarra");
        titulo.getStyleClass().add("titulo-tarjeta");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);

        anterior.getStyleClass().addAll("pizarra-control", "pizarra-flecha-nav");
        ayuda(anterior, "Paso anterior");
        anterior.setOnAction(e -> {
            estado.anterior();
            pintar();
        });
        siguiente.getStyleClass().addAll("pizarra-control", "pizarra-flecha-nav");
        ayuda(siguiente, "Paso siguiente");
        siguiente.setOnAction(e -> {
            estado.siguiente();
            pintar();
        });
        etiquetaPasos.getStyleClass().add("pizarra-etiqueta-pasos");
        etiquetaPasos.setMinWidth(Region.USE_PREF_SIZE);
        botonModo.getStyleClass().add("pizarra-control");
        botonModo.setMinWidth(Region.USE_PREF_SIZE);
        botonModo.setOnAction(e -> {
            estado.alternarModo();
            pintar();
        });

        armarInterruptor();

        exportar.setGraphic(icono(ICONO_DESCARGA, "pizarra-icono"));
        exportar.getStyleClass().addAll("pizarra-control", "boton-icono", "pizarra-exportar");
        exportar.setAccessibleText(EXPORTAR);
        exportar.setOnAction(e -> {
            if (alExportar != null) {
                alExportar.run();
            }
        });
        StackPane envolturaPdf = new StackPane(exportar);
        Tooltip.install(envolturaPdf, ayudaExportar);
        ayudaExportar.setShowDelay(javafx.util.Duration.millis(300));
        habilitarExportar(false);

        titulo.setMinWidth(Region.USE_PREF_SIZE);
        interruptor.setMinWidth(Region.USE_PREF_SIZE);
        HBox navegacion = new HBox(8, anterior, etiquetaPasos, siguiente);
        navegacion.setAlignment(Pos.CENTER_LEFT);
        navegacion.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane controles = new FlowPane(8, 8, navegacion, botonModo, interruptor, envolturaPdf);
        controles.setAlignment(Pos.CENTER_RIGHT);
        controles.setRowValignment(javafx.geometry.VPos.CENTER);
        controles.setPrefWrapLength(10_000);
        HBox filaTitulo = new HBox(8, titulo, hueco, controles);
        filaTitulo.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(8, filaTitulo);
        cabecera.widthProperty().addListener((obs, antes, ancho) ->
                acomodarCabecera(cabecera, filaTitulo, titulo, controles, ancho.doubleValue()));
        cabecera.getStyleClass().add("pizarra-cabecera");

        rotulo.getStyleClass().add("pizarra-rotulo");
        enunciado.setRenglonesPrimero(true);
        formulaResultado.setRenglonesPrimero(true);
        enunciado.getStyleClass().add("pizarra-enunciado");
        filas.getStyleClass().add("pizarra-filas");

        Label rotuloResultado = new Label("Resultado");
        rotuloResultado.getStyleClass().add("pizarra-resultado-rotulo");
        formulaResultado.getStyleClass().add("pizarra-resultado-formula");
        superficieResultado.getStyleClass().add("pizarra-resultado-final");
        superficieResultado.getChildren().add(formulaResultado);
        superficieResultado.setAlignment(Pos.CENTER_LEFT);
        superficieResultado.setMaxWidth(Region.USE_PREF_SIZE);
        superficieResultado.setMinWidth(0);
        superficieResultado.setAccessibleText("Resultado");
        bloqueResultado.getStyleClass().add("pizarra-resultado-bloque");
        pieResultado.getStyleClass().add("pizarra-resultado-pie");
        pieResultado.setMinWidth(0);
        bloqueResultado.getChildren().addAll(rotuloResultado, superficieResultado, pieResultado);

        cuerpo.getStyleClass().add("pizarra-cuerpo");
        armarComprobacion();
        cuerpo.getChildren().addAll(rotulo, enunciado, filas, bloqueResultado, seccionComprobacion);

        getChildren().addAll(cabecera, cuerpo);
        limpiar();
    }

    private static void acomodarCabecera(VBox cabecera, HBox filaTitulo, Label titulo, FlowPane controles,
            double ancho) {
        boolean unaFila = ancho <= 0 || ancho >= titulo.prefWidth(-1) + 8 + controles.prefWidth(-1);
        boolean ahora = filaTitulo.getChildren().contains(controles);
        if (unaFila == ahora) {
            return;
        }
        if (unaFila) {
            cabecera.getChildren().remove(controles);
            filaTitulo.getChildren().add(controles);
        } else {
            filaTitulo.getChildren().remove(controles);
            cabecera.getChildren().add(controles);
        }
    }

    private void armarInterruptor() {
        Region perilla = new Region();
        perilla.getStyleClass().add("interruptor-perilla");
        StackPane riel = new StackPane(perilla);
        riel.getStyleClass().add("interruptor-riel");
        Label texto = new Label("Explicaciones");
        texto.getStyleClass().add("interruptor-texto");
        HBox grafico = new HBox(6, icono(ICONO_FOCO, "pizarra-icono"), texto, riel);
        grafico.setAlignment(Pos.CENTER_LEFT);
        interruptor.setGraphic(grafico);
        interruptor.getStyleClass().add("interruptor-explicaciones");
        interruptor.setAccessibleRole(AccessibleRole.TOGGLE_BUTTON);
        interruptor.setAccessibleText("Explicaciones");
        interruptor.setTooltip(new Tooltip("Mostrar u ocultar todas las explicaciones"));
        interruptor.setSelected(estado.explicaciones());
        interruptor.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                interruptor.fire();
                e.consume();
            }
        });
        interruptor.selectedProperty().addListener((obs, antes, si) -> {
            if (si != estado.explicaciones()) {
                conservarLectura(() -> {
                    estado.setExplicaciones(si);
                    pintar();
                });
                onExplicaciones.accept(si);
            }
        });
    }

    private void armarComprobacion() {
        Label titulo = new Label("Comprobación");
        titulo.getStyleClass().add("pizarra-comprobacion-titulo");
        metodoComprobacion.getStyleClass().add("pizarra-comprobacion-metodo");
        metodoComprobacion.setWrapText(true);
        estadoComprobacion.getStyleClass().add("pizarra-comprobacion-estado");
        estadoComprobacion.setWrapText(true);
        configurarDesplegable(verDetalle, VER_COMPROBACION, OCULTAR_COMPROBACION, detalleComprobacion);
        verDetalle.getStyleClass().add("pizarra-ver-detalle");
        detalleComprobacion.getStyleClass().add("pizarra-comprobacion-detalle");
        seccionComprobacion.getStyleClass().add("pizarra-comprobacion-seccion");
        seccionComprobacion.getChildren().addAll(titulo, verDetalle, detalleComprobacion);
    }

    static final String VER_COMPROBACION = "Ver comprobación";
    static final String OCULTAR_COMPROBACION = "Ocultar comprobación";
    static final String VER_DESARROLLO = "Ver desarrollo";
    static final String OCULTAR_DESARROLLO = "Ocultar desarrollo";
    private static final String CHEVRON = "M1 1 L6 6 L11 1";

    static ToggleButton desplegable(String ver, String ocultar, Node contenido) {
        ToggleButton b = new ToggleButton();
        configurarDesplegable(b, ver, ocultar, contenido);
        return b;
    }

    static void configurarDesplegable(ToggleButton b, String ver, String ocultar, Node contenido) {
        SVGPath chevron = new SVGPath();
        chevron.setContent(CHEVRON);
        chevron.getStyleClass().add("pizarra-chevron");
        b.setText(ver);
        b.setGraphic(chevron);
        b.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        b.setGraphicTextGap(8);
        b.getStyleClass().add("pizarra-desplegable");
        b.setAlignment(Pos.CENTER_LEFT);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMinWidth(0);
        b.setAccessibleRole(AccessibleRole.TOGGLE_BUTTON);
        b.setAccessibleText(ver);
        b.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                b.fire();
                e.consume();
            }
        });
        b.selectedProperty().addListener((obs, antes, si) -> {
            b.setText(si ? ocultar : ver);
            b.setAccessibleText(si ? ocultar : ver);
            chevron.setRotate(si ? 180 : 0);
            mostrarNodo(contenido, si);
        });
        mostrarNodo(contenido, b.isSelected());
    }

    public void setOnExplicaciones(Consumer<Boolean> accion) {
        onExplicaciones = accion == null ? si -> { } : accion;
    }

    public void setExplicaciones(boolean si) {
        if (si != estado.explicaciones()) {
            estado.setExplicaciones(si);
            interruptor.setSelected(si);
            pintar();
        }
    }

    public boolean explicaciones() {
        return estado.explicaciones();
    }

    public void mostrar(Contenido c) {
        hayResultado = true;
        habilitarExportar(true);
        pasos.clear();
        notasSubpasos.clear();
        filas.getChildren().clear();
        marco = RenglonesPaso.Marco.de(c.rotulo(), c.latexEntrada());
        String previo = c.latexEntrada();
        for (VistaPaso p : ReorganizarPasos.de(c.pasos(), marco)) {
            Fila f = new Fila(p, previo);
            pasos.add(f);
            filas.getChildren().add(f.bloque);
            if (p.despues() != null) {
                previo = ImpresoraLatex.imprimir(p.despues());
            }
        }
        estado.nuevo(pasos.size());
        if (modoInicial == EstadoPizarra.Modo.PASO_A_PASO) {
            estado.pasoAPaso();
        }

        rotulo.setText(c.rotulo() == null ? "" : c.rotulo());
        mostrarNodo(rotulo, c.rotulo() != null);
        enunciado.setLatex(c.latexEntrada());
        mostrarNodo(enunciado, c.latexEntrada() != null);
        mostrarNodo(filas, !pasos.isEmpty());

        formulaResultado.setLatex(c.latexResultado());
        bloqueResultado.setUserData(c.latexResultado());
        pieResultado.getChildren().clear();
        if (!c.pieResultado().isEmpty()) {
            pieResultado.getChildren().add(new VistaDesarrollo(new Desarrollo(c.pieResultado()), TAMANO, TAMANO_MINIMO));
        }
        mostrarNodo(pieResultado, !c.pieResultado().isEmpty());
        llenarComprobacion(c.comprobacion());
        mostrarNodo(cuerpo, true);
        pintar();
    }

    private void llenarComprobacion(Comprobacion c) {
        seccionComprobacion.setUserData(c);
        detalleComprobacion.getChildren().clear();
        verDetalle.setSelected(false);
        mostrarNodo(detalleComprobacion, false);
        if (c == null) {
            return;
        }
        metodoComprobacion.setText(c.metodo() == null ? "" : c.metodo());
        mostrarNodo(metodoComprobacion, c.metodo() != null);
        String marca = switch (c.estado()) {
            case COINCIDE -> "✓ ";
            case NO_COINCIDE -> "✗ ";
            case SIN_COMPROBAR -> "";
        };
        estadoComprobacion.setText(marca + (c.mensaje() == null ? "" : c.mensaje()));
        estadoComprobacion.pseudoClassStateChanged(MAL, c.estado() == Comprobacion.Estado.NO_COINCIDE);
        estadoComprobacion.pseudoClassStateChanged(NEUTRA, c.estado() == Comprobacion.Estado.SIN_COMPROBAR);
        mostrarNodo(verDetalle, true);
        boolean exacta = !c.cadena().isEmpty() && c.estado() == Comprobacion.Estado.COINCIDE;
        if (!exacta) {
            if (c.metodo() != null) {
                detalleComprobacion.getChildren().add(metodoComprobacion);
            }
            detalleComprobacion.getChildren().add(estadoComprobacion);
        }
        {
            if (!c.cadena().isEmpty()) {
                detalleComprobacion.getChildren().add(new CadenaIgualdades(c.cadena(), TAMANO, TAMANO_MINIMO));
            } else if (!c.lineas().isEmpty()) {
                detalleComprobacion.getChildren().add(Igualdades.deTextos(c.lineas(), TAMANO, TAMANO_MINIMO));
            }
            if (c.tituloExtra() != null && !c.lineasExtra().isEmpty()) {
                Label t = new Label(c.tituloExtra());
                t.getStyleClass().add("pizarra-comprobacion-subtitulo");
                t.setWrapText(true);
                detalleComprobacion.getChildren().addAll(t, Igualdades.deTextos(c.lineasExtra(), TAMANO, TAMANO_MINIMO));
            }
        }
    }

    public void setModoInicial(EstadoPizarra.Modo modo) {
        modoInicial = modo == null ? EstadoPizarra.Modo.TODOS : modo;
    }

    public void setOnExportar(Runnable accion) {
        alExportar = accion;
        habilitarExportar(hayResultado);
    }

    private void habilitarExportar(boolean resultado) {
        boolean si = resultado && alExportar != null;
        exportar.setDisable(!si);
        ayudaExportar.setText(si ? EXPORTAR : EXPORTAR_SIN_RESULTADO);
        exportar.setAccessibleHelp(ayudaExportar.getText());
    }

    public Button botonExportar() {
        return exportar;
    }

    public void limpiar() {
        hayResultado = false;
        habilitarExportar(false);
        pasos.clear();
        notasSubpasos.clear();
        filas.getChildren().clear();
        estado.nuevo(0);
        formulaResultado.setLatex(null);
        bloqueResultado.setUserData(null);
        pieResultado.getChildren().clear();
        mostrarNodo(pieResultado, false);
        llenarComprobacion(null);
        mostrarNodo(cuerpo, false);
        pintar();
    }

    public List<Node> filasPasos() {
        return pasos.stream().map(f -> (Node) f.bloque).toList();
    }

    public List<Node> numeros() {
        return pasos.stream().map(f -> (Node) f.numero).toList();
    }

    public void agregarAlResultado(Node... extras) {
        bloqueResultado.getChildren().addAll(extras);
    }

    public void setComprobacionPermitida(boolean si) {
        if (comprobacionPermitida != si) {
            comprobacionPermitida = si;
            pintar();
        }
    }

    public List<Node> cierre() {
        return List.of(bloqueResultado, seccionComprobacion);
    }

    private void pintar() {
        etiquetaPasos.setText(estado.etiqueta());
        botonModo.setText(estado.textoModo());
        botonModo.setDisable(estado.total() == 0);
        anterior.setDisable(!estado.puedeAnterior());
        siguiente.setDisable(!estado.puedeSiguiente());
        interruptor.pseudoClassStateChanged(ENCENDIDO, estado.explicaciones());
        for (Fila f : pasos) {
            f.pintar();
        }
        for (Node n : notasSubpasos) {
            mostrarNodo(n, estado.explicaciones());
        }
        boolean cierre = estado.resultadoVisible() && cuerpo.isVisible();
        mostrarNodo(bloqueResultado, cierre && bloqueResultado.getUserData() != null);
        mostrarNodo(seccionComprobacion, cierre && comprobacionPermitida && seccionComprobacion.getUserData() != null);
    }

    private void conservarLectura(Runnable cambio) {
        ScrollPane desplazable = desplazable();
        Node ancla = desplazable == null ? null : ancla(desplazable);
        double antes = ancla == null ? 0 : ancla.localToScene(0, 0).getY();
        cambio.run();
        if (ancla == null || getScene() == null) {
            return;
        }
        Parent raiz = getScene().getRoot();
        raiz.applyCss();
        raiz.layout();
        double despues = ancla.localToScene(0, 0).getY();
        Node contenido = desplazable.getContent();
        double alto = contenido.getLayoutBounds().getHeight();
        double vista = desplazable.getViewportBounds().getHeight();
        if (alto <= vista || Math.abs(despues - antes) < 0.5) {
            return;
        }
        double rango = desplazable.getVmax() - desplazable.getVmin();
        double v = desplazable.getVvalue() + (despues - antes) / (alto - vista) * rango;
        desplazable.setVvalue(Math.max(desplazable.getVmin(), Math.min(desplazable.getVmax(), v)));
    }

    private ScrollPane desplazable() {
        for (Node n = getParent(); n != null; n = n.getParent()) {
            if (n instanceof ScrollPane sp) {
                return sp;
            }
        }
        return null;
    }

    private Node ancla(ScrollPane desplazable) {
        if (pasos.isEmpty()) {
            return null;
        }
        if (estado.modo() == EstadoPizarra.Modo.PASO_A_PASO) {
            return pasos.get(estado.actual() - 1).bloque;
        }
        Bounds vista = desplazable.localToScene(desplazable.getLayoutBounds());
        for (Fila f : pasos) {
            Bounds b = f.bloque.localToScene(f.bloque.getLayoutBounds());
            if (b != null && vista != null && b.getMaxY() > vista.getMinY()) {
                return f.bloque;
            }
        }
        return pasos.getLast().bloque;
    }

    private final class Fila {

        private final int n;
        private final VBox bloque = new VBox();
        private final HBox flecha;
        private final StackPane numero;
        private final SVGPath punta = new SVGPath();
        private final Label metodo;
        private final VBox formula = new VBox(8);
        private final PasoConNota cuerpoPaso;
        private final Button botonNota;
        private final VBox nota;
        private final Hyperlink ocultar = new Hyperlink("Ocultar");
        private ToggleButton verDesarrollo;
        private Node desarrollo;
        private boolean abiertoAlLlegar;

        Fila(VistaPaso p, String previo) {
            n = p.orden();

            numero = new StackPane(new Label(String.valueOf(n)));
            numero.getStyleClass().add("pizarra-numero");
            numero.setMinWidth(Region.USE_PREF_SIZE);
            punta.setContent(FLECHA);
            punta.getStyleClass().add("pizarra-flecha");
            metodo = new Label(p.titulo());
            metodo.getStyleClass().add("pizarra-metodo");
            metodo.setWrapText(true);
            if (p.ayuda() != null && !p.ayuda().equals(p.titulo())) {
                metodo.setTooltip(new Tooltip(p.ayuda()));
            }

            botonNota = new Button(null, icono(ICONO_AYUDA, "boton-nota-icono"));
            botonNota.getStyleClass().add("boton-nota");
            botonNota.setAccessibleText("Ver explicación del paso " + n);
            botonNota.setTooltip(new Tooltip(VER_EXPLICACION));
            botonNota.setOnAction(e -> {
                estado.alternarNota(n);
                pintar();
                ocultar.requestFocus();
            });

            flecha = new HBox(8, numero, punta, metodo, botonNota);
            flecha.setAlignment(Pos.CENTER_LEFT);
            flecha.getStyleClass().add("pizarra-paso-flecha");

            formula.getStyleClass().add("pizarra-formula");
            formula.setMinWidth(0);
            llenarFormula(formula, p, previo, true, String.valueOf(n));

            nota = armarNota(p);
            cuerpoPaso = new PasoConNota(formula, nota);
            cuerpoPaso.getStyleClass().add("pizarra-fila");

            bloque.getChildren().addAll(flecha, cuerpoPaso);
            bloque.getStyleClass().add("pizarra-paso");
        }

        private void llenarFormula(VBox destino, VistaPaso p, String previo, boolean principal, String numeracion) {
            if (principal && ReorganizarPasos.AUXILIAR.equals(p.codigo())) {
                llenarAuxiliar(destino, p);
                return;
            }
            if (p.desarrollo() != null) {
                VistaDesarrollo d = new VistaDesarrollo(p.desarrollo(), TAMANO, TAMANO_MINIMO);
                d.getStyleClass().add("pizarra-despues");
                destino.getChildren().add(d);
                return;
            }
            String antes = p.antes() == null ? null : ImpresoraLatex.imprimir(p.antes());
            String despues = p.despues() == null ? null : ImpresoraLatex.imprimir(p.despues());
            boolean general = marco.operacion() == RenglonesPaso.Operacion.GENERAL;
            boolean parte = general && principal && antes != null && previo != null && !mismo(antes, previo);

            if (parte && p.trabajo() != null) {
                Label con = new Label("Se trabaja con");
                con.getStyleClass().add("pizarra-parte");
                con.setMinWidth(Region.USE_PREF_SIZE);
                FormulaAjustable f = formula(antes);
                HBox.setHgrow(f, Priority.ALWAYS);
                HBox fila = new HBox(10, con, f);
                fila.setAlignment(Pos.CENTER_LEFT);
                fila.getStyleClass().add("pizarra-parte-fila");
                destino.getChildren().add(fila);
            }
            List<Igualdades.Renglon> renglones = RenglonesPaso.renglones(p, marco, principal);
            boolean hayTrabajo = p.trabajo() != null;
            String conclusion;
            if (despues == null) {
                conclusion = null;
            } else if (general && !(hayTrabajo || parte)) {
                conclusion = antes == null || mismo(antes, despues) ? despues : antes + " = " + despues;
            } else if (!hayTrabajo || renglones != null || !RenglonesPaso.dibujoConcluye(p.trabajo())) {
                boolean hay = renglones != null && !renglones.isEmpty();
                conclusion = RenglonesPaso.conclusion(p, marco, principal, hay,
                        hay ? renglones.getLast() : null);
            } else {
                conclusion = null;
            }
            if (renglones != null) {
                List<Igualdades.Renglon> todos = new ArrayList<>(renglones);
                if (conclusion != null && !todos.isEmpty() && mismaIgualdad(todos.getLast().latex(), conclusion)) {
                    conclusion = null;
                }
                if (conclusion != null) {
                    todos.add(Igualdades.Renglon.deLatex(conclusion));
                }
                if (!todos.isEmpty()) {
                    Igualdades bloqueLineas = new Igualdades(todos, TAMANO, TAMANO_MINIMO);
                    bloqueLineas.getStyleClass().add("pizarra-despues");
                    destino.getChildren().add(bloqueLineas);
                }
            } else {
                if (hayTrabajo) {
                    destino.getChildren().add(Pizarra.de(p.trabajo()));
                }
                if (conclusion != null) {
                    FormulaAjustable f = formula(conclusion.startsWith("=") && antes != null
                            ? antes + " " + conclusion : conclusion);
                    f.getStyleClass().add("pizarra-despues");
                    destino.getChildren().add(f);
                }
            }
            if (!p.hijos().isEmpty()) {
                destino.getChildren().add(subpasos(p, numeracion));
            }
        }

        private Node subpasos(VistaPaso p, String numeracion) {
            VBox internos = new VBox(14);
            internos.getStyleClass().add("pizarra-subpasos");
            internos.setMinWidth(0);
            String previo = p.antes() == null ? null : ImpresoraLatex.imprimir(p.antes());
            for (VistaPaso h : p.hijos()) {
                String num = numeracion + "." + h.orden();
                Label numero = new Label(num);
                numero.getStyleClass().add("pizarra-subpaso-numero");
                numero.setMinWidth(Region.USE_PREF_SIZE);
                SVGPath flechaSub = new SVGPath();
                flechaSub.setContent(FLECHA);
                flechaSub.getStyleClass().addAll("pizarra-flecha", "pizarra-flecha-sub");
                Label titulo = new Label(h.titulo());
                titulo.getStyleClass().add("pizarra-subpaso-titulo");
                titulo.setWrapText(true);
                if (h.ayuda() != null && !h.ayuda().equals(h.titulo())) {
                    titulo.setTooltip(new Tooltip(h.ayuda()));
                }
                HBox cabeza = new HBox(8, numero, flechaSub, titulo);
                cabeza.setAlignment(Pos.CENTER_LEFT);
                VBox operaciones = new VBox(8);
                operaciones.setMinWidth(0);
                operaciones.getStyleClass().add("pizarra-subpaso-operaciones");
                llenarFormula(operaciones, h, previo, false, num);
                Label explicacion = new Label(FormateadorTexto.pizarra(h.justificacion()));
                explicacion.setWrapText(true);
                explicacion.setMinWidth(0);
                explicacion.getStyleClass().add("pizarra-nota-texto");
                VBox nota = new VBox(explicacion);
                nota.getStyleClass().add("pizarra-nota");
                notasSubpasos.add(nota);
                mostrarNodo(nota, estado.explicaciones());
                VBox sub = new VBox(6, cabeza, operaciones, nota);
                sub.getStyleClass().add("pizarra-subpaso");
                sub.setMinWidth(0);
                internos.getChildren().add(sub);
            }
            String grupo = p.grupo() == null ? "Pasos internos" : p.grupo();
            String accion = grupo + " (" + p.hijos().size() + (p.hijos().size() == 1 ? " paso)" : " pasos)");
            ToggleButton ver = new ToggleButton("▸ " + accion);
            ver.getStyleClass().addAll("pizarra-ver-subpasos", "pasos-anidados");
            ver.setAccessibleText("Ver " + accion);
            ver.setWrapText(true);
            ver.setMinWidth(0);
            mostrarNodo(internos, false);
            ver.selectedProperty().addListener((obs, antes, si) -> {
                ver.setText((si ? "▾ " : "▸ ") + accion);
                mostrarNodo(internos, si);
            });
            VBox caja = new VBox(10, ver, internos);
            caja.getStyleClass().add("pizarra-subpasos-caja");
            caja.setMinWidth(0);
            return caja;
        }

        private VBox armarNota(VistaPaso p) {
            VBox caja = new VBox(4);
            if (p.trabajo() instanceof TrabajoIntermedio.TablaRuffini t) {
                List<String> faltan = ModeloRuffini.de(t).faltantes();
                if (!faltan.isEmpty()) {
                    Label pregunta = new Label(faltan.size() == 1 ? "¿Por qué aparece un cero?"
                            : "¿Por qué aparecen ceros?");
                    pregunta.getStyleClass().add("pizarra-nota-titulo");
                    Label porque = new Label(porQueCero(faltan));
                    porque.setWrapText(true);
                    porque.getStyleClass().add("pizarra-nota-texto");
                    caja.getChildren().addAll(pregunta, porque);
                }
            }
            Label texto = new Label(FormateadorTexto.pizarra(p.justificacion()));
            texto.setWrapText(true);
            texto.getStyleClass().add("pizarra-nota-texto");
            ocultar.getStyleClass().add("pizarra-ocultar");
            ocultar.setAccessibleText("Ocultar la explicación del paso " + n);
            ocultar.setOnAction(e -> {
                estado.alternarNota(n);
                pintar();
                botonNota.requestFocus();
            });
            caja.getChildren().addAll(texto, ocultar);
            caja.getStyleClass().add("pizarra-nota");
            caja.setMaxHeight(Region.USE_PREF_SIZE);
            return caja;
        }

        private void llenarAuxiliar(VBox destino, VistaPaso p) {
            DesarrolloAuxiliar d = new DesarrolloAuxiliar(p.hijos(), TAMANO, TAMANO_MINIMO);
            if (!d.largo()) {
                VBox caja = d.caja();
                caja.getStyleClass().add("pizarra-desarrollo-corto");
                destino.getChildren().add(caja);
                return;
            }
            String resumen = ImpresoraLatex.imprimir(p.antes()) + " = " + ImpresoraLatex.imprimir(p.despues());
            Igualdades r = new Igualdades(List.of(Igualdades.Renglon.deLatex(resumen)), TAMANO, TAMANO_MINIMO);
            r.getStyleClass().add("pizarra-resumen");
            VBox caja = d.caja();
            caja.getStyleClass().add("pizarra-desarrollo-aux");
            mostrarNodo(caja, false);
            verDesarrollo = desplegable("Ver desarrollo", "Ocultar desarrollo", caja);
            verDesarrollo.getStyleClass().add("pizarra-ver-desarrollo");
            desarrollo = caja;
            destino.getChildren().addAll(r, verDesarrollo, caja);
        }

        void pintar() {
            boolean visible = estado.pasoVisible(n);
            if (verDesarrollo != null && !abiertoAlLlegar && estado.modo() == EstadoPizarra.Modo.PASO_A_PASO
                    && estado.actual() == n) {
                abiertoAlLlegar = true;
                verDesarrollo.setSelected(true);
            }
            numero.pseudoClassStateChanged(PENDIENTE, !visible);
            mostrarNodo(punta, visible);
            mostrarNodo(metodo, visible);
            mostrarNodo(formula, visible);
            mostrarNodo(cuerpoPaso, visible);
            mostrarNodo(botonNota, estado.botonNotaVisible(n));
            mostrarNodo(nota, estado.notaVisible(n));
            mostrarNodo(ocultar, estado.ocultarVisible(n));
            cuerpoPaso.requestLayout();
        }
    }

    static String porQueCero(List<String> faltan) {
        List<String> nombres = faltan.stream().map(t -> t.isEmpty() ? "el término independiente" : t).toList();
        if (nombres.size() == 1) {
            String t = nombres.getFirst();
            return (t.startsWith("el ") ? "Falta " + t : "Falta el término en " + t)
                    + ". Por eso su coeficiente es 0.";
        }
        String lista = String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.getLast();
        return "Faltan los términos en " + lista + ". Por eso sus coeficientes son 0.";
    }

    private static boolean mismo(String a, String b) {
        return a.replace(" ", "").equals(b.replace(" ", ""));
    }

    static boolean mismaIgualdad(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return normal(a).equals(normal(b));
    }

    private static String normal(String latex) {
        return latex.replace("\\left", "").replace("\\right", "").replace("\\,", "").replace("\\cdot", "·")
                .replaceAll("\\s+", "").replaceAll("^=", "");
    }

    private static FormulaAjustable formula(String latex) {
        FormulaAjustable f = Igualdades.formula(TAMANO, TAMANO_MINIMO);
        f.setLatex(latex);
        return f;
    }

    static final class PasoConNota extends Region {

        static final double SEPARACION = 32;
        static final double NOTA_MINIMA = 200;
        static final double NOTA_MAXIMA = 320;
        static final double DEBAJO = 10;

        private final Region contenido;
        private final Region nota;

        PasoConNota(Region contenido, Region nota) {
            this.contenido = contenido;
            this.nota = nota;
            getChildren().addAll(contenido, nota);
            setMinWidth(0);
        }

        private boolean conNota() {
            return nota.isManaged();
        }

        boolean alCostado(double ancho) {
            return conNota() && ancho >= contenido.prefWidth(-1) + SEPARACION + NOTA_MINIMA;
        }

        private double anchoNota(double ancho) {
            return Math.min(NOTA_MAXIMA, ancho - contenido.prefWidth(-1) - SEPARACION);
        }

        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override
        protected double computeMinWidth(double alto) {
            return 0;
        }

        @Override
        protected double computePrefWidth(double alto) {
            return contenido.prefWidth(-1) + (conNota() ? SEPARACION + NOTA_MINIMA : 0);
        }

        @Override
        protected double computePrefHeight(double ancho) {
            if (!conNota()) {
                return contenido.prefHeight(ancho);
            }
            if (ancho < 0 || alCostado(ancho)) {
                double nw = ancho < 0 ? NOTA_MINIMA : anchoNota(ancho);
                double cw = ancho < 0 ? -1 : ancho - SEPARACION - nw;
                return Math.max(contenido.prefHeight(cw), nota.prefHeight(nw));
            }
            return contenido.prefHeight(ancho) + DEBAJO + nota.prefHeight(ancho);
        }

        @Override
        protected double computeMinHeight(double ancho) {
            return computePrefHeight(ancho);
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            if (!conNota()) {
                contenido.resizeRelocate(0, 0, w, contenido.prefHeight(w));
                return;
            }
            if (alCostado(w)) {
                double nw = anchoNota(w);
                double cw = w - SEPARACION - nw;
                contenido.resizeRelocate(0, 0, cw, contenido.prefHeight(cw));
                nota.resizeRelocate(snapPositionX(cw + SEPARACION), 0, nw, nota.prefHeight(nw));
            } else {
                double hc = contenido.prefHeight(w);
                contenido.resizeRelocate(0, 0, w, hc);
                nota.resizeRelocate(0, snapPositionY(hc + DEBAJO), w, nota.prefHeight(w));
            }
        }
    }

    private static SVGPath icono(String contenido, String clase) {
        SVGPath s = new SVGPath();
        s.setContent(contenido);
        s.getStyleClass().add(clase);
        return s;
    }

    private static void ayuda(Button b, String texto) {
        b.setAccessibleText(texto);
        b.setTooltip(new Tooltip(texto));
    }

    private static void mostrarNodo(Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }

    EstadoPizarra estado() {
        return estado;
    }

    Button botonNota(int paso) {
        return pasos.get(paso - 1).botonNota;
    }

    Node nota(int paso) {
        return pasos.get(paso - 1).nota;
    }

    Hyperlink ocultar(int paso) {
        return pasos.get(paso - 1).ocultar;
    }

    Node formula(int paso) {
        return pasos.get(paso - 1).formula;
    }

    Node numero(int paso) {
        return pasos.get(paso - 1).numero;
    }

    PasoConNota cuerpoPaso(int paso) {
        return pasos.get(paso - 1).cuerpoPaso;
    }

    Label etiquetaPasos() {
        return etiquetaPasos;
    }

    Button anterior() {
        return anterior;
    }

    Button siguiente() {
        return siguiente;
    }

    Button botonModo() {
        return botonModo;
    }

    ToggleButton interruptor() {
        return interruptor;
    }

    Node recuadroResultado() {
        return bloqueResultado;
    }

    StackPane superficieResultado() {
        return superficieResultado;
    }

    Node comprobacion() {
        return seccionComprobacion;
    }

    ToggleButton verDetalle() {
        return verDetalle;
    }

    Node detalleComprobacion() {
        return detalleComprobacion;
    }

    Label estadoComprobacion() {
        return estadoComprobacion;
    }

    Node pieResultado() {
        return pieResultado;
    }

    Label metodoComprobacion() {
        return metodoComprobacion;
    }

    Label metodo(int paso) {
        return pasos.get(paso - 1).metodo;
    }
}
