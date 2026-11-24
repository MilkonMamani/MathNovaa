package pe.mathnova.presentation.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pe.mathnova.domain.escritura.Muestra;
import pe.mathnova.domain.escritura.Punto;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.infrastructure.escritura.ArchivoMuestras;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;

/** Primer hito del reconocedor propio: capturar, guardar y reproducir trazos reales. */
class MuestrasEscrituraTest {

    @TempDir
    Path carpeta;

    private Stage ventana;
    private PanelEscritura panel;
    private ScrollPane pagina;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        panel = enFx(() -> {
            PanelEscritura p = new PanelEscritura(new ReconocedorSimulado(0), latex -> { }, carpeta);
            // Una página más alta que la ventana, como "Resolver ejercicio": se puede desplazar.
            VBox contenido = new VBox(p, new StackPane());
            contenido.setMinHeight(2000);
            pagina = new ScrollPane(contenido);
            pagina.setFitToWidth(true);
            ventana = Fx.ventana(pagina, 520, 500);
            return p;
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private LienzoEscritura lienzo() {
        return panel.lienzo();
    }

    private void trazo(double[]... puntos) throws Exception {
        enFx(() -> Fx.trazo(lienzo().canvas(), puntos));
    }

    /** El «2» de un solo contacto y la «x» de dos diagonales separadas. */
    private void escribir2x() throws Exception {
        trazo(new double[] {40, 80}, new double[] {55, 65}, new double[] {75, 62}, new double[] {88, 75},
                new double[] {80, 100}, new double[] {50, 140}, new double[] {40, 150}, new double[] {95, 150});
        trazo(new double[] {120, 100}, new double[] {140, 125}, new double[] {160, 150});
        Thread.sleep(15); // que la segunda diagonal empiece un poco después
        trazo(new double[] {160, 100}, new double[] {140, 125}, new double[] {120, 150});
    }

    private ButtonBase boton(String nombre) throws Exception {
        return enFx(() -> panel.lookupAll(".herramienta-escritura").stream()
                .map(n -> (ButtonBase) n).filter(b -> nombre.equals(b.getAccessibleText())).findFirst().orElseThrow());
    }

    @Test
    void dosXSeGuardaSeLimpiaSeCargaYSeDeshaceLaUltimaDiagonal() throws Exception {
        escribir2x();
        List<Trazo> dibujados = enFx(() -> lienzo().trazos());
        assertEquals(3, dibujados.size(), "«2» de un contacto + «x» de dos");
        assertEquals(8, dibujados.get(0).puntos().size());

        // Puntos en orden, relativos al lienzo, con el tiempo desde el primer punto.
        assertEquals(0, dibujados.getFirst().puntos().getFirst().t());
        assertEquals(40, dibujados.getFirst().puntos().getFirst().x(), 1e-3);
        assertEquals(80, dibujados.getFirst().puntos().getFirst().y(), 1e-3);
        long antes = -1;
        for (Trazo t : dibujados) {
            for (Punto p : t.puntos()) {
                assertTrue(p.t() >= antes, "el tiempo no retrocede");
                antes = p.t();
            }
        }
        assertTrue(dibujados.get(2).puntos().getFirst().t() > dibujados.get(1).puntos().getLast().t(),
                "la segunda diagonal empieza después de la primera");

        // Guardar con la etiqueta del desarrollador.
        BarraMuestras barra = panel.muestras();
        assertNotNull(barra, "con carpeta de muestras aparece la barra de desarrollo");
        Path archivo = enFx(() -> {
            barra.etiqueta().setText("2x");
            return barra.guardar();
        });
        assertNotNull(archivo);
        assertTrue(Files.exists(archivo));
        assertEquals(carpeta, archivo.getParent());
        String json = Files.readString(archivo);
        assertTrue(json.contains("\"etiqueta\" : \"2x\""), json);
        Muestra leida = ArchivoMuestras.leer(archivo);
        double ancho = enFx(() -> lienzo().canvas().getWidth());
        assertEquals(ancho, leida.ancho(), 1e-9, "guarda el ancho del lienzo de captura");
        assertEquals(LienzoEscritura.ALTO, leida.alto(), 1e-9);
        assertEquals(dibujados, leida.trazos(), "el JSON tiene los mismos puntos y tiempos");

        // Limpiar.
        ButtonBase borrarTodo = boton("Borrar todo");
        enFx(() -> borrarTodo.fire());
        enFx(() -> assertTrue(lienzo().isVacio()));

        // Cargar: el mismo «2x».
        assertTrue(enFx(() -> barra.cargar(archivo)));
        enFx(() -> {
            assertEquals(dibujados, lienzo().trazos());
            assertFalse(lienzo().isVacio());
            assertFalse(panel.lookup(".lienzo-vacio").isVisible());
            assertTrue(barra.estado().getText().contains("2x"));
        });

        // Deshacer quita la última diagonal.
        ButtonBase deshacer = boton("Deshacer");
        enFx(() -> deshacer.fire());
        enFx(() -> {
            assertEquals(2, lienzo().trazos().size());
            assertEquals(dibujados.subList(0, 2), lienzo().trazos());
        });
    }

    @Test
    void sinEtiquetaOSinTrazosNoGuarda() throws Exception {
        BarraMuestras barra = panel.muestras();
        assertNull(enFx(() -> {
            barra.etiqueta().setText("2x");
            return barra.guardar();
        }), "lienzo vacío");
        trazo(new double[] {20, 60}, new double[] {60, 100});
        assertNull(enFx(() -> {
            barra.etiqueta().setText("  ");
            return barra.guardar();
        }), "sin etiqueta");
        try (var archivos = Files.list(carpeta)) {
            assertEquals(0, archivos.count());
        }
    }

    @Test
    void sinCarpetaNoHayBarraDeDesarrollo() throws Exception {
        PanelEscritura normal = enFx(() -> new PanelEscritura(new ReconocedorSimulado(0), latex -> { }));
        assertNull(normal.muestras());
        assertNull(normal.lookup(".barra-muestras"));
    }

    @Test
    void unToqueConSusEventosDeMouseSintetizadosQuedaUnaSolaVez() throws Exception {
        enFx(() -> {
            Node c = lienzo().canvas();
            double[][] puntos = {{30, 60}, {50, 80}, {70, 100}};
            for (int i = 0; i < puntos.length; i++) {
                var tipoToque = i == 0 ? TouchEvent.TOUCH_PRESSED : TouchEvent.TOUCH_MOVED;
                var estado = i == 0 ? TouchPoint.State.PRESSED : TouchPoint.State.MOVED;
                toque(c, tipoToque, estado, puntos[i][0], puntos[i][1]);
                // Windows manda además el mismo gesto como mouse, marcado "sintetizado".
                raton(c, i == 0 ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_DRAGGED, puntos[i][0], puntos[i][1], true);
            }
            toque(c, TouchEvent.TOUCH_RELEASED, TouchPoint.State.RELEASED, 70, 100);
            raton(c, MouseEvent.MOUSE_RELEASED, 70, 100, true);

            assertEquals(1, lienzo().trazos().size(), "un gesto, un trazo");
            assertEquals(3, lienzo().trazos().getFirst().puntos().size(), "sin puntos repetidos");
        });
        // El mouse de verdad sigue escribiendo.
        trazo(new double[] {100, 60}, new double[] {140, 100});
        enFx(() -> assertEquals(2, lienzo().trazos().size()));
    }

    @Test
    void escribirConElDedoNoDesplazaLaPaginaPeroLaRuedaSi() throws Exception {
        enFx(() -> {
            pagina.applyCss();
            pagina.layout();
            Node c = lienzo().canvas();
            desplazar(c, true, -120); // dedo
            assertEquals(0, pagina.getVvalue(), 1e-9, "el desplazamiento táctil no mueve la página");
            desplazar(c, false, -120); // rueda del mouse
            assertTrue(pagina.getVvalue() > 0, "la rueda sí mueve la página");
        });
    }

    @Test
    void lasCoordenadasSeRecortanAlLienzo() throws Exception {
        double ancho = enFx(() -> lienzo().canvas().getWidth());
        trazo(new double[] {20, 60}, new double[] {-30, -10}, new double[] {ancho + 50, 400});
        enFx(() -> {
            List<Punto> p = lienzo().trazos().getFirst().puntos();
            assertEquals(new Point2D(0, 0), new Point2D(p.get(1).x(), p.get(1).y()));
            assertEquals(new Point2D(ancho, LienzoEscritura.ALTO), new Point2D(p.get(2).x(), p.get(2).y()));
        });
    }

    @Test
    void unaMuestraDeUnLienzoMasAnchoSeReduceParaEntrar() throws Exception {
        double ancho = enFx(() -> lienzo().canvas().getWidth());
        List<Punto> raya = new ArrayList<>(List.of(new Punto(0, 0, 0), new Punto(ancho * 2, 220, 40)));
        Muestra grande = new Muestra("-", ancho * 2, 220, List.of(new Trazo(raya)));
        enFx(() -> {
            lienzo().cargar(grande);
            Punto fin = lienzo().trazos().getFirst().puntos().getLast();
            assertEquals(ancho, fin.x(), 1e-6);
            assertEquals(110, fin.y(), 1e-6, "misma escala en x e y");
            assertEquals(40, fin.t(), "el tiempo no se escala");
        });
    }

    // ------------------------------------------------------------------ eventos

    private static void toque(Node n, EventType<TouchEvent> tipo, TouchPoint.State estado, double x, double y) {
        Point2D e = n.localToScene(x, y);
        TouchPoint p = new TouchPoint(1, estado, e.getX(), e.getY(), e.getX(), e.getY(), n, null);
        Event.fireEvent(n, new TouchEvent(n, n, tipo, p, List.of(p), 1, false, false, false, false));
    }

    private static void raton(Node n, EventType<MouseEvent> tipo, double x, double y, boolean sintetizado) {
        Point2D e = n.localToScene(x, y);
        Event.fireEvent(n, new MouseEvent(tipo, e.getX(), e.getY(), e.getX(), e.getY(), MouseButton.PRIMARY, 1,
                false, false, false, false, tipo != MouseEvent.MOUSE_RELEASED, false, false, sintetizado, false,
                false, null));
    }

    private static void desplazar(Node n, boolean directo, double dy) {
        Point2D e = n.localToScene(40, 60);
        Event.fireEvent(n, new ScrollEvent(ScrollEvent.SCROLL, e.getX(), e.getY(), e.getX(), e.getY(),
                false, false, false, false, directo, false, 0, dy, 0, dy,
                ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                directo ? 1 : 0, null));
    }
}
