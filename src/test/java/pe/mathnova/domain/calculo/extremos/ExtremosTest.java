package pe.mathnova.domain.calculo.extremos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos.Clase;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos.Punto;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Extremos;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

class ExtremosTest {

    private static final MotorExtremos MOTOR = new MotorExtremos(new OrquestadorFactorizacion());

    private static ResolucionExtremos resolver(String texto) {
        return MOTOR.resolver((Extremos) Parser.parse(texto));
    }

    private static Racional q(long n) {
        return Racional.de(n);
    }

    private static Punto punto(ResolucionExtremos r, long x, long y) {
        return r.puntos().stream().filter(p -> p.coordenadas().equals(List.of(q(x), q(y)))).findFirst()
                .orElseThrow(() -> new AssertionError("falta (" + x + ", " + y + ") en " + r.texto()));
    }

    @Test
    void seLeenLosExtremos() {
        Extremos e = assertInstanceOf(Extremos.class, Parser.parse("extremos(x^2 + y^2 - 2x)"));
        assertNull(e.restriccion());
        Extremos l = assertInstanceOf(Extremos.class, Parser.parse("extremos(xy; x + y = 10)"));
        assertAll(
                () -> assertTrue(l.conRestriccion()),
                () -> assertEquals("extremos(xy; x + y = 10)", ImpresoraTexto.imprimir(l)),
                () -> assertEquals(l, Parser.parse(ImpresoraTexto.imprimir(l))),
                () -> assertThrows(ErrorSintaxis.class, () -> Parser.parse("extremos(xy; x + y)")),
                () -> assertThrows(ErrorSintaxis.class, () -> Parser.parse("extremos()")),
                () -> assertThrows(ErrorSintaxis.class, () -> Parser.parse("1 + extremos(x y)")));
    }

    @Test
    void elEditorLoReconoce() {
        assertEquals("extremos(x^2 + y^2)", TraductorLatex.traducir("\\operatorname{extremos}\\left(x^2+y^2\\right)"));
    }

    @Test
    void unMinimo() {
        ResolucionExtremos r = resolver("extremos(x^2 + y^2 - 2x)");
        Punto p = punto(r, 1, 0);
        assertAll(r.texto(),
                () -> assertEquals(1, r.puntos().size()),
                () -> assertEquals(Clase.MINIMO, p.clase()),
                () -> assertEquals(q(4), p.d()),
                () -> assertEquals(q(2), p.fxx()),
                () -> assertEquals("(1, 0) mínimo local", r.texto()),
                () -> assertTrue(r.codigos().contains("V07")));
    }

    @Test
    void sillaYMinimo() {
        ResolucionExtremos r = resolver("extremos(x^3 + y^3 - 3xy)");
        Punto silla = punto(r, 0, 0);
        Punto minimo = punto(r, 1, 1);
        assertAll(r.texto(),
                () -> assertEquals(2, r.puntos().size()),
                () -> assertEquals(Clase.SILLA, silla.clase()),
                () -> assertEquals(q(-9), silla.d()),
                () -> assertEquals(Clase.MINIMO, minimo.clase()),
                () -> assertEquals(q(27), minimo.d()),
                () -> assertEquals(q(6), minimo.fxx()),
                () -> assertEquals("(0, 0) punto de silla; (1, 1) mínimo local", r.texto()));
        PasoLimite criticos = r.pasos().get(1);
        assertTrue(criticos.hijos().stream().anyMatch(h -> h.titulo().startsWith("Despejar")), criticos.hijos()
                .toString());
    }

    @Test
    void unaSilla() {
        ResolucionExtremos r = resolver("extremos(x^2 - y^2)");
        assertEquals(Clase.SILLA, punto(r, 0, 0).clase());
        assertEquals(q(-4), punto(r, 0, 0).d());
    }

    @Test
    void elCriterioNoDecide() {
        ResolucionExtremos r = resolver("extremos(x^4 + y^4)");
        Punto p = punto(r, 0, 0);
        assertAll(
                () -> assertEquals(1, r.puntos().size()),
                () -> assertEquals(Clase.NO_DECIDE, p.clase()),
                () -> assertEquals(q(0), p.d()),
                () -> assertEquals("(0, 0) el criterio no decide", r.texto()));
    }

    @Test
    void lagrangeConUnMaximo() {
        ResolucionExtremos r = resolver("extremos(xy; x + y = 10)");
        Punto p = punto(r, 5, 5);
        assertAll(r.texto(),
                () -> assertEquals(ResolucionExtremos.Tipo.LAGRANGE, r.tipo()),
                () -> assertEquals(1, r.puntos().size()),
                () -> assertEquals(q(5), p.lambda()),
                () -> assertEquals(q(25), p.valor()),
                () -> assertTrue(p.clase().esMaximo()),
                () -> assertEquals("(5, 5) máximo, f = 25", r.texto()),
                () -> assertTrue(r.codigos().contains("V08")));
    }

    @Test
    void lagrangeConUnMinimo() {
        ResolucionExtremos r = resolver("extremos(x^2 + y^2; x + 2y = 5)");
        Punto p = punto(r, 1, 2);
        assertAll(r.texto(),
                () -> assertEquals(q(2), p.lambda()),
                () -> assertEquals(q(5), p.valor()),
                () -> assertTrue(p.clase().esMinimo()),
                () -> assertEquals("(1, 2) mínimo, f = 5", r.texto()));
    }

    @Test
    void lagrangeConRestriccionCuadratica() {
        ResolucionExtremos r = resolver("extremos(x + y; x^2 + y^2 = 2)");
        assertAll(r.texto(),
                () -> assertEquals(2, r.puntos().size()),
                () -> assertTrue(punto(r, -1, -1).clase().esMinimo()),
                () -> assertTrue(punto(r, 1, 1).clase().esMaximo()));
    }

    @Test
    void soloDosVariablesYPolinomios() {
        assertThrows(IllegalArgumentException.class, () -> resolver("extremos(x^2 + 1)"));
        assertThrows(IllegalArgumentException.class, () -> resolver("extremos(sen(x) + y^2)"));
    }

    @Test
    void conRaicesIrracionalesSeAvisa() {
        ResolucionExtremos r = resolver("extremos(x^3 - 6x + y^2)");
        assertTrue(r.puntos().isEmpty());
        assertTrue(r.avisos().stream().anyMatch(a -> a.contains("irracionales")), r.avisos().toString());
    }
}
