package pe.mathnova.domain.analisis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.Resolucion;

/**
 * Propiedades de un polinomio en una variable (RF66), con valores exactos (RN02): nunca double.
 *
 * @param raices            raíces racionales con su multiplicidad, de menor a mayor; salen de los
 *                          factores de grado 1 de la forma factorizada
 * @param corteY            f(0)
 * @param signos            el signo de f en cada intervalo entre raíces consecutivas, evaluando un
 *                          punto racional del intervalo; vacío si {@code signosCompletos} es false
 * @param signosCompletos   false si algún factor irreducible puede tener raíces reales no racionales
 *                          (x² − 2): entonces los intervalos entre raíces racionales no bastan
 * @param vertice           solo en grado 2: (−b/2a, f(−b/2a))
 */
public record AnalisisPolinomio(char variable, int grado, Racional coeficientePrincipal, List<Raiz> raices,
                                Racional corteY, List<Tramo> signos, boolean signosCompletos,
                                Optional<Punto> vertice) {

    public AnalisisPolinomio {
        raices = List.copyOf(raices);
        signos = List.copyOf(signos);
    }

    /** Una raíz racional y cuántas veces se repite. */
    public record Raiz(Racional valor, int multiplicidad) {
    }

    /** Un punto exacto del plano. */
    public record Punto(Racional x, Racional y) {
    }

    /**
     * Un intervalo abierto entre raíces consecutivas.
     *
     * @param desde  extremo izquierdo; null es −∞
     * @param hasta  extremo derecho; null es +∞
     * @param prueba el punto racional donde se evaluó
     * @param signo  +1 o −1 (0 solo para el polinomio constante cero, que no llega aquí)
     */
    public record Tramo(Racional desde, Racional hasta, Racional prueba, int signo) {
    }

    /** Vacío si la entrada tiene más de una letra o es el polinomio cero. */
    public static Optional<AnalisisPolinomio> de(Resolucion r) {
        return de(Polinomio.desde(r.entrada()), r.resultado());
    }

    /**
     * @param p     el polinomio
     * @param forma su factorización en Q (de ahí salen las raíces racionales)
     */
    public static Optional<AnalisisPolinomio> de(Polinomio p, FormaFactorizada forma) {
        if (p.esCero() || p.variables().size() > 1) {
            return Optional.empty();
        }
        char x = p.variables().isEmpty() ? 'x' : p.variables().first();
        int grado = p.grado();
        Racional principal = grado == 0 ? p.constante() : p.coeficiente(Monomio.de(x, grado));

        List<Raiz> raices = new ArrayList<>();
        boolean completos = true;
        for (Factor f : forma.factores()) {
            Polinomio q = f.polinomio();
            if (q.grado() == 1) {
                Racional a = q.coeficiente(Monomio.de(x, 1));
                raices.add(new Raiz(q.constante().negar().dividir(a), f.multiplicidad()));
            } else if (!sinRaicesReales(q, x)) {
                completos = false;
            }
        }
        raices.sort(Comparator.comparing(Raiz::valor));

        Optional<Punto> vertice = Optional.empty();
        if (grado == 2) {
            Racional a = p.coeficiente(Monomio.de(x, 2));
            Racional b = p.coeficiente(Monomio.de(x, 1));
            Racional h = b.negar().dividir(a.multiplicar(Racional.de(2)));
            vertice = Optional.of(new Punto(h, evaluar(p, x, h)));
        }
        return Optional.of(new AnalisisPolinomio(x, grado, principal, raices, evaluar(p, x, Racional.CERO),
                completos ? signos(p, x, raices) : List.of(), completos, vertice));
    }

    /** f(valor), exacto. */
    public static Racional evaluar(Polinomio p, char x, Racional valor) {
        return p.evaluar(Map.of(x, valor));
    }

    /** Un cuadrático con discriminante negativo nunca se anula; de lo demás no se sabe. */
    private static boolean sinRaicesReales(Polinomio q, char x) {
        if (q.grado() != 2) {
            return false;
        }
        Racional a = q.coeficiente(Monomio.de(x, 2));
        Racional b = q.coeficiente(Monomio.de(x, 1));
        Racional c = q.constante();
        Racional delta = b.multiplicar(b).restar(a.multiplicar(c).multiplicar(Racional.de(BigInteger.valueOf(4))));
        return delta.signo() < 0;
    }

    /** Un punto de prueba por intervalo: antes de la primera raíz, entre cada par y después de la última. */
    private static List<Tramo> signos(Polinomio p, char x, List<Raiz> raices) {
        List<Tramo> tramos = new ArrayList<>();
        if (raices.isEmpty()) {
            tramos.add(new Tramo(null, null, Racional.CERO, evaluar(p, x, Racional.CERO).signo()));
            return tramos;
        }
        Racional dos = Racional.de(2);
        for (int i = 0; i <= raices.size(); i++) {
            Racional desde = i == 0 ? null : raices.get(i - 1).valor();
            Racional hasta = i == raices.size() ? null : raices.get(i).valor();
            Racional prueba;
            if (desde == null) {
                prueba = hasta.restar(Racional.UNO);
            } else if (hasta == null) {
                prueba = desde.sumar(Racional.UNO);
            } else {
                prueba = desde.sumar(hasta).dividir(dos);
            }
            tramos.add(new Tramo(desde, hasta, prueba, evaluar(p, x, prueba).signo()));
        }
        return tramos;
    }
}
