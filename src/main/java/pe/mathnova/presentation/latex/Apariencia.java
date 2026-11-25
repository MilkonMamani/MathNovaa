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

public final class Apariencia {

    public static final String CLASE_ESCALABLE = "escala-letra";

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

    public static DoubleProperty escala() {
        return ESCALA;
    }

    public static BooleanProperty oscuro() {
        return OSCURO;
    }

    public static ReadOnlyIntegerProperty version() {
        return VERSION.getReadOnlyProperty();
    }

    static Color tinta() {
        return OSCURO.get() ? TINTA_OSCURA : Color.BLACK;
    }

    public static double factor(Node n) {
        for (Parent p = n == null ? null : n.getParent(); p != null; p = p.getParent()) {
            if (p.getStyleClass().contains(CLASE_ESCALABLE)) {
                return ESCALA.get();
            }
        }
        return 1;
    }
}
