package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Qué se ve en la Pizarra: notas ocultas por defecto, el interruptor y el modo paso a paso. */
class EstadoPizarraTest {

    @Test
    void lasNotasEmpiezanOcultasYSoloSeVeElBotonDeAyuda() {
        EstadoPizarra e = new EstadoPizarra(false);
        e.nuevo(4);
        for (int i = 1; i <= 4; i++) {
            assertFalse(e.notaVisible(i));
            assertTrue(e.botonNotaVisible(i));
        }
    }

    @Test
    void unaNotaSeAbreSolaYSeCierraConOcultar() {
        EstadoPizarra e = new EstadoPizarra(false);
        e.nuevo(4);
        e.alternarNota(2);
        assertTrue(e.notaVisible(2));
        assertTrue(e.ocultarVisible(2));
        assertFalse(e.botonNotaVisible(2));
        assertFalse(e.notaVisible(1));
        assertFalse(e.notaVisible(3));
        e.alternarNota(2);
        assertFalse(e.notaVisible(2));
    }

    @Test
    void elInterruptorMuestraTodasSinOcultarYLimpiaLasAperturas() {
        EstadoPizarra e = new EstadoPizarra(false);
        e.nuevo(4);
        e.alternarNota(3);
        e.setExplicaciones(true);
        for (int i = 1; i <= 4; i++) {
            assertTrue(e.notaVisible(i));
            assertFalse(e.ocultarVisible(i), "con el interruptor no hay \"Ocultar\"");
            assertFalse(e.botonNotaVisible(i));
        }
        e.alternarNota(1); // no hace nada con el interruptor encendido
        e.setExplicaciones(false);
        for (int i = 1; i <= 4; i++) {
            assertFalse(e.notaVisible(i), "la 3, abierta antes del interruptor, también se cierra");
        }
    }

    @Test
    void pasoAPaso() {
        EstadoPizarra e = new EstadoPizarra(false);
        e.nuevo(4);
        assertEquals("Todos los pasos", e.etiqueta());
        assertEquals("Paso a paso", e.textoModo());
        assertTrue(e.resultadoVisible());

        e.pasoAPaso();
        assertEquals("Paso 1 de 4", e.etiqueta());
        assertEquals("Ver todo", e.textoModo());
        assertFalse(e.puedeAnterior());
        assertFalse(e.resultadoVisible());
        e.siguiente();
        assertEquals("Paso 2 de 4", e.etiqueta());
        assertTrue(e.pasoVisible(2));
        assertFalse(e.pasoVisible(3));
        assertFalse(e.pasoVisible(4));
        assertFalse(e.resultadoVisible());
        e.siguiente();
        e.siguiente();
        assertEquals("Paso 4 de 4", e.etiqueta());
        assertFalse(e.puedeSiguiente());
        assertTrue(e.resultadoVisible());
        e.siguiente();
        assertEquals("Paso 4 de 4", e.etiqueta());
        e.anterior();
        assertEquals("Paso 3 de 4", e.etiqueta());

        e.verTodo();
        assertEquals("Todos los pasos", e.etiqueta());
        assertTrue(e.pasoVisible(4));
        assertTrue(e.resultadoVisible());
    }

    @Test
    void unaNotaDeUnPasoQueNoSeVeTampocoSeVe() {
        EstadoPizarra e = new EstadoPizarra(true);
        e.nuevo(3);
        e.pasoAPaso();
        assertTrue(e.notaVisible(1));
        assertFalse(e.notaVisible(2));
        assertFalse(e.botonNotaVisible(2));
    }

    @Test
    void unResultadoNuevoVuelveATodosLosPasos() {
        EstadoPizarra e = new EstadoPizarra(false);
        e.nuevo(4);
        e.pasoAPaso();
        e.alternarNota(1);
        e.nuevo(2);
        assertEquals("Todos los pasos", e.etiqueta());
        assertFalse(e.notaVisible(1));
    }

    @Test
    void sinPasosNoHayPasoAPaso() {
        EstadoPizarra e = new EstadoPizarra(false);
        e.nuevo(0);
        e.pasoAPaso();
        assertEquals("Todos los pasos", e.etiqueta());
        assertTrue(e.resultadoVisible());
    }
}
