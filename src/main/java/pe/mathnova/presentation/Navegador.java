package pe.mathnova.presentation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pe.mathnova.presentation.controller.PrincipalController;
import pe.mathnova.presentation.controller.ResolucionController;

/**
 * Cambia la pantalla de la zona de contenido, junto a la barra lateral, dentro de la misma ventana.
 * Desde el Sprint S3b, "Resolver ejercicio" es una sola pantalla: el editor visual con su teclado,
 * la solución y el procedimiento (mockups A, F, G y H). Se carga una sola vez y conserva lo escrito.
 */
@Component
public class Navegador {

    private static final String ESTILOS = "/css/mathnova.css";

    private final ApplicationContext contexto;
    private PrincipalController principal;
    private Parent resolucion;
    private ResolucionController resolucionControlador;

    public Navegador(ApplicationContext contexto) {
        this.contexto = contexto;
    }

    /** Arma la ventana con la barra lateral y la pantalla "Resolver ejercicio". */
    public void iniciar(Stage stage) {
        FXMLLoader marco = cargar("/fxml/principal.fxml");
        principal = marco.getController();
        principal.mostrar(pantallaResolucion());

        Scene escena = new Scene(marco.getRoot(), 1280, 800);
        escena.getStylesheets().add(Objects.requireNonNull(getClass().getResource(ESTILOS)).toExternalForm());
        stage.setTitle("MathNova");
        stage.setMinWidth(860);
        stage.setMinHeight(600);
        stage.setScene(escena);
    }

    /** "Resolver" en la barra lateral: la pantalla con el editor, con lo último que se escribió. */
    public void mostrarEntrada() {
        principal.mostrar(pantallaResolucion());
        resolucionControlador.enfocar();
    }

    /** Abre la pantalla y resuelve el texto (sección 9) con la animación en cascada. */
    public void mostrarResolucion(String texto) {
        principal.mostrar(pantallaResolucion());
        resolucionControlador.resolver(texto);
    }

    private Parent pantallaResolucion() {
        if (resolucion == null) {
            FXMLLoader loader = cargar("/fxml/resolucion.fxml");
            resolucion = loader.getRoot();
            resolucionControlador = loader.getController();
        }
        return resolucion;
    }

    private FXMLLoader cargar(String fxml) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        loader.setControllerFactory(contexto::getBean);
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar " + fxml, e);
        }
        return loader;
    }
}
