package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pe.mathnova.application.service.EjemplosService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.Navegador;
import pe.mathnova.presentation.resolucion.FormulaAjustable;

/** Sprint 9, parte B: la pantalla Ejemplos sobre la base de prueba de target/ (los ejemplos de V4). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
class EjemplosPantallaTest {

    private static final PseudoClass SELECCIONADA = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass ACTIVO = PseudoClass.getPseudoClass("activo");

    @Autowired
    private EjemplosService ejemplos;

    /** Navegador de prueba: anota Resolver y Practicar. */
    private static final class NavegadorPrueba extends Navegador {
        final List<String> abiertos = new ArrayList<>();
        boolean practica;

        NavegadorPrueba() {
            super(null);
        }

        @Override
        public void abrirEnResolver(String texto, Campo campo, String forzado) {
            abiertos.add("resolver | " + texto + " | " + campo + " | " + forzado);
        }

        @Override
        public boolean sePuedePracticar(String texto, Campo campo) {
            return practica;
        }

        @Override
        public void abrirEnPractica(String texto, Campo campo) {
            abiertos.add("practicar | " + texto + " | " + campo);
        }
    }

    private final NavegadorPrueba navegador = new NavegadorPrueba();
    private EjemplosController controlador;
    private Parent raiz;
    private Stage ventana;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        abrirCon(false);
    }

    private void abrirCon(boolean practica) throws Exception {
        navegador.practica = practica;
        Fx.enFx(() -> {
            if (ventana != null) {
                ventana.close();
            }
            FXMLLoader loader = new FXMLLoader(EjemplosPantallaTest.class.getResource("/fxml/ejemplos.fxml"));
            loader.setControllerFactory(c -> new EjemplosController(ejemplos, navegador));
            try {
                loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            raiz = loader.getRoot();
            controlador = loader.getController();
            ventana = Fx.ventana(raiz, 1280, 800);
            raiz.applyCss();
            raiz.layout();
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        Fx.enFx(() -> ventana.close());
    }

    private List<ToggleButton> pestanas() {
        return raiz.lookupAll(".pestana").stream().map(ToggleButton.class::cast).toList();
    }

    private List<Button> temas() {
        return raiz.lookupAll(".ejemplos-tema").stream().map(Button.class::cast).toList();
    }

    private List<Node> tarjetas() {
        return List.copyOf(raiz.lookupAll(".ejemplo-tarjeta"));
    }

    private static Button boton(Node tarjeta, String texto) {
        return tarjeta.lookupAll(".ejemplo-boton").stream().map(Button.class::cast)
                .filter(b -> texto.equals(b.getText())).findFirst().orElseThrow();
    }

    private void capturar(String nombre) throws Exception {
        Fx.enFx(() -> {
            try {
                File carpeta = new File("target/capturas");
                carpeta.mkdirs();
                ImageIO.write(SwingFXUtils.fromFXImage(raiz.snapshot(null, null), null), "png",
                        new File(carpeta, nombre));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }

    @Test
    void abreEnFundamentosConFactorizacionEnQ() throws Exception {
        Fx.enFx(() -> {
            assertEquals(List.of("Fundamentos de Matemática", "Cálculo 1", "Cálculo 2", "Cálculo 3"),
                    pestanas().stream().map(ToggleButton::getText).toList());
            assertTrue(pestanas().getFirst().isSelected());
            assertTrue(pestanas().getFirst().getPseudoClassStates().contains(SELECCIONADA));
            assertEquals("Factorización en Q", controlador.temaActivo().nombre());

            List<Button> temas = temas();
            assertEquals(3, temas.size());
            assertTrue(temas.getFirst().getPseudoClassStates().contains(ACTIVO));
            assertFalse(temas.get(1).getPseudoClassStates().contains(ACTIVO));
            Label cantidad = (Label) temas.getFirst().lookup(".ejemplos-tema-cantidad");
            assertEquals("4", cantidad.getText());

            List<Node> tarjetas = tarjetas();
            assertEquals(4, tarjetas.size());
            Node ruffini = tarjetas.get(1);
            assertEquals("FACTORIZA", ((Label) ruffini.lookup(".ejemplo-verbo")).getText());
            assertEquals("Ruffini y aspa simple", ((Label) ruffini.lookup(".ejemplo-nota")).getText());
            assertNotNull(findFormula(ruffini), "la fórmula se dibuja con jlatexmath, no como texto");
            assertTrue(boton(ruffini, "Resolver").getStyleClass().contains("boton-primario"));
            assertTrue(boton(ruffini, "Practicar").getStyleClass().contains("boton-secundario"));
        });
        capturar("ejemplos-fundamentos.png");
    }

    private static Node findFormula(Node tarjeta) {
        Node caja = tarjeta.lookup(".ejemplo-formula");
        return ((javafx.scene.layout.StackPane) caja).getChildren().stream()
                .filter(FormulaAjustable.class::isInstance).findFirst().orElse(null);
    }

    @Test
    void cambiarDeTemaYDeCurso() throws Exception {
        Fx.enFx(() -> {
            temas().get(1).fire();
            assertEquals("Factorización en R y C", controlador.temaActivo().nombre());
            assertTrue(temas().get(1).getPseudoClassStates().contains(ACTIVO));
            assertFalse(temas().getFirst().getPseudoClassStates().contains(ACTIVO));
            assertEquals(5, tarjetas().size());
            assertEquals("FACTORIZA EN R", ((Label) tarjetas().getFirst().lookup(".ejemplo-verbo")).getText());

            pestanas().get(3).fire();
            assertEquals("Cálculo 3", controlador.cursoActivo().nombre());
            assertEquals(5, temas().size());
            assertEquals("Factorización en varias variables", controlador.temaActivo().nombre());
            // Un segundo clic en la pestaña activa no la apaga
            pestanas().get(3).fire();
            assertTrue(pestanas().get(3).isSelected());
            assertEquals("Cálculo 3", controlador.cursoActivo().nombre());

            temas().get(3).fire();
            assertEquals("Extremos", controlador.temaActivo().nombre());
            assertEquals(List.of("HALLA LOS EXTREMOS", "OPTIMIZA CON LA RESTRICCIÓN"), tarjetas().stream()
                    .map(t -> ((Label) t.lookup(".ejemplo-verbo")).getText()).toList());
        });
        capturar("ejemplos-calculo3.png");
    }

    @Test
    void resolverAbreElEjemploEnElEditorConSuCampo() throws Exception {
        Fx.enFx(() -> {
            boton(tarjetas().get(1), "Resolver").fire();
            temas().get(1).fire();
            boton(tarjetas().get(1), "Resolver").fire();
            raiz.applyCss();
            raiz.layout();
        });
        capturar("ejemplos-r-y-c.png");
        assertEquals(List.of("resolver | 2x^3 - 3x^2 - 3x + 2 | null | null", "resolver | x^2 + 4 | C | null"),
                navegador.abiertos);
    }

    @Test
    void practicarEsperaLaPantallaPractica() throws Exception {
        Fx.enFx(() -> {
            Button practicar = boton(tarjetas().getFirst(), "Practicar");
            assertTrue(practicar.isDisabled());
            assertEquals(EjemplosController.PRACTICA_PRONTO, practicar.getAccessibleHelp());
        });
        // Con la pantalla Práctica (parte D), el botón abre el ejemplo allí
        abrirCon(true);
        Fx.enFx(() -> {
            Button practicar = boton(tarjetas().getFirst(), "Practicar");
            assertFalse(practicar.isDisabled());
            practicar.fire();
        });
        assertEquals(List.of("practicar | x^3 - 2x^2 - x + 2 | null"), navegador.abiertos);
    }

    @Test
    void lasTarjetasSeRepartenElAncho() throws Exception {
        Fx.enFx(() -> {
            raiz.applyCss();
            raiz.layout();
            List<Node> tarjetas = tarjetas();
            double ancho = ((javafx.scene.layout.Region) tarjetas.getFirst()).getWidth();
            assertTrue(ancho >= EjemplosController.ANCHO_MINIMO_TARJETA, "ancho " + ancho);
            for (Node t : tarjetas) {
                assertEquals(ancho, ((javafx.scene.layout.Region) t).getWidth(), 0.5, "todas del mismo ancho");
            }
            // Dos tarjetas por fila a 1280 px (también con la pantalla escalada)
            assertEquals(tarjetas.get(0).getLayoutY(), tarjetas.get(1).getLayoutY(), 0.5);
            assertTrue(tarjetas.get(2).getLayoutY() > tarjetas.get(1).getLayoutY());
        });
    }
}
