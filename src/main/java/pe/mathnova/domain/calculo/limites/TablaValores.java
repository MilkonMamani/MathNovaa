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

public record TablaValores(char variable, List<Fila> izquierda, List<Fila> derecha) {

    public static final int DECIMALES = 6;
    private static final MathContext MC = new MathContext(30);

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
