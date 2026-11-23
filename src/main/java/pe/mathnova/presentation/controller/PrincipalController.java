package pe.mathnova.presentation.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.presentation.Navegador;

/**
 * Barra lateral y zona de contenido. "Resolver" y "Configuración" funcionan; las demás opciones se
 * ven deshabilitadas.
 */
@Component
@Scope("prototype")
public class PrincipalController {

    private static final PseudoClass SELECCIONADO = PseudoClass.getPseudoClass("seleccionado");
    private static final String PROXIMO_SPRINT = "Disponible próximamente";

    /** Ícono de 20 × 20 dibujado con trazos. */
    private record Opcion(String nombre, String icono, String ayuda, boolean habilitada) {
    }

    static final String RESOLVER = "Resolver";
    static final String CONFIGURACION = "Configuración";

    private static final List<Opcion> OPCIONES = List.of(
            new Opcion(RESOLVER, "M2 11 H5 L8 18 L13 2 H18", "Resolver un ejercicio paso a paso", true),
            new Opcion("Historial", "M10 1 A9 9 0 1 1 9.99 1 Z M10 5 V10 L13.5 12.5", PROXIMO_SPRINT, false),
            new Opcion("Práctica", "M3 17 L4 13 L14 3 L17 6 L7 16 Z M12 5 L15 8", PROXIMO_SPRINT, false),
            new Opcion("Ejemplos", "M1 4 Q5.5 2 10 4 Q14.5 2 19 4 V17 Q14.5 15 10 17 Q5.5 15 1 17 Z M10 4 V17",
                    PROXIMO_SPRINT, false),
            new Opcion(CONFIGURACION, "M2 5 H18 M2 15 H18 M7 2 V8 M13 12 V18", "Preferencias de la pizarra", true));

    private final Navegador navegador;
    private final Map<String, Button> botones = new LinkedHashMap<>();

    @FXML
    private VBox menu;
    @FXML
    private StackPane contenido;

    public PrincipalController(Navegador navegador) {
        this.navegador = navegador;
    }

    @FXML
    void initialize() {
        for (Opcion o : OPCIONES) {
            menu.getChildren().add(crear(o));
        }
    }

    private Node crear(Opcion o) {
        SVGPath icono = new SVGPath();
        icono.setContent(o.icono());
        icono.getStyleClass().add("icono");

        Button boton = new Button(o.nombre(), icono);
        boton.getStyleClass().add("nav-item");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setAccessibleHelp(o.ayuda());
        Tooltip ayuda = new Tooltip(o.ayuda());
        ayuda.setShowDelay(Duration.millis(300));
        if (o.habilitada()) {
            boton.setTooltip(ayuda);
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
        Tooltip.install(envoltura, ayuda);
        return envoltura;
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
