package pe.mathnova.presentation.pizarra;

import java.util.List;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * Lo que dibuja {@link AspaSimple}, como texto de pizarra. Para 6x² + x − 2:
 * izquierda [2x, 3x], derecha [−1, 2], productos ["2x·2 = 4x", "3x·(−1) = −3x"],
 * suma "4x − 3x = x ✓" y resultado "(2x − 1)(3x + 2)".
 *
 * <p>En el aspa homogénea (M04) la derecha lleva la segunda letra: para 2x² + 5xy − 3y² queda
 * izquierda [2x, x], derecha [−y, 3y], productos ["2x·3y = 6xy", "x·(−y) = −xy"] y "6xy − xy = 5xy ✓".
 */
public record ModeloAspa(List<String> izquierda, List<String> derecha, List<String> productos, String suma,
                         boolean sumaCorrecta, String resultado) {

    public static ModeloAspa de(TrabajoIntermedio.AspaSimple a) {
        char v = a.variable();
        char w = a.segunda();
        boolean homogenea = w != 0;
        String x1 = FormateadorTexto.termino(a.a1(), v, 1);
        String x2 = FormateadorTexto.termino(a.a2(), v, 1);
        // Productos cruzados: fila 1 izquierda × fila 2 derecha y fila 2 izquierda × fila 1 derecha.
        Racional p1 = a.a1().multiplicar(a.c2());
        Racional p2 = a.a2().multiplicar(a.c1());
        Racional b = p1.sumar(p2);
        String producto1 = x1 + "·" + derechaEnParentesis(a.c2(), w) + " = " + cruce(p1, v, w);
        String producto2 = x2 + "·" + derechaEnParentesis(a.c1(), w) + " = " + cruce(p2, v, w);
        String suma = cruce(p1, v, w) + (p2.signo() < 0 ? " " + FormateadorTexto.MENOS + " " : " + ")
                + cruce(p2.signo() < 0 ? p2.negar() : p2, v, w) + " = " + cruce(b, v, w) + " ✓";
        String resultado = homogenea
                ? "(" + ModeloAspaDobleXY.trinomio(a.a1(), v, a.c1(), w, Racional.CERO) + ")("
                        + ModeloAspaDobleXY.trinomio(a.a2(), v, a.c2(), w, Racional.CERO) + ")"
                : "(" + FormateadorTexto.suma(v, a.a1(), a.c1()) + ")(" + FormateadorTexto.suma(v, a.a2(), a.c2()) + ")";
        return new ModeloAspa(List.of(x1, x2), List.of(derecha(a.c1(), w), derecha(a.c2(), w)),
                List.of(producto1, producto2), suma, true, resultado);
    }

    private static String derecha(Racional c, char w) {
        return w == 0 ? FormateadorTexto.numero(c) : FormateadorTexto.termino(c, w, 1);
    }

    private static String derechaEnParentesis(Racional c, char w) {
        return w == 0 ? FormateadorTexto.numeroEnParentesis(c) : FormateadorTexto.terminoEnParentesis(c, w, 1);
    }

    /** "4x", o "6xy" en la homogénea. */
    private static String cruce(Racional c, char v, char w) {
        return FormateadorTexto.termino(c, v, 1) + (w == 0 ? "" : String.valueOf(w));
    }
}
