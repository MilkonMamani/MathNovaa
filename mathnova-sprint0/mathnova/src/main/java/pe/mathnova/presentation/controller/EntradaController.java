package pe.mathnova.presentation.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Controlador de la pantalla de entrada. En el Sprint 0 solo prueba que la base responde. */
@Component
@Scope("prototype")
public class EntradaController {

    private final JdbcTemplate jdbc;

    @FXML
    private Label estado;

    public EntradaController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @FXML
    void initialize() {
        Integer tecnicas = jdbc.queryForObject("SELECT COUNT(*) FROM tecnica", Integer.class);
        estado.setText("Sprint 0 listo. Técnicas en el catálogo: " + tecnicas);
    }
}
