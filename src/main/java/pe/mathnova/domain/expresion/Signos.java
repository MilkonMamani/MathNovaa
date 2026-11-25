package pe.mathnova.domain.expresion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;

public final class Signos {

    private Signos() {
    }

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

    public record Termino(boolean negativo, Expresion valor) {
    }

    public static Termino separar(Expresion t) {
        boolean negativo = false;
        Expresion valor = t;
        for (Optional<Expresion> s = sinMenos(valor); s.isPresent(); s = sinMenos(valor)) {
            negativo = !negativo;
            valor = s.get();
        }
        return new Termino(negativo, valor);
    }

    public static boolean conMenos(Expresion t) {
        return sinMenos(t).isPresent();
    }

    private static Expresion conCoeficiente(Racional k, List<Expresion> resto) {
        List<Expresion> fs = new ArrayList<>();
        if (!k.equals(Racional.UNO) || resto.isEmpty()) {
            fs.add(new Constante(k));
        }
        fs.addAll(resto);
        return fs.size() == 1 ? fs.getFirst() : new Producto(fs);
    }
}
