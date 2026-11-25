package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio.TablaRuffini;

public final class RuffiniRepetido extends MetodoBase {

    public RuffiniRepetido() {
        super("F17");
    }

    Optional<ResultadoMetodo> continuar(Polinomio cociente, TablaRuffini anterior) {
        TablaRuffini.Division ultima = anterior.divisiones().getLast();
        Racional r = ultima.raiz();
        Univariado sinEscalar = Univariado.cociente(anterior.variable(), ultima);
        if (sinEscalar.grado() < 1 || !sinEscalar.evaluar(r).esCero()) {
            return Optional.empty();
        }
        List<TablaRuffini.Division> tablas = new ArrayList<>(anterior.divisiones());
        Repeticion rep = repetir(sinEscalar, r, tablas);
        Polinomio ajuste = Polinomio.constante(Racional.de(r.den()).inverso());
        Polinomio resto = rep.resto().aPolinomio().multiplicar(ajuste)
                .multiplicar(Polinomio.constante(Racional.de(r.den()).potencia(rep.veces()).inverso()));
        return Optional.of(armar(cociente, anterior.variable(), r, rep.veces(), rep.veces() + 1, resto,
                List.of(new TablaRuffini.Candidato(r, Racional.CERO)), tablas));
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() >= 2;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        if (u.grado() < 2) {
            throw noAplica("Es de grado " + u.grado() + ": no puede tener raíces repetidas");
        }
        List<Racional> candidatos = RaicesRacionales.candidatos(u)
                .orElseThrow(() -> noAplica("Los coeficientes son demasiado grandes para buscar raíces"));
        List<TablaRuffini.Candidato> probados = new ArrayList<>();
        boolean hayRaiz = false;
        for (Racional r : candidatos) {
            Racional residuo = u.evaluar(r);
            probados.add(new TablaRuffini.Candidato(r, residuo));
            if (!residuo.esCero()) {
                continue;
            }
            hayRaiz = true;
            List<TablaRuffini.Division> tablas = new ArrayList<>();
            Repeticion rep = repetir(u, r, tablas);
            if (rep.veces() >= 2) {
                Polinomio resto = rep.resto().aPolinomio()
                        .multiplicar(Polinomio.constante(Racional.de(r.den()).potencia(rep.veces()).inverso()));
                return armar(p, u.variable(), r, rep.veces(), rep.veces(), resto, probados, tablas);
            }
        }
        throw noAplica(hayRaiz ? "Ninguna raíz racional se repite" : "No tiene raíces racionales");
    }

    private record Repeticion(int veces, Univariado resto) {
    }

    private static Repeticion repetir(Univariado u, Racional r, List<TablaRuffini.Division> tablas) {
        int veces = 0;
        Univariado actual = u;
        while (actual.grado() >= 1 && actual.evaluar(r).esCero()) {
            TablaRuffini.Division d = actual.ruffini(r, tablas.isEmpty() ? actual.ceros() : List.of());
            tablas.add(d);
            actual = Univariado.cociente(u.variable(), d);
            veces++;
        }
        return new Repeticion(veces, actual);
    }

    private ResultadoMetodo armar(Polinomio antes, char v, Racional r, int veces, int total, Polinomio resto,
                                  List<TablaRuffini.Candidato> candidatos, List<TablaRuffini.Division> tablas) {
        List<Factor> factores = new ArrayList<>();
        factores.add(new Factor(Ruffini.lineal(v, r), veces));
        factores.add(Factor.de(resto));
        String justificacion = v + " = " + Formato.numero(r) + " vuelve a anular el cociente: es raíz "
                + multiplicidad(total) + ", así que " + Formato.parentesis(Ruffini.lineal(v, r))
                + " queda elevado a " + total;
        return resultado(justificacion, antes, factores, new TablaRuffini(v, candidatos, tablas));
    }

    static String multiplicidad(int n) {
        return switch (n) {
            case 2 -> "doble";
            case 3 -> "triple";
            case 4 -> "cuádruple";
            default -> "de multiplicidad " + n;
        };
    }

    @Override
    public String pista() {
        return "El valor que hizo cero el polinomio, ¿vuelve a hacer cero lo que quedó?";
    }
}
