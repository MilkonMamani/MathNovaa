package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio.TablaRuffini;
import pe.mathnova.presentation.FormateadorTexto;

final class PasosDeRuffini {

    private PasosDeRuffini() {
    }

    static boolean esRuffini(Paso p) {
        return (p.codigoTecnica().equals("F16") || p.codigoTecnica().equals("F17"))
                && p.trabajo() instanceof TablaRuffini;
    }

    static int largoDeLaCadena(List<Paso> pasos, int desde) {
        if (!esRuffini(pasos.get(desde))) {
            return 1;
        }
        int n = 1;
        List<TablaRuffini.Division> tablas = new ArrayList<>(((TablaRuffini) pasos.get(desde).trabajo()).divisiones());
        while (desde + n < pasos.size() && esRuffini(pasos.get(desde + n))) {
            List<TablaRuffini.Division> nuevas = continuacion(tablas, (TablaRuffini) pasos.get(desde + n).trabajo());
            if (nuevas == null) {
                break;
            }
            tablas.addAll(nuevas);
            n++;
        }
        return n;
    }

    private static List<TablaRuffini.Division> continuacion(List<TablaRuffini.Division> apiladas, TablaRuffini siguiente) {
        TablaRuffini.Division ultima = apiladas.getLast();
        List<TablaRuffini.Division> ds = siguiente.divisiones();
        if (ds.getFirst().equals(ultima)) {
            return ds.subList(1, ds.size());
        }
        List<Racional> cociente = ultima.resultados().subList(0, ultima.resultados().size() - 1);
        return ds.getFirst().coeficientes().equals(cociente) ? ds : null;
    }

    static TablaRuffini tablaUnica(List<Paso> cadena) {
        TablaRuffini primera = (TablaRuffini) cadena.getFirst().trabajo();
        List<TablaRuffini.Division> tablas = new ArrayList<>(primera.divisiones());
        List<TablaRuffini.Candidato> candidatos = new ArrayList<>(primera.candidatos());
        for (Paso p : cadena.subList(1, cadena.size())) {
            TablaRuffini t = (TablaRuffini) p.trabajo();
            tablas.addAll(continuacion(tablas, t));
            for (TablaRuffini.Candidato c : t.candidatos()) {
                if (!candidatos.contains(c)) {
                    candidatos.add(c);
                }
            }
        }
        return new TablaRuffini(primera.variable(), candidatos, tablas);
    }

    static Expresion despues(List<Paso> cadena) {
        Racional constante = Racional.UNO;
        Map<Polinomio, Integer> factores = new LinkedHashMap<>();
        for (int i = 0; i < cadena.size(); i++) {
            List<Factor> fs = cadena.get(i).despues();
            int hasta = i == cadena.size() - 1 ? fs.size() : fs.size() - 1;
            for (Factor f : fs.subList(0, hasta)) {
                if (f.polinomio().esConstante()) {
                    constante = constante.multiplicar(f.polinomio().constante().potencia(f.multiplicidad()));
                } else {
                    factores.merge(f.polinomio(), f.multiplicidad(), Integer::sum);
                }
            }
        }
        List<Factor> lista = new ArrayList<>();
        factores.forEach((p, m) -> lista.add(new Factor(p, m)));
        return new FormaFactorizada(constante, lista).aExpresion();
    }

    static String justificacion(List<Paso> cadena, TablaRuffini tabla) {
        List<String> raices = tabla.divisiones().stream()
                .map(d -> tabla.variable() + " = " + FormateadorTexto.numero(d.raiz())).toList();
        String lista = raices.size() == 1 ? raices.getFirst()
                : String.join(", ", raices.subList(0, raices.size() - 1)) + " y " + raices.getLast();
        String texto = "Se aplica Ruffini " + raices.size() + " veces seguidas, cada una sobre el cociente de la "
                + "anterior, con " + lista + ".";
        String primera = cadena.getFirst().justificacion();
        int completa = primera.indexOf("Se complet");
        return completa < 0 ? texto : texto + " " + primera.substring(completa);
    }
}
