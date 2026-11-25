package pe.mathnova.domain.calculo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.Variable;

public record FuncionRacional(Polinomio numerador, Polinomio denominador) {

    private static final int GRADO_MAXIMO = 60;

    public FuncionRacional {
        Objects.requireNonNull(numerador, "numerador");
        Objects.requireNonNull(denominador, "denominador");
        if (denominador.esCero()) {
            throw new ArithmeticException("División entre cero");
        }
    }

    public static FuncionRacional de(Polinomio p) {
        return new FuncionRacional(p, Polinomio.UNO);
    }

    public static Optional<FuncionRacional> de(Expresion e) {
        try {
            return Optional.ofNullable(convertir(e));
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static FuncionRacional convertir(Expresion e) {
        return switch (e) {
            case Constante c -> de(Polinomio.constante(c.valor()));
            case Variable v -> de(Polinomio.variable(v.nombre()));
            case Negacion n -> {
                FuncionRacional a = convertir(n.argumento());
                yield a == null ? null : new FuncionRacional(a.numerador.negar(), a.denominador);
            }
            case Suma s -> {
                FuncionRacional r = de(Polinomio.CERO);
                for (Expresion t : s.terminos()) {
                    FuncionRacional a = convertir(t);
                    if (a == null) {
                        yield null;
                    }
                    r = r.sumar(a);
                }
                yield r;
            }
            case Producto p -> {
                FuncionRacional r = de(Polinomio.UNO);
                for (Expresion f : p.factores()) {
                    FuncionRacional a = convertir(f);
                    if (a == null) {
                        yield null;
                    }
                    r = r.multiplicar(a);
                }
                yield r;
            }
            case Cociente c -> {
                FuncionRacional a = convertir(c.numerador());
                FuncionRacional b = convertir(c.denominador());
                yield a == null || b == null ? null : a.dividir(b);
            }
            case Potencia p when p.exponente() instanceof Constante k && k.valor().esEntero()
                    && k.valor().num().abs().bitLength() < 8 -> {
                FuncionRacional b = convertir(p.base());
                if (b == null) {
                    yield null;
                }
                int n = k.valor().num().intValueExact();
                FuncionRacional r = de(Polinomio.UNO);
                for (int i = 0; i < Math.abs(n); i++) {
                    r = r.multiplicar(b);
                }
                yield n < 0 ? de(Polinomio.UNO).dividir(r) : r;
            }
            default -> null;
        };
    }

    private static void revisar(Polinomio p) {
        if (p.grado() > GRADO_MAXIMO) {
            throw new ArithmeticException("Grado demasiado alto");
        }
    }

    public FuncionRacional sumar(FuncionRacional o) {
        if (denominador.equals(o.denominador)) {
            return new FuncionRacional(numerador.sumar(o.numerador), denominador);
        }
        Polinomio n = numerador.multiplicar(o.denominador).sumar(o.numerador.multiplicar(denominador));
        Polinomio d = denominador.multiplicar(o.denominador);
        revisar(n);
        revisar(d);
        return new FuncionRacional(n, d);
    }

    public FuncionRacional multiplicar(FuncionRacional o) {
        Polinomio n = numerador.multiplicar(o.numerador);
        Polinomio d = denominador.multiplicar(o.denominador);
        revisar(n);
        revisar(d);
        return new FuncionRacional(n, d);
    }

    public FuncionRacional dividir(FuncionRacional o) {
        if (o.numerador.esCero()) {
            throw new ArithmeticException("División entre cero");
        }
        return multiplicar(new FuncionRacional(o.denominador, o.numerador));
    }

    public boolean igual(FuncionRacional o) {
        return numerador.multiplicar(o.denominador).equals(o.numerador.multiplicar(denominador));
    }

    public boolean esPolinomio() {
        return denominador.esConstante();
    }

    public Optional<Racional> evaluar(char x, Racional v) {
        Racional d = denominador.evaluar(Map.of(x, v));
        if (d.esCero()) {
            return Optional.empty();
        }
        return Optional.of(numerador.evaluar(Map.of(x, v)).dividir(d));
    }

    public Expresion aExpresion() {
        return Simplificador.simplificar(new Cociente(numerador.aExpresion(), denominador.aExpresion()));
    }
}
