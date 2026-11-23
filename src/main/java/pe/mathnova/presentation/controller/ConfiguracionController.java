package pe.mathnova.presentation.controller;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.application.service.ConfiguracionService;

/** Pantalla "Configuración": el ajuste "Mostrar las explicaciones" de la Pizarra. */
@Component
@Scope("prototype")
public class ConfiguracionController {

    private static final PseudoClass ENCENDIDO = PseudoClass.getPseudoClass("encendido");

    private final ConfiguracionService configuracion;

    @FXML
    private ToggleButton mostrarExplicaciones;
    /** Se está poniendo lo que dice la base: no hay que guardarlo otra vez. */
    private boolean cargando;

    public ConfiguracionController(ConfiguracionService configuracion) {
        this.configuracion = configuracion;
    }

    @FXML
    void initialize() {
        cargar();
        // Espacio ya lo maneja el botón; Enter también lo cambia.
        mostrarExplicaciones.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                mostrarExplicaciones.fire();
                e.consume();
            }
        });
        mostrarExplicaciones.selectedProperty().addListener((obs, antes, si) -> {
            mostrarExplicaciones.pseudoClassStateChanged(ENCENDIDO, si);
            if (!cargando) {
                configuracion.guardarMostrarExplicaciones(si);
            }
        });
    }

    /** Lee la preferencia otra vez (pudo cambiar con el interruptor de la Pizarra). */
    public void cargar() {
        boolean si = configuracion.mostrarExplicaciones();
        cargando = true;
        mostrarExplicaciones.setSelected(si);
        cargando = false;
        mostrarExplicaciones.pseudoClassStateChanged(ENCENDIDO, si);
    }
}
