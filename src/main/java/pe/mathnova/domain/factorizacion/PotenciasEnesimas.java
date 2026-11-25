package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;

public final class PotenciasEnesimas extends MetodoBase {

    public PotenciasEnesimas() {
        super("F11");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.terminos().size() == 2 && p.grado() >= 5;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        List<Termino> ts = Termino.de(p);
        if (ts.size() != 2) {
            throw noAplica("Tiene " + ts.size() + " términos; hacen falta 2");
        }
        Termino t1 = ts.get(0);
        Termino t2 = ts.get(1);
        if (!t1.positivo()) {
            throw noAplica("El primer término es negativo: primero se extrae −1 (F01)");
        }
        for (int n = 5; n <= p.grado(); n += 2) {
            Optional<Termino> a = t1.raiz(n);
            Optional<Termino> b = t2.raiz(n);
            if (a.isPresent() && b.isPresent()) {
                return factorizar(p, t1, t2, a.get(), b.get(), n);
            }
        }
        throw noAplica("No hay un exponente impar n ≥ 5 con " + Formato.termino(t1) + " y "
                + Formato.termino(t2.abs()) + " potencias n-ésimas exactas");
    }

    private ResultadoMetodo factorizar(Polinomio p, Termino t1, Termino t2, Termino a, Termino b, int n) {
        Termino menosB = b.negar();
        Polinomio cociente = Polinomio.CERO;
        for (int k = 0; k < n; k++) {
            cociente = cociente.sumar(a.potencia(n - 1 - k).multiplicar(menosB.potencia(k)).aPolinomio());
        }
        boolean suma = t2.positivo();
        String justificacion = Formato.termino(t1) + " = " + Formato.base(a) + Formato.superindice(n) + " y "
                + Formato.termino(t2.abs()) + " = " + Formato.base(b.abs()) + Formato.superindice(n)
                + ", con n = " + n + " impar";
        String identidad = suma
                ? "aⁿ + bⁿ = (a + b)(aⁿ⁻¹ − aⁿ⁻²b + … + bⁿ⁻¹)"
                : "aⁿ − bⁿ = (a − b)(aⁿ⁻¹ + aⁿ⁻²b + … + bⁿ⁻¹)";
        var trabajo = new TrabajoIntermedio.Comparacion(identidad + " con a = " + Formato.termino(a) + ", b = "
                + Formato.termino(b.abs()) + " y n = " + n);
        List<Factor> factores = List.of(Factor.de(a.aPolinomio().sumar(b.aPolinomio())), Factor.de(cociente));
        return resultado(justificacion, p, factores, trabajo);
    }

    @Override
    public String pista() {
        return "Son dos términos con el mismo exponente alto: piensa en aⁿ − bⁿ o aⁿ + bⁿ.";
    }
}
