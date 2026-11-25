package pe.mathnova.presentation.latex;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Window;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;

public final class ImagenLatex {

    private ImagenLatex() {
    }

    public static Image de(RenderizadorLatex renderizador, Expresion e) {
        if (e == null) {
            return null;
        }
        return de(renderizador, ImpresoraLatex.imprimir(e));
    }

    public static Image de(RenderizadorLatex renderizador, String latex) {
        return de(renderizador, latex, 1);
    }

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
