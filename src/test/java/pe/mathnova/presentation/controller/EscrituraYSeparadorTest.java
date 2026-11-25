package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.List;

import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.escritura.LienzoEscritura;

class EscrituraYSeparadorTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static final class Preferencias extends ConfiguracionService {
        Preferencias() {
            super(null);
        }

        @Override
        public boolean animacionesActivas() {
            return false;
        }

        @Override
        public boolean mostrarCodigos() {
            return false;
        }

        @Override
        public boolean mostrarExplicaciones() {
            return false;
        }

        @Override
        public void guardarMostrarExplicaciones(boolean si) {
        }
    }

    private Stage ventana;
    private Parent raiz;
    private SplitPane division;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        enFx(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER,
                        new Preferencias(), new ReconocedorSimulado(0)));
                raiz = loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            ventana = Fx.ventana(raiz, 1280, 900);
            division = (SplitPane) raiz.lookup("#division");
            ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire();
        });
        acomodar();
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private void acomodar() throws Exception {
        Thread.sleep(150);
        enFx(() -> {
            raiz.applyCss();
            raiz.layout();
        });
    }

    @Test
    void moverElSeparadorConservaLosTrazos() throws Exception {
        LienzoEscritura lienzo = enFx(() -> (LienzoEscritura) raiz.lookup(".lienzo-escritura"));
        enFx(() -> {
            var c = raiz.lookup(".lienzo-escritura-canvas");
            Fx.trazo(c, new double[] {40, 80}, new double[] {80, 150});
            Fx.trazo(c, new double[] {120, 100}, new double[] {160, 150});
            Fx.trazo(c, new double[] {160, 100}, new double[] {120, 150});
        });
        List<Trazo> antes = enFx(lienzo::trazos);
        assertEquals(3, antes.size());
        double anchoAntes = enFx(() -> lienzo.getWidth());

        enFx(() -> division.setDividerPositions(0.3));
        acomodar();
        enFx(() -> division.setDividerPositions(0.55));
        acomodar();

        enFx(() -> {
            assertSame(lienzo, raiz.lookup(".lienzo-escritura"), "el lienzo no se vuelve a crear");
            assertNotEquals(anchoAntes, lienzo.getWidth(), 1.0, "el separador sí cambió el ancho");
            assertEquals(antes, lienzo.trazos(), "los trazos siguen ahí");
        });
    }
}
