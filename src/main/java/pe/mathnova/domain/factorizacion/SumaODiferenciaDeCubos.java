package pe.mathnova.domain.factorizacion;

import java.util.List;

import pe.mathnova.domain.algebra.Polinomio;

public final class SumaODiferenciaDeCubos extends MetodoBase {

    private final boolean suma;

    public SumaODiferenciaDeCubos(boolean suma) {
        super(suma ? "F08" : "F09");
        this.suma = suma;
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
        if (!t1.positivo()) {
            throw noAplica("El primer término es negativo: primero se extrae −1 (F01)");
        }
        if (t2.positivo() != suma) {
            throw noAplica(suma ? "Es una diferencia: corresponde F09" : "Es una suma: corresponde F08");
        }
        Termino segundo = t2.abs();
        Termino a = t1.raiz(3).orElseThrow(() -> noAplica(Formato.termino(t1) + " no es un cubo perfecto"));
        Termino b = segundo.raiz(3).orElseThrow(() -> noAplica(Formato.termino(segundo) + " no es un cubo perfecto"));

        Polinomio pa = a.aPolinomio();
        Polinomio pb = b.aPolinomio();
        Polinomio ab = a.multiplicar(b).aPolinomio();
        Polinomio cuadrados = pa.potencia(2).sumar(pb.potencia(2));
        List<Factor> factores = suma
                ? List.of(Factor.de(pa.sumar(pb)), Factor.de(cuadrados.restar(ab)))
                : List.of(Factor.de(pa.restar(pb)), Factor.de(cuadrados.sumar(ab)));

        String justificacion = Formato.termino(t1) + " = " + Formato.base(a) + "³ y "
                + Formato.termino(segundo) + " = " + Formato.base(b) + "³";
        String identidad = suma ? "a³ + b³ = (a + b)(a² − ab + b²)" : "a³ − b³ = (a − b)(a² + ab + b²)";
        var trabajo = new TrabajoIntermedio.Comparacion(identidad + " con a = " + Formato.termino(a)
                + " y b = " + Formato.termino(b));
        return resultado(justificacion, p, factores, trabajo);
    }

    @Override
    public String pista() {
        return codigo().equals("F08")
                ? "Son dos términos sumados y cada uno es el cubo de algo."
                : "Son dos términos restados y cada uno es el cubo de algo.";
    }
}
