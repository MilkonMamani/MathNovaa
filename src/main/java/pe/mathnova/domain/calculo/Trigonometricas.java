package pe.mathnova.domain.calculo;

import java.util.Map;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;

/**
 * Polinomios en cos θ, sen θ (y cos φ, sen φ) para las coordenadas polares, cilíndricas y esféricas (Sprint 8).
 * Cada función se guarda como una letra auxiliar: α = cos θ, β = sen θ, γ = cos φ, δ = sen φ. Así r²·α² + r²·β²
 * se reduce con sen² + cos² = 1 a r², algo que el simplificador de árboles no hace.
 */
public final class Trigonometricas {

    public static final char THETA = 'θ';
    public static final char PHI = 'φ';
    /** cos θ. */
    public static final char COS_T = 'α';
    /** sen θ. */
    public static final char SEN_T = 'β';
    /** cos φ. */
    public static final char COS_F = 'γ';
    /** sen φ. */
    public static final char SEN_F = 'δ';

    private Trigonometricas() {
    }

    /** Las letras auxiliares. */
    public static boolean esAuxiliar(char c) {
        return c == COS_T || c == SEN_T || c == COS_F || c == SEN_F;
    }

    /**
     * p con sen² + cos² = 1 aplicado: se prueba cambiar sen² por 1 − cos² y cos² por 1 − sen² (en θ y en φ)
     * y se queda la forma con menos términos.
     */
    public static Polinomio reducir(Polinomio p) {
        Polinomio mejor = p;
        for (int t = 0; t < 3; t++) {
            for (int f = 0; f < 3; f++) {
                Polinomio q = p;
                if (t == 1) {
                    q = cambiarCuadrado(q, SEN_T, COS_T);
                } else if (t == 2) {
                    q = cambiarCuadrado(q, COS_T, SEN_T);
                }
                if (f == 1) {
                    q = cambiarCuadrado(q, SEN_F, COS_F);
                } else if (f == 2) {
                    q = cambiarCuadrado(q, COS_F, SEN_F);
                }
                if (q.terminos().size() < mejor.terminos().size()) {
                    mejor = q;
                }
            }
        }
        return mejor;
    }

    /** La forma canónica (sen θ y sen φ con exponente 0 o 1): dos polinomios son iguales si sus formas coinciden. */
    public static Polinomio canonica(Polinomio p) {
        return cambiarCuadrado(cambiarCuadrado(p, SEN_T, COS_T), SEN_F, COS_F);
    }

    /** Cada s² se cambia por 1 − c², hasta que s quede con exponente 0 o 1. */
    static Polinomio cambiarCuadrado(Polinomio p, char s, char c) {
        Polinomio r = Polinomio.CERO;
        Polinomio unoMenos = Polinomio.UNO.restar(Polinomio.termino(Racional.UNO, Monomio.de(c, 2)));
        for (Map.Entry<Monomio, Racional> t : p.terminos().entrySet()) {
            int e = t.getKey().exponente(s);
            Monomio resto = e == 0 ? t.getKey() : t.getKey().dividir(Monomio.de(s, e));
            Polinomio termino = Polinomio.termino(t.getValue(), resto);
            if (e % 2 == 1) {
                termino = termino.multiplicar(Polinomio.variable(s));
            }
            r = r.sumar(termino.multiplicar(unoMenos.potencia(e / 2)));
        }
        return r;
    }

    /** El polinomio con las letras auxiliares cambiadas por cos θ, sen θ, cos φ y sen φ, simplificado. */
    public static Expresion aExpresion(Polinomio p) {
        Expresion e = p.aExpresion();
        e = Expresiones.reemplazar(e, new Variable(COS_T), new Funcion(TipoFuncion.COS, new Variable(THETA), 0));
        e = Expresiones.reemplazar(e, new Variable(SEN_T), new Funcion(TipoFuncion.SEN, new Variable(THETA), 0));
        e = Expresiones.reemplazar(e, new Variable(COS_F), new Funcion(TipoFuncion.COS, new Variable(PHI), 0));
        e = Expresiones.reemplazar(e, new Variable(SEN_F), new Funcion(TipoFuncion.SEN, new Variable(PHI), 0));
        return funcionesAlFinal(Simplificador.simplificar(e));
    }

    /** ρ²·sen φ y no sen φ·ρ²: en cada producto las funciones van después de las letras. */
    private static Expresion funcionesAlFinal(Expresion e) {
        return switch (e) {
            case pe.mathnova.domain.expresion.Producto p -> {
                java.util.List<Expresion> letras = new java.util.ArrayList<>();
                java.util.List<Expresion> funciones = new java.util.ArrayList<>();
                for (Expresion f : p.factores()) {
                    boolean esFuncion = f instanceof Funcion
                            || f instanceof pe.mathnova.domain.expresion.Potencia pw && pw.base() instanceof Funcion;
                    (esFuncion ? funciones : letras).add(funcionesAlFinal(f));
                }
                letras.addAll(funciones);
                yield new pe.mathnova.domain.expresion.Producto(letras);
            }
            case pe.mathnova.domain.expresion.Suma s -> new pe.mathnova.domain.expresion.Suma(
                    s.terminos().stream().map(Trigonometricas::funcionesAlFinal).toList());
            case pe.mathnova.domain.expresion.Negacion n -> new pe.mathnova.domain.expresion.Negacion(
                    funcionesAlFinal(n.argumento()));
            case pe.mathnova.domain.expresion.Cociente c -> new pe.mathnova.domain.expresion.Cociente(
                    funcionesAlFinal(c.numerador()), funcionesAlFinal(c.denominador()));
            default -> e;
        };
    }

    /** ∂p/∂θ con d(cos θ) = −sen θ dθ y d(sen θ) = cos θ dθ (y lo mismo en φ). */
    public static Polinomio derivarAngulo(Polinomio p, char angulo) {
        char c = angulo == THETA ? COS_T : COS_F;
        char s = angulo == THETA ? SEN_T : SEN_F;
        Polinomio dc = Polinomios.derivar(p, c).multiplicar(Polinomio.variable(s).negar());
        Polinomio ds = Polinomios.derivar(p, s).multiplicar(Polinomio.variable(c));
        return dc.sumar(ds);
    }
}
