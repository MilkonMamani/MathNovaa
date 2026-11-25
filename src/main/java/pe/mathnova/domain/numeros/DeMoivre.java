package pe.mathnova.domain.numeros;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;

public final class DeMoivre {

    public record Raiz(int k, Racional angulo, NumeroAlgebraico valor) {
    }

    private DeMoivre() {
    }

    public static Optional<List<Raiz>> raices(int n, Racional c) {
        if (c.esCero() || n < 1) {
            return Optional.empty();
        }
        Racional modulo = c.signo() < 0 ? c.negar() : c;
        NumeroAlgebraico rho = NumeroAlgebraico.raiz(n, modulo);
        int base = c.signo() < 0 ? 1 : 0;
        List<Raiz> r = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            Racional angulo = Racional.de(base + 2L * k, n);
            Optional<NumeroAlgebraico> cos = coseno(angulo);
            Optional<NumeroAlgebraico> sen = coseno(Racional.de(1, 2).restar(angulo));
            if (cos.isEmpty() || sen.isEmpty()) {
                return Optional.empty();
            }
            r.add(new Raiz(k, angulo, rho.multiplicar(cos.get().sumar(NumeroAlgebraico.I.multiplicar(sen.get())))));
        }
        return Optional.of(r);
    }

    public static Optional<NumeroAlgebraico> coseno(Racional a) {
        Racional doce = a.multiplicar(Racional.de(12));
        if (!doce.esEntero()) {
            return Optional.empty();
        }
        int m = Math.floorMod(doce.num().intValue(), 24);
        if (m > 12) {
            m = 24 - m;
        }
        boolean negativo = m > 6;
        if (negativo) {
            m = 12 - m;
        }
        NumeroAlgebraico v = switch (m) {
            case 0 -> NumeroAlgebraico.UNO;
            case 1 -> NumeroAlgebraico.raiz(2, 6).sumar(NumeroAlgebraico.raiz(2, 2)).multiplicar(Racional.de(1, 4));
            case 2 -> NumeroAlgebraico.raiz(2, 3).multiplicar(Racional.de(1, 2));
            case 3 -> NumeroAlgebraico.raiz(2, 2).multiplicar(Racional.de(1, 2));
            case 4 -> NumeroAlgebraico.de(Racional.de(1, 2));
            case 5 -> NumeroAlgebraico.raiz(2, 6).restar(NumeroAlgebraico.raiz(2, 2)).multiplicar(Racional.de(1, 4));
            default -> NumeroAlgebraico.CERO;
        };
        return Optional.of(negativo ? v.negar() : v);
    }

    public static String angulo(Racional a) {
        if (a.esCero()) {
            return "0";
        }
        String num = a.num().equals(java.math.BigInteger.ONE) ? "π" : a.num() + "π";
        return a.esEntero() ? num : num + "/" + a.den();
    }
}
