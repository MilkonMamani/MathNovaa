package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;

class PantallaResolucionTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final ResolverService SERVICIO = new ResolverService(new FactorizacionService(MOTOR), MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private final PantallaResolucion p = new PantallaResolucion();

    private void escribir(String latex) {
        p.cambiar(PantallaResolucion.analizar(latex, SERVICIO));
    }

    private void resolver(boolean verificado) {
        int v = p.version();
        p.empezarCalculo();
        assertEquals(EstadoResolucion.CALCULANDO, p.estado());
        assertFalse(p.botonActivo());
        p.resultado(v, verificado);
    }

    @Test
    void alEmpezarEstaVacio() {
        assertEquals(EstadoResolucion.VACIO, p.estado());
        assertFalse(p.botonActivo());
        assertNull(p.mensaje());
        assertFalse(p.atenuado());
        assertFalse(p.etiquetaVisible());
    }

    @Test
    void resolverDejaTodoActual() {
        escribir("x^2-5x+6");
        assertEquals(EstadoResolucion.ESCRIBIENDO, p.estado());
        assertTrue(p.botonActivo());
        resolver(true);
        assertEquals(EstadoResolucion.RESUELTO, p.estado());
        assertTrue(p.verificado());
        assertTrue(p.etiquetaVisible());
        assertFalse(p.atenuado());
    }

    @Test
    void alCambiarElTextoSeAtenua() {
        escribir("x^2-5x+6");
        resolver(true);

        escribir("x^2-5x+");
        assertFalse(p.verificado());
        assertFalse(p.etiquetaVisible());
        assertTrue(p.atenuado());
        assertEquals(EstadoResolucion.ESCRIBIENDO, p.estado());
        p.confirmar(p.version());
        assertEquals(EstadoResolucion.ERROR, p.estado());
        assertNotNull(p.mensaje());
        assertFalse(p.botonActivo());
        assertTrue(p.atenuado());
    }

    @Disabled("RF71 retirado: ya no hay línea de resultado en vivo bajo el editor")
    @Test
    void unCalculoEnVivoViejoSeDescarta() {
    }

    @Test
    void elResultadoQueLlegaTardeQuedaAtenuado() {
        escribir("x^2-1");
        int v = p.version();
        p.empezarCalculo();
        escribir("x^2-4");
        p.resultado(v, true);
        assertEquals(EstadoResolucion.ESCRIBIENDO, p.estado());
        assertTrue(p.atenuado());
        assertFalse(p.verificado());
        assertTrue(p.botonActivo());
    }

    @Test
    void incompletoDiceQueCasillaFalta() {
        escribir("x^{\\placeholder{}}");
        assertEquals(EstadoResolucion.INCOMPLETO, p.estado());
        assertEquals("Completa el exponente", p.mensaje());
        assertEquals(0, p.casilla());
        assertFalse(p.botonActivo());
        escribir("\\int_{1}^{\\placeholder{}}x\\,dx");
        assertEquals(TraductorLatex.OTRO_LIMITE, p.mensaje());
        assertEquals(0, p.casilla());
    }

    @Test
    void elMismoTextoNoCuentaComoCambio() {
        escribir("x^2-5x+6");
        resolver(true);
        assertFalse(p.cambiar(PantallaResolucion.analizarTexto("x^2 - 5x + 6", SERVICIO)));
        assertEquals(EstadoResolucion.RESUELTO, p.estado());
        assertTrue(p.verificado());
    }

    @Test
    void unFalloEsError() {
        escribir("x^2-5x+6");
        int v = p.version();
        p.empezarCalculo();
        p.fallo(v, "No se pudo resolver: algo");
        assertEquals(EstadoResolucion.ERROR, p.estado());
        assertEquals("No se pudo resolver: algo", p.falloCalculo());
        assertNull(p.mensaje(), "el fallo va en la tarjeta, no bajo el editor");
        assertFalse(p.etiquetaVisible());
    }

    @Test
    void elCampoSoloImportaParaFactorizarSimplificarYEcuaciones() {
        assertTrue(ResolucionController.campoImporta(SERVICIO.decidir("x^4 + 1")));
        assertTrue(ResolucionController.campoImporta(SERVICIO.decidir("(x^2 - 1)/(x + 1)")));
        assertTrue(ResolucionController.campoImporta(SERVICIO.decidir("x^2 + 1 = 0")));
        assertFalse(ResolucionController.campoImporta(SERVICIO.decidir("lim(x→2, (x^2 - 4)/(x - 2))")));
        assertFalse(ResolucionController.campoImporta(SERVICIO.decidir("d/dx(x^2)")));
        assertFalse(ResolucionController.campoImporta(SERVICIO.decidir("∫(x, x)")));
        assertFalse(ResolucionController.campoImporta(SERVICIO.decidir("∬(x y, x, 0, 1, y, 0, 1)")));
    }
}
