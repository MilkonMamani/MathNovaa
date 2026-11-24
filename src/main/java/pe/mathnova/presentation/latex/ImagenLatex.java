package pe.mathnova.presentation.latex;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Window;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;

/** Pasa una expresión a una imagen de JavaFX con el LaTeX de ImpresoraLatex. */
public final class ImagenLatex {

    private ImagenLatex() {
    }

    /** null si jlatexmath no pudo dibujarla: se deja el espacio vacío en vez de romper la pantalla. */
    public static Image de(RenderizadorLatex renderizador, Expresion e) {
        if (e == null) {
            return null;
        }
        return de(renderizador, ImpresoraLatex.imprimir(e));
    }

    /** Lo mismo, con el LaTeX ya escrito (las soluciones de una ecuación, HU38). */
    public static Image de(RenderizadorLatex renderizador, String latex) {
        return de(renderizador, latex, 1);
    }

    /**
     * Con los píxeles de la pantalla: la imagen mide {@code escala} veces su tamaño lógico. Se muestra con
     * {@link #anchoLogico} y {@link #altoLogico}; null si jlatexmath no pudo dibujarla.
     */
    public static Image de(RenderizadorLatex renderizador, String latex, double escala) {
        if (latex == null) {
            return null;
        }
        try {
            return SwingFXUtils.toFXImage(renderizador.dibujar(latex, escala), null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * La escala de la pantalla donde está el nodo (1 = 100 %, 1,25 = 125 %…). Si todavía no está en una
     * ventana, la de la pantalla principal.
     */
    public static double escala(Node n) {
        Scene escena = n == null ? null : n.getScene();
        Window ventana = escena == null ? null : escena.getWindow();
        double s = ventana != null ? ventana.getRenderScaleX() : principal();
        return s >= 1 ? s : 1;
    }

    private static double principal() {
        try {
            return Screen.getPrimary().getOutputScaleX();
        } catch (RuntimeException e) {
            return 1;
        }
    }

    public static double anchoLogico(Image i, double escala) {
        return i == null ? 0 : i.getWidth() / escala;
    }

    public static double altoLogico(Image i, double escala) {
        return i == null ? 0 : i.getHeight() / escala;
    }
}
