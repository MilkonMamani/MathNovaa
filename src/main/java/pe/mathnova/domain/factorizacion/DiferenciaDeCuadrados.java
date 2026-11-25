package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;

public final class DiferenciaDeCuadrados extends MetodoBase {

    public DiferenciaDeCuadrados() {
        super("F05");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.terminos().size() == 2;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        List<Termino> ts = Termino.de(p);
        if (ts.size() != 2) {
            throw noAplica("Tiene " + ts.size() + " términos; hacen falta 2");
        }
        Termino t1 = ts.get(0);
        Termino t2 = ts.get(1);
        if (t1.positivo() == t2.positivo()) {
            throw noAplica(Formato.polinomio(p) + (t1.positivo() ? " es una suma, no una diferencia"
                    : " tiene los dos términos negativos"));
        }
        Termino positivo = t1.positivo() ? t1 : t2;
        Termino negativo = (t1.positivo() ? t2 : t1).abs();
        Termino a = positivo.raiz(2).orElseThrow(() ->
                noAplica(Formato.termino(positivo) + " no es un cuadrado perfecto"));
        Termino b = negativo.raiz(2).orElseThrow(() ->
                noAplica(Formato.termino(negativo) + " no es un cuadrado perfecto"));

        String justificacion = Formato.termino(positivo) + " = " + Formato.base(a) + "² y "
                + Formato.termino(negativo) + " = " + Formato.base(b) + "²";
        var trabajo = new TrabajoIntermedio.Comparacion("a² − b² = (a + b)(a − b) con a = "
                + Formato.termino(a) + " y b = " + Formato.termino(b));
        List<Factor> factores = List.of(
                Factor.de(a.aPolinomio().sumar(b.aPolinomio())),
                Factor.de(a.aPolinomio().restar(b.aPolinomio())));
        return resultado(justificacion, p, factores, trabajo);
    }

    static Optional<Polinomio> conjugado(Polinomio q, Polinomio factor) {
        List<Termino> ts = Termino.de(q);
        if (ts.size() != 2 || ts.get(0).positivo() == ts.get(1).positivo()) {
            return Optional.empty();
        }
        Termino positivo = ts.get(0).positivo() ? ts.get(0) : ts.get(1);
        Termino negativo = (ts.get(0).positivo() ? ts.get(1) : ts.get(0)).abs();
        Optional<Termino> a = positivo.raiz(2);
        Optional<Termino> b = negativo.raiz(2);
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty();
        }
        Polinomio mas = a.get().aPolinomio().sumar(b.get().aPolinomio());
        Polinomio menos = a.get().aPolinomio().restar(b.get().aPolinomio());
        if (factor.equals(mas)) {
            return Optional.of(menos);
        }
        if (factor.equals(menos)) {
            return Optional.of(mas);
        }
        return Optional.empty();
    }

    @Override
    public String pista() {
        return "¿Son dos términos restados y cada uno es el cuadrado de algo?";
    }
}
