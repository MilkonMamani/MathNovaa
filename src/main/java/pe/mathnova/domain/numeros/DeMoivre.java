package pe.mathnova.domain.numeros;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;

/**
 * Raíces de xⁿ = c con la fórmula de De Moivre: x_k = ⁿ√|c|·(cos θ_k + i·sen θ_k), con
 * θ_k = (θ + 2kπ)/n y θ = 0 si c > 0, π si c < 0. Los cosenos y senos se dan exactos cuando el
 * ángulo es múltiplo de 15° (π/12); si no, no hay forma exacta en esta representación.
 */
public final class DeMoivre {

    /**
     * @param angulo θ_k en unidades de π (θ_k = angulo·π)
     */
    public record Raiz(int k, Racional angulo, NumeroAlgebraico valor) {
    }

    private DeMoivre() {
    }

    /** Las n raíces en orden de k; vacío si algún ángulo no es múltiplo de π/12. */
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

    /** cos(a·π) exacto si a es múltiplo de 1/12. */
    public static Optional<NumeroAlgebraico> coseno(Racional a) {
        Racional doce = a.multiplicar(Racional.de(12));
        if (!doce.esEntero()) {
            return Optional.empty();
        }
        int m = Math.floorMod(doce.num().intValue(), 24);
        if (m > 12) {
            m = 24 - m; // cos(−x) = cos x
        }
        boolean negativo = m > 6;
        if (negativo) {
            m = 12 - m; // cos(π − x) = −cos x
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

    /** "π/3", "π", "5π/4", "0". */
    public static String angulo(Racional a) {
        if (a.esCero()) {
            return "0";
        }
        String num = a.num().equals(java.math.BigInteger.ONE) ? "π" : a.num() + "π";
        return a.esEntero() ? num : num + "/" + a.den();
    }
}
