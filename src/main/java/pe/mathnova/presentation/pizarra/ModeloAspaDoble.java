package pe.mathnova.presentation.pizarra;

import java.util.List;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * Lo que dibuja {@link AspaDobleEspecial}. Para x⁴ + 5x³ + 11x² + 14x + 4:
 * <ul>
 *   <li>columnas x² | x | independiente: [x², 3x, 1] y [x², 2x, 4]</li>
 *   <li>extremos: "x⁴ = x²·x²" y "4 = 1·4"</li>
 *   <li>"se tiene 1·x² + 4·x², faltan 6x²" y "6x² = 3x·2x"</li>
 *   <li>aspas laterales: "x²·2x + x²·3x = 5x³ ✓" y "3x·4 + 2x·1 = 14x ✓"</li>
 *   <li>resultado "(x² + 3x + 1)(x² + 2x + 4)"</li>
 * </ul>
 */
public record ModeloAspaDoble(List<String> fila1, List<String> fila2, List<String> extremos, String seTiene,
                              String central, List<String> laterales, String resultado) {

    public static ModeloAspaDoble de(TrabajoIntermedio.AspaDobleEspecial a) {
        char v = a.variable();
        List<String> fila1 = List.of(FormateadorTexto.termino(a.a1(), v, 2), FormateadorTexto.termino(a.p(), v, 1),
                FormateadorTexto.numero(a.e1()));
        List<String> fila2 = List.of(FormateadorTexto.termino(a.a2(), v, 2), FormateadorTexto.termino(a.q(), v, 1),
                FormateadorTexto.numero(a.e2()));
        Racional a4 = a.a1().multiplicar(a.a2());
        Racional a0 = a.e1().multiplicar(a.e2());
        List<String> extremos = List.of(
                FormateadorTexto.termino(a4, v, 4) + " = " + fila1.get(0) + "·" + fila2.get(0),
                FormateadorTexto.numero(a0) + " = " + FormateadorTexto.numeroEnParentesis(a.e1()) + "·"
                        + FormateadorTexto.numeroEnParentesis(a.e2()));

        // Lo que dan las aspas de los extremos en x²: a2·e1 + a1·e2.
        Racional t1 = a.a2().multiplicar(a.e1());
        Racional t2 = a.a1().multiplicar(a.e2());
        Racional tiene = t1.sumar(t2);
        Racional falta = a.coeficienteX2().restar(tiene);
        String seTiene = "se tiene " + FormateadorTexto.numeroEnParentesis(t1) + "·" + v + "² + "
                + FormateadorTexto.numeroEnParentesis(t2) + "·" + v + "², "
                + (falta.signo() >= 0 ? "faltan " : "sobran ")
                + FormateadorTexto.termino(falta.signo() >= 0 ? falta : falta.negar(), v, 2);
        String central = FormateadorTexto.termino(falta, v, 2) + " = " + FormateadorTexto.terminoEnParentesis(a.p(), v, 1)
                + "·" + FormateadorTexto.terminoEnParentesis(a.q(), v, 1);

        Racional b = a.a1().multiplicar(a.q()).sumar(a.a2().multiplicar(a.p()));
        Racional d = a.p().multiplicar(a.e2()).sumar(a.q().multiplicar(a.e1()));
        String x3 = fila1.get(0) + "·" + FormateadorTexto.terminoEnParentesis(a.q(), v, 1) + " + " + fila2.get(0) + "·"
                + FormateadorTexto.terminoEnParentesis(a.p(), v, 1) + " = " + FormateadorTexto.termino(b, v, 3) + " ✓";
        String x1 = FormateadorTexto.terminoEnParentesis(a.p(), v, 1) + "·" + FormateadorTexto.numeroEnParentesis(a.e2())
                + " + " + FormateadorTexto.terminoEnParentesis(a.q(), v, 1) + "·"
                + FormateadorTexto.numeroEnParentesis(a.e1()) + " = " + FormateadorTexto.termino(d, v, 1) + " ✓";
        String resultado = "(" + FormateadorTexto.suma(v, a.a1(), a.p(), a.e1()) + ")("
                + FormateadorTexto.suma(v, a.a2(), a.q(), a.e2()) + ")";
        return new ModeloAspaDoble(fila1, fila2, extremos, seTiene, central, List.of(x3, x1), resultado);
    }
}
