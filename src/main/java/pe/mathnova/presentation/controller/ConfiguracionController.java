package pe.mathnova.presentation.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.ConfiguracionService.ModoPizarra;
import pe.mathnova.application.service.ConfiguracionService.TamanoLetra;
import pe.mathnova.application.service.ConfiguracionService.Tema;
import pe.mathnova.application.service.HistorialService;
import pe.mathnova.application.service.InformacionService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.resolucion.FormulaAjustable;

/**
 * Pantalla "Configuración" (Sprint 9, parte E): Resolución, Apariencia, Datos y Acerca de. Cada ajuste se guarda
 * al cambiarlo, con el aviso "Guardado", y ConfiguracionService lo aplica en toda la app sin reiniciar.
 */
@Component
@Scope("prototype")
public class ConfiguracionController {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionController.class);
    private static final PseudoClass ENCENDIDO = PseudoClass.getPseudoClass("encendido");
    static final String GUARDADO = "Guardado";
    static final String VISTA_PREVIA = "x^{2} - 5x + 6 = \\left(x - 2\\right)\\left(x - 3\\right)";

    private final ConfiguracionService configuracion;
    private final HistorialService historial;
    private final InformacionService informacion;

    @FXML
    private ScrollPane raiz;
    @FXML
    private ToggleButton campoQ;
    @FXML
    private ToggleButton campoR;
    @FXML
    private ToggleButton campoC;
    @FXML
    private ToggleButton modoTodos;
    @FXML
    private ToggleButton modoPaso;
    @FXML
    private ToggleButton mostrarExplicaciones;
    @FXML
    private ToggleButton mostrarCodigos;
    @FXML
    private ToggleButton letraNormal;
    @FXML
    private ToggleButton letraGrande;
    @FXML
    private ToggleButton letraMuyGrande;
    @FXML
    private StackPane vistaPrevia;
    @FXML
    private ToggleButton temaClaro;
    @FXML
    private ToggleButton temaOscuro;
    @FXML
    private Label datos;
    @FXML
    private Button borrarHistorial;
    @FXML
    private GridPane versiones;

    private final ToggleGroup grupoCampo = new ToggleGroup();
    private final ToggleGroup grupoModo = new ToggleGroup();
    private final ToggleGroup grupoLetra = new ToggleGroup();
    private final ToggleGroup grupoTema = new ToggleGroup();
    /** Se está poniendo lo que dice la base: no hay que guardarlo otra vez. */
    private boolean cargando;
    private String ultimoAviso;
    /** Para las pruebas: la confirmación de "Borrar todo el historial". */
    private Predicate<Window> confirmar = ConfiguracionController::confirmarBorrado;
    private Consumer<String> avisar = texto -> AvisoBreve.mostrar(raiz, texto, Duration.seconds(1.6));

    public ConfiguracionController(ConfiguracionService configuracion, HistorialService historial,
                                   InformacionService informacion) {
        this.configuracion = configuracion;
        this.historial = historial;
        this.informacion = informacion;
    }

    @FXML
    void initialize() {
        Map<ToggleButton, Campo> campos = Map.of(campoQ, Campo.Q, campoR, Campo.R, campoC, Campo.C);
        Map<ToggleButton, ModoPizarra> modos = Map.of(modoTodos, ModoPizarra.TODOS, modoPaso, ModoPizarra.PASO_A_PASO);
        Map<ToggleButton, TamanoLetra> letras = Map.of(letraNormal, TamanoLetra.NORMAL, letraGrande, TamanoLetra.GRANDE,
                letraMuyGrande, TamanoLetra.MUY_GRANDE);
        Map<ToggleButton, Tema> temas = Map.of(temaClaro, Tema.CLARO, temaOscuro, Tema.OSCURO);
        unir(grupoCampo, campos, c -> configuracion.guardarCampoPorDefecto(c));
        unir(grupoModo, modos, m -> configuracion.guardarModoPizarra(m));
        unir(grupoLetra, letras, t -> configuracion.guardarTamanoLetra(t));
        unir(grupoTema, temas, t -> configuracion.guardarTema(t));
        interruptor(mostrarExplicaciones, si -> configuracion.guardarMostrarExplicaciones(si));
        interruptor(mostrarCodigos, si -> configuracion.guardarMostrarCodigos(si));

        FormulaAjustable previa = new FormulaAjustable(22, 22);
        previa.setPartible(false);
        previa.setLatex(VISTA_PREVIA);
        vistaPrevia.getChildren().setAll(previa);

        // Si otra pantalla cambia un ajuste (el interruptor "Explicaciones" de la Pizarra), aquí también cambia.
        configuracion.explicacionesProperty().addListener((obs, antes, si) -> poner(mostrarExplicaciones, si));
        configuracion.codigosProperty().addListener((obs, antes, si) -> poner(mostrarCodigos, si));

        armarVersiones();
        cargar();
    }

    /** Botones unidos: siempre hay uno elegido; elegir otro lo guarda. */
    private <T> void unir(ToggleGroup grupo, Map<ToggleButton, T> opciones, Consumer<T> guardar) {
        opciones.keySet().forEach(b -> b.setToggleGroup(grupo));
        boolean[] reponiendo = {false};
        grupo.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                if (antes != null) {
                    // un clic en el elegido no lo deja sin nada, y volver a elegirlo no es un cambio
                    reponiendo[0] = true;
                    antes.setSelected(true);
                    reponiendo[0] = false;
                }
                return;
            }
            if (!cargando && !reponiendo[0]) {
                guardar.accept(opciones.get((ToggleButton) ahora));
                avisarGuardado();
            }
        });
    }

    private void interruptor(ToggleButton t, Consumer<Boolean> guardar) {
        // Espacio ya lo maneja el botón; Enter también lo cambia.
        t.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                t.fire();
                e.consume();
            }
        });
        t.selectedProperty().addListener((obs, antes, si) -> {
            t.pseudoClassStateChanged(ENCENDIDO, si);
            if (!cargando) {
                guardar.accept(si);
                avisarGuardado();
            }
        });
    }

    private void avisarGuardado() {
        ultimoAviso = GUARDADO;
        avisar.accept(GUARDADO);
    }

    private void poner(ToggleButton t, boolean si) {
        boolean antes = cargando;
        cargando = true;
        t.setSelected(si);
        t.pseudoClassStateChanged(ENCENDIDO, si);
        cargando = antes;
    }

    /** Lee todo otra vez (al volver a la pantalla): los ajustes y cuántos ejercicios hay. */
    public void cargar() {
        cargando = true;
        try {
            Campo c = configuracion.campoProperty().get();
            seleccionar(grupoCampo, c == Campo.R ? campoR : c == Campo.C ? campoC : campoQ);
            seleccionar(grupoModo, configuracion.modoProperty().get() == ModoPizarra.PASO_A_PASO ? modoPaso : modoTodos);
            TamanoLetra t = configuracion.letraProperty().get();
            seleccionar(grupoLetra, t == TamanoLetra.GRANDE ? letraGrande
                    : t == TamanoLetra.MUY_GRANDE ? letraMuyGrande : letraNormal);
            seleccionar(grupoTema, configuracion.temaProperty().get() == Tema.OSCURO ? temaOscuro : temaClaro);
            poner(mostrarExplicaciones, configuracion.explicacionesProperty().get());
            poner(mostrarCodigos, configuracion.codigosProperty().get());
        } finally {
            cargando = false;
        }
        contarEjercicios();
    }

    private static void seleccionar(ToggleGroup g, Toggle t) {
        g.selectToggle(t);
    }

    // ---------------------------------------------------------------- Datos

    private void contarEjercicios() {
        long n;
        try {
            n = historial.ejerciciosGuardados();
        } catch (RuntimeException e) {
            LOG.warn("No se pudo contar el historial", e);
            n = 0;
        }
        datos.setText(datos(n, informacion.rutaBase()));
        borrarHistorial.setDisable(n == 0);
    }

    /** "12 ejercicios guardados. Se guardan en ~/.mathnova/mathnova.db." */
    static String datos(long n, String ruta) {
        String cuantos = n == 0 ? "Todavía no hay ejercicios guardados" : n == 1 ? "1 ejercicio guardado"
                : n + " ejercicios guardados";
        return cuantos + ". Se guardan en " + ruta + ".";
    }

    /** "Borrar todo el historial", con confirmación. Los ejemplos no se tocan. */
    @FXML
    void borrarHistorial() {
        Window w = raiz.getScene() == null ? null : raiz.getScene().getWindow();
        if (!confirmar.test(w)) {
            return;
        }
        try {
            historial.borrarTodo();
            ultimoAviso = "Historial borrado";
            avisar.accept(ultimoAviso);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo borrar el historial", e);
            ultimoAviso = "No se pudo borrar el historial";
            avisar.accept(ultimoAviso);
        }
        contarEjercicios();
    }

    private static boolean confirmarBorrado(Window ventana) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Borrar todo el historial");
        if (ventana != null) {
            d.initOwner(ventana);
            if (ventana.getScene() != null) {
                d.getDialogPane().getStylesheets().setAll(ventana.getScene().getStylesheets());
                d.getDialogPane().getStyleClass().addAll(ventana.getScene().getRoot().getStyleClass());
            }
        }
        Label titulo = new Label("¿Borrar todo el historial?");
        titulo.getStyleClass().add("titulo-tarjeta");
        Label texto = new Label("Se borran todos los ejercicios guardados, con sus pasos y sus prácticas. Los "
                + "ejemplos no se tocan. Esto no se puede deshacer.");
        texto.getStyleClass().add("subtitulo-tarjeta");
        texto.setWrapText(true);
        VBox caja = new VBox(12, titulo, texto);
        caja.setPrefWidth(420);
        d.getDialogPane().setContent(caja);
        ButtonType borrar = new ButtonType("Borrar todo", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().setAll(cancelar, borrar);
        d.getDialogPane().lookupButton(borrar).getStyleClass().add("boton-primario");
        d.getDialogPane().lookupButton(cancelar).getStyleClass().add("boton-secundario");
        return d.showAndWait().filter(b -> b == borrar).isPresent();
    }

    // ---------------------------------------------------------------- Acerca de

    private void armarVersiones() {
        versiones.getChildren().clear();
        List<InformacionService.Version> lista = informacion.versiones();
        for (int i = 0; i < lista.size(); i++) {
            Label nombre = new Label(lista.get(i).nombre());
            nombre.getStyleClass().add("subtitulo-tarjeta");
            Label valor = new Label(lista.get(i).valor());
            valor.getStyleClass().add("ajuste-version");
            versiones.addRow(i, nombre, valor);
        }
    }

    // ---------------------------------------------------------------- pruebas

    void setConfirmar(Predicate<Window> confirmar) {
        this.confirmar = confirmar;
    }

    void setAvisar(Consumer<String> avisar) {
        this.avisar = avisar;
    }

    String ultimoAviso() {
        return ultimoAviso;
    }
}
