package pe.mathnova.presentation.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.presentation.Navegador;

/**
 * Barra lateral y zona de contenido. La barra empieza compacta (solo íconos) y el botón de tres rayas
 * la despliega con el nombre junto a cada ícono; alternarla solo cambia su ancho y sus etiquetas, la
 * pantalla del centro no se toca. "Resolver" y "Configuración" funcionan; las demás opciones se ven
 * deshabilitadas.
 */
@Component
@Scope("prototype")
public class PrincipalController {

    private static final PseudoClass SELECCIONADO = PseudoClass.getPseudoClass("seleccionado");
    private static final PseudoClass DESPLEGADO = PseudoClass.getPseudoClass("desplegado");
    private static final String PROXIMO_SPRINT = "Disponible próximamente";

    /**
     * Lado de la cuadrícula en que se dibujan los íconos, a escala 1: el dibujo ocupa de 3 a 21, es
     * decir unos 18 × 18 px, con trazo de 1,5 px (mathnova.css).
     */
    private static final double CUADRICULA = 24;

    /** Ícono dibujado con trazos sobre la cuadrícula de 24 × 24. */
    private record Opcion(String nombre, String icono, String ayuda, boolean habilitada) {
    }

    static final String RESOLVER = "Resolver";
    static final String CONFIGURACION = "Configuración";

    /** Tres rayas del botón que despliega y compacta la barra. */
    static final String ICONO_MENU = "M5 7 H19 M5 12 H19 M5 17 H19";

    static final List<Opcion> OPCIONES = List.of(
            // un signo igual entre dos corchetes cortos
            new Opcion(RESOLVER, "M8 6 H5 V18 H8 M16 6 H19 V18 H16 M9.5 10 H14.5 M9.5 14 H14.5",
                    "Resolver un ejercicio paso a paso", true),
            // contorno de una hoja con la esquina doblada; su borde se curva en una flecha de regreso
            new Opcion("Historial", "M10 21 H6.5 A1.5 1.5 0 0 1 5 19.5 V4.5 A1.5 1.5 0 0 1 6.5 3 H13 L19 9 V14.5"
                    + " A3.5 3.5 0 0 1 15.5 18 H11 M13 3 V9 H19 M13.5 15.5 L11 18 L13.5 20.5",
                    PROXIMO_SPRINT, false),
            // lápiz inclinado: cuerpo, punta separada y casquillo
            new Opcion("Práctica", "M15.5 4.5 L19.5 8.5 L9 19 L4 20 L5 15 Z M5 15 L9 19 M13.5 6.5 L17.5 10.5",
                    PROXIMO_SPRINT, false),
            // dos fichas ligeramente desplazadas
            new Opcion("Ejemplos", "M6 8 H14 A2 2 0 0 1 16 10 V18 A2 2 0 0 1 14 20 H6 A2 2 0 0 1 4 18 V10"
                    + " A2 2 0 0 1 6 8 Z M8 8 V6 A2 2 0 0 1 10 4 H18 A2 2 0 0 1 20 6 V14 A2 2 0 0 1 18 16 H16",
                    PROXIMO_SPRINT, false),
            // dos líneas de ajuste, cada una con su control circular
            new Opcion(CONFIGURACION, "M4 8 H7 M11 8 H20 M11 8 A2 2 0 1 1 7 8 A2 2 0 1 1 11 8 Z"
                    + " M4 16 H13 M17 16 H20 M17 16 A2 2 0 1 1 13 16 A2 2 0 1 1 17 16 Z",
                    "Preferencias de la pizarra", true));

    private final Navegador navegador;
    private final Map<String, Button> botones = new LinkedHashMap<>();
    private final List<Runnable> alCambiarAncho = new ArrayList<>();
    private boolean desplegado;

    @FXML
    private VBox menu;
    @FXML
    private Button alternar;
    @FXML
    private Label marca;
    @FXML
    private StackPane contenido;

    public PrincipalController(Navegador navegador) {
        this.navegador = navegador;
    }

    @FXML
    void initialize() {
        alternar.setGraphic(icono(ICONO_MENU));
        for (Opcion o : OPCIONES) {
            menu.getChildren().add(crear(o));
        }
        aplicarAncho();
    }

    /** Las tres rayas: compacta o despliega la barra. No cambia la sección ni el ejercicio. */
    @FXML
    void alternarMenu() {
        desplegado = !desplegado;
        aplicarAncho();
    }

    public boolean estaDesplegado() {
        return desplegado;
    }

    private void aplicarAncho() {
        menu.pseudoClassStateChanged(DESPLEGADO, desplegado);
        marca.setVisible(desplegado);
        marca.setManaged(desplegado);
        String accion = desplegado ? "Contraer menú" : "Expandir menú";
        alternar.setAccessibleText(accion);
        alternar.setTooltip(conRetraso(new Tooltip(accion)));
        alCambiarAncho.forEach(Runnable::run);
    }

    private Node crear(Opcion o) {
        Button boton = new Button(o.nombre(), icono(o.icono()));
        boton.getStyleClass().add("nav-item");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setAccessibleText(o.nombre());
        boton.setAccessibleHelp(o.ayuda());
        Tooltip ayuda = conRetraso(new Tooltip());
        // Compacta: el tooltip dice el nombre (el texto no se ve). Desplegada: la ayuda.
        Runnable textoAyuda = () -> ayuda.setText(!desplegado ? (o.habilitada() ? o.nombre()
                : o.nombre() + " · " + o.ayuda()) : o.ayuda());
        alCambiarAncho.add(textoAyuda);
        textoAyuda.run();
        if (o.habilitada()) {
            boton.setTooltip(ayuda);
            nombreAlEnfocar(boton, ayuda);
            boton.setOnAction(e -> {
                if (o.nombre().equals(CONFIGURACION)) {
                    navegador.mostrarConfiguracion();
                } else {
                    navegador.mostrarEntrada();
                }
            });
            botones.put(o.nombre(), boton);
            boton.pseudoClassStateChanged(SELECCIONADO, o.nombre().equals(RESOLVER));
            return boton;
        }
        // Un control deshabilitado no recibe el mouse: el tooltip va en un contenedor.
        boton.setDisable(true);
        StackPane envoltura = new StackPane(boton);
        envoltura.getStyleClass().add("nav-envoltura");
        Tooltip.install(envoltura, ayuda);
        return envoltura;
    }

    /** Con la barra compacta, al llegar con Tab a una opción se muestra su nombre a la derecha. */
    private void nombreAlEnfocar(Button boton, Tooltip ayuda) {
        boton.focusVisibleProperty().addListener((obs, antes, ahora) -> {
            if (ahora && !desplegado && boton.getScene() != null && boton.getScene().getWindow() != null) {
                Bounds b = boton.localToScreen(boton.getBoundsInLocal());
                ayuda.show(boton, b.getMaxX() + 6, b.getMinY() + 8);
            } else if (!ahora) {
                ayuda.hide();
            }
        });
        alCambiarAncho.add(ayuda::hide);
    }

    private static Tooltip conRetraso(Tooltip t) {
        t.setShowDelay(Duration.millis(300));
        return t;
    }

    /**
     * Ícono de la cuadrícula 24 × 24 (dibujo de unos 18 × 18). El marco invisible ocupa la cuadrícula
     * entera, así todos los íconos se centran por la cuadrícula y no por lo que dibujan.
     */
    static Node icono(String trazos) {
        Rectangle marco = new Rectangle(CUADRICULA, CUADRICULA);
        marco.setFill(null);
        marco.setStroke(null);
        SVGPath dibujo = new SVGPath();
        dibujo.setContent(trazos);
        dibujo.getStyleClass().add("icono");
        StackPane caja = new StackPane(new Group(marco, dibujo));
        caja.getStyleClass().add("nav-icono");
        caja.setMinSize(CUADRICULA, CUADRICULA);
        caja.setPrefSize(CUADRICULA, CUADRICULA);
        caja.setMaxSize(CUADRICULA, CUADRICULA);
        return caja;
    }

    /** Marca la opción de la pantalla que está a la vista. */
    public void seleccionar(String nombre) {
        botones.forEach((n, b) -> b.pseudoClassStateChanged(SELECCIONADO, n.equals(nombre)));
    }

    /** Pone una pantalla en la zona de contenido. */
    public void mostrar(Node pantalla) {
        contenido.getChildren().setAll(pantalla);
    }

    public boolean muestra(Node pantalla) {
        return contenido.getChildren().contains(pantalla);
    }
}
