package pe.mathnova.domain.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EscrituraTest {

    @Test
    void lasConfusionesTipicasVanEnLosDosSentidos() {
        assertEquals(List.of("x", "\\times"), TablaConfusiones.alternativas("x"));
        assertEquals(List.of("\\times", "x"), TablaConfusiones.alternativas("\\times"));
        assertEquals(List.of("2", "z"), TablaConfusiones.alternativas("2"));
        assertEquals(List.of("l", "1"), TablaConfusiones.alternativas("l"));
        assertEquals(List.of("5", "s"), TablaConfusiones.alternativas("5"));
        assertEquals(List.of("o", "0"), TablaConfusiones.alternativas("o"));
        assertEquals(List.of("-", TablaConfusiones.RAYA_FRACCION), TablaConfusiones.alternativas("-"));
        assertEquals(List.of("7"), TablaConfusiones.alternativas("7"), "sin confusión, solo él mismo");
    }

    @Test
    void enUnBotonSeVeElSimbolo() {
        assertEquals("×", TablaConfusiones.aLaVista("\\times"));
        assertEquals("−", TablaConfusiones.aLaVista("-"));
        assertEquals("x", TablaConfusiones.aLaVista("x"));
    }

    @Test
    void elegirCambiaElSimboloYCorreLosQueSiguen() {
        SimboloDudoso primera = new SimboloDudoso(1, "x", TablaConfusiones.alternativas("x"), 0.4);
        SimboloDudoso ultima = new SimboloDudoso(3, "x", TablaConfusiones.alternativas("x"), 0.5);
        ResultadoReconocimiento r = new ResultadoReconocimiento("2x+x", List.of(primera, ultima));

        ResultadoReconocimiento elegido = r.elegir(primera, "\\times");
        assertEquals("2\\times+x", elegido.latex());
        assertEquals(1, elegido.dudosos().size(), "el elegido deja de ser dudoso");
        SimboloDudoso corrida = elegido.dudosos().getFirst();
        assertEquals("x", elegido.latex().substring(corrida.indice(), corrida.indice() + 1));
        assertEquals(8, corrida.indice());
    }

    @Test
    void unComandoSeguidoDeUnaLetraSeSepara() {
        SimboloDudoso d = new SimboloDudoso(1, "x", TablaConfusiones.alternativas("x"), 0.4);
        ResultadoReconocimiento r = new ResultadoReconocimiento("2xy", List.of(d));
        assertEquals("2\\times y", r.elegir(d, "\\times").latex());
    }

    @Test
    void elegirLoMismoNoCambiaElLatex() {
        SimboloDudoso d = new SimboloDudoso(0, "x", TablaConfusiones.alternativas("x"), 0.4);
        ResultadoReconocimiento r = new ResultadoReconocimiento("x+1", List.of(d));
        ResultadoReconocimiento igual = r.elegir(d, "x");
        assertEquals("x+1", igual.latex());
        assertTrue(igual.dudosos().isEmpty());
    }

    @Test
    void unSimboloDeOtroResultadoNoSePuedeElegir() {
        ResultadoReconocimiento r = new ResultadoReconocimiento("x", List.of());
        SimboloDudoso ajeno = new SimboloDudoso(0, "x", List.of("x"), 0.1);
        assertThrows(IllegalArgumentException.class, () -> r.elegir(ajeno, "x"));
    }

    @Test
    void losTrazosSonListasDePuntosInmutables() {
        List<Punto> puntos = new java.util.ArrayList<>(List.of(new Punto(1, 2, 3)));
        Trazo t = new Trazo(puntos);
        puntos.add(new Punto(4, 5, 6));
        assertEquals(1, t.puntos().size());
    }
}
