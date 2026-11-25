package pe.mathnova.domain.numeros;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.expresion.Visitante;

public final class EvaluacionNumerica {

    public static final int CIFRAS = 40;
    public static final int CIFRAS_IGUALES = 30;
    public static final MathContext MC = new MathContext(CIFRAS);
    public static final List<Racional> PUNTOS = List.of(Racional.de(1, 3), Racional.de(-5, 7),
            Racional.de(13, 11), Racional.de(17, 9), Racional.de(-29, 13));

    private EvaluacionNumerica() {
    }

    public static boolean iguales(Expresion a, Expresion b, char variable) {
        for (Racional p : PUNTOS) {
            Map<Character, ComplejoDecimal> x = Map.of(variable, ComplejoDecimal.de(p, MC));
            ComplejoDecimal va = evaluar(a, x, MC);
            ComplejoDecimal vb = evaluar(b, x, MC);
            if (!va.cercano(vb, CIFRAS_IGUALES, MC)) {
                return false;
            }
        }
        return true;
    }

    public static ComplejoDecimal evaluar(Expresion e, Map<Character, ComplejoDecimal> variables, MathContext mc) {
        return e.aceptar(new Evaluador(variables, mc));
    }

    public static ComplejoDecimal evaluar(NumeroAlgebraico z, MathContext mc) {
        return evaluar(z.aExpresion(), Map.of(), mc);
    }

    static int signo(NumeroAlgebraico z) {
        MathContext mc = new MathContext(60);
        BigDecimal v = evaluar(z, mc).re();
        return v.abs().compareTo(BigDecimal.ONE.movePointLeft(45)) < 0 ? 0 : v.signum();
    }

    private record Evaluador(Map<Character, ComplejoDecimal> variables, MathContext mc)
            implements Visitante<ComplejoDecimal> {

        @Override
        public ComplejoDecimal visitar(Constante c) {
            return ComplejoDecimal.de(c.valor(), mc);
        }

        @Override
        public ComplejoDecimal visitar(Variable v) {
            ComplejoDecimal x = variables.get(v.nombre());
            if (x == null) {
                throw new ArithmeticException("Sin valor para " + v.nombre());
            }
            return x;
        }

        @Override
        public ComplejoDecimal visitar(ConstanteEspecial c) {
            return switch (c.tipo()) {
                case E -> ComplejoDecimal.real(Decimales.exp(BigDecimal.ONE, mc));
                case PI -> ComplejoDecimal.real(Decimales.pi(mc));
                case I -> ComplejoDecimal.I;
            };
        }

        @Override
        public ComplejoDecimal visitar(Suma s) {
            ComplejoDecimal r = ComplejoDecimal.CERO;
            for (Expresion t : s.terminos()) {
                r = r.sumar(t.aceptar(this), mc);
            }
            return r;
        }

        @Override
        public ComplejoDecimal visitar(Producto p) {
            ComplejoDecimal r = ComplejoDecimal.UNO;
            for (Expresion f : p.factores()) {
                r = r.multiplicar(f.aceptar(this), mc);
            }
            return r;
        }

        @Override
        public ComplejoDecimal visitar(Cociente c) {
            return c.numerador().aceptar(this).dividir(c.denominador().aceptar(this), mc);
        }

        @Override
        public ComplejoDecimal visitar(Potencia p) {
            ComplejoDecimal b = p.base().aceptar(this);
            if (p.exponente() instanceof Constante k && k.valor().esEntero()) {
                return b.potencia(k.valor().num().intValueExact(), mc);
            }
            ComplejoDecimal e = p.exponente().aceptar(this);
            if (b.esReal(mc.getPrecision()) && b.re().signum() > 0 && e.esReal(mc.getPrecision())) {
                return ComplejoDecimal.real(Decimales.exp(e.re().multiply(Decimales.ln(b.re(), mc), mc), mc));
            }
            throw new ArithmeticException("Potencia no real");
        }

        @Override
        public ComplejoDecimal visitar(Negacion n) {
            return n.argumento().aceptar(this).negar();
        }

        @Override
        public ComplejoDecimal visitar(Raiz r) {
            ComplejoDecimal x = r.radicando().aceptar(this);
            if (x.esReal(mc.getPrecision() - 5)) {
                if (x.re().signum() >= 0 || r.indice() % 2 == 1) {
                    return ComplejoDecimal.real(Decimales.raiz(x.re(), r.indice(), mc));
                }
            }
            if (r.indice() == 2) {
                return x.raizCuadrada(mc);
            }
            throw new ArithmeticException("Raíz no real de índice " + r.indice());
        }

        @Override
        public ComplejoDecimal visitar(Funcion f) {
            ComplejoDecimal z = f.argumento().aceptar(this);
            if (!z.esReal(mc.getPrecision() - 5)) {
                throw new ArithmeticException("Función de un complejo");
            }
            BigDecimal x = z.re();
            BigDecimal r = switch (f.tipo()) {
                case SEN -> Decimales.sen(x, mc);
                case COS -> Decimales.cos(x, mc);
                case TAN -> Decimales.sen(x, mc).divide(Decimales.cos(x, mc), mc);
                case COT -> Decimales.cos(x, mc).divide(Decimales.sen(x, mc), mc);
                case SEC -> BigDecimal.ONE.divide(Decimales.cos(x, mc), mc);
                case CSC -> BigDecimal.ONE.divide(Decimales.sen(x, mc), mc);
                case ARCSEN -> Decimales.arcsen(x, mc);
                case ARCCOS -> Decimales.arccos(x, mc);
                case ARCTAN -> Decimales.arctan(x, mc);
                case ARCSEC -> Decimales.arccos(BigDecimal.ONE.divide(x, mc), mc);
                case LN -> Decimales.ln(x, mc);
                case LOG -> Decimales.ln(x, mc).divide(Decimales.ln(BigDecimal.valueOf(f.base()), mc), mc);
                case EXP -> Decimales.exp(x, mc);
                case SQRT -> x.sqrt(mc);
                case ABS -> x.abs();
            };
            return ComplejoDecimal.real(r);
        }

        private static ComplejoDecimal no() {
            throw new ArithmeticException("No se evalúa una operación");
        }

        @Override
        public ComplejoDecimal visitar(Limite l) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(Derivada d) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(pe.mathnova.domain.expresion.Gradiente g) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(pe.mathnova.domain.expresion.Direccional d) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(DerivadaParcial d) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(Integral i) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(IntegralMultiple i) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(pe.mathnova.domain.expresion.LimiteDoble l) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(pe.mathnova.domain.expresion.Extremos x) {
            return no();
        }

        @Override
        public ComplejoDecimal visitar(Ecuacion e) {
            return no();
        }
    }
}
