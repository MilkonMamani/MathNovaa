package pe.mathnova.presentation.pizarra;

import java.util.List;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * Lo que dibuja {@link AspaDoble} (M05). Para x² + 3xy + 2y² + 4x + 5y + 3:
 * <ul>
 *   <li>columnas x | y | independiente: [x, y, 1] y [x, 2y, 3]</li>
 *   <li>extremos: "x² = x·x", "2y² = y·2y" y "3 = 1·3"</li>
 *   <li>aspas: "x·2y + x·y = 3xy ✓", "y·3 + 2y·1 = 5y ✓" y "x·3 + x·1 = 4x ✓"</li>
 *   <li>resultado "(x + y + 1)(x + 2y + 3)"</li>
 * </ul>
 */
public record ModeloAspaDobleXY(List<String> titulos, List<String> fila1, List<String> fila2, List<String> extremos,
                                List<String> aspas, String resultado) {

    public static ModeloAspaDobleXY de(TrabajoIntermedio.AspaDoble a) {
        char x = a.x();
        char y = a.y();
        List<String> fila1 = List.of(t(a.a1(), x), t(a.c1(), y), FormateadorTexto.numero(a.f1()));
        List<String> fila2 = List.of(t(a.a2(), x), t(a.c2(), y), FormateadorTexto.numero(a.f2()));
        List<String> extremos = List.of(
                FormateadorTexto.termino(a.a1().multiplicar(a.a2()), x, 2) + " = " + p(a.a1(), x) + "·" + p(a.a2(), x),
                FormateadorTexto.termino(a.c1().multiplicar(a.c2()), y, 2) + " = " + p(a.c1(), y) + "·" + p(a.c2(), y),
                FormateadorTexto.numero(a.f1().multiplicar(a.f2())) + " = " + FormateadorTexto.numeroEnParentesis(a.f1())
                        + "·" + FormateadorTexto.numeroEnParentesis(a.f2()));
        Racional xy = a.a1().multiplicar(a.c2()).sumar(a.a2().multiplicar(a.c1()));
        Racional ey = a.c1().multiplicar(a.f2()).sumar(a.c2().multiplicar(a.f1()));
        Racional dx = a.a1().multiplicar(a.f2()).sumar(a.a2().multiplicar(a.f1()));
        List<String> aspas = List.of(
                p(a.a1(), x) + "·" + p(a.c2(), y) + " + " + p(a.a2(), x) + "·" + p(a.c1(), y) + " = "
                        + FormateadorTexto.termino(xy, x, 1) + y + " ✓  (aspa xy)",
                p(a.c1(), y) + "·" + FormateadorTexto.numeroEnParentesis(a.f2()) + " + " + p(a.c2(), y) + "·"
                        + FormateadorTexto.numeroEnParentesis(a.f1()) + " = " + FormateadorTexto.termino(ey, y, 1)
                        + " ✓  (aspa y)",
                p(a.a1(), x) + "·" + FormateadorTexto.numeroEnParentesis(a.f2()) + " + " + p(a.a2(), x) + "·"
                        + FormateadorTexto.numeroEnParentesis(a.f1()) + " = " + FormateadorTexto.termino(dx, x, 1)
                        + " ✓  (aspa x, de los extremos)");
        String resultado = "(" + trinomio(a.a1(), x, a.c1(), y, a.f1()) + ")(" + trinomio(a.a2(), x, a.c2(), y, a.f2()) + ")";
        return new ModeloAspaDobleXY(List.of(String.valueOf(x), String.valueOf(y), "indep."), fila1, fila2, extremos,
                aspas, resultado);
    }

    private static String t(Racional c, char v) {
        return FormateadorTexto.termino(c, v, 1);
    }

    private static String p(Racional c, char v) {
        return FormateadorTexto.terminoEnParentesis(c, v, 1);
    }

    /** "x + 2y + 3", "2x − y − 1". */
    static String trinomio(Racional a, char x, Racional c, char y, Racional f) {
        StringBuilder sb = new StringBuilder();
        agregar(sb, a, String.valueOf(x));
        agregar(sb, c, String.valueOf(y));
        agregar(sb, f, "");
        return sb.isEmpty() ? "0" : sb.toString();
    }

    private static void agregar(StringBuilder sb, Racional c, String letra) {
        if (c.esCero()) {
            return;
        }
        Racional abs = c.signo() < 0 ? c.negar() : c;
        String cuerpo = letra.isEmpty() ? abs.toString() : (abs.equals(Racional.UNO) ? "" : abs.toString()) + letra;
        if (sb.isEmpty()) {
            sb.append(c.signo() < 0 ? FormateadorTexto.MENOS : "").append(cuerpo);
        } else {
            sb.append(c.signo() < 0 ? " " + FormateadorTexto.MENOS + " " : " + ").append(cuerpo);
        }
    }
}
