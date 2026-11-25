package pe.mathnova.presentation.latex;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;

/**
 * Una fórmula fija (sin ajuste al ancho) dibujada con los píxeles de la pantalla: nítida a 125 %, 150 % o
 * 200 %. Si la ventana pasa a otra pantalla con otra escala, se vuelve a dibujar.
 */
public final class VistaLatex extends ImageView {

    private final RenderizadorLatex renderizador;
    private final String latex;
    private double escala = -1;
    private final ChangeListener<Number> alCambiarEscala = (obs, antes, ahora) -> dibujar();

    public VistaLatex(RenderizadorLatex renderizador, String latex) {
        this.renderizador = renderizador;
        this.latex = latex;
        setPreserveRatio(true);
        setSmooth(true);
        getStyleClass().add("vista-latex");
        sceneProperty().addListener((obs, antes, ahora) -> seguir(antes, ahora));
        dibujar();
    }

    public String getLatex() {
        return latex;
    }

    private void seguir(Scene antes, Scene ahora) {
        if (antes != null) {
            antes.windowProperty().removeListener(cambioVentana);
            if (antes.getWindow() != null) {
                antes.getWindow().renderScaleXProperty().removeListener(alCambiarEscala);
            }
        }
        if (ahora != null) {
            ahora.windowProperty().addListener(cambioVentana);
            if (ahora.getWindow() != null) {
                ahora.getWindow().renderScaleXProperty().addListener(alCambiarEscala);
            }
        }
        dibujar();
    }

    private final ChangeListener<Window> cambioVentana = (obs, antes, ahora) -> {
        if (antes != null) {
            antes.renderScaleXProperty().removeListener(alCambiarEscala);
        }
        if (ahora != null) {
            ahora.renderScaleXProperty().addListener(alCambiarEscala);
        }
        dibujar();
    };

    private void dibujar() {
        double s = ImagenLatex.escala(this);
        double f = Apariencia.factor(this);
        int v = Apariencia.version().get();
        if (s == escala && f == factor && v == version && getImage() != null) {
            return;
        }
        escala = s;
        factor = f;
        version = v;
        // Con letra grande se dibuja a s·f píxeles por píxel y se muestra como si fueran s: queda f veces más grande
        Image img = ImagenLatex.de(renderizador, latex, s * f);
        setImage(img);
        setFitWidth(ImagenLatex.anchoLogico(img, s));
        setFitHeight(ImagenLatex.altoLogico(img, s));
    }

    private double factor = 1;
    private int version = -1;
    /** Sprint 9, parte E: el tamaño de letra o el tema cambiaron (referencia fuerte para el listener débil). */
    private final javafx.beans.InvalidationListener cambioApariencia = obs -> dibujar();
    {
        Apariencia.version().addListener(new javafx.beans.WeakInvalidationListener(cambioApariencia));
    }
}
