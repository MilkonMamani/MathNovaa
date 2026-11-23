package pe.mathnova.presentation.latex;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

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
        if (latex == null) {
            return null;
        }
        try {
            return SwingFXUtils.toFXImage(renderizador.dibujar(latex), null);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
