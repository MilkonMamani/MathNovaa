package pe.mathnova.domain.factorizacion;

import java.util.List;

import pe.mathnova.domain.algebra.Polinomio;

/** F10: a³ ± 3a²b + 3ab² ± b³ = (a ± b)³. a es la raíz cúbica del primer término y b la del último, con su signo. */
public final class CuboPerfecto extends MetodoBase {

    public CuboPerfecto() {
        super("F10");
    }

    /** Solo con 4 términos y grado 3. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.terminos().size() == 4 && p.grado() == 3;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        List<Termino> ts = Termino.de(p);
        if (ts.size() != 4) {
            throw noAplica("Tiene " + ts.size() + " términos; hacen falta 4");
        }
        Termino t1 = ts.get(0);
        Termino t4 = ts.get(3);
        Termino a = t1.raiz(3).orElseThrow(() -> noAplica(Formato.termino(t1) + " no es un cubo perfecto"));
        Termino b = t4.raiz(3).orElseThrow(() -> noAplica(Formato.termino(t4) + " no es un cubo perfecto"));
        Polinomio binomio = a.aPolinomio().sumar(b.aPolinomio());
        Polinomio cubo = binomio.potencia(3);
        if (!cubo.equals(p)) {
            throw noAplica(Formato.termino(t1) + " = " + Formato.base(a) + "³ y " + Formato.termino(t4) + " = "
                    + Formato.base(b) + "³, pero " + Formato.parentesis(binomio) + "³ = " + Formato.polinomio(cubo)
                    + ", que no coincide");
        }
        String justificacion = Formato.termino(t1) + " = " + Formato.base(a) + "³, "
                + Formato.termino(t4) + " = " + Formato.base(b) + "³, "
                + Formato.termino(ts.get(1)) + " = 3·" + Formato.base(a) + "²·" + Formato.base(b) + " y "
                + Formato.termino(ts.get(2)) + " = 3·" + Formato.base(a) + "·" + Formato.base(b) + "²";
        boolean menos = !b.positivo();
        String identidad = menos ? "a³ − 3a²b + 3ab² − b³ = (a − b)³" : "a³ + 3a²b + 3ab² + b³ = (a + b)³";
        var trabajo = new TrabajoIntermedio.Comparacion(identidad + " con a = " + Formato.termino(a)
                + " y b = " + Formato.termino(b.abs()));
        return resultado(justificacion, p, List.of(new Factor(binomio, 3)), trabajo);
    }
}
