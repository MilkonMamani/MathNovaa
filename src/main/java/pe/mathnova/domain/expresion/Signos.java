package pe.mathnova.domain.expresion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;

/**
 * El signo con el que se escribe un término, decidido por la forma del árbol y no por el texto. Lo usan las
 * impresoras de pizarra y de LaTeX para que una suma escriba un solo operador: x + (−3) se lee "x − 3",
 * x + (−2x) se lee "x − 2x" y x − (−3) se lee "x + 3".
 *
 * <p>Solo cambia la presentación: el árbol que entra no se modifica y lo que devuelve tiene el mismo valor
 * absoluto. Una suma nunca se considera negativa por su primer término (x − (a + b) conserva el paréntesis), y
 * los exponentes no se tocan (x⁻², (−x)²).
 */
public final class Signos {

    private Signos() {
    }

    /**
     * Si el término se escribe con un menos delante, el término sin ese menos:
     * <ul>
     *   <li>−u → u</li>
     *   <li>−3 → 3</li>
     *   <li>(−2)·x → 2x; (−1)·x → x</li>
     *   <li>(−3)/(x + 1) → 3/(x + 1), con el numerador negativo según estas mismas reglas</li>
     * </ul>
     * Vacío si el término se escribe sin menos delante.
     */
    public static Optional<Expresion> sinMenos(Expresion t) {
        return switch (t) {
            case Negacion n -> Optional.of(n.argumento());
            case Constante c when c.valor().signo() < 0 -> Optional.of(new Constante(c.valor().negar()));
            case Producto p when p.factores().getFirst() instanceof Constante k && k.valor().signo() < 0 ->
                    Optional.of(conCoeficiente(k.valor().negar(), p.factores().subList(1, p.factores().size())));
            case Cociente c when !(c.numerador() instanceof Suma) ->
                    sinMenos(c.numerador()).map(n -> new Cociente(n, c.denominador()));
            default -> Optional.empty();
        };
    }

    /**
     * Un término de una suma separado en su signo y lo que va después del operador.
     *
     * @param negativo si se escribe " − " delante; si no, " + "
     * @param valor    el término sin signo: −(−3) da (false, 3), −2x da (true, 2x)
     */
    public record Termino(boolean negativo, Expresion valor) {
    }

    /** El signo de un término quitando los menos que se cancelan: x − (−3) se lee x + 3. */
    public static Termino separar(Expresion t) {
        boolean negativo = false;
        Expresion valor = t;
        for (Optional<Expresion> s = sinMenos(valor); s.isPresent(); s = sinMenos(valor)) {
            negativo = !negativo;
            valor = s.get();
        }
        return new Termino(negativo, valor);
    }

    /** Si el término se escribe con un menos delante (ver {@link #sinMenos}). */
    public static boolean conMenos(Expresion t) {
        return sinMenos(t).isPresent();
    }

    /** k·resto, sin escribir el 1: 1·x es x. */
    private static Expresion conCoeficiente(Racional k, List<Expresion> resto) {
        List<Expresion> fs = new ArrayList<>();
        if (!k.equals(Racional.UNO) || resto.isEmpty()) {
            fs.add(new Constante(k));
        }
        fs.addAll(resto);
        return fs.size() == 1 ? fs.getFirst() : new Producto(fs);
    }
}
