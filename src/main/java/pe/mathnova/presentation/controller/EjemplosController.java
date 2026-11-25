package pe.mathnova.presentation.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.application.service.EjemplosService;
import pe.mathnova.application.service.EjemplosService.Curso;
import pe.mathnova.application.service.EjemplosService.Ejemplo;
import pe.mathnova.application.service.EjemplosService.Tema;
import pe.mathnova.presentation.Navegador;

/**
 * Pantalla "Ejemplos" (Sprint 9, parte B): una pestaña por curso, subrayada en azul la activa; a la izquierda
 * los temas del curso con su cantidad de ejemplos, el activo con fondo azul claro; a la derecha una tarjeta por
 * ejemplo con el verbo, la fórmula, la nota y los botones Resolver y Practicar. Resolver lo abre en el editor y
 * lo resuelve; Practicar lo abre en Práctica ya iniciado (parte D).
 */
@Component
@Scope("prototype")
public class EjemplosController {

    private static final Logger LOG = LoggerFactory.getLogger(EjemplosController.class);

    private static final PseudoClass ACTIVO = PseudoClass.getPseudoClass("activo");
    static final String PRACTICA_PRONTO = "Por ahora se practican factorizaciones en Q y límites";

    /** Ancho mínimo de una tarjeta: con menos espacio se pasa a menos columnas. */
    static final double ANCHO_MINIMO_TARJETA = 300;
    /** Ancho del nombre de un tema en la columna de 260 px (sin márgenes ni la pastilla de la cantidad). */
    static final double ANCHO_NOMBRE_TEMA = 168;

    private final EjemplosService ejemplos;
    private final Navegador navegador;
    private final ToggleGroup grupoCursos = new ToggleGroup();
    private final List<Button> botonesTema = new ArrayList<>();
    private List<Curso> cursos = List.of();
    private Curso cursoActivo;
    private Tema temaActivo;
    private List<Ejemplo> visibles = List.of();

    @FXML
    private VBox raiz;
    @FXML
    private HBox pestanas;
    @FXML
    private VBox temas;
    @FXML
    private ScrollPane desplazamiento;
    @FXML
    private FlowPane tarjetas;
    @FXML
    private Label sinEjemplos;

    public EjemplosController(EjemplosService ejemplos, Navegador navegador) {
        this.ejemplos = ejemplos;
        this.navegador = navegador;
    }

    @FXML
    void initialize() {
        try {
            cursos = ejemplos.cursos();
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron leer los ejemplos", e);
            cursos = List.of();
        }
        for (Curso c : cursos) {
            ToggleButton pestana = new ToggleButton(c.nombre());
            pestana.getStyleClass().add("pestana");
            pestana.setToggleGroup(grupoCursos);
            pestana.setUserData(c);
            pestana.setAccessibleText("Curso " + c.nombre());
            pestanas.getChildren().add(pestana);
        }
        grupoCursos.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                // Una pestaña no se apaga con un segundo clic: siempre hay un curso a la vista.
                grupoCursos.selectToggle(antes);
                return;
            }
            elegirCurso((Curso) ahora.getUserData());
        });
        desplazamiento.viewportBoundsProperty().addListener((obs, antes, ahora) -> ajustarAnchos());
        if (!grupoCursos.getToggles().isEmpty()) {
            grupoCursos.selectToggle(grupoCursos.getToggles().getFirst());
        } else {
            mostrarTarjetas(List.of());
        }
    }

    // ---------------------------------------------------------------- curso y tema

    private void elegirCurso(Curso c) {
        cursoActivo = c;
        botonesTema.clear();
        temas.getChildren().clear();
        Label rotulo = new Label("Temas");
        rotulo.getStyleClass().add("ejemplos-temas-rotulo");
        temas.getChildren().add(rotulo);
        for (Tema t : c.temas()) {
            Button b = botonTema(t);
            botonesTema.add(b);
            temas.getChildren().add(b);
        }
        elegirTema(c.temas().stream().filter(t -> t.cantidad() > 0).findFirst()
                .orElse(c.temas().isEmpty() ? null : c.temas().getFirst()));
    }

    /** "Factorización en Q" a la izquierda y su cantidad en una pastilla a la derecha. */
    private Button botonTema(Tema t) {
        // Un Text con ancho fijo: un Label que parte líneas dentro del gráfico de un botón se mide con ancho -1
        // y queda una palabra por renglón.
        Text nombre = new Text(t.nombre());
        nombre.getStyleClass().add("ejemplos-tema-nombre");
        nombre.setWrappingWidth(ANCHO_NOMBRE_TEMA);
        Label cantidad = new Label(String.valueOf(t.cantidad()));
        cantidad.getStyleClass().add("ejemplos-tema-cantidad");
        cantidad.setMinWidth(Region.USE_PREF_SIZE);
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox fila = new HBox(8, nombre, hueco, cantidad);
        fila.setAlignment(Pos.CENTER_LEFT);
        Button b = new Button(null, fila);
        b.getStyleClass().add("ejemplos-tema");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMaxHeight(Region.USE_PREF_SIZE);
        b.setUserData(t);
        b.setAccessibleText(t.nombre() + ", " + conteo(t.cantidad()));
        b.setOnAction(e -> elegirTema(t));
        return b;
    }

    private void elegirTema(Tema t) {
        temaActivo = t;
        for (Button b : botonesTema) {
            b.pseudoClassStateChanged(ACTIVO, b.getUserData() == t);
        }
        List<Ejemplo> lista = List.of();
        if (t != null) {
            try {
                lista = ejemplos.ejemplos(t.id());
            } catch (RuntimeException e) {
                LOG.warn("No se pudieron leer los ejemplos de {}", t.nombre(), e);
            }
        }
        mostrarTarjetas(lista);
    }

    private void mostrarTarjetas(List<Ejemplo> lista) {
        visibles = lista;
        List<Node> nodos = new ArrayList<>();
        for (Ejemplo e : lista) {
            nodos.add(tarjeta(e));
        }
        tarjetas.getChildren().setAll(nodos);
        boolean vacio = lista.isEmpty();
        sinEjemplos.setVisible(vacio);
        sinEjemplos.setManaged(vacio);
        desplazamiento.setVisible(!vacio);
        desplazamiento.setVvalue(0);
        ajustarAnchos();
    }

    /** "1 ejemplo", "4 ejemplos". */
    static String conteo(int n) {
        return n == 1 ? "1 ejemplo" : n + " ejemplos";
    }

    // ---------------------------------------------------------------- tarjeta

    /** Verbo en mayúsculas pequeñas, la fórmula, la nota y los dos botones. */
    private Node tarjeta(Ejemplo e) {
        Label verbo = new Label(e.enunciado().toUpperCase(Locale.ROOT));
        verbo.getStyleClass().add("ejemplo-verbo");

        StackPane formula = new StackPane(HistorialController.formula(e.latex(), e.texto(), 22, 14));
        formula.setAlignment(Pos.CENTER_LEFT);
        formula.getStyleClass().add("ejemplo-formula");
        Tooltip.install(formula, conRetraso(new Tooltip(e.texto())));

        Label nota = new Label(e.nota());
        nota.getStyleClass().add("ejemplo-nota");
        nota.setWrapText(true);

        Button resolver = new Button("Resolver");
        resolver.getStyleClass().addAll("boton-primario", "ejemplo-boton");
        resolver.setAccessibleText("Resolver " + e.texto());
        resolver.setOnAction(ev -> resolver(e));

        Button practicar = new Button("Practicar");
        practicar.getStyleClass().addAll("boton-secundario", "ejemplo-boton");
        practicar.setAccessibleText("Practicar " + e.texto());
        practicar.setOnAction(ev -> practicar(e));
        Node envolturaPracticar = practicar;
        if (!navegador.sePuedePracticar(e.texto(), e.campo())) {
            // Un botón deshabilitado no recibe el mouse: el tooltip va en un contenedor.
            practicar.setDisable(true);
            practicar.setAccessibleHelp(PRACTICA_PRONTO);
            StackPane envoltura = new StackPane(practicar);
            Tooltip.install(envoltura, conRetraso(new Tooltip(PRACTICA_PRONTO)));
            envolturaPracticar = envoltura;
        }

        HBox botones = new HBox(10, resolver, envolturaPracticar);
        botones.setAlignment(Pos.CENTER_LEFT);
        Region hueco = new Region();
        VBox.setVgrow(hueco, Priority.ALWAYS);

        VBox tarjeta = new VBox(verbo, formula, nota, hueco, botones);
        tarjeta.getStyleClass().add("ejemplo-tarjeta");
        tarjeta.setUserData(e);
        return tarjeta;
    }

    /** Reparte el ancho: tantas columnas como quepan con el ancho mínimo, todas del mismo ancho. */
    private void ajustarAnchos() {
        double ancho = desplazamiento.getViewportBounds().getWidth() - tarjetas.getInsets().getLeft()
                - tarjetas.getInsets().getRight();
        if (ancho <= 0) {
            return;
        }
        double hueco = tarjetas.getHgap();
        int columnas = Math.max(1, (int) Math.floor((ancho + hueco) / (ANCHO_MINIMO_TARJETA + hueco)));
        // Un píxel de holgura por columna: con la pantalla escalada (125 %), 449 px se ajustan a 449,6 y la última
        // tarjeta ya no cabía en la fila.
        double cada = Math.floor((ancho - hueco * (columnas - 1)) / columnas) - 1;
        for (Node n : tarjetas.getChildren()) {
            if (n instanceof Region r) {
                r.setMinWidth(Math.min(cada, ANCHO_MINIMO_TARJETA));
                r.setPrefWidth(cada);
                r.setMaxWidth(cada);
            }
        }
    }

    // ---------------------------------------------------------------- acciones

    /** Resolver: el ejemplo en el editor, en su campo, resuelto en la pizarra. */
    void resolver(Ejemplo e) {
        navegador.abrirEnResolver(e.texto(), e.campo(), null);
    }

    /** Practicar: el ejemplo en la pantalla Práctica, ya iniciado (parte D). */
    void practicar(Ejemplo e) {
        navegador.abrirEnPractica(e.texto(), e.campo());
    }

    private static Tooltip conRetraso(Tooltip t) {
        t.setShowDelay(Duration.millis(300));
        return t;
    }

    // ---------------------------------------------------------------- pruebas

    Curso cursoActivo() {
        return cursoActivo;
    }

    Tema temaActivo() {
        return temaActivo;
    }

    List<Ejemplo> ejemplosVisibles() {
        return visibles;
    }

    /** Elige el curso por su nombre, como un clic en su pestaña (pruebas). */
    void elegirCurso(String nombre) {
        grupoCursos.getToggles().stream().filter(t -> ((Curso) t.getUserData()).nombre().equals(nombre))
                .findFirst().ifPresent(grupoCursos::selectToggle);
    }
}
