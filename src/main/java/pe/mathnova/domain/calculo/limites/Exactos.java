package pe.mathnova.domain.calculo.limites;

import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.numeros.Atomo;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.numeros.Radical;

/**
 * Cuentas exactas con {@link NumeroAlgebraico} que necesita el motor de límites: e y π como átomos,
 * los valores notables de sen, cos, ln y las inversas, y la división con respaldo simbólico (1/e).
 */
final class Exactos {

    static final NumeroAlgebraico E = NumeroAlgebraico.de(Atomo.simbolico(new ConstanteEspecial(ConstanteEspecial.Tipo.E)));
    static final NumeroAlgebraico PI = NumeroAlgebraico.de(Atomo.simbolico(new ConstanteEspecial(ConstanteEspecial.Tipo.PI)));
    private static final Atomo ATOMO_E = Atomo.simbolico(new ConstanteEspecial(ConstanteEspecial.Tipo.E));
    private static final Atomo ATOMO_PI = Atomo.simbolico(new ConstanteEspecial(ConstanteEspecial.Tipo.PI));

    /** sen(rπ) para r en [0, 1/2], con r = k/12. */
    private static final Map<Racional, NumeroAlgebraico> SENOS = Map.of(
            Racional.CERO, NumeroAlgebraico.CERO,
            Racional.de(1, 6), NumeroAlgebraico.de(Racional.de(1, 2)),
            Racional.de(1, 4), NumeroAlgebraico.raiz(2, 2).dividir(Racional.de(2)),
            Racional.de(1, 3), NumeroAlgebraico.raiz(2, 3).dividir(Racional.de(2)),
            Racional.de(1, 2), NumeroAlgebraico.UNO);

    private Exactos() {
    }

    /** Un valor que solo se conoce por su expresión: sen 1, ln 2, e^(1/2). */
    static NumeroAlgebraico simbolico(Expresion e) {
        return NumeroAlgebraico.de(Atomo.simbolico(e));
    }

    static NumeroAlgebraico dividir(NumeroAlgebraico a, NumeroAlgebraico b) {
        try {
            return a.dividir(b);
        } catch (ArithmeticException e) {
            return simbolico(new Cociente(a.aExpresion(), b.aExpresion()));
        }
    }

    /** r si z = r·π (incluido 0); vacío si no. */
    static Optional<Racional> multiploDePi(NumeroAlgebraico z) {
        if (z.esCero()) {
            return Optional.of(Racional.CERO);
        }
        if (z.terminos().size() != 1) {
            return Optional.empty();
        }
        var t = z.terminos().entrySet().iterator().next();
        NumeroAlgebraico.Unidad u = t.getKey();
        if (u.imaginaria() || !u.raiz().esRacional() || u.atomos().size() != 1
                || !Integer.valueOf(1).equals(u.atomos().get(ATOMO_PI))) {
            return Optional.empty();
        }
        return Optional.of(t.getValue());
    }

    /** n si z = eⁿ con n entero (1 = e⁰); vacío si no. */
    static Optional<Integer> potenciaDeE(NumeroAlgebraico z) {
        if (z.equals(NumeroAlgebraico.UNO)) {
            return Optional.of(0);
        }
        if (z.terminos().size() != 1) {
            return Optional.empty();
        }
        var t = z.terminos().entrySet().iterator().next();
        NumeroAlgebraico.Unidad u = t.getKey();
        if (!t.getValue().equals(Racional.UNO) || u.imaginaria() || !u.raiz().esRacional()
                || u.atomos().size() != 1 || !u.atomos().containsKey(ATOMO_E)) {
            return Optional.empty();
        }
        return Optional.of(u.atomos().get(ATOMO_E));
    }

    /** eᶻ exacto si z es entero (e⁰ = 1, e¹ = e, e² …); si no, el valor simbólico. */
    static NumeroAlgebraico exp(NumeroAlgebraico z) {
        if (z.esRacional() && z.aRacional().esEntero() && z.aRacional().num().bitLength() < 12) {
            int n = z.aRacional().num().intValueExact();
            if (n >= 0) {
                return E.potencia(n);
            }
            return simbolico(new Cociente(pe.mathnova.domain.expresion.Constante.de(1), E.potencia(-n).aExpresion()));
        }
        return simbolico(new Potencia(new ConstanteEspecial(ConstanteEspecial.Tipo.E), z.aExpresion()));
    }

    /** sen(rπ) exacto si r es múltiplo de 1/6 o de 1/4; vacío si no. */
    static Optional<NumeroAlgebraico> senoDe(Racional r) {
        // r módulo 2, en [0, 2)
        Racional dos = Racional.de(2);
        Racional m = r.restar(dos.multiplicar(Racional.de(piso(r.dividir(dos)))));
        boolean negativo = false;
        if (m.compareTo(Racional.UNO) >= 0) {
            m = m.restar(Racional.UNO);
            negativo = true;
        }
        if (m.compareTo(Racional.de(1, 2)) > 0) {
            m = Racional.UNO.restar(m);
        }
        NumeroAlgebraico v = SENOS.get(m);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.of(negativo ? v.negar() : v);
    }

    private static java.math.BigInteger piso(Racional q) {
        java.math.BigInteger[] dr = q.num().divideAndRemainder(q.den());
        return q.signo() < 0 && dr[1].signum() != 0 ? dr[0].subtract(java.math.BigInteger.ONE) : dr[0];
    }

    /** El valor de sen, cos o tan en z: exacto en los ángulos notables, si no simbólico. */
    static Valor trigonometrica(TipoFuncion tipo, NumeroAlgebraico z) {
        Optional<Racional> r = multiploDePi(z);
        Optional<NumeroAlgebraico> sen = r.flatMap(Exactos::senoDe);
        Optional<NumeroAlgebraico> cos = r.flatMap(q -> senoDe(q.sumar(Racional.de(1, 2))));
        if (sen.isEmpty() || cos.isEmpty()) {
            if (!z.esReal()) {
                return new Valor.Indefinido("El argumento no es real", false);
            }
            return new Valor.Numero(simbolico(new Funcion(tipo, z.aExpresion())));
        }
        NumeroAlgebraico s = sen.get();
        NumeroAlgebraico c = cos.get();
        return switch (tipo) {
            case SEN -> new Valor.Numero(s);
            case COS -> new Valor.Numero(c);
            case TAN -> cociente(s, c);
            case COT -> cociente(c, s);
            case SEC -> cociente(NumeroAlgebraico.UNO, c);
            case CSC -> cociente(NumeroAlgebraico.UNO, s);
            default -> throw new IllegalArgumentException("No es trigonométrica: " + tipo);
        };
    }

    private static Valor cociente(NumeroAlgebraico a, NumeroAlgebraico b) {
        if (b.esCero()) {
            return a.esCero() ? new Valor.Indeterminado(Indeterminacion.CERO_SOBRE_CERO) : new Valor.SobreCero(a);
        }
        return new Valor.Numero(dividir(a, b));
    }

    /** arcsen, arccos y arctan en sus valores notables; si no, simbólico (dentro del dominio). */
    static Valor inversa(TipoFuncion tipo, NumeroAlgebraico z) {
        boolean fuera = z.restar(NumeroAlgebraico.UNO).signo() > 0 || z.sumar(NumeroAlgebraico.UNO).signo() < 0;
        if (tipo != TipoFuncion.ARCTAN && fuera) {
            return new Valor.Indefinido(tipo.nombre() + " solo está definida entre −1 y 1", false);
        }
        Racional mitad = Racional.de(1, 2);
        if (z.esRacional()) {
            Racional q = z.aRacional();
            Racional r = switch (tipo) {
                case ARCSEN -> q.esCero() ? Racional.CERO : q.equals(Racional.UNO) ? mitad
                        : q.equals(Racional.UNO.negar()) ? mitad.negar() : q.equals(mitad) ? Racional.de(1, 6)
                        : q.equals(mitad.negar()) ? Racional.de(-1, 6) : null;
                case ARCCOS -> q.equals(Racional.UNO) ? Racional.CERO : q.esCero() ? mitad
                        : q.equals(Racional.UNO.negar()) ? Racional.UNO : q.equals(mitad) ? Racional.de(1, 3)
                        : q.equals(mitad.negar()) ? Racional.de(2, 3) : null;
                case ARCTAN -> q.esCero() ? Racional.CERO : q.equals(Racional.UNO) ? Racional.de(1, 4)
                        : q.equals(Racional.UNO.negar()) ? Racional.de(-1, 4) : null;
                default -> null;
            };
            if (r != null) {
                return new Valor.Numero(PI.multiplicar(r));
            }
        }
        return new Valor.Numero(simbolico(new Funcion(tipo, z.aExpresion())));
    }

    /** ln z y log_b z: exactos en 1, en eⁿ y en bⁿ; −∞ en 0 (desde la derecha). */
    static Valor logaritmo(Funcion f, NumeroAlgebraico z) {
        int s = z.signo();
        if (s == 0) {
            return new Valor.Infinito(-1);
        }
        if (s < 0) {
            return new Valor.Indefinido(f.tipo().nombre() + " no está definido para números negativos", false);
        }
        if (f.tipo() == TipoFuncion.LN) {
            Optional<Integer> n = potenciaDeE(z);
            if (n.isPresent()) {
                return new Valor.Numero(NumeroAlgebraico.de(n.get()));
            }
            return new Valor.Numero(simbolico(new Funcion(TipoFuncion.LN, z.aExpresion())));
        }
        if (z.esRacional()) {
            Racional q = z.aRacional();
            Racional b = Racional.de(f.base());
            for (int n = -40; n <= 40; n++) {
                if (b.potencia(n).equals(q)) {
                    return new Valor.Numero(NumeroAlgebraico.de(n));
                }
            }
        }
        return new Valor.Numero(simbolico(new Funcion(TipoFuncion.LOG, z.aExpresion(), f.base())));
    }

    /** ⁿ√z real: exacta con radicales; vacío si n es par y z negativo. */
    static Optional<NumeroAlgebraico> raiz(int n, NumeroAlgebraico z) {
        if (!z.esReal()) {
            return Optional.empty();
        }
        int s = z.signo();
        if (s < 0 && n % 2 == 0) {
            return Optional.empty();
        }
        if (z.esRacional()) {
            Racional q = z.aRacional();
            if (s < 0) {
                return Optional.of(NumeroAlgebraico.de(Radical.raiz(n, q.negar())).negar());
            }
            return Optional.of(NumeroAlgebraico.de(Radical.raiz(n, q)));
        }
        try {
            return Optional.of(n == 2 ? z.raizCuadrada() : z.raizReal(n));
        } catch (ArithmeticException e) {
            return Optional.of(simbolico(new pe.mathnova.domain.expresion.Raiz(n, z.aExpresion())));
        }
    }
}
