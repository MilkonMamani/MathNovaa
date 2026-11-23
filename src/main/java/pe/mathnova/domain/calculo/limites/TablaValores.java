package pe.mathnova.domain.calculo.limites;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;

/**
 * La forma numérica de "ver" un límite (pestaña Análisis): f evaluada en puntos que se acercan al
 * punto por cada lado. Es lo único del motor de límites que usa decimales (BigDecimal, 30 cifras): no
 * decide nada, solo muestra.
 * <ul>
 *   <li>x → a: a − 0.1, a − 0.01, a − 0.001 por la izquierda y a + 0.001, a + 0.01, a + 0.1 por la derecha</li>
 *   <li>x → ±∞: ±10, ±100, …, ±10⁶</li>
 * </ul>
 *
 * @param izquierda las filas que se acercan por la izquierda (vacía al infinito)
 * @param derecha   las que se acercan por la derecha, o hacia ±∞
 */
public record TablaValores(char variable, List<Fila> izquierda, List<Fila> derecha) {

    public static final int DECIMALES = 6;
    private static final MathContext MC = new MathContext(30);

    /** x y f(x) ya como texto: "1.999", "3.999000"; "no definida" si f no es real ahí. */
    public record Fila(String x, String valor) {
    }

    public TablaValores {
        izquierda = List.copyOf(izquierda);
        derecha = List.copyOf(derecha);
    }

    public static TablaValores de(Limite l) {
        char x = l.variable();
        List<Fila> izquierda = new ArrayList<>();
        List<Fila> derecha = new ArrayList<>();
        if (l.punto() instanceof Limite.Infinito inf) {
            for (int k = 1; k <= 6; k++) {
                BigDecimal v = BigDecimal.TEN.pow(k);
                derecha.add(fila(l, inf.positivo() ? v : v.negate()));
            }
            return new TablaValores(x, izquierda, derecha);
        }
        BigDecimal a = EvaluacionNumerica.evaluar(((Limite.Finito) l.punto()).valor(), Map.of(), MC).re();
        for (int k = 1; k <= 3; k++) {
            izquierda.add(fila(l, a.subtract(BigDecimal.ONE.movePointLeft(k))));
        }
        for (int k = 3; k >= 1; k--) {
            derecha.add(fila(l, a.add(BigDecimal.ONE.movePointLeft(k))));
        }
        return new TablaValores(x, izquierda, derecha);
    }

    private static Fila fila(Limite l, BigDecimal x) {
        String textoX = texto(x.scale() > DECIMALES ? x.setScale(DECIMALES, RoundingMode.HALF_UP) : x);
        try {
            ComplejoDecimal v = EvaluacionNumerica.evaluar(l.cuerpo(), Map.of(l.variable(), ComplejoDecimal.real(x)), MC);
            if (!v.esReal(20)) {
                return new Fila(textoX, "no definida");
            }
            return new Fila(textoX, texto(v.re().setScale(DECIMALES, RoundingMode.HALF_UP)));
        } catch (ArithmeticException | IllegalArgumentException e) {
            return new Fila(textoX, "no definida");
        }
    }

    private static String texto(BigDecimal d) {
        return d.toPlainString();
    }
}
