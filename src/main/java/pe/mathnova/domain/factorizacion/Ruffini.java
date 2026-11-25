package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio.TablaRuffini;

/**
 * F16: Ruffini con el teorema de las raíces racionales. Prueba los candidatos p/q en el orden de
 * RN09 hasta que uno deja residuo 0; los términos que faltan se completan con 0. Saca un solo factor
 * (qx − p) por paso; el cociente sigue en el orquestador (F17 si la raíz se repite).
 */
public final class Ruffini extends MetodoBase {

    private static final int CANDIDATOS_VISIBLES = 10;

    public Ruffini() {
        super("F16");
    }

    /** Solo desde grado 3, en una letra. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() >= 3;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        if (u.grado() < 3) {
            throw noAplica("Es de grado " + u.grado() + "; Ruffini se usa desde grado 3");
        }
        List<Racional> candidatos = RaicesRacionales.candidatos(u)
                .orElseThrow(() -> noAplica("Los coeficientes son demasiado grandes para buscar raíces"));
        List<TablaRuffini.Candidato> probados = new ArrayList<>();
        for (Racional r : candidatos) {
            Racional residuo = u.evaluar(r);
            probados.add(new TablaRuffini.Candidato(r, residuo));
            if (residuo.esCero()) {
                return conRaiz(p, u, r, probados, candidatos);
            }
        }
        throw noAplica("Ninguno de los candidatos " + RaicesRacionales.texto(candidatos, CANDIDATOS_VISIBLES)
                + " anula el polinomio: no tiene raíces racionales");
    }

    private ResultadoMetodo conRaiz(Polinomio p, Univariado u, Racional r, List<TablaRuffini.Candidato> probados,
                                    List<Racional> candidatos) {
        char v = u.variable();
        List<Integer> ceros = u.ceros();
        TablaRuffini.Division d = u.ruffini(r, ceros);
        Univariado cociente = Univariado.cociente(v, d);
        Racional den = Racional.de(r.den());
        List<Factor> factores = List.of(
                Factor.de(lineal(v, r)),
                Factor.de(cociente.multiplicar(den.inverso()).aPolinomio()));

        StringBuilder j = new StringBuilder();
        Univariado e = u.primitivo();
        j.append("Los candidatos p/q salen de los divisores de ").append(Formato.numero(abs(e.coef(0))))
                .append(" y de ").append(Formato.numero(abs(e.principal()))).append(": ")
                .append(RaicesRacionales.texto(candidatos, CANDIDATOS_VISIBLES)).append(". ")
                .append(v).append(" = ").append(Formato.numero(r)).append(" deja residuo 0.");
        if (!ceros.isEmpty()) {
            j.append(' ').append(completados(v, u.grado(), ceros));
        }
        return resultado(j.toString(), p, factores, new TablaRuffini(v, probados, List.of(d)));
    }

    /** (q·x − p) para la raíz p/q. */
    static Polinomio lineal(char v, Racional r) {
        return Polinomio.termino(Racional.de(r.den()), Monomio.de(v, 1)).sumar(Polinomio.constante(Racional.de(r.num()).negar()));
    }

    /** "Se completa con 0 el término de x² que falta" o "Se completan con 0 los términos de x² y x que faltan". */
    static String completados(char v, int grado, List<Integer> posiciones) {
        List<String> nombres = new ArrayList<>();
        for (int pos : posiciones) {
            int exp = grado - pos;
            nombres.add(exp == 0 ? "independiente" : exp == 1 ? String.valueOf(v) : v + Formato.superindice(exp));
        }
        if (nombres.size() == 1) {
            String n = nombres.getFirst();
            return n.equals("independiente") ? "Se completa con 0 el término independiente que falta."
                    : "Se completa con 0 el término de " + n + " que falta.";
        }
        String lista = String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.getLast();
        return "Se completan con 0 los términos de " + lista + " que faltan.";
    }

    static Racional abs(Racional r) {
        return r.signo() < 0 ? r.negar() : r;
    }

    /** Sprint 9, parte D: orienta sin decir el nombre del método. */
    @Override
    public String pista() {
        return "Prueba si algún divisor del término independiente (entre los divisores del coeficiente principal) hace cero el polinomio.";
    }
}
