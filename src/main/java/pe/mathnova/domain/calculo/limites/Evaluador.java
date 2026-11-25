package pe.mathnova.domain.calculo.limites;

import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.TipoFuncion;
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
import pe.mathnova.domain.numeros.NumeroAlgebraico;

public final class Evaluador implements Visitante<Valor> {

    private final char variable;
    private final Valor valorVariable;

    private Evaluador(char variable, Valor valorVariable) {
        this.variable = variable;
        this.valorVariable = valorVariable;
    }

    public static Valor evaluar(Expresion e, char variable, Valor valor) {
        return e.aceptar(new Evaluador(variable, valor));
    }

    public static Optional<NumeroAlgebraico> constante(Expresion e) {
        Valor v = e.aceptar(new Evaluador('\0', null));
        return v instanceof Valor.Numero n ? Optional.of(n.valor()) : Optional.empty();
    }

    private static boolean esCero(Valor v) {
        return v instanceof Valor.Numero n && n.esCero();
    }

    private static int signo(NumeroAlgebraico z) {
        return z.signo();
    }

    private static Valor anidada(Valor.Indeterminado i) {
        return new Valor.Indefinido("Hay una indeterminación " + i.tipo().texto() + " dentro de una parte de la expresión",
                false);
    }

    private static Valor hijo(Valor v) {
        return v instanceof Valor.Indeterminado i ? anidada(i) : v;
    }

    static Valor sumar(Valor a, Valor b) {
        if (a instanceof Valor.Indefinido) {
            return a;
        }
        if (b instanceof Valor.Indefinido) {
            return b;
        }
        if (a instanceof Valor.Indeterminado) {
            return b instanceof Valor.Numero || b instanceof Valor.Infinito ? a
                    : new Valor.Indefinido("No se puede combinar la indeterminación", false);
        }
        return switch (a) {
            case Valor.Numero x when b instanceof Valor.Numero y -> new Valor.Numero(x.valor().sumar(y.valor()));
            case Valor.Numero x when b instanceof Valor.Infinito y -> y;
            case Valor.Numero x when b instanceof Valor.SobreCero y -> y;
            case Valor.Infinito x when b instanceof Valor.Numero -> x;
            case Valor.Infinito x when b instanceof Valor.Infinito y -> x.signo() == y.signo() ? x
                    : new Valor.Indeterminado(Indeterminacion.INFINITO_MENOS_INFINITO);
            case Valor.SobreCero x when b instanceof Valor.Numero -> x;
            default -> new Valor.Indefinido("Hay una división entre cero que no se puede combinar", false);
        };
    }

    static Valor negar(Valor v) {
        return switch (v) {
            case Valor.Numero n -> new Valor.Numero(n.valor().negar());
            case Valor.Infinito i -> new Valor.Infinito(-i.signo());
            case Valor.SobreCero s -> new Valor.SobreCero(s.k().negar());
            default -> v;
        };
    }

    static Valor multiplicar(Valor a, Valor b) {
        if (a instanceof Valor.Indefinido) {
            return a;
        }
        if (b instanceof Valor.Indefinido) {
            return b;
        }
        if (a instanceof Valor.Indeterminado) {
            return b instanceof Valor.Numero n && !n.esCero() || b instanceof Valor.Infinito ? a
                    : new Valor.Indefinido("No se puede combinar la indeterminación", false);
        }
        if (b instanceof Valor.Numero && !(a instanceof Valor.Numero)) {
            return multiplicar(b, a);
        }
        return switch (a) {
            case Valor.Numero x when b instanceof Valor.Numero y -> new Valor.Numero(x.valor().multiplicar(y.valor()));
            case Valor.Numero x when b instanceof Valor.Infinito y -> x.esCero()
                    ? new Valor.Indeterminado(Indeterminacion.CERO_POR_INFINITO)
                    : new Valor.Infinito(y.signo() * signo(x.valor()));
            case Valor.Numero x when b instanceof Valor.SobreCero y -> x.esCero()
                    ? new Valor.Indeterminado(Indeterminacion.CERO_POR_INFINITO)
                    : new Valor.SobreCero(y.k().multiplicar(x.valor()));
            case Valor.Infinito x when b instanceof Valor.Infinito y -> new Valor.Infinito(x.signo() * y.signo());
            case Valor.SobreCero x when b instanceof Valor.SobreCero y -> new Valor.SobreCero(x.k().multiplicar(y.k()));
            default -> new Valor.Indefinido("Hay una división entre cero que no se puede combinar", false);
        };
    }

    static Valor dividir(Valor a, Valor b) {
        if (a instanceof Valor.Indefinido) {
            return a;
        }
        if (b instanceof Valor.Indefinido) {
            return b;
        }
        if (a instanceof Valor.Indeterminado i) {
            return anidada(i);
        }
        boolean infA = a instanceof Valor.Infinito || a instanceof Valor.SobreCero;
        boolean infB = b instanceof Valor.Infinito || b instanceof Valor.SobreCero;
        if (infA && infB) {
            return new Valor.Indeterminado(Indeterminacion.INFINITO_SOBRE_INFINITO);
        }
        return switch (a) {
            case Valor.Numero x when b instanceof Valor.Numero y -> {
                if (!y.esCero()) {
                    yield new Valor.Numero(Exactos.dividir(x.valor(), y.valor()));
                }
                yield x.esCero() ? new Valor.Indeterminado(Indeterminacion.CERO_SOBRE_CERO) : new Valor.SobreCero(x.valor());
            }
            case Valor.Numero x when infB -> new Valor.Numero(NumeroAlgebraico.CERO);
            case Valor.Infinito x when b instanceof Valor.Numero y -> y.esCero()
                    ? new Valor.Indefinido("∞ dividido entre 0", false)
                    : new Valor.Infinito(x.signo() * signo(y.valor()));
            case Valor.SobreCero x when b instanceof Valor.Numero y -> y.esCero()
                    ? new Valor.Indefinido("∞ dividido entre 0", false)
                    : new Valor.SobreCero(Exactos.dividir(x.k(), y.valor()));
            default -> new Valor.Indefinido("No se puede evaluar el cociente", false);
        };
    }

    static Valor potencia(Valor b, Valor e) {
        if (b instanceof Valor.Indefinido) {
            return b;
        }
        if (e instanceof Valor.Indefinido) {
            return e;
        }
        if (b instanceof Valor.SobreCero || e instanceof Valor.SobreCero) {
            return new Valor.Indefinido("Hay una división entre cero dentro de la potencia", false);
        }
        return switch (b) {
            case Valor.Numero x when e instanceof Valor.Numero y -> numeroANumero(x.valor(), y.valor());
            case Valor.Numero x when e instanceof Valor.Infinito y -> numeroAInfinito(x.valor(), y.signo());
            case Valor.Infinito x when e instanceof Valor.Numero y -> infinitoANumero(x.signo(), y);
            case Valor.Infinito x when e instanceof Valor.Infinito y -> x.signo() > 0
                    ? (y.signo() > 0 ? new Valor.Infinito(1) : new Valor.Numero(NumeroAlgebraico.CERO))
                    : new Valor.Indefinido("Una potencia de base negativa con exponente infinito no tiene límite", true);
            default -> new Valor.Indefinido("No se puede evaluar la potencia", false);
        };
    }

    private static Valor numeroANumero(NumeroAlgebraico b, NumeroAlgebraico e) {
        if (b.equals(Exactos.E)) {
            return new Valor.Numero(Exactos.exp(e));
        }
        if (e.esRacional()) {
            Racional r = e.aRacional();
            if (b.esCero()) {
                return r.signo() > 0 ? new Valor.Numero(NumeroAlgebraico.CERO)
                        : r.esCero() ? new Valor.Indeterminado(Indeterminacion.CERO_A_LA_CERO)
                        : new Valor.SobreCero(NumeroAlgebraico.UNO);
            }
            if (r.num().bitLength() > 16 || r.den().bitLength() > 16) {
                return new Valor.Numero(Exactos.simbolico(new Potencia(b.aExpresion(), e.aExpresion())));
            }
            int p = r.num().intValueExact();
            int q = r.den().intValueExact();
            NumeroAlgebraico bp;
            try {
                bp = b.potencia(p);
            } catch (ArithmeticException noExacta) {
                return new Valor.Numero(Exactos.simbolico(new Potencia(b.aExpresion(), e.aExpresion())));
            }
            if (q == 1) {
                return new Valor.Numero(bp);
            }
            return Exactos.raiz(q, bp).<Valor>map(Valor.Numero::new)
                    .orElse(new Valor.Indefinido("Raíz par de un número negativo", false));
        }
        if (b.signo() > 0) {
            return new Valor.Numero(Exactos.simbolico(new Potencia(b.aExpresion(), e.aExpresion())));
        }
        return new Valor.Indefinido("Una base negativa con exponente irracional no es real", false);
    }

    private static Valor numeroAInfinito(NumeroAlgebraico b, int s) {
        int contraUno = b.restar(NumeroAlgebraico.UNO).signo();
        int contraMenosUno = b.sumar(NumeroAlgebraico.UNO).signo();
        if (contraUno == 0) {
            return new Valor.Indeterminado(Indeterminacion.UNO_A_LA_INFINITO);
        }
        boolean grande = contraUno > 0;
        boolean entre = contraUno < 0 && contraMenosUno > 0;
        if (s > 0) {
            if (grande) {
                return new Valor.Infinito(1);
            }
            if (entre) {
                return new Valor.Numero(NumeroAlgebraico.CERO);
            }
        } else {
            if (grande) {
                return new Valor.Numero(NumeroAlgebraico.CERO);
            }
            if (entre && b.signo() >= 0) {
                return new Valor.Infinito(1);
            }
        }
        return new Valor.Indefinido("Una potencia de base negativa con exponente infinito no tiene límite", true);
    }

    private static Valor infinitoANumero(int s, Valor.Numero exponente) {
        NumeroAlgebraico c = exponente.valor();
        if (exponente.esCero()) {
            return new Valor.Indeterminado(Indeterminacion.INFINITO_A_LA_CERO);
        }
        if (c.signo() < 0) {
            return new Valor.Numero(NumeroAlgebraico.CERO);
        }
        if (s > 0) {
            return new Valor.Infinito(1);
        }
        if (c.esRacional() && c.aRacional().esEntero()) {
            return new Valor.Infinito(c.aRacional().num().testBit(0) ? -1 : 1);
        }
        return new Valor.Indefinido("Una potencia de base negativa con exponente fraccionario no es real", false);
    }

    @Override
    public Valor visitar(Constante c) {
        return new Valor.Numero(NumeroAlgebraico.de(c.valor()));
    }

    @Override
    public Valor visitar(Variable v) {
        if (v.nombre() == variable && valorVariable != null) {
            return valorVariable;
        }
        return new Valor.Indefinido("La letra " + v.nombre() + " no tiene un valor", false);
    }

    @Override
    public Valor visitar(ConstanteEspecial c) {
        return switch (c.tipo()) {
            case E -> new Valor.Numero(Exactos.E);
            case PI -> new Valor.Numero(Exactos.PI);
            case I -> new Valor.Indefinido("i no es un número real", false);
        };
    }

    @Override
    public Valor visitar(Suma s) {
        Valor r = null;
        for (Expresion t : s.terminos()) {
            Valor v = hijo(t.aceptar(this));
            r = r == null ? v : sumar(r, v);
        }
        return r;
    }

    @Override
    public Valor visitar(Producto p) {
        Valor r = null;
        for (Expresion f : p.factores()) {
            Valor v = hijo(f.aceptar(this));
            r = r == null ? v : multiplicar(r, v);
        }
        return r;
    }

    @Override
    public Valor visitar(Cociente c) {
        return dividir(hijo(c.numerador().aceptar(this)), hijo(c.denominador().aceptar(this)));
    }

    @Override
    public Valor visitar(Potencia p) {
        return potencia(hijo(p.base().aceptar(this)), hijo(p.exponente().aceptar(this)));
    }

    @Override
    public Valor visitar(Negacion n) {
        return negar(n.argumento().aceptar(this));
    }

    @Override
    public Valor visitar(Raiz r) {
        return raiz(r.indice(), hijo(r.radicando().aceptar(this)));
    }

    private static Valor raiz(int n, Valor v) {
        return switch (v) {
            case Valor.Numero x -> Exactos.raiz(n, x.valor()).<Valor>map(Valor.Numero::new)
                    .orElse(new Valor.Indefinido("Raíz par de un número negativo", false));
            case Valor.Infinito i -> i.signo() > 0 || n % 2 == 1 ? i
                    : new Valor.Indefinido("Raíz par de un número negativo", false);
            case Valor.SobreCero s -> new Valor.Indefinido("Hay una división entre cero dentro de la raíz", false);
            default -> v;
        };
    }

    @Override
    public Valor visitar(Funcion f) {
        Valor v = hijo(f.argumento().aceptar(this));
        if (v instanceof Valor.Indefinido) {
            return v;
        }
        if (v instanceof Valor.SobreCero) {
            return new Valor.Indefinido("Hay una división entre cero dentro de " + f.tipo().nombre(), false);
        }
        if (v instanceof Valor.Infinito i) {
            return alInfinito(f, i.signo());
        }
        NumeroAlgebraico z = ((Valor.Numero) v).valor();
        return switch (f.tipo()) {
            case SEN, COS, TAN, COT, SEC, CSC -> Exactos.trigonometrica(f.tipo(), z);
            case ARCSEN, ARCCOS, ARCTAN -> Exactos.inversa(f.tipo(), z);
            case ARCSEC -> new Valor.Numero(Exactos.simbolico(new Funcion(TipoFuncion.ARCSEC, z.aExpresion())));
            case LN, LOG -> Exactos.logaritmo(f, z);
            case EXP -> new Valor.Numero(Exactos.exp(z));
            case SQRT -> raiz(2, v);
            case ABS -> new Valor.Numero(z.signo() < 0 ? z.negar() : z);
        };
    }

    private static Valor alInfinito(Funcion f, int s) {
        String x = s > 0 ? "∞" : "−∞";
        return switch (f.tipo()) {
            case SEN, COS -> new Valor.Indefinido(f.tipo().nombre() + " oscila entre −1 y 1 cuando su argumento tiende a "
                    + x + ": no se acerca a ningún valor", true);
            case TAN, COT, SEC, CSC -> new Valor.Indefinido(f.tipo().nombre() + " no tiene límite cuando su argumento "
                    + "tiende a " + x + ": es periódica", true);
            case ARCTAN -> new Valor.Numero(Exactos.PI.multiplicar(Racional.de(s, 2)));
            case ARCSEN, ARCCOS -> new Valor.Indefinido(f.tipo().nombre() + " solo está definida entre −1 y 1", false);
            case ARCSEC -> new Valor.Numero(Exactos.PI.multiplicar(Racional.de(1, 2)));
            case LN, LOG, SQRT -> s > 0 ? new Valor.Infinito(1)
                    : new Valor.Indefinido(f.tipo().nombre() + " no está definida para números negativos", false);
            case EXP -> s > 0 ? new Valor.Infinito(1) : new Valor.Numero(NumeroAlgebraico.CERO);
            case ABS -> new Valor.Infinito(1);
        };
    }

    private static Valor operacion() {
        return new Valor.Indefinido("Una operación no se evalúa", false);
    }

    @Override
    public Valor visitar(Limite l) {
        return operacion();
    }

    @Override
    public Valor visitar(Derivada d) {
        return operacion();
    }

    @Override
    public Valor visitar(pe.mathnova.domain.expresion.Gradiente g) {
        return operacion();
    }

    @Override
    public Valor visitar(pe.mathnova.domain.expresion.Direccional d) {
        return operacion();
    }

    @Override
    public Valor visitar(DerivadaParcial d) {
        return operacion();
    }

    @Override
    public Valor visitar(Integral i) {
        return operacion();
    }

    @Override
    public Valor visitar(IntegralMultiple i) {
        return operacion();
    }

    @Override
    public Valor visitar(pe.mathnova.domain.expresion.LimiteDoble l) {
        return operacion();
    }

    @Override
    public Valor visitar(pe.mathnova.domain.expresion.Extremos x) {
        return operacion();
    }

    @Override
    public Valor visitar(Ecuacion e) {
        return operacion();
    }
}
