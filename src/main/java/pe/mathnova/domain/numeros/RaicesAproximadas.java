package pe.mathnova.domain.numeros;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pe.mathnova.domain.algebra.Racional;

public final class RaicesAproximadas {

    public static final int PRECISION = 50;
    public static final int DECIMALES = 10;
    private static final MathContext MC = new MathContext(PRECISION);
    private static final int ITERACIONES = 2000;

    private RaicesAproximadas() {
    }

    public static List<ComplejoDecimal> raices(List<Racional> coeficientes) {
        int n = coeficientes.size() - 1;
        if (n < 1) {
            return List.of();
        }
        ComplejoDecimal principal = ComplejoDecimal.de(coeficientes.getLast(), MC);
        List<ComplejoDecimal> a = new ArrayList<>();
        for (Racional c : coeficientes) {
            a.add(ComplejoDecimal.de(c, MC).dividir(principal, MC));
        }
        BigDecimal cota = BigDecimal.ONE;
        for (int k = 0; k < n; k++) {
            cota = cota.max(a.get(k).modulo(MC).add(BigDecimal.ONE));
        }
        ComplejoDecimal semilla = new ComplejoDecimal(new BigDecimal("0.4"), new BigDecimal("0.9"));
        List<ComplejoDecimal> z = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            z.add(semilla.potencia(k, MC).multiplicar(cota.min(BigDecimal.valueOf(4)), MC));
        }
        BigDecimal tolerancia = BigDecimal.ONE.movePointLeft(PRECISION - 8);
        for (int it = 0; it < ITERACIONES; it++) {
            BigDecimal cambio = BigDecimal.ZERO;
            for (int i = 0; i < n; i++) {
                ComplejoDecimal den = ComplejoDecimal.UNO;
                for (int j = 0; j < n; j++) {
                    if (j != i) {
                        den = den.multiplicar(z.get(i).restar(z.get(j), MC), MC);
                    }
                }
                if (den.modulo(MC).signum() == 0) {
                    den = new ComplejoDecimal(tolerancia, tolerancia);
                }
                ComplejoDecimal paso = evaluar(a, z.get(i)).dividir(den, MC);
                z.set(i, z.get(i).restar(paso, MC));
                cambio = cambio.max(paso.modulo(MC));
            }
            if (cambio.compareTo(tolerancia) < 0) {
                break;
            }
        }
        List<ComplejoDecimal> derivada = new ArrayList<>();
        for (int k = 1; k <= n; k++) {
            derivada.add(a.get(k).multiplicar(BigDecimal.valueOf(k), MC));
        }
        List<ComplejoDecimal> r = new ArrayList<>();
        for (ComplejoDecimal x : z) {
            for (int k = 0; k < 20; k++) {
                ComplejoDecimal d = evaluar(derivada, x);
                if (d.modulo(MC).signum() == 0) {
                    break;
                }
                x = x.restar(evaluar(a, x).dividir(d, MC), MC);
            }
            if (x.esReal(PRECISION - 20)) {
                x = ComplejoDecimal.real(x.re());
            }
            r.add(x);
        }
        r.sort(Comparator.comparing(ComplejoDecimal::re).thenComparing(ComplejoDecimal::im));
        return r;
    }

    public static List<BigDecimal> reales(List<Racional> coeficientes) {
        return raices(coeficientes).stream().filter(x -> x.esReal(PRECISION - 20)).map(ComplejoDecimal::re).toList();
    }

    public static String texto(BigDecimal x) {
        return x.setScale(DECIMALES, RoundingMode.HALF_UP).toPlainString();
    }

    public static String texto(ComplejoDecimal x) {
        if (x.esReal(PRECISION - 20)) {
            return texto(x.re());
        }
        BigDecimal b = x.im().setScale(DECIMALES, RoundingMode.HALF_UP);
        return texto(x.re()) + (b.signum() < 0 ? " - " : " + ") + b.abs().toPlainString() + "i";
    }

    private static ComplejoDecimal evaluar(List<ComplejoDecimal> a, ComplejoDecimal x) {
        ComplejoDecimal r = ComplejoDecimal.CERO;
        for (int i = a.size() - 1; i >= 0; i--) {
            r = r.multiplicar(x, MC).sumar(a.get(i), MC);
        }
        return r;
    }
}
