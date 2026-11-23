package pe.mathnova.domain.factorizacion;

import java.util.List;

import pe.mathnova.domain.algebra.Polinomio;

/**
 * M08: coeficientes indeterminados en varias letras, como respaldo cuando M01–M07 no aplican. Se
 * plantea p = P·Q con coeficientes por determinar; para hallarlos se pasa a una letra con la reducción
 * de Kronecker (y = x^d), se factoriza en Q y se regresa.
 */
final class CoeficientesIndeterminadosVariables extends MetodoBase {

    CoeficientesIndeterminadosVariables() {
        super("M08");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        int v = p.variables().size();
        return v >= 2 && v <= 3 && p.grado() >= 2 && p.grado() <= Multivariable.GRADO_TOTAL_MAXIMO
                && p.terminos().size() >= 2;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Multivariable.Reduccion r = Multivariable.kronecker(p)
                .orElseThrow(() -> noAplica("Solo con dos o tres letras y grado total hasta 6"));
        Polinomio q = r.factor().orElseThrow(() -> noAplica(r.completa()
                ? "Con " + r.sustitucion() + " ninguna combinación de factores regresa a un factor: no se parte"
                : "No se encontró un factor con " + r.sustitucion()));
        Polinomio h = Multivariable.dividir(p, q).orElseThrow();
        String pq = "(" + Formato.polinomio(q) + ")(" + Formato.polinomio(h) + ")";
        List<String> lineas = List.of(
                "Se plantea " + Formato.polinomio(p) + " = P·Q, con los coeficientes de P y Q por determinar",
                "Para hallarlos se pasa a una letra con " + r.sustitucion() + ": " + Formato.polinomio(r.enUnaLetra()),
                "Se factoriza en Q y se regresa: P = " + Formato.polinomio(q) + ", Q = " + Formato.polinomio(h),
                "Comprobación: " + pq + " = " + Formato.polinomio(p) + " ✓");
        return resultado("Ningún otro método de varias variables aplica: se buscan los coeficientes de los factores",
                p, List.of(Factor.de(q), Factor.de(h)), new TrabajoIntermedio.Agrupacion(lineas));
    }
}
