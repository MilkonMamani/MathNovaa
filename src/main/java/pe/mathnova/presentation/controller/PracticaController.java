package pe.mathnova.presentation.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pe.mathnova.application.service.PracticaService;
import pe.mathnova.application.service.PracticaService.Opcion;
import pe.mathnova.application.service.PracticaService.PasoMostrado;
import pe.mathnova.application.service.PracticaService.Practica;
import pe.mathnova.application.service.PracticaService.Pregunta;
import pe.mathnova.application.service.PracticaService.Respuesta;
import pe.mathnova.domain.factorizacion.Campo;

/**
 * Pantalla "Práctica" (Sprint 9, parte D). Sin ejercicio, las tarjetas de "Elige un ejercicio". Con ejercicio, a
 * la izquierda el avance y a la derecha la pizarra: el enunciado, los pasos acertados con el formato de la
 * Pizarra y el recuadro punteado con la pregunta. Una opción equivocada queda tachada y gris, sin poder pulsarse
 * otra vez, con "⚠ No aplica." y el motivo debajo. Toda la lógica está en {@link PracticaService}.
 */
@Component
@Scope("prototype")
public class PracticaController {

    private static final Logger LOG = LoggerFactory.getLogger(PracticaController.class);
    private static final PseudoClass DESCARTADA = PseudoClass.getPseudoClass("descartada");
    static final double ANCHO_TARJETA = 280;

    private final PracticaService servicio;
    private Practica practica;
    private Pregunta pregunta;
    private final Map<String, Button> botones = new HashMap<>();
    /** Cambia con cada ejercicio pedido: uno que llega tarde se descarta. */
    private int pedido;
    /** Para las pruebas: corre las tareas (por omisión, en un hilo aparte). */
    private Consumer<Task<?>> ejecutor = t -> {
        Thread hilo = new Thread(t, "practica");
        hilo.setDaemon(true);
        hilo.start();
    };

    @FXML
    private VBox vistaElegir;
    @FXML
    private FlowPane propuestas;
    @FXML
    private Label cargandoPropuestas;
    @FXML
    private HBox vistaPractica;
    @FXML
    private Label tituloEjercicio;
    @FXML
    private Label pasoActual;
    @FXML
    private ProgressBar progreso;
    @FXML
    private Label aciertos;
    @FXML
    private Label fallidos;
    @FXML
    private Button verPista;
    @FXML
    private ScrollPane desplazamientoPizarra;
    @FXML
    private Label rotulo;
    @FXML
    private StackPane enunciado;
    @FXML
    private VBox pasos;
    @FXML
    private VBox cajaPregunta;
    @FXML
    private Label textoPregunta;
    @FXML
    private StackPane formulaPregunta;
    @FXML
    private FlowPane opciones;
    @FXML
    private VBox avisoNoAplica;
    @FXML
    private Label motivo;
    @FXML
    private HBox cajaPista;
    @FXML
    private Label pista;
    @FXML
    private VBox cajaFinal;
    @FXML
    private StackPane resultado;
    @FXML
    private Label resumen;
    @FXML
    private Label error;

    public PracticaController(PracticaService servicio) {
        this.servicio = servicio;
    }

    @FXML
    void initialize() {
        mostrarElegir();
        cargarPropuestas();
    }

    // ---------------------------------------------------------------- elegir un ejercicio

    private void cargarPropuestas() {
        Task<List<PracticaService.Propuesta>> t = new Task<>() {
            @Override
            protected List<PracticaService.Propuesta> call() {
                return servicio.propuestas();
            }
        };
        t.setOnSucceeded(e -> {
            List<Node> tarjetas = new ArrayList<>();
            for (PracticaService.Propuesta p : t.getValue()) {
                tarjetas.add(tarjeta(p));
            }
            propuestas.getChildren().setAll(tarjetas);
            mostrar(cargandoPropuestas, false);
        });
        t.setOnFailed(e -> {
            LOG.warn("No se pudieron preparar los ejercicios de práctica", t.getException());
            cargandoPropuestas.setText("No se pudieron preparar los ejercicios.");
        });
        ejecutor.accept(t);
    }

    /** "Límites · 3 pasos", el título, la fórmula y "Empezar". */
    private Node tarjeta(PracticaService.Propuesta p) {
        Label tema = new Label(p.tema() + " · " + (p.pasos() == 1 ? "1 paso" : p.pasos() + " pasos"));
        tema.getStyleClass().add("practica-tarjeta-tema");
        Label titulo = new Label(p.titulo());
        titulo.getStyleClass().add("practica-tarjeta-titulo");
        StackPane formula = new StackPane(HistorialController.formula(p.latex(), p.texto(), 20, 13));
        formula.setAlignment(Pos.CENTER_LEFT);
        formula.getStyleClass().add("ejemplo-formula");
        Button empezar = new Button("Empezar");
        empezar.getStyleClass().addAll("boton-primario", "ejemplo-boton");
        empezar.setAccessibleText("Empezar " + p.texto());
        empezar.setOnAction(e -> empezar(p.texto(), p.campo()));
        Region hueco = new Region();
        VBox.setVgrow(hueco, Priority.ALWAYS);
        VBox t = new VBox(8, tema, titulo, formula, hueco, empezar);
        t.getStyleClass().addAll("ejemplo-tarjeta", "practica-tarjeta");
        t.setPrefWidth(ANCHO_TARJETA);
        t.setUserData(p);
        return t;
    }

    // ---------------------------------------------------------------- empezar

    /** Empieza (o repite) un ejercicio: lo resuelve el motor y aparece la primera pregunta. */
    public void empezar(String texto, Campo campo) {
        int este = ++pedido;
        mostrar(error, false);
        Task<Practica> t = new Task<>() {
            @Override
            protected Practica call() {
                return servicio.iniciar(texto, campo);
            }
        };
        t.setOnSucceeded(e -> {
            if (este == pedido) {
                abrir(t.getValue());
            }
        });
        t.setOnFailed(e -> {
            if (este == pedido) {
                LOG.warn("No se pudo empezar la práctica de {}", texto, t.getException());
                mostrarElegir();
                Throwable c = t.getException();
                error.setText("No se puede practicar este ejercicio: "
                        + (c == null || c.getMessage() == null ? "error desconocido" : c.getMessage()));
                mostrar(error, true);
            }
        });
        ejecutor.accept(t);
    }

    private void abrir(Practica p) {
        practica = p;
        tituloEjercicio.setText(p.titulo());
        rotulo.setText(p.titulo().toUpperCase(java.util.Locale.ROOT));
        enunciado.getChildren().setAll(HistorialController.formula(p.latexEnunciado(), p.texto(), 24, 14));
        pasos.getChildren().clear();
        mostrar(cajaFinal, false);
        mostrar(cajaPregunta, true);
        mostrar(vistaElegir, false);
        mostrar(vistaPractica, true);
        mostrar(error, false);
        desplazamientoPizarra.setVvalue(0);
        siguiente();
    }

    /** La pregunta que toca, o el final. */
    private void siguiente() {
        PracticaService.Estado e = servicio.estado(practica.sesionId());
        pintarAvance(e);
        var q = servicio.actual(practica.sesionId());
        if (q.isEmpty()) {
            terminar(e);
            return;
        }
        pregunta = q.get();
        textoPregunta.setText("Paso " + pregunta.numero() + ". " + pregunta.pregunta());
        formulaPregunta.getChildren().setAll(HistorialController.formula(pregunta.latexSobre(), pregunta.latexSobre(),
                20, 13));
        botones.clear();
        List<Node> lista = new ArrayList<>();
        for (Opcion o : pregunta.opciones()) {
            Button b = new Button(o.texto());
            b.getStyleClass().add("practica-opcion");
            b.setWrapText(true);
            b.setAccessibleText(o.texto());
            b.setOnAction(ev -> elegir(o));
            botones.put(o.codigo(), b);
            lista.add(b);
        }
        opciones.getChildren().setAll(lista);
        mostrar(avisoNoAplica, false);
        mostrar(cajaPista, false);
        verPista.setDisable(false);
    }

    private void pintarAvance(PracticaService.Estado e) {
        int k = Math.min(e.paso(), e.total());
        pasoActual.setText(e.terminada() ? "Terminaste los " + e.total() + " pasos" : "Paso " + k + " de " + e.total());
        progreso.setProgress(e.total() == 0 ? 0 : (double) (e.paso() - 1) / e.total());
        aciertos.setText(String.valueOf(e.aciertos()));
        fallidos.setText(String.valueOf(e.fallidos()));
    }

    // ---------------------------------------------------------------- responder

    void elegir(Opcion o) {
        Respuesta r;
        try {
            r = servicio.responder(practica.sesionId(), pregunta.numero(), o.codigo());
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo guardar la respuesta", ex);
            return;
        }
        if (!r.correcta()) {
            Button b = botones.get(o.codigo());
            b.setDisable(true);
            b.pseudoClassStateChanged(DESCARTADA, true);
            b.setAccessibleHelp("No aplica: " + r.motivo());
            motivo.setText(r.motivo());
            mostrar(avisoNoAplica, true);
            pintarAvance(r.estado());
            return;
        }
        for (PasoMostrado p : r.pasos()) {
            pasos.getChildren().add(HistorialController.paso(p.numero(), p.nivel(), p.titulo(), p.latex(), p.nota()));
        }
        siguiente();
    }

    @FXML
    void verPista() {
        if (practica == null || pregunta == null) {
            return;
        }
        pista.setText(servicio.pista(practica.sesionId(), pregunta.numero()));
        mostrar(cajaPista, true);
    }

    private void terminar(PracticaService.Estado e) {
        pregunta = null;
        mostrar(cajaPregunta, false);
        resultado.getChildren().setAll(HistorialController.formula(practica.resultadoLatex(), practica.resultadoTexto(),
                22, 14));
        resumen.setText(resumen(e.aciertos(), e.total()));
        mostrar(cajaFinal, true);
        verPista.setDisable(true);
    }

    /** "Terminaste. Acertaste 2 de 3 pasos a la primera." */
    static String resumen(int a, int n) {
        return "Terminaste. Acertaste " + a + " de " + n + (n == 1 ? " paso" : " pasos") + " a la primera.";
    }

    @FXML
    void elegirOtro() {
        pedido++;
        practica = null;
        pregunta = null;
        mostrarElegir();
    }

    @FXML
    void repetir() {
        if (practica != null) {
            empezar(practica.texto(), practica.campo());
        }
    }

    private void mostrarElegir() {
        mostrar(vistaPractica, false);
        mostrar(vistaElegir, true);
    }

    private static void mostrar(Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }

    // ---------------------------------------------------------------- pruebas

    void setEjecutor(Consumer<Task<?>> ejecutor) {
        this.ejecutor = ejecutor;
    }

    Practica practica() {
        return practica;
    }

    Pregunta pregunta() {
        return pregunta;
    }

    Map<String, Button> botones() {
        return botones;
    }
}
