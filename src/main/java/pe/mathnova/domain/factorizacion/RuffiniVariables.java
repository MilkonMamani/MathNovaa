package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

final class RuffiniVariables extends MetodoBase {

    private static final int CANDIDATOS_MAXIMOS = 40;

    RuffiniVariables() {
        super("M07");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 2 && p.gradoEn(Multivariable.principal(p)) >= 2;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        char x = Multivariable.principal(p);
        char y = Multivariable.segunda(p);
        int n = p.gradoEn(x);
        Polinomio enX = Multivariable.sustituir(p, y, Polinomio.UNO);
        if (enX.gradoEn(x) < 1 || enX.constante().esCero()) {
            throw noAplica("Con " + y + " = 1 no queda un polinomio en " + x + " con término independiente");
        }
        int gy = p.gradoEn(y);
        List<Integer> potencias = new ArrayList<>();
        potencias.add(1);
        for (int j = 0; j <= gy; j++) {
            if (j != 1) {
                potencias.add(j);
            }
        }
        for (Racional r : candidatos(enX, x)) {
            for (int j : potencias) {
                Polinomio raiz = Polinomio.termino(r, Monomio.de(y, j));
                if (!Multivariable.sustituir(p, x, raiz).esCero()) {
                    continue;
                }
                return dividir(p, x, y, n, raiz);
            }
        }
        throw noAplica("Ningún x = r·" + y + "ʲ (con r entre los divisores) anula el polinomio");
    }

    private static List<Racional> candidatos(Polinomio enX, char x) {
        BigInteger a0 = enX.constante().num();
        BigInteger an = enX.coeficiente(Monomio.de(x, enX.gradoEn(x))).num();
        TreeSet<Racional> positivos = new TreeSet<>();
        for (BigInteger num : Multivariable.divisores(a0)) {
            for (BigInteger den : Multivariable.divisores(an)) {
                positivos.add(Racional.de(num).dividir(Racional.de(den)));
            }
        }
        List<Racional> r = new ArrayList<>();
        for (Racional q : positivos) {
            r.add(q);
            r.add(q.negar());
            if (r.size() >= CANDIDATOS_MAXIMOS) {
                break;
            }
        }
        return r;
    }

    private ResultadoMetodo dividir(Polinomio p, char x, char y, int n, Polinomio raiz) {
        Polinomio divisor = Polinomio.variable(x).restar(raiz);
        String rt = Formato.polinomio(raiz);
        List<String> lineas = new ArrayList<>();
        lineas.add("Con " + y + " como parámetro, " + x + " = " + rt + " anula el polinomio: p(" + rt + ", " + y + ") = 0");
        Polinomio actual = p;
        int multiplicidad = 0;
        while (actual.gradoEn(x) >= 1 && Multivariable.sustituir(actual, x, raiz).esCero()) {
            List<Polinomio> coef = new ArrayList<>();
            for (int k = actual.gradoEn(x); k >= 0; k--) {
                coef.add(Multivariable.coeficienteEn(actual, x, k));
            }
            List<Polinomio> abajo = new ArrayList<>();
            Polinomio acumulado = Polinomio.CERO;
            for (Polinomio a : coef) {
                acumulado = a.sumar(acumulado.multiplicar(raiz));
                abajo.add(acumulado);
            }
            lineas.add("Ruffini con " + x + " = " + rt + ":  " + fila(coef) + "  →  " + fila(abajo.subList(0, abajo.size() - 1))
                    + " | residuo " + Formato.polinomio(abajo.getLast()));
            Polinomio cociente = Multivariable.dividir(actual, divisor).orElseThrow();
            actual = cociente;
            multiplicidad++;
            if (multiplicidad > 1) {
                lineas.add(x + " = " + rt + " vuelve a anular el cociente: el factor (" + Formato.polinomio(divisor)
                        + ") se repite");
            }
        }
        lineas.add("Cociente: " + Formato.polinomio(actual));
        List<Factor> factores = new ArrayList<>();
        factores.add(new Factor(divisor, multiplicidad));
        factores.add(Factor.de(actual));
        return resultado(x + " = " + rt + " anula el polinomio: (" + Formato.polinomio(divisor) + ") es un factor y se "
                + "divide por Ruffini respecto de " + x, p, factores, new TrabajoIntermedio.Agrupacion(lineas));
    }

    private static String fila(List<Polinomio> ps) {
        List<String> r = new ArrayList<>();
        for (Polinomio q : ps) {
            r.add(Formato.polinomio(q));
        }
        return String.join(" | ", r);
    }

    @Override
    public String pista() {
        return "Míralo como un polinomio de una sola letra y busca un valor de esa letra que lo haga cero.";
    }
}
