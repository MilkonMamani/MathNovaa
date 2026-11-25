package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.HistorialService;
import pe.mathnova.application.service.InformacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.Navegador;
import pe.mathnova.presentation.latex.Apariencia;
import pe.mathnova.presentation.resolucion.FormulaAjustable;

/**
 * Sprint 9, parte E: la pantalla Configuración con los servicios de verdad (base de prueba de target/). Cada
 * ajuste se guarda al cambiarlo y se aplica sin reiniciar: en la vista previa, en el tema de la ventana y en
 * Resolver. Al terminar, todo vuelve a lo de fábrica.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
class ConfiguracionPantallaTest {

    @Autowired
    private ConfiguracionService configuracion;
    @Autowired
    private HistorialService historial;
    @Autowired
    private InformacionService informacion;
    @Autowired
    private ResolverService resolver;
    @Autowired
    private FactorizacionService factorizacion;
    @Autowired
    private JdbcTemplate jdbc;

    private ConfiguracionController controlador;
    private Parent raiz;
    private StackPane marco;
    private Stage ventana;
    private final List<String> avisos = new ArrayList<>();

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        vaciar();
        Fx.enFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/configuracion.fxml"));
            loader.setControllerFactory(c -> new ConfiguracionController(configuracion, historial, informacion));
            try {
                raiz = loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            controlador = loader.getController();
            controlador.setAvisar(avisos::add);
            marco = new StackPane(raiz);
            ventana = Fx.ventana(marco, 1280, 1000);
            Navegador.aplicarApariencia(marco, configuracion);
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        Fx.enFx(() -> {
            configuracion.guardarCampoPorDefecto(Campo.Q);
            configuracion.guardarModoPizarra(ConfiguracionService.ModoPizarra.TODOS);
            configuracion.guardarMostrarExplicaciones(false);
            configuracion.guardarMostrarCodigos(false);
            configuracion.guardarTamanoLetra(ConfiguracionService.TamanoLetra.NORMAL);
            configuracion.guardarTema(ConfiguracionService.Tema.CLARO);
            Apariencia.escala().set(1);
            Apariencia.oscuro().set(false);
            ventana.close();
        });
        vaciar();
    }

    private void vaciar() {
        jdbc.update("DELETE FROM resolucion");
        jdbc.update("DELETE FROM ejercicio");
    }

    private <T extends Node> T buscar(String id) {
        @SuppressWarnings("unchecked")
        T n = (T) raiz.lookup(id);
        assertNotNull(n, id);
        return n;
    }

    private String valor(String clave) {
        return jdbc.queryForObject("SELECT valor FROM configuracion WHERE clave = ?", String.class, clave);
    }

    private void capturar(Parent nodo, String nombre) throws Exception {
        Fx.enFx(() -> {
            nodo.applyCss();
            nodo.layout();
            try {
                File carpeta = new File("target/capturas");
                carpeta.mkdirs();
                ImageIO.write(SwingFXUtils.fromFXImage(nodo.snapshot(null, null), null), "png", new File(carpeta, nombre));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }

    /** Visible él y todos sus contenedores. */
    private static boolean seVe(Node n) {
        for (Node p = n; p != null; p = p.getParent()) {
            if (!p.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /** Terminó de aparecer (la animación sube la opacidad de él o de su fila). */
    private static boolean opaco(Node n) {
        for (Node p = n; p != null; p = p.getParent()) {
            if (p.getOpacity() < 1) {
                return false;
            }
        }
        return true;
    }

    @Test
    void cadaAjusteSeGuardaAlCambiarloConElAviso() throws Exception {
        Fx.enFx(() -> {
            assertTrue(((ToggleButton) buscar("#campoQ")).isSelected(), "Q por defecto (RN27)");
            assertTrue(((ToggleButton) buscar("#modoTodos")).isSelected());
            assertFalse(((ToggleButton) buscar("#mostrarCodigos")).isSelected(), "RNF09");
            assertTrue(((ToggleButton) buscar("#letraNormal")).isSelected());
            assertTrue(((ToggleButton) buscar("#temaClaro")).isSelected());
        });
        capturar(marco, "configuracion-claro.png");

        Fx.enFx(() -> ((ToggleButton) buscar("#campoR")).fire());
        assertEquals("R", valor(ConfiguracionService.CAMPO_POR_DEFECTO));
        Fx.enFx(() -> ((ToggleButton) buscar("#modoPaso")).fire());
        assertEquals("paso", valor(ConfiguracionService.MODO_PIZARRA));
        Fx.enFx(() -> ((ToggleButton) buscar("#mostrarExplicaciones")).fire());
        assertEquals("true", valor(ConfiguracionService.MOSTRAR_EXPLICACIONES));
        Fx.enFx(() -> ((ToggleButton) buscar("#mostrarCodigos")).fire());
        assertEquals("true", valor(ConfiguracionService.MOSTRAR_CODIGOS));
        Fx.enFx(() -> ((ToggleButton) buscar("#letraGrande")).fire());
        assertEquals("grande", valor(ConfiguracionService.TAMANO_LETRA));
        Fx.enFx(() -> ((ToggleButton) buscar("#temaOscuro")).fire());
        assertEquals("oscuro", valor(ConfiguracionService.TEMA));
        assertEquals(List.of("Guardado", "Guardado", "Guardado", "Guardado", "Guardado", "Guardado"), avisos);

        // Un clic en el elegido no lo deja sin nada ni guarda otra vez
        Fx.enFx(() -> ((ToggleButton) buscar("#temaOscuro")).fire());
        assertTrue(Fx.enFx(() -> ((ToggleButton) buscar("#temaOscuro")).isSelected()));
        assertEquals(6, avisos.size());
    }

    @Test
    void laVistaPreviaCreceConLaLetraEnVivo() throws Exception {
        double normal = Fx.enFx(() -> {
            marco.applyCss();
            marco.layout();
            return ((FormulaAjustable) buscar("#vistaPrevia").lookup(".formula-ajustable")).prefWidth(-1);
        });
        Fx.enFx(() -> ((ToggleButton) buscar("#letraMuyGrande")).fire());
        double muyGrande = Fx.enFx(() -> {
            marco.applyCss();
            marco.layout();
            return ((FormulaAjustable) buscar("#vistaPrevia").lookup(".formula-ajustable")).prefWidth(-1);
        });
        assertEquals(1.4, muyGrande / normal, 0.08, normal + " → " + muyGrande);
        assertTrue(Fx.enFx(() -> marco.getStyleClass().contains("letra-muy-grande")));
    }

    @Test
    void elTemaOscuroCambiaLaVentanaSinReiniciar() throws Exception {
        Fx.enFx(() -> ((ToggleButton) buscar("#temaOscuro")).fire());
        assertTrue(Fx.enFx(() -> marco.getStyleClass().contains("tema-oscuro")));
        assertTrue(Apariencia.oscuro().get(), "las fórmulas pasan a tinta clara");
        capturar(marco, "configuracion-oscuro.png");
        Fx.enFx(() -> ((ToggleButton) buscar("#temaClaro")).fire());
        assertFalse(Fx.enFx(() -> marco.getStyleClass().contains("tema-oscuro")));
        assertFalse(Apariencia.oscuro().get());
    }

    @Test
    void datosYBorrarTodoElHistorial() throws Exception {
        resolver.resolverYGuardar("x^3 - 2x^2 - x + 2", Campo.Q, null);
        resolver.resolverYGuardar("x^2 - 4", Campo.Q, null);
        Fx.enFx(() -> controlador.cargar());
        String texto = Fx.enFx(() -> ((Label) buscar("#datos")).getText());
        assertTrue(texto.startsWith("2 ejercicios guardados. Se guardan en "), texto);
        assertTrue(texto.endsWith("prueba-mathnova.db."), texto);
        int ejemplos = jdbc.queryForObject("SELECT COUNT(*) FROM ejemplo", Integer.class);

        // Cancelar no borra
        Fx.enFx(() -> {
            controlador.setConfirmar(w -> false);
            ((Button) buscar("#borrarHistorial")).fire();
        });
        assertEquals(2, historial.ejerciciosGuardados());
        // Confirmar sí
        Fx.enFx(() -> {
            controlador.setConfirmar(w -> true);
            ((Button) buscar("#borrarHistorial")).fire();
        });
        assertEquals(0, historial.ejerciciosGuardados());
        assertEquals(ejemplos, jdbc.queryForObject("SELECT COUNT(*) FROM ejemplo", Integer.class));
        Fx.enFx(() -> {
            assertEquals("Todavía no hay ejercicios guardados. Se guardan en " + informacion.rutaBase() + ".",
                    ((Label) buscar("#datos")).getText());
            assertTrue(((Button) buscar("#borrarHistorial")).isDisabled());
        });
    }

    @Test
    void acercaDe() throws Exception {
        List<String> textos = Fx.enFx(() -> buscar("#versiones").lookupAll(".label").stream()
                .map(n -> ((Label) n).getText()).toList());
        for (String nombre : List.of("MathNova", "Motor de resolución", "Java", "JavaFX", "SQLite")) {
            assertTrue(textos.contains(nombre), textos.toString());
        }
        assertTrue(textos.contains("0.2.0"), "la versión del motor: " + textos);
        assertTrue(textos.stream().anyMatch(t -> t.startsWith("21")), "Java 21: " + textos);
        assertTrue(textos.stream().anyMatch(t -> t.startsWith("3.")), "SQLite 3: " + textos);
    }

    @Test
    void resolverTomaLosAjustesSinReiniciar() throws Exception {
        Fx.enFx(() -> {
            configuracion.guardarCampoPorDefecto(Campo.R);
            configuracion.guardarModoPizarra(ConfiguracionService.ModoPizarra.PASO_A_PASO);
            configuracion.guardarTema(ConfiguracionService.Tema.OSCURO);
        });
        ResolucionController[] c = new ResolucionController[1];
        Parent[] r = new Parent[1];
        Fx.enFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
            loader.setControllerFactory(k -> new ResolucionController(factorizacion, resolver, configuracion));
            try {
                r[0] = loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            c[0] = loader.getController();
            marco.getChildren().setAll(r[0]);
        });
        Fx.esperar(() -> r[0].lookup(".editor-matematico") != null, "el editor no cargó");
        // El campo por defecto
        assertTrue(Fx.enFx(() -> ((ToggleButton) r[0].lookup("#campoR")).isSelected()));
        // y cambia en vivo
        Fx.enFx(() -> configuracion.guardarCampoPorDefecto(Campo.Q));
        assertTrue(Fx.enFx(() -> ((ToggleButton) r[0].lookup("#campoQ")).isSelected()));

        Fx.enFx(() -> c[0].resolver("x^3 - 2x^2 - x + 2"));
        Fx.esperar(() -> c[0].estado() == EstadoResolucion.RESUELTO, "no se resolvió");
        // Paso a paso: los pasos que faltan muestran solo su número; el método se ve solo en el primero
        Fx.esperar(() -> r[0].lookupAll(".pizarra-metodo").stream().filter(ConfiguracionPantallaTest::seVe).count() == 1
                && opaco(r[0].lookupAll(".pizarra-metodo").stream().filter(ConfiguracionPantallaTest::seVe)
                .findFirst().orElseThrow()), "no apareció solo el primer paso");
        // Paso a paso: la pizarra abre en el paso 1
        String etiqueta = Fx.enFx(() -> ((Label) r[0].lookup(".pizarra-etiqueta-pasos")).getText());
        assertTrue(etiqueta.startsWith("Paso 1 de "), etiqueta);
        capturar(marco, "resolver-oscuro.png");
    }
}
