package pe.mathnova.presentation.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.application.service.HistorialService;
import pe.mathnova.application.service.HistorialService.Filtro;
import pe.mathnova.application.service.HistorialService.Periodo;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.resolucion.FormulaAjustable;

/**
 * Pantalla "Historial" (HU15): buscador, curso, fecha y "Solo favoritos" arriba; la tabla en una tarjeta
 * blanca; el conteo debajo. El ojo abre la hoja de la resolución a la derecha, con el fondo oscurecido; la
 * estrella marca el favorito; la papelera pregunta antes de borrar. Toda la lógica está en
 * {@link HistorialService}; aquí solo se dibuja.
 */
@Component
@Scope("prototype")
public class HistorialController {

    private static final Logger LOG = LoggerFactory.getLogger(HistorialController.class);

    private static final PseudoClass MARCADO = PseudoClass.getPseudoClass("marcado");
    static final String SIN_RESULTADOS = "No hay ejercicios que coincidan con la búsqueda.";
    static final String VACIO = "Todavía no hay ejercicios. Resuelve uno en Resolver y aparecerá aquí.";
    static final String EXPORTAR = "Guardar esta hoja como PDF";
    static final String EXPORTAR_ESPERA = "Espera a que cargue la hoja";

    /** Anchos de las columnas (Ejercicio se lleva el resto). */
    static final double ANCHO_OPERACION = 150;
    static final double ANCHO_TEMA = 180;
    static final double ANCHO_CAMPO = 60;
    static final double ANCHO_FECHA = 124;
    static final double ANCHO_ACCIONES = 3 * 44 + 2 * 4;

    /** Ícono de la cuadrícula 24 × 24, trazo de 1,6 px (mathnova.css). */
    static final String ICONO_OJO = "M2.5 12 C5 7.5 8.3 5.5 12 5.5 C15.7 5.5 19 7.5 21.5 12 C19 16.5 15.7 18.5 12 18.5"
            + " C8.3 18.5 5 16.5 2.5 12 Z M12 9 A3 3 0 1 1 12 15 A3 3 0 1 1 12 9 Z";
    static final String ICONO_ESTRELLA = "M12 3.6 L14.5 8.9 L20.3 9.6 L16 13.6 L17.2 19.4 L12 16.5 L6.8 19.4"
            + " L8 13.6 L3.7 9.6 L9.5 8.9 Z";
    static final String ICONO_PAPELERA = "M4.5 7 H19.5 M9.5 7 V4.5 H14.5 V7 M6.5 7 L7.4 19.5 H16.6 L17.5 7"
            + " M10 10.5 V16 M14 10.5 V16";
    /** La flecha de la pizarra entre el número y el título del paso. */
    private static final String FLECHA = "M0 6 H15 M10 1 L15 6 L10 11";

    private final HistorialService historial;
    private final pe.mathnova.presentation.Navegador navegador;
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(250));
    private final List<VistaFila> vistas = new ArrayList<>();
    private int pagina;
    private HistorialService.Hoja hojaAbierta;
    /** Cambia con cada hoja pedida: una hoja que llega tarde se descarta. */
    private int pedidoHoja;
    private HistorialService.Fila porEliminar;
    private Node focoAntes;
    /** Sprint 9, parte C: el PDF de la hoja; null sin el servicio (pruebas sin base). */
    private ExportarPdf exportador;
    private final Tooltip ayudaExportar = new Tooltip();
    /** Para las pruebas: corre la carga de la hoja (por omisión, en un hilo aparte). */
    private Consumer<Task<HistorialService.Hoja>> ejecutor = t -> {
        Thread hilo = new Thread(t, "hoja-historial");
        hilo.setDaemon(true);
        hilo.start();
    };

    @FXML
    private StackPane raiz;
    @FXML
    private VBox pantalla;
    @FXML
    private TextField buscador;
    @FXML
    private ComboBox<String> selectorCurso;
    @FXML
    private ComboBox<Periodo> selectorFecha;
    @FXML
    private ToggleButton soloFavoritos;
    @FXML
    private HBox encabezado;
    @FXML
    private ScrollPane desplazamiento;
    @FXML
    private VBox filas;
    @FXML
    private Label sinResultados;
    @FXML
    private Label conteo;
    @FXML
    private Button mostrarMas;
    @FXML
    private Region velo;
    @FXML
    private VBox hoja;
    @FXML
    private Label verboHoja;
    @FXML
    private StackPane formulaHoja;
    @FXML
    private ScrollPane desplazamientoHoja;
    @FXML
    private VBox cuerpoHoja;
    @FXML
    private Button abrirEnResolver;
    @FXML
    private Button practicar;
    @FXML
    private StackPane envolturaExportar;
    @FXML
    private Button exportarPdf;
    @FXML
    private Button cerrarHoja;
    @FXML
    private VBox dialogo;
    @FXML
    private StackPane formulaDialogo;
    @FXML
    private Button confirmarEliminar;
    @FXML
    private Button cancelarEliminar;

    public HistorialController(HistorialService historial, pe.mathnova.presentation.Navegador navegador) {
        this.historial = historial;
        this.navegador = navegador;
    }

    @FXML
    void initialize() {
        selectorCurso.getItems().setAll(historial.cursos());
        selectorCurso.getSelectionModel().selectFirst();
        selectorFecha.getItems().setAll(Periodo.values());
        selectorFecha.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Periodo p) {
                return p == null ? "" : p.texto();
            }

            @Override
            public Periodo fromString(String s) {
                return null;
            }
        });
        selectorFecha.getSelectionModel().select(Periodo.CUALQUIERA);

        esperaBusqueda.setOnFinished(e -> recargar());
        buscador.textProperty().addListener((obs, antes, ahora) -> esperaBusqueda.playFromStart());
        selectorCurso.valueProperty().addListener((obs, antes, ahora) -> recargar());
        selectorFecha.valueProperty().addListener((obs, antes, ahora) -> recargar());
        soloFavoritos.selectedProperty().addListener((obs, antes, si) -> {
            soloFavoritos.pseudoClassStateChanged(MARCADO, si);
            recargar();
        });

        armarEncabezado();
        Tooltip.install(envolturaExportar, ayudaExportar);
        ayudaExportar.setShowDelay(Duration.millis(300));
        habilitarExportar(false);
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && (hoja.isVisible() || dialogo.isVisible())) {
                cerrarCapas();
                e.consume();
            }
        });
        cerrarCapas();
        recargar();
    }

    // ---------------------------------------------------------------- tabla

    private void armarEncabezado() {
        encabezado.getChildren().setAll(
                columna(cabecera("Ejercicio"), -1),
                columna(cabecera("Operación"), ANCHO_OPERACION),
                columna(cabecera("Tema"), ANCHO_TEMA),
                columna(cabecera("Campo"), ANCHO_CAMPO),
                columna(cabecera("Fecha"), ANCHO_FECHA),
                columna(cabecera("Acciones"), ANCHO_ACCIONES));
    }

    private static Label cabecera(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("historial-cabecera");
        return l;
    }

    /** Una celda: ancho fijo, o el resto si {@code ancho} es negativo. */
    private static Node columna(Node contenido, double ancho) {
        StackPane celda = new StackPane(contenido);
        celda.setAlignment(Pos.CENTER_LEFT);
        celda.getStyleClass().add("historial-celda");
        if (ancho < 0) {
            celda.setMinWidth(120);
            celda.setPrefWidth(200);
            celda.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(celda, Priority.ALWAYS);
            Rectangle recorte = new Rectangle();
            recorte.widthProperty().bind(celda.widthProperty());
            recorte.heightProperty().bind(celda.heightProperty());
            celda.setClip(recorte);
        } else {
            celda.setMinWidth(ancho);
            celda.setPrefWidth(ancho);
            celda.setMaxWidth(ancho);
        }
        return celda;
    }

    private Filtro filtro() {
        return new Filtro(buscador.getText(), selectorCurso.getValue(),
                selectorFecha.getValue() == null ? Periodo.CUALQUIERA : selectorFecha.getValue(), soloFavoritos.isSelected());
    }

    /** Vuelve a la primera página con los filtros de la barra (también al entrar a la pantalla). */
    public void recargar() {
        esperaBusqueda.stop();
        pagina = 0;
        vistas.clear();
        filas.getChildren().clear();
        cargarPagina();
        desplazamiento.setVvalue(0);
    }

    @FXML
    void mostrarMas() {
        pagina++;
        cargarPagina();
    }

    private void cargarPagina() {
        HistorialService.Pagina p;
        try {
            p = historial.listar(filtro(), pagina);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer el historial", e);
            p = new HistorialService.Pagina(List.of(), 0, pagina, false);
        }
        for (HistorialService.Fila f : p.filas()) {
            VistaFila v = new VistaFila(f);
            vistas.add(v);
            filas.getChildren().add(v.nodo);
        }
        boolean vacio = p.total() == 0;
        sinResultados.setText(sinFiltros() ? VACIO : SIN_RESULTADOS);
        mostrar(sinResultados, vacio);
        mostrar(desplazamiento, !vacio);
        conteo.setText(conteo(p.total()));
        mostrar(conteo, !vacio);
        mostrar(mostrarMas, p.hayMas());
    }

    private boolean sinFiltros() {
        Filtro f = filtro();
        return (f.texto() == null || f.texto().isBlank()) && (f.curso() == null
                || HistorialService.TODOS_LOS_CURSOS.equals(f.curso())) && f.periodo() == Periodo.CUALQUIERA
                && !f.soloFavoritos();
    }

    /** "1 ejercicio", "12 ejercicios". */
    static String conteo(long n) {
        return n == 1 ? "1 ejercicio" : n + " ejercicios";
    }

    /** Una fila de la tabla, con sus tres botones. */
    private final class VistaFila {
        final HistorialService.Fila fila;
        final HBox nodo;
        final Button estrella;

        VistaFila(HistorialService.Fila f) {
            this.fila = f;
            Node formula = formula(f.latex(), f.texto(), 17, 12);
            Tooltip.install(formula, new Tooltip(f.texto()));

            Button ojo = botonIcono(ICONO_OJO, "Ver la hoja con todos los pasos");
            ojo.setOnAction(e -> abrirHoja(f));
            estrella = botonIcono(ICONO_ESTRELLA, "");
            estrella.getStyleClass().add("boton-estrella");
            estrella.setOnAction(e -> alternarFavorito(this));
            Button papelera = botonIcono(ICONO_PAPELERA, "Eliminar este ejercicio");
            papelera.setOnAction(e -> pedirEliminar(f));
            HBox acciones = new HBox(4, ojo, estrella, papelera);
            acciones.setAlignment(Pos.CENTER_LEFT);

            nodo = new HBox(
                    columna(formula, -1),
                    columna(texto(f.operacion(), "historial-texto"), ANCHO_OPERACION),
                    columna(texto(f.tema(), "historial-texto-2"), ANCHO_TEMA),
                    columna(texto(f.campo(), "historial-texto-2"), ANCHO_CAMPO),
                    columna(texto(historial.fecha(f.fecha()), "historial-texto-2"), ANCHO_FECHA),
                    columna(acciones, ANCHO_ACCIONES));
            nodo.setAlignment(Pos.CENTER_LEFT);
            nodo.getStyleClass().add("historial-fila");
            pintarEstrella(f.favorito());
        }

        void pintarEstrella(boolean marcada) {
            estrella.pseudoClassStateChanged(MARCADO, marcada);
            String ayuda = marcada ? "Quitar de favoritos" : "Marcar como favorito";
            estrella.setTooltip(conRetraso(new Tooltip(ayuda)));
            estrella.setAccessibleText(ayuda);
        }
    }

    private static Label texto(String s, String clase) {
        Label l = new Label(s);
        l.getStyleClass().add(clase);
        l.setTooltip(conRetraso(new Tooltip(s)));
        return l;
    }

    private void alternarFavorito(VistaFila v) {
        boolean marcada;
        try {
            marcada = historial.alternarFavorito(v.fila.id());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo cambiar el favorito", e);
            return;
        }
        if (soloFavoritos.isSelected() && !marcada) {
            recargar();
            return;
        }
        // El favorito es del ejercicio: todas sus filas cambian.
        for (VistaFila otra : vistas) {
            if (otra.fila.ejercicioId() == v.fila.ejercicioId()) {
                otra.pintarEstrella(marcada);
            }
        }
    }

    // ---------------------------------------------------------------- eliminar (RN26)

    private void pedirEliminar(HistorialService.Fila f) {
        porEliminar = f;
        formulaDialogo.getChildren().setAll(formula(f.latex(), f.texto(), 18, 13));
        abrirCapa(dialogo);
        cancelarEliminar.requestFocus();
    }

    @FXML
    void confirmarEliminar() {
        HistorialService.Fila f = porEliminar;
        cerrarCapas();
        if (f == null) {
            return;
        }
        try {
            historial.eliminar(f.id());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo eliminar el ejercicio {}", f.id(), e);
        }
        recargar();
    }

    // ---------------------------------------------------------------- hoja

    private void abrirHoja(HistorialService.Fila f) {
        int este = ++pedidoHoja;
        hojaAbierta = null;
        verboHoja.setText(f.operacion());
        formulaHoja.getChildren().setAll(formula(f.latex(), f.texto(), 22, 14));
        Label cargando = new Label("Cargando la hoja…");
        cargando.getStyleClass().add("historial-texto-2");
        cuerpoHoja.getChildren().setAll(cargando);
        abrirEnResolver.setDisable(true);
        practicar.setDisable(true);
        habilitarExportar(false);
        abrirCapa(hoja);
        cerrarHoja.requestFocus();

        Task<HistorialService.Hoja> tarea = new Task<>() {
            @Override
            protected HistorialService.Hoja call() {
                return historial.hoja(f.id());
            }
        };
        tarea.setOnSucceeded(e -> {
            if (este == pedidoHoja && hoja.isVisible()) {
                llenarHoja(tarea.getValue());
            }
        });
        tarea.setOnFailed(e -> {
            if (este == pedidoHoja) {
                LOG.warn("No se pudo abrir la hoja {}", f.id(), tarea.getException());
                Label error = new Label("No se pudo abrir la hoja de este ejercicio.");
                error.getStyleClass().add("historial-texto-2");
                cuerpoHoja.getChildren().setAll(error);
            }
        });
        ejecutor.accept(tarea);
    }

    /** Arriba el verbo y la expresión; los pasos numerados; al final el resultado en el recuadro azul claro. */
    void llenarHoja(HistorialService.Hoja h) {
        hojaAbierta = h;
        verboHoja.setText(h.verbo());
        formulaHoja.getChildren().setAll(formula(h.latexEntrada(), h.entradaTexto(), 22, 14));
        List<Node> nodos = new ArrayList<>();
        for (HistorialService.PasoHoja p : h.pasos()) {
            nodos.add(paso(p));
        }
        if (!h.descartados().isEmpty()) {
            Label titulo = new Label("Métodos que no aplicaron");
            titulo.getStyleClass().add("historial-hoja-seccion");
            nodos.add(titulo);
            for (HistorialService.Descartado d : h.descartados()) {
                Label nombre = new Label(d.titulo());
                nombre.getStyleClass().add("historial-descartado");
                nodos.add(new VBox(4, nombre, nota(d.motivo())));
            }
        }
        if (!h.restricciones().isEmpty()) {
            Label r = new Label("Restricciones: " + String.join(", ", h.restricciones()));
            r.getStyleClass().add("historial-texto-2");
            r.setWrapText(true);
            nodos.add(r);
        }
        Label rotulo = new Label("Resultado");
        rotulo.getStyleClass().add("pizarra-resultado-rotulo");
        StackPane recuadro = new StackPane(formula(h.resultadoLatex(), h.resultadoTexto(), 22, 14));
        recuadro.setAlignment(Pos.CENTER_LEFT);
        recuadro.getStyleClass().add("pizarra-resultado-final");
        VBox resultado = new VBox(6, rotulo, recuadro);
        resultado.getStyleClass().add("pizarra-resultado-bloque");
        nodos.add(resultado);
        cuerpoHoja.getChildren().setAll(nodos);
        desplazamientoHoja.setVvalue(0);
        abrirEnResolver.setDisable(h.entradaTexto() == null || h.entradaTexto().isBlank());
        boolean practicable = h.entradaTexto() != null && navegador.sePuedePracticar(h.entradaTexto(),
                h.campo() == null ? null : Campo.valueOf(h.campo()));
        practicar.setDisable(!practicable);
        practicar.setAccessibleHelp(practicable ? "Practicar este ejercicio paso a paso"
                : "Por ahora se practican factorizaciones en Q y límites");
        habilitarExportar(true);
    }

    /** "Practicar" de la hoja (Sprint 9, parte D): el ejercicio en la pantalla Práctica, ya iniciado. */
    @FXML
    void practicar() {
        HistorialService.Hoja h = hojaAbierta;
        if (h == null) {
            return;
        }
        cerrarCapas();
        navegador.abrirEnPractica(h.entradaTexto(), h.campo() == null ? null : Campo.valueOf(h.campo()));
    }

    // ---------------------------------------------------------------- PDF (Sprint 9, parte C)

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setExportacion(pe.mathnova.application.service.ExportacionPdfService servicio) {
        exportador = servicio == null ? null : new ExportarPdf(servicio);
        if (exportarPdf != null) {
            habilitarExportar(hojaAbierta != null);
        }
    }

    private void habilitarExportar(boolean hojaLista) {
        boolean si = hojaLista && exportador != null;
        exportarPdf.setDisable(!si);
        ayudaExportar.setText(exportador == null ? "No disponible" : si ? EXPORTAR : EXPORTAR_ESPERA);
        exportarPdf.setAccessibleHelp(ayudaExportar.getText());
    }

    /** "Exportar PDF" de la hoja: el mismo PDF que en Resolver. Desde aquí no hay gráfica que incluir. */
    @FXML
    void exportarPdf() {
        HistorialService.Hoja h = hojaAbierta;
        if (h == null || exportador == null) {
            return;
        }
        exportador.exportar(exportarPdf, hoy -> exportador.servicio().desdeHistorial(h.id(), hoy), null);
    }

    /** Para las pruebas. */
    ExportarPdf exportador() {
        return exportador;
    }

    /** "1 → Factor común", la fórmula y la nota con la línea azul a la izquierda. */
    private Node paso(HistorialService.PasoHoja p) {
        return paso(p.numero(), p.nivel(), p.titulo(), p.latex(), p.nota());
    }

    /** Un paso con el formato de la Pizarra; también lo usa la Práctica (Sprint 9, parte D). */
    static Node paso(String numeroPaso, int nivel, String tituloPaso, String latex, String nota) {
        Label numero = new Label(numeroPaso);
        numero.getStyleClass().add("historial-paso-numero");
        SVGPath punta = new SVGPath();
        punta.setContent(FLECHA);
        punta.getStyleClass().add("pizarra-flecha");
        Label titulo = new Label(tituloPaso);
        titulo.getStyleClass().add("pizarra-metodo");
        titulo.setWrapText(true);
        HBox flecha = new HBox(8, numero, punta, titulo);
        flecha.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(6, flecha);
        if (latex != null && !latex.isBlank()) {
            caja.getChildren().add(formula(latex, latex, 18, 12));
        }
        if (nota != null && !nota.isBlank()) {
            caja.getChildren().add(nota(nota));
        }
        caja.getStyleClass().add("historial-paso");
        caja.setPadding(new javafx.geometry.Insets(0, 0, 0, 22 * nivel));
        return caja;
    }

    static Node nota(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("pizarra-nota-texto");
        l.setWrapText(true);
        VBox caja = new VBox(l);
        caja.getStyleClass().add("pizarra-nota");
        return caja;
    }

    @FXML
    void abrirEnResolver() {
        HistorialService.Hoja h = hojaAbierta;
        if (h == null) {
            return;
        }
        cerrarCapas();
        Campo campo = h.campo() == null ? null : Campo.valueOf(h.campo());
        navegador.abrirEnResolver(h.entradaTexto(), campo, h.metodoForzado());
    }

    // ---------------------------------------------------------------- capas

    private void abrirCapa(Node capa) {
        if (!hoja.isVisible() && !dialogo.isVisible()) {
            focoAntes = raiz.getScene() == null ? null : raiz.getScene().getFocusOwner();
        }
        hoja.setVisible(capa == hoja);
        dialogo.setVisible(capa == dialogo);
        velo.setVisible(true);
        pantalla.setMouseTransparent(true);
    }

    /** Cierra la hoja o el diálogo, y el velo. */
    @FXML
    void cerrarCapas() {
        boolean habiaCapa = hoja.isVisible() || dialogo.isVisible();
        pedidoHoja++;
        hoja.setVisible(false);
        dialogo.setVisible(false);
        velo.setVisible(false);
        pantalla.setMouseTransparent(false);
        porEliminar = null;
        if (habiaCapa && focoAntes != null && focoAntes.getScene() != null) {
            focoAntes.requestFocus();
        }
        focoAntes = null;
    }

    // ---------------------------------------------------------------- piezas

    /**
     * La fórmula con la misma fuente de la pizarra (jlatexmath); si no cabe, se dibuja más chica. Si el LaTeX
     * no se puede dibujar, el texto.
     */
    static Node formula(String latex, String texto, double maximo, double minimo) {
        if (latex != null && !latex.isBlank()) {
            try {
                new pe.mathnova.presentation.latex.RenderizadorLatex((float) minimo).dibujar(latex);
                FormulaAjustable f = new FormulaAjustable(maximo, minimo);
                f.setPartible(false);
                f.setLatex(latex);
                f.setMinWidth(0);
                f.setMaxWidth(Double.MAX_VALUE);
                f.setMaxHeight(Region.USE_PREF_SIZE); // centrada en la fila, como el texto
                return f;
            } catch (RuntimeException e) {
                // sigue con el texto
            }
        }
        Label l = new Label(texto == null ? "" : texto);
        l.getStyleClass().add("historial-texto");
        return l;
    }

    private static Button botonIcono(String trazos, String ayuda) {
        SVGPath dibujo = new SVGPath();
        dibujo.setContent(trazos);
        dibujo.getStyleClass().add("historial-icono");
        Rectangle marco = new Rectangle(24, 24);
        marco.setFill(null);
        marco.setStroke(null);
        Button b = new Button(null, new Group(marco, dibujo));
        b.getStyleClass().add("boton-icono-historial");
        if (!ayuda.isEmpty()) {
            b.setTooltip(conRetraso(new Tooltip(ayuda)));
            b.setAccessibleText(ayuda);
        }
        return b;
    }

    private static Tooltip conRetraso(Tooltip t) {
        t.setShowDelay(Duration.millis(300));
        return t;
    }

    private static void mostrar(Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }

    // ---------------------------------------------------------------- pruebas

    void setEjecutor(Consumer<Task<HistorialService.Hoja>> ejecutor) {
        this.ejecutor = ejecutor;
    }

    /** Las filas que se ven (pruebas). */
    List<HistorialService.Fila> filasVisibles() {
        return vistas.stream().map(v -> v.fila).toList();
    }

    boolean hojaVisible() {
        return hoja.isVisible();
    }

    boolean dialogoVisible() {
        return dialogo.isVisible();
    }
}
