package pe.mathnova.presentation.grafica;

import java.util.function.DoubleUnaryOperator;

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

public final class CurvaExpresion implements DoubleUnaryOperator {

    private final Expresion expresion;
    private final char variable;

    public CurvaExpresion(Expresion expresion, char variable) {
        this.expresion = expresion;
        this.variable = variable;
    }

    @Override
    public double applyAsDouble(double x) {
        try {
            return expresion.aceptar(new Evaluador(java.util.Map.of(variable, x)));
        } catch (ArithmeticException e) {
            return Double.NaN;
        }
    }

    public static double evaluar(Expresion e, java.util.Map<Character, Double> valores) {
        try {
            return e.aceptar(new Evaluador(valores));
        } catch (ArithmeticException fuera) {
            return Double.NaN;
        }
    }

    private record Evaluador(java.util.Map<Character, Double> valores) implements Visitante<Double> {

        @Override
        public Double visitar(Constante c) {
            return FuncionReal.aDouble(c.valor());
        }

        @Override
        public Double visitar(Variable v) {
            Double x = valores.get(v.nombre());
            if (x == null) {
                throw new ArithmeticException("Otra letra");
            }
            return x;
        }

        @Override
        public Double visitar(ConstanteEspecial c) {
            return switch (c.tipo()) {
                case E -> Math.E;
                case PI -> Math.PI;
                case I -> throw new ArithmeticException("i no es real");
            };
        }

        @Override
        public Double visitar(Suma s) {
            double r = 0;
            for (Expresion t : s.terminos()) {
                r += t.aceptar(this);
            }
            return r;
        }

        @Override
        public Double visitar(Producto p) {
            double r = 1;
            for (Expresion f : p.factores()) {
                r *= f.aceptar(this);
            }
            return r;
        }

        @Override
        public Double visitar(Cociente c) {
            return c.numerador().aceptar(this) / c.denominador().aceptar(this);
        }

        @Override
        public Double visitar(Potencia p) {
            return Math.pow(p.base().aceptar(this), p.exponente().aceptar(this));
        }

        @Override
        public Double visitar(Negacion n) {
            return -n.argumento().aceptar(this);
        }

        @Override
        public Double visitar(Raiz r) {
            double v = r.radicando().aceptar(this);
            if (v < 0 && r.indice() % 2 == 1) {
                return -Math.pow(-v, 1.0 / r.indice());
            }
            return Math.pow(v, 1.0 / r.indice());
        }

        @Override
        public Double visitar(Funcion f) {
            double a = f.argumento().aceptar(this);
            return switch (f.tipo()) {
                case SEN -> Math.sin(a);
                case COS -> Math.cos(a);
                case TAN -> Math.tan(a);
                case COT -> 1 / Math.tan(a);
                case SEC -> 1 / Math.cos(a);
                case CSC -> 1 / Math.sin(a);
                case ARCSEN -> Math.asin(a);
                case ARCCOS -> Math.acos(a);
                case ARCTAN -> Math.atan(a);
                case ARCSEC -> Math.abs(a) >= 1 ? Math.acos(1 / a) : Double.NaN;
                case LN -> a > 0 ? Math.log(a) : Double.NaN;
                case LOG -> a > 0 ? Math.log(a) / Math.log(f.base()) : Double.NaN;
                case EXP -> Math.exp(a);
                case SQRT -> a >= 0 ? Math.sqrt(a) : Double.NaN;
                case ABS -> Math.abs(a);
            };
        }

        private static Double no() {
            throw new ArithmeticException("No se traza una operación");
        }

        @Override
        public Double visitar(Limite l) {
            return no();
        }

        @Override
        public Double visitar(Derivada d) {
            return no();
        }

        @Override
        public Double visitar(pe.mathnova.domain.expresion.Gradiente g) {
            return no();
        }

        @Override
        public Double visitar(pe.mathnova.domain.expresion.Direccional d) {
            return no();
        }

        @Override
        public Double visitar(DerivadaParcial d) {
            return no();
        }

        @Override
        public Double visitar(Integral i) {
            return no();
        }

        @Override
        public Double visitar(IntegralMultiple i) {
            return no();
        }

        @Override
        public Double visitar(pe.mathnova.domain.expresion.LimiteDoble l) {
            return no();
        }

        @Override
        public Double visitar(pe.mathnova.domain.expresion.Extremos x) {
            return no();
        }

        @Override
        public Double visitar(Ecuacion e) {
            return no();
        }
    }
}
