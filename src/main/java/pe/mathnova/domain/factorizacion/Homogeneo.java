package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

final class Homogeneo extends MetodoBase {

    static final char T = 't';

    Homogeneo() {
        super("M06");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 2 && p.grado() >= 3 && Multivariable.homogeneo(p) && !p.variables().contains(T);
    }

    static Polinomio enT(Polinomio p, char x, char y) {
        return Multivariable.sustituir(Multivariable.sustituir(p, x, Polinomio.variable(T)), y, Polinomio.UNO);
    }

    static Polinomio homogeneizar(Polinomio g, char x, char y, int k) {
        Polinomio r = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : g.terminos().entrySet()) {
            int i = t.getKey().exponente(T);
            r = r.sumar(Polinomio.termino(t.getValue(), Monomio.de(x, i).multiplicar(Monomio.de(y, k - i))));
        }
        return r;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        char x = Multivariable.principal(p);
        char y = Multivariable.segunda(p);
        int n = p.grado();
        Polinomio enT = enT(p, x, y);
        String antes = Formato.polinomio(p) + " = " + y + Formato.superindice(n) + "(" + Formato.polinomio(enT) + ")";
        Resolucion r = Multivariable.Univariados.factorizar(enT);
        FormaFactorizada forma = r.resultado();
        if (forma.factores().size() == 1 && forma.factores().getFirst().multiplicidad() == 1) {
            throw noAplica("Con t = " + x + "/" + y + " queda " + Formato.polinomio(enT) + ", que no se factoriza en Q");
        }
        for (Factor f : forma.factores()) {
            if (f.multiplicidad() > 1) {
                throw noAplica("Con t = " + x + "/" + y + " queda " + Formato.polinomio(enT) + " con el factor ("
                        + Formato.polinomio(f.polinomio()) + ") repetido: Ruffini respecto de " + x
                        + " (con " + y + " como parámetro) deja ver la multiplicidad");
            }
        }
        List<Factor> factores = new ArrayList<>();
        if (!forma.constante().equals(Racional.UNO)) {
            factores.add(Factor.de(Polinomio.constante(forma.constante())));
        }
        int usado = 0;
        List<String> enX = new ArrayList<>();
        for (Factor f : forma.factores()) {
            int k = f.polinomio().grado();
            usado += k * f.multiplicidad();
            Polinomio h = homogeneizar(f.polinomio(), x, y, k);
            factores.add(new Factor(h, f.multiplicidad()));
            enX.add("(" + Formato.polinomio(h) + ")");
        }
        if (usado < n) {
            factores.add(new Factor(Polinomio.variable(y), n - usado));
            enX.addFirst(y + (n - usado > 1 ? Formato.superindice(n - usado) : ""));
        }
        var trabajo = new TrabajoIntermedio.Sustitucion("t", "t = " + x + "/" + y, antes,
                Formato.polinomio(enT) + " = " + forma.aTexto(), String.join("", enX));
        return resultado("Todos los términos son de grado " + n + ": es homogéneo, y con t = " + x + "/" + y
                + " queda un polinomio en una letra", p, factores, trabajo);
    }

    @Override
    public String pista() {
        return "Todos los términos tienen el mismo grado total: divide entre una potencia de y y trabaja con x/y.";
    }
}
