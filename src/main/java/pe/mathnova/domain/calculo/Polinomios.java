package pe.mathnova.domain.calculo;

import java.util.Map;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * Operaciones con polinomios de varias letras que usan los límites dobles, los extremos y las integrales
 * múltiples (Sprint 8): sustituir una letra por otro polinomio, partir en partes homogéneas y mirar una
 * letra como la variable principal.
 */
public final class Polinomios {

    private Polinomios() {
    }

    /** p con la letra v cambiada por el polinomio q. */
    public static Polinomio sustituir(Polinomio p, char v, Polinomio q) {
        Polinomio r = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : p.terminos().entrySet()) {
            int e = t.getKey().exponente(v);
            Monomio resto = e == 0 ? t.getKey() : t.getKey().dividir(Monomio.de(v, e));
            r = r.sumar(Polinomio.termino(t.getValue(), resto).multiplicar(q.potencia(e)));
        }
        return r;
    }

    /** p con la letra v cambiada por el número c. */
    public static Polinomio sustituir(Polinomio p, char v, Racional c) {
        return sustituir(p, v, Polinomio.constante(c));
    }

    /** Las partes homogéneas de p, por grado total: x²y + x³ + y → {1: y, 3: x²y + x³}. */
    public static TreeMap<Integer, Polinomio> homogeneas(Polinomio p) {
        TreeMap<Integer, Polinomio> r = new TreeMap<>();
        p.terminos().forEach((m, c) -> r.merge(m.grado(), Polinomio.termino(c, m), Polinomio::sumar));
        return r;
    }

    /** p como polinomio en v: el coeficiente de v^k, que es un polinomio en las demás letras. */
    public static TreeMap<Integer, Polinomio> enPotenciasDe(Polinomio p, char v) {
        TreeMap<Integer, Polinomio> r = new TreeMap<>();
        p.terminos().forEach((m, c) -> {
            int e = m.exponente(v);
            Monomio resto = e == 0 ? m : m.dividir(Monomio.de(v, e));
            r.merge(e, Polinomio.termino(c, resto), Polinomio::sumar);
        });
        return r;
    }

    /** La suma de los valores absolutos de los coeficientes. */
    public static Racional sumaDeAbsolutos(Polinomio p) {
        Racional s = Racional.CERO;
        for (Racional c : p.terminos().values()) {
            s = s.sumar(c.signo() < 0 ? c.negar() : c);
        }
        return s;
    }

    /** Si cada término tiene a lo más esas letras. */
    public static boolean soloLetras(Polinomio p, java.util.Collection<Character> letras) {
        return letras.containsAll(p.variables());
    }

    /** La derivada parcial respecto de v. */
    public static Polinomio derivar(Polinomio p, char v) {
        Polinomio r = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : p.terminos().entrySet()) {
            int e = t.getKey().exponente(v);
            if (e > 0) {
                Monomio m = t.getKey().dividir(Monomio.de(v, 1));
                r = r.sumar(Polinomio.termino(t.getValue().multiplicar(Racional.de(e)), m));
            }
        }
        return r;
    }
}
