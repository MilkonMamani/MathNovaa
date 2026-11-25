package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.domain.calculo.limites.MotorLimites;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.animacion.AnimadorResolucion;
import pe.mathnova.presentation.latex.RenderizadorLatex;

/**
 * "Ver desarrollo" en la Pizarra: formato A o B según las filas (también al cambiar el ancho), cerrado en "Todos
 * los pasos", abierto solo al llegar en "Paso a paso" y respetando al alumno después; un ejercicio nuevo no hereda
 * nada abierto. Sin "Comprobación" en un límite.
 */
class DesplegablesPizarraTest {

    private static final MotorLimites LIMITES = new MotorLimites(new OrquestadorFactorizacion());

    private Stage ventana;
    private PizarraResolucion pizarra;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        pizarra = enFx(() -> {
            AnimadorResolucion animador = new AnimadorResolucion();
            animador.setActivas(false);
            PizarraResolucion p = new PizarraResolucion(new RenderizadorLatex(18f), animador, false);
            ventana = Fx.ventana(new StackPane(p), 900, 1400);
            return p;
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private void mostrarLimite(String texto) {
        var r = LIMITES.resolver((Limite) Parser.parse(texto));
        String entrada = ImpresoraLatex.imprimir(r.entrada());
        pizarra.mostrar(new PizarraResolucion.Contenido("CALCULA EL LÍMITE", entrada,
                VistaResolucion.de(r, false).pasos(), entrada + " = " + r.resultado().latex(),
                (PizarraResolucion.Comprobacion) null));
        acomodar();
    }

    private void acomodar() {
        ventana.getScene().getRoot().applyCss();
        ventana.getScene().getRoot().layout();
    }

    private static boolean seVe(Node n) {
        for (Node x = n; x != null; x = x.getParent()) {
            if (!x.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private List<ToggleButton> verDesarrollo() {
        return pizarra.lookupAll(".pizarra-ver-desarrollo").stream().map(n -> (ToggleButton) n).toList();
    }

    @Test
    void tresFilasALaVistaYCuatroPlegadasAunqueCambieElAncho() throws Exception {
        enFx(() -> {
            mostrarLimite("lim(x->1, (x^3 - x)/(x - 1))");
            assertEquals(List.of(), verDesarrollo(), "3 filas: formato A, sin control");
            mostrarLimite("lim(x->1, (x^4 - 1)/(x - 1))");
            assertEquals(1, verDesarrollo().size(), "4 filas: formato B");
            ventana.setWidth(420);
            acomodar();
            assertEquals(1, verDesarrollo().size(), "el mismo formato con la pizarra angosta");
        });
    }

    @Test
    void enTodosLosPasosEmpiezaCerradoYAbreEnElMismoLugar() throws Exception {
        enFx(() -> {
            mostrarLimite("lim(x->2, (x^3 - 3x - 2)/(x - 2))");
            ToggleButton ver = verDesarrollo().getFirst();
            assertEquals("Ver desarrollo", ver.getText());
            assertFalse(seVe(pizarra.lookup(".pizarra-desarrollo-aux")));
            assertFalse(seVe(pizarra.lookup(".ruffini")), "la tabla va dentro del desplegable");
            ver.fire();
            acomodar();
            assertEquals("Ocultar desarrollo", ver.getText());
            assertTrue(seVe(pizarra.lookup(".pizarra-desarrollo-aux")));
            assertTrue(seVe(pizarra.lookup(".ruffini")));
            assertFalse(seVe(pizarra.comprobacion()), "un límite termina en el resultado");
        });
    }

    @Test
    void pasoAPasoLoAbreAlLlegarYDespuesRespetaAlAlumno() throws Exception {
        enFx(() -> {
            mostrarLimite("lim(x->2, (x^3 - 3x - 2)/(x - 2))");
            pizarra.botonModo().fire(); // Paso a paso, en el paso 1
            ToggleButton ver = verDesarrollo().getFirst();
            assertFalse(ver.isSelected(), "todavía no se llegó a la factorización");
            pizarra.siguiente().fire(); // paso 2: factorizar
            assertTrue(ver.isSelected(), "se abre al llegar por primera vez");
            ver.fire(); // el alumno lo pliega
            pizarra.siguiente().fire();
            pizarra.anterior().fire(); // vuelve al paso 2
            assertFalse(ver.isSelected(), "respeta que lo plegó");
        });
    }

    @Test
    void unEjercicioNuevoNoHeredaLoAbierto() throws Exception {
        enFx(() -> {
            mostrarLimite("lim(x->2, (x^3 - 3x - 2)/(x - 2))");
            verDesarrollo().getFirst().fire();
            mostrarLimite("lim(x->1, (x^4 - 1)/(x - 1))");
            assertFalse(verDesarrollo().getFirst().isSelected());
            pizarra.mostrar(new PizarraResolucion.Contenido("FACTORIZA", "x^{2}-1", List.of(),
                    "\\left(x+1\\right)\\left(x-1\\right)",
                    new PizarraResolucion.Comprobacion("Por expansión", "Ver expansión", List.of(),
                            PizarraResolucion.Comprobacion.Estado.COINCIDE, "Coincide con la entrada",
                            List.of(new CadenaIgualdades.Eslabon("\\left(x+1\\right)\\left(x-1\\right)"),
                                    new CadenaIgualdades.Eslabon("x^{2} - x + x - 1"),
                                    new CadenaIgualdades.Eslabon("x^{2} - 1")), null, List.of())));
            acomodar();
            pizarra.verDetalle().fire();
            pizarra.mostrar(new PizarraResolucion.Contenido("FACTORIZA", "x^{2}-4", List.of(),
                    "\\left(x+2\\right)\\left(x-2\\right)",
                    new PizarraResolucion.Comprobacion("Por expansión", "Ver expansión", List.of(),
                            PizarraResolucion.Comprobacion.Estado.COINCIDE, "Coincide con la entrada",
                            List.of(new CadenaIgualdades.Eslabon("\\left(x+2\\right)\\left(x-2\\right)"),
                                    new CadenaIgualdades.Eslabon("x^{2} - 4")), null, List.of())));
            acomodar();
            assertFalse(pizarra.verDetalle().isSelected(), "la comprobación empieza cerrada");
            assertEquals("Ver comprobación", pizarra.verDetalle().getText());
            assertFalse(seVe(pizarra.detalleComprobacion()));
        });
    }
}
