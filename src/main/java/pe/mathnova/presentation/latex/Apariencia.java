package pe.mathnova.presentation.latex;

import java.awt.Color;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Cómo se dibujan las fórmulas en toda la app (Sprint 9, parte E): el tamaño de letra de Configuración (solo
 * dentro de la Pizarra, la hoja del Historial y Práctica, que llevan la clase {@link #CLASE_ESCALABLE}) y el
 * color de la tinta según el tema. Las fórmulas escuchan {@link #version()} y se vuelven a dibujar al cambiar
 * algo, sin reiniciar. Lo actualiza el Navegador desde ConfiguracionService.
 */
public final class Apariencia {

    /** Lleva esta clase el contenedor cuyas fórmulas y textos crecen con el tamaño de letra. */
    public static final String CLASE_ESCALABLE = "escala-letra";

    /** La tinta de las fórmulas en el tema oscuro: el mismo gris claro del texto. */
    static final Color TINTA_OSCURA = new Color(0xe2, 0xe8, 0xf0);

    private static final DoubleProperty ESCALA = new SimpleDoubleProperty(1);
    private static final BooleanProperty OSCURO = new SimpleBooleanProperty(false);
    private static final ReadOnlyIntegerWrapper VERSION = new ReadOnlyIntegerWrapper(0);

    static {
        ESCALA.addListener((obs, antes, ahora) -> VERSION.set(VERSION.get() + 1));
        OSCURO.addListener((obs, antes, ahora) -> VERSION.set(VERSION.get() + 1));
    }

    private Apariencia() {
    }

    /** 1; 1,2 o 1,4 con letra grande o muy grande. */
    public static DoubleProperty escala() {
        return ESCALA;
    }

    public static BooleanProperty oscuro() {
        return OSCURO;
    }

    /** Cambia cada vez que cambia algo: quien dibuja fórmulas lo escucha. */
    public static ReadOnlyIntegerProperty version() {
        return VERSION.getReadOnlyProperty();
    }

    static Color tinta() {
        return OSCURO.get() ? TINTA_OSCURA : Color.BLACK;
    }

    /** El factor de la letra para un nodo: la escala si está dentro de un contenedor escalable; si no, 1. */
    public static double factor(Node n) {
        for (Parent p = n == null ? null : n.getParent(); p != null; p = p.getParent()) {
            if (p.getStyleClass().contains(CLASE_ESCALABLE)) {
                return ESCALA.get();
            }
        }
        return 1;
    }
}
