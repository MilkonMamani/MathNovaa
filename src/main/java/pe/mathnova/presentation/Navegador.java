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

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.controller.ConfiguracionController;
import pe.mathnova.presentation.controller.HistorialController;
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
    private Parent historial;
    private HistorialController historialControlador;
    private Parent ejemplos;
    private Parent practica;
    private pe.mathnova.presentation.controller.PracticaController practicaControlador;

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
        aplicarApariencia(escena.getRoot());
        autoprueba();
    }

    /**
     * Sprint 9, parte E: el tema y el tamaño de letra de Configuración, en toda la app y sin reiniciar. El tema es
     * una clase en la raíz (los colores del CSS); el tamaño, una clase para los textos y la escala de las fórmulas.
     */
    public static void aplicarApariencia(javafx.scene.Parent raiz, pe.mathnova.application.service.ConfiguracionService c) {
        Runnable tema = () -> {
            boolean oscuro = c.temaProperty().get() == pe.mathnova.application.service.ConfiguracionService.Tema.OSCURO;
            raiz.getStyleClass().remove("tema-oscuro");
            if (oscuro) {
                raiz.getStyleClass().add("tema-oscuro");
            }
            pe.mathnova.presentation.latex.Apariencia.oscuro().set(oscuro);
        };
        Runnable letra = () -> {
            var t = c.letraProperty().get();
            raiz.getStyleClass().removeAll("letra-grande", "letra-muy-grande");
            switch (t) {
                case GRANDE -> raiz.getStyleClass().add("letra-grande");
                case MUY_GRANDE -> raiz.getStyleClass().add("letra-muy-grande");
                default -> { }
            }
            pe.mathnova.presentation.latex.Apariencia.escala().set(t.factor());
        };
        tema.run();
        letra.run();
        c.temaProperty().addListener((obs, antes, ahora) -> tema.run());
        c.letraProperty().addListener((obs, antes, ahora) -> letra.run());
    }

    private void aplicarApariencia(javafx.scene.Parent raiz) {
        aplicarApariencia(raiz, contexto.getBean(pe.mathnova.application.service.ConfiguracionService.class));
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

    /** "Historial" en la barra lateral (HU15): se recarga cada vez, con lo último que se resolvió. */
    public void mostrarHistorial() {
        if (historial == null) {
            FXMLLoader loader = cargar("/fxml/historial.fxml");
            historial = loader.getRoot();
            historialControlador = loader.getController();
        } else {
            historialControlador.recargar();
        }
        principal.mostrar(historial);
        principal.seleccionar("Historial");
    }

    /**
     * "Ejemplos" en la barra lateral (Sprint 9, parte B). Los ejemplos son un catálogo fijo: la pantalla se arma
     * una vez y conserva el curso y el tema elegidos.
     */
    public void mostrarEjemplos() {
        if (ejemplos == null) {
            ejemplos = cargar("/fxml/ejemplos.fxml").getRoot();
        }
        principal.mostrar(ejemplos);
        principal.seleccionar("Ejemplos");
    }

    /**
     * "Práctica" en la barra lateral (Sprint 9, parte D). Se arma una vez: si había un ejercicio a medias, sigue
     * ahí.
     */
    public void mostrarPractica() {
        pantallaPractica();
        principal.mostrar(practica);
        principal.seleccionar("Práctica");
    }

    /** Si "Practicar" puede abrir este ejercicio: una factorización en Q o un límite. */
    public boolean sePuedePracticar(String texto, Campo campo) {
        if (contexto == null) {
            return false; // pruebas sin Spring
        }
        return contexto.getBean(pe.mathnova.application.service.PracticaService.class).sePuedePracticar(texto, campo);
    }

    /**
     * "Practicar" de una tarjeta de Ejemplos o de la hoja del Historial: la pantalla Práctica con ese ejercicio ya
     * iniciado.
     *
     * @param campo Q, R o C para una factorización; null si no aplica
     */
    public void abrirEnPractica(String texto, Campo campo) {
        pantallaPractica();
        principal.mostrar(practica);
        principal.seleccionar("Práctica");
        practicaControlador.empezar(texto, campo);
    }

    private void pantallaPractica() {
        if (practica == null) {
            FXMLLoader loader = cargar("/fxml/practica.fxml");
            practica = loader.getRoot();
            practicaControlador = loader.getController();
        }
    }

    /**
     * "Abrir en Resolver" de la hoja del Historial y "Resolver" de una tarjeta de Ejemplos: el ejercicio en el
     * editor, en su campo y con su método, y la pizarra resuelta.
     */
    public void abrirEnResolver(String texto, Campo campo, String forzado) {
        principal.mostrar(pantallaResolucion());
        principal.seleccionar("Resolver");
        resolucionControlador.actualizarPreferencias();
        resolucionControlador.resolver(texto, campo, forzado);
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
