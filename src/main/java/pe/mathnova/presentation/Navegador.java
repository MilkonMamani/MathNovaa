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

import pe.mathnova.presentation.controller.ConfiguracionController;
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
    private Parent configuracion;
    private ConfiguracionController configuracionControlador;

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
        autoprueba();
    }

    /** {@code --mathnova.autoprueba=<muestra.json | carpeta>}: comprueba la instalación (ver AutoPrueba). */
    private void autoprueba() {
        var env = contexto.getEnvironment();
        String rutas = env.getProperty("mathnova.autoprueba");
        if (rutas == null || rutas.isBlank()) {
            return;
        }
        java.nio.file.Path salida = java.nio.file.Path.of(env.getProperty("mathnova.autoprueba.salida",
                java.nio.file.Path.of(System.getProperty("user.home"), ".mathnova", "autoprueba.txt").toString()));
        boolean salir = Boolean.parseBoolean(env.getProperty("mathnova.autoprueba.salir", "false"));
        Object r = contexto.getBean(pe.mathnova.domain.escritura.ReconocedorEscritura.class);
        String pid = r instanceof pe.mathnova.infrastructure.escritura.ReconocedorUniMERNet u
                ? "UniMERNet en " + u.carpeta() : r.getClass().getSimpleName();
        javafx.application.Platform.runLater(() -> new AutoPrueba(resolucionControlador, rutas, salida, salir, pid).iniciar());
    }

    /** "Resolver" en la barra lateral: la pantalla con el editor, con lo último que se escribió. */
    public void mostrarEntrada() {
        principal.mostrar(pantallaResolucion());
        principal.seleccionar("Resolver");
        resolucionControlador.actualizarPreferencias();
        resolucionControlador.enfocar();
    }

    /** "Configuración" en la barra lateral o el engranaje del teclado. */
    public void mostrarConfiguracion() {
        if (configuracion == null) {
            FXMLLoader loader = cargar("/fxml/configuracion.fxml");
            configuracion = loader.getRoot();
            configuracionControlador = loader.getController();
        } else {
            configuracionControlador.cargar();
        }
        principal.mostrar(configuracion);
        principal.seleccionar("Configuración");
    }

    /** Abre la pantalla y resuelve el texto (sección 9) con la animación en cascada. */
    public void mostrarResolucion(String texto) {
        principal.mostrar(pantallaResolucion());
        principal.seleccionar("Resolver");
        resolucionControlador.resolver(texto);
    }

    private Parent pantallaResolucion() {
        if (resolucion == null) {
            FXMLLoader loader = cargar("/fxml/resolucion.fxml");
            resolucion = loader.getRoot();
            resolucionControlador = loader.getController();
            resolucionControlador.setOnConfiguracion(this::mostrarConfiguracion);
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
