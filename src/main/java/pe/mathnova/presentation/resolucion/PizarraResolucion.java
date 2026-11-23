package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.animacion.AnimadorResolucion;
import pe.mathnova.presentation.latex.ImagenLatex;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.pizarra.Pizarra;

/**
 * La Pizarra de "Resolver ejercicio": el procedimiento escrito como en un cuaderno.
 *
 * <p>Cabecera: "Pizarra", ‹ "Todos los pasos" / "Paso N de M" ›, "Paso a paso" / "Ver todo", el
 * interruptor "Explicaciones" y exportar PDF. Cuerpo, sobre una cuadrícula de 24 px: el tipo de
 * ejercicio ("FACTORIZA"), la expresión original y una línea gruesa; cada paso con una flecha azul y el
 * nombre del método encima, y una fila de tres columnas fijas (número, fórmula y nota); al final el
 * resultado en un recuadro doble y la Comprobación.
 *
 * <p>Las notas "por qué aplica" empiezan ocultas: en su columna, de ancho fijo, solo hay un botón "?".
 * Abrir o cerrar una nota no mueve nada más. Qué se ve lo decide {@link EstadoPizarra}.
 */
public final class PizarraResolucion extends VBox {

    /** Ancho fijo de la columna de notas, esté vacía o llena. */
    static final double ANCHO_NOTA = 220;
    static final String VER_EXPLICACION = "Ver explicación";

    private static final PseudoClass PENDIENTE = PseudoClass.getPseudoClass("pendiente");
    private static final PseudoClass ENCENDIDO = PseudoClass.getPseudoClass("encendido");
    /** Ícono de foco: el bulbo y la base. */
    private static final String ICONO_FOCO =
            "M5 7 A5 5 0 1 1 11 11.5 V13 H5 V11.5 A5 5 0 0 1 5 7 Z M6 15.5 H10";
    private static final String ICONO_AYUDA =
            "M10 1 A9 9 0 1 1 9.99 1 Z M7.5 7.5 A2.5 2.5 0 1 1 10 10 V11.5 M10 14.4 V14.6";
    private static final String ICONO_PDF = "M4 1 H11 L16 6 V19 H4 Z M11 1 V6 H16 M7 12 L10 15 L13 12 M10 8 V15";

    /**
     * Lo que se dibuja de un resultado.
     *
     * @param rotulo       "FACTORIZA", "DERIVA"…; null si no se sabe
     * @param latexEntrada la expresión original; null si no se pudo dibujar
     * @param pasos        los pasos, numerados desde 1
     * @param latexResultado lo que va en el recuadro doble; null si no hay
     * @param comprobacion el texto que sigue a "Comprobación."; null si no hay
     */
    public record Contenido(String rotulo, String latexEntrada, List<VistaPaso> pasos, String latexResultado,
                            String comprobacion) {
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
    private final Label rotulo = new Label();
    private final StackPane enunciado = new StackPane();
    private final Region linea = new Region();
    private final VBox filas = new VBox();
    private final StackPane recuadroResultado = new StackPane();
    private final FormulaAjustable formulaResultado = new FormulaAjustable(24, 16);
    private final Text textoComprobacion = new Text();
    private final TextFlow comprobacion;

    private final List<Fila> pasos = new ArrayList<>();

    public PizarraResolucion(RenderizadorLatex renderizador, AnimadorResolucion animador, boolean explicaciones) {
        this.renderizador = renderizador;
        this.animador = animador;
        this.estado = new EstadoPizarra(explicaciones);
        getStyleClass().add("pizarra-resolucion");

        // ---- cabecera
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

        SVGPath iconoPdf = icono(ICONO_PDF, "pizarra-icono");
        Button exportar = new Button(null, iconoPdf);
        exportar.getStyleClass().addAll("pizarra-control", "boton-icono");
        exportar.setAccessibleText("Exportar PDF");
        exportar.setDisable(true);
        // Un botón desactivado no muestra su tooltip: va en un contenedor.
        StackPane envolturaPdf = new StackPane(exportar);
        Tooltip.install(envolturaPdf, new Tooltip("Exportar PDF: disponible próximamente"));

        HBox cabecera = new HBox(8, titulo, hueco, anterior, etiquetaPasos, siguiente, botonModo, interruptor,
                envolturaPdf);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("pizarra-cabecera");

        // ---- cuerpo
        rotulo.getStyleClass().add("pizarra-rotulo");
        enunciado.getStyleClass().add("pizarra-enunciado");
        enunciado.setAlignment(Pos.CENTER_LEFT);
        linea.getStyleClass().add("pizarra-linea");
        filas.getStyleClass().add("pizarra-filas");

        recuadroResultado.getStyleClass().add("pizarra-resultado-doble");
        recuadroResultado.getChildren().add(formulaResultado);
        recuadroResultado.setMaxWidth(Region.USE_PREF_SIZE);
        recuadroResultado.setAccessibleText("Resultado");
        Text negrita = new Text("Comprobación. ");
        negrita.getStyleClass().addAll("pizarra-comprobacion", "pizarra-comprobacion-titulo");
        textoComprobacion.getStyleClass().add("pizarra-comprobacion");
        comprobacion = new TextFlow(negrita, textoComprobacion);
        comprobacion.getStyleClass().add("pizarra-comprobacion-linea");

        cuerpo.getStyleClass().add("pizarra-cuerpo");
        cuerpo.getChildren().addAll(rotulo, enunciado, linea, filas, recuadroResultado, comprobacion);

        getChildren().addAll(cabecera, cuerpo);
        limpiar();
    }

    /** El interruptor "Explicaciones": ícono de foco, el texto y un switch; Espacio o Enter lo cambian. */
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
        // Espacio ya lo maneja el botón; Enter también lo cambia.
        interruptor.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                interruptor.fire();
                e.consume();
            }
        });
        interruptor.selectedProperty().addListener((obs, antes, si) -> {
            if (si != estado.explicaciones()) {
                estado.setExplicaciones(si);
                pintar();
                onExplicaciones.accept(si);
            }
        });
    }

    /** Se llama cuando el usuario cambia el interruptor (para guardar la preferencia). */
    public void setOnExplicaciones(Consumer<Boolean> accion) {
        onExplicaciones = accion == null ? si -> { } : accion;
    }

    /** Pone el interruptor sin avisar (la preferencia cambió en Configuración). */
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

    // ------------------------------------------------------------------ contenido

    /** Dibuja un resultado nuevo, con todos los pasos a la vista y las notas como diga el interruptor. */
    public void mostrar(Contenido c) {
        pasos.clear();
        filas.getChildren().clear();
        for (VistaPaso p : c.pasos()) {
            Fila f = new Fila(p);
            pasos.add(f);
            filas.getChildren().add(f.bloque);
        }
        estado.nuevo(pasos.size());

        rotulo.setText(c.rotulo() == null ? "" : c.rotulo());
        mostrarNodo(rotulo, c.rotulo() != null);
        enunciado.getChildren().clear();
        if (c.latexEntrada() != null) {
            ImageView img = new ImageView(ImagenLatex.de(new RenderizadorLatex(22f), c.latexEntrada()));
            img.setPreserveRatio(true);
            enunciado.getChildren().add(img);
        }
        mostrarNodo(enunciado, c.latexEntrada() != null);
        mostrarNodo(linea, true);
        mostrarNodo(filas, !pasos.isEmpty());

        formulaResultado.setLatex(c.latexResultado());
        recuadroResultado.setUserData(c.latexResultado());
        textoComprobacion.setText(c.comprobacion() == null ? "" : c.comprobacion());
        comprobacion.setUserData(c.comprobacion());
        mostrarNodo(cuerpo, true);
        pintar();
    }

    /** Sin resultado: solo la cabecera. */
    public void limpiar() {
        pasos.clear();
        filas.getChildren().clear();
        estado.nuevo(0);
        formulaResultado.setLatex(null);
        recuadroResultado.setUserData(null);
        comprobacion.setUserData(null);
        mostrarNodo(cuerpo, false);
        pintar();
    }

    /** Las filas de los pasos (flecha y fila), para la animación en cascada. */
    public List<Node> filasPasos() {
        return pasos.stream().map(f -> (Node) f.bloque).toList();
    }

    /** El círculo numerado de cada paso, para la animación. */
    public List<Node> numeros() {
        return pasos.stream().map(f -> (Node) f.numero).toList();
    }

    /** El recuadro del resultado y la Comprobación, que entran después de los pasos. */
    public List<Node> cierre() {
        return List.of(recuadroResultado, comprobacion);
    }

    // ------------------------------------------------------------------ dibujo

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
        boolean cierre = estado.resultadoVisible() && cuerpo.isVisible();
        mostrarNodo(recuadroResultado, cierre && recuadroResultado.getUserData() != null);
        mostrarNodo(comprobacion, cierre && comprobacion.getUserData() != null);
    }

    /** Un paso: la flecha con el método y la fila número | fórmula | nota. */
    private final class Fila {

        private final int n;
        private final VBox bloque = new VBox();
        private final HBox flecha;
        private final StackPane numero;
        private final VBox formula = new VBox(6);
        private final StackPane columnaNota = new StackPane();
        private final Button botonNota;
        private final VBox nota;
        private final Hyperlink ocultar = new Hyperlink("Ocultar");

        Fila(VistaPaso p) {
            n = p.orden();

            SVGPath punta = new SVGPath();
            punta.setContent("M6 0 V22 M1 17 L6 22 L11 17");
            punta.getStyleClass().add("pizarra-flecha");
            Label metodo = new Label(p.titulo());
            metodo.getStyleClass().add("pizarra-metodo");
            metodo.setWrapText(true);
            if (p.ayuda() != null && !p.ayuda().equals(p.titulo())) {
                metodo.setTooltip(new Tooltip(p.ayuda()));
            }
            flecha = new HBox(10, punta, metodo);
            flecha.setAlignment(Pos.CENTER_LEFT);
            flecha.getStyleClass().add("pizarra-paso-flecha");

            numero = new StackPane(new Label(String.valueOf(n)));
            numero.getStyleClass().add("pizarra-numero");

            formula.getStyleClass().add("pizarra-formula");
            formula.setMinWidth(0);
            HBox.setHgrow(formula, Priority.ALWAYS);
            if (p.trabajo() != null) {
                formula.getChildren().add(Pizarra.de(p.trabajo()));
            }
            if (p.despues() != null) {
                FormulaAjustable f = new FormulaAjustable(FilaPaso.TAMANO, FilaPaso.TAMANO_MINIMO);
                f.getStyleClass().add("pizarra-despues");
                f.setLatex(ImpresoraLatex.imprimir(p.despues()));
                formula.getChildren().add(f);
            }
            if (!p.hijos().isEmpty()) {
                VBox internos = new VBox(6);
                for (VistaPaso h : p.hijos()) {
                    internos.getChildren().add(new FilaPaso(h, renderizador, animador).nodo());
                }
                String grupo = p.grupo() == null ? "Pasos internos" : p.grupo();
                TitledPane anidados = new TitledPane(grupo + " (" + p.hijos().size()
                        + (p.hijos().size() == 1 ? " paso)" : " pasos)"), internos);
                anidados.setExpanded(false);
                anidados.setAnimated(animador.activas());
                anidados.getStyleClass().addAll("seccion-plegable", "pasos-anidados");
                formula.getChildren().add(anidados);
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
            nota = new VBox(6, texto, ocultar);
            nota.getStyleClass().add("pizarra-nota");
            nota.setMaxHeight(Region.USE_PREF_SIZE);

            columnaNota.getStyleClass().add("pizarra-columna-nota");
            columnaNota.setAlignment(Pos.TOP_LEFT);
            columnaNota.setMinWidth(ANCHO_NOTA);
            columnaNota.setPrefWidth(ANCHO_NOTA);
            columnaNota.setMaxWidth(ANCHO_NOTA);
            columnaNota.getChildren().addAll(botonNota, nota);

            HBox fila = new HBox(12, numero, formula, columnaNota);
            fila.setAlignment(Pos.TOP_LEFT);
            fila.getStyleClass().add("pizarra-fila");
            bloque.getChildren().addAll(flecha, fila);
            bloque.getStyleClass().add("pizarra-paso");
        }

        void pintar() {
            boolean visible = estado.pasoVisible(n);
            numero.pseudoClassStateChanged(PENDIENTE, !visible);
            mostrarNodo(flecha, visible);
            mostrarNodo(formula, visible);
            mostrarNodo(columnaNota, visible);
            mostrarNodo(botonNota, estado.botonNotaVisible(n));
            mostrarNodo(nota, estado.notaVisible(n));
            mostrarNodo(ocultar, estado.ocultarVisible(n));
        }
    }

    // ------------------------------------------------------------------ apoyo

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

    // ------------------------------------------------------------------ para las pruebas

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
        return recuadroResultado;
    }

    Node comprobacion() {
        return comprobacion;
    }
}
