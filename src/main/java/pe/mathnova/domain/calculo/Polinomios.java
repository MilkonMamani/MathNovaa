package pe.mathnova.domain.calculo;

import java.util.Map;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

public final class Polinomios {

    private Polinomios() {
    }

    public static Polinomio sustituir(Polinomio p, char v, Polinomio q) {
        Polinomio r = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : p.terminos().entrySet()) {
            int e = t.getKey().exponente(v);
            Monomio resto = e == 0 ? t.getKey() : t.getKey().dividir(Monomio.de(v, e));
            r = r.sumar(Polinomio.termino(t.getValue(), resto).multiplicar(q.potencia(e)));
        }
        return r;
    }

    public static Polinomio sustituir(Polinomio p, char v, Racional c) {
        return sustituir(p, v, Polinomio.constante(c));
    }

    public static TreeMap<Integer, Polinomio> homogeneas(Polinomio p) {
        TreeMap<Integer, Polinomio> r = new TreeMap<>();
        p.terminos().forEach((m, c) -> r.merge(m.grado(), Polinomio.termino(c, m), Polinomio::sumar));
        return r;
    }

    public static TreeMap<Integer, Polinomio> enPotenciasDe(Polinomio p, char v) {
        TreeMap<Integer, Polinomio> r = new TreeMap<>();
        p.terminos().forEach((m, c) -> {
            int e = m.exponente(v);
            Monomio resto = e == 0 ? m : m.dividir(Monomio.de(v, e));
            r.merge(e, Polinomio.termino(c, resto), Polinomio::sumar);
        });
        return r;
    }

    public static Racional sumaDeAbsolutos(Polinomio p) {
        Racional s = Racional.CERO;
        for (Racional c : p.terminos().values()) {
            s = s.sumar(c.signo() < 0 ? c.negar() : c);
        }
        return s;
    }

    public static boolean soloLetras(Polinomio p, java.util.Collection<Character> letras) {
        return letras.containsAll(p.variables());
    }

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
