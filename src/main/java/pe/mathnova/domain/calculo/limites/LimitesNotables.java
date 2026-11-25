package pe.mathnova.domain.calculo.limites;

import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.numeros.NumeroAlgebraico;

final class LimitesNotables {

    record Notable(NumeroAlgebraico valor, String forma, String detalle) {
    }

    private LimitesNotables() {
    }

    static Optional<Notable> buscar(Expresion f, char x, boolean enCero, boolean alInfinito) {
        Expresion g = Simplificador.simplificar(f);
        if (alInfinito) {
            return numeroE(g, x);
        }
        if (!enCero || !(g instanceof Cociente c)) {
            return Optional.empty();
        }
        Escalado arriba = escalado(c.numerador());
        Escalado abajo = escalado(c.denominador());
        Optional<Racional[]> den1 = monomio(abajo.resto, x).filter(m -> m[1].equals(Racional.UNO));
        Optional<Racional[]> den2 = monomio(abajo.resto, x).filter(m -> m[1].equals(Racional.de(2)));
        Racional q = arriba.factor.dividir(abajo.factor);
        Expresion n = arriba.resto;

        if (n instanceof Funcion fn && (fn.tipo() == TipoFuncion.SEN || fn.tipo() == TipoFuncion.TAN)
                && den1.isPresent()) {
            Optional<Racional> k = lineal(fn.argumento(), x);
            if (k.isPresent()) {
                String nombre = fn.tipo().nombre();
                return Optional.of(resultado(q, k.get(), den1.get()[0], nombre + " u/u → 1 cuando u → 0", nombre, x));
            }
        }
        if (abajo.resto instanceof Funcion fd && fd.tipo() == TipoFuncion.SEN) {
            Optional<Racional[]> num1 = monomio(n, x).filter(m -> m[1].equals(Racional.UNO));
            Optional<Racional> k = lineal(fd.argumento(), x);
            if (num1.isPresent() && k.isPresent()) {
                Racional v = q.multiplicar(num1.get()[0]).dividir(k.get());
                return Optional.of(new Notable(NumeroAlgebraico.de(v), "u/sen u → 1 cuando u → 0",
                        "Con u = " + textoU(k.get(), x) + ": el cociente vale " + v + "·u/sen u → " + v + "·1 = " + v));
            }
        }
        if (n instanceof Suma s && s.terminos().size() == 2 && den2.isPresent()) {
            Optional<Racional> k = unoMenosCoseno(s, x);
            if (k.isPresent()) {
                Racional c2 = den2.get()[0];
                Racional v = q.multiplicar(k.get().multiplicar(k.get())).dividir(c2.multiplicar(Racional.de(2)));
                return Optional.of(new Notable(NumeroAlgebraico.de(v), "(1 − cos u)/u² → 1/2 cuando u → 0",
                        "Con u = " + textoU(k.get(), x) + ": el cociente vale " + q.multiplicar(k.get().multiplicar(k.get()))
                                .dividir(c2) + "·(1 − cos u)/u² → " + q.multiplicar(k.get().multiplicar(k.get())).dividir(c2)
                                + "·1/2 = " + v));
            }
        }
        if (n instanceof Suma s && s.terminos().size() == 2 && den1.isPresent()) {
            Optional<Racional> k = exponencialMenosUno(s, x);
            if (k.isPresent()) {
                return Optional.of(resultado(q, k.get(), den1.get()[0], "(eᵘ − 1)/u → 1 cuando u → 0", "(e^u - 1)", x));
            }
        }
        if (n instanceof Funcion fn && fn.tipo() == TipoFuncion.LN && den1.isPresent()
                && fn.argumento() instanceof Suma s && s.terminos().size() == 2) {
            Optional<Racional> k = unoMas(s, x);
            if (k.isPresent()) {
                return Optional.of(resultado(q, k.get(), den1.get()[0], "ln(1 + u)/u → 1 cuando u → 0", "ln(1 + u)", x));
            }
        }
        return Optional.empty();
    }

    private static Notable resultado(Racional q, Racional k, Racional c, String forma, String nombre, char x) {
        Racional v = q.multiplicar(k).dividir(c);
        return new Notable(NumeroAlgebraico.de(v), forma, "Con u = " + textoU(k, x) + ": el cociente vale "
                + v + "·" + (nombre.startsWith("(") || nombre.startsWith("ln") ? nombre.replace("(e^u - 1)", "(eᵘ − 1)")
                : nombre + " u") + "/u → " + v + "·1 = " + v);
    }

    private static String textoU(Racional k, char x) {
        return k.equals(Racional.UNO) ? String.valueOf(x) : k.equals(Racional.UNO.negar()) ? "-" + x : k + "" + x;
    }

    private static Optional<Notable> numeroE(Expresion g, char x) {
        if (!(g instanceof Potencia p)) {
            return Optional.empty();
        }
        Optional<Racional> b = lineal(p.exponente(), x);
        if (b.isEmpty() || !(p.base() instanceof Suma s) || s.terminos().size() != 2) {
            return Optional.empty();
        }
        int uno = esUno(s.terminos().get(0)) ? 0 : esUno(s.terminos().get(1)) ? 1 : -1;
        if (uno < 0) {
            return Optional.empty();
        }
        Expresion resto = s.terminos().get(1 - uno);
        Escalado e = escalado(resto);
        if (!(e.resto instanceof Cociente q) || !(q.numerador() instanceof Constante a)) {
            return Optional.empty();
        }
        Optional<Racional[]> den = monomio(q.denominador(), x).filter(m -> m[1].equals(Racional.UNO));
        if (den.isEmpty()) {
            return Optional.empty();
        }
        Racional coef = e.factor.multiplicar(a.valor()).dividir(den.get()[0]);
        Racional exponente = coef.multiplicar(b.get());
        NumeroAlgebraico v = Exactos.exp(NumeroAlgebraico.de(exponente));
        String detalle = coef.equals(Racional.UNO) && b.get().equals(Racional.UNO)
                ? "Es exactamente la definición del número e"
                : "(1 + " + coef + "/" + x + ")^(" + b.get() + x + ") = [(1 + 1/u)ᵘ]^(" + exponente + ") con u = "
                        + Racional.UNO.dividir(coef) + x + ", y tiende a e^(" + exponente + ")";
        return Optional.of(new Notable(v, "(1 + 1/u)ᵘ → e cuando u → ∞", detalle));
    }

    private record Escalado(Racional factor, Expresion resto) {
    }

    private static Escalado escalado(Expresion e) {
        return switch (e) {
            case Negacion n -> {
                Escalado d = escalado(n.argumento());
                yield new Escalado(d.factor.negar(), d.resto);
            }
            case Producto p when p.factores().getFirst() instanceof Constante c -> {
                List<Expresion> resto = p.factores().subList(1, p.factores().size());
                yield new Escalado(c.valor(), resto.size() == 1 ? resto.getFirst() : new Producto(resto));
            }
            case Cociente q when q.denominador() instanceof Constante d && !d.valor().esCero() -> {
                Escalado a = escalado(q.numerador());
                yield new Escalado(a.factor.dividir(d.valor()), a.resto);
            }
            default -> new Escalado(Racional.UNO, e);
        };
    }

    private static Optional<Racional[]> monomio(Expresion e, char x) {
        try {
            Polinomio p = Polinomio.desde(e);
            if (p.terminos().size() != 1 || !p.variables().equals(java.util.Set.of(x))) {
                return Optional.empty();
            }
            var t = p.terminos().entrySet().iterator().next();
            Monomio m = t.getKey();
            return Optional.of(new Racional[] {t.getValue(), Racional.de(m.exponente(x))});
        } catch (NoEsPolinomioException | ArithmeticException noEs) {
            return Optional.empty();
        }
    }

    private static Optional<Racional> lineal(Expresion e, char x) {
        return monomio(e, x).filter(m -> m[1].equals(Racional.UNO)).map(m -> m[0]);
    }

    private static Optional<Racional> unoMenosCoseno(Suma s, char x) {
        Expresion a = s.terminos().get(0);
        Expresion b = s.terminos().get(1);
        if (!esUno(a)) {
            Expresion t = a;
            a = b;
            b = t;
        }
        if (esUno(a) && b instanceof Negacion n && n.argumento() instanceof Funcion f && f.tipo() == TipoFuncion.COS) {
            return lineal(f.argumento(), x);
        }
        return Optional.empty();
    }

    private static Optional<Racional> exponencialMenosUno(Suma s, char x) {
        for (int i = 0; i < 2; i++) {
            Expresion a = s.terminos().get(i);
            Expresion b = s.terminos().get(1 - i);
            if (b instanceof Negacion n && esUno(n.argumento()) || b instanceof Constante c
                    && c.valor().equals(Racional.UNO.negar())) {
                if (a instanceof Potencia p && p.base() instanceof ConstanteEspecial e
                        && e.tipo() == ConstanteEspecial.Tipo.E) {
                    return lineal(p.exponente(), x);
                }
                if (a instanceof Funcion f && f.tipo() == TipoFuncion.EXP) {
                    return lineal(f.argumento(), x);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Racional> unoMas(Suma s, char x) {
        for (int i = 0; i < 2; i++) {
            if (esUno(s.terminos().get(i))) {
                return lineal(s.terminos().get(1 - i), x);
            }
        }
        return Optional.empty();
    }

    private static boolean esUno(Expresion e) {
        return e instanceof Constante c && c.valor().equals(Racional.UNO);
    }
}
