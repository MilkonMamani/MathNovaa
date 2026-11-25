package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * M04: aspa simple homogénea, Ax² + Bxy + Cy² = (a1x + c1y)(a2x + c2y). Es el aspa simple de F13 con
 * la segunda letra en la columna de la derecha: con t = x/y, At² + Bt + C debe tener Δ cuadrado perfecto.
 */
final class AspaSimpleHomogenea extends MetodoBase {

    AspaSimpleHomogenea() {
        super("M04");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 2 && p.grado() == 2 && p.terminos().size() == 3 && Multivariable.homogeneo(p);
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        if (Contenido.tieneFracciones(p)) {
            throw noAplica("Tiene coeficientes fraccionarios");
        }
        char x = Multivariable.principal(p);
        char y = Multivariable.segunda(p);
        Racional a = p.coeficiente(Monomio.de(x, 2));
        Racional b = p.coeficiente(Monomio.de(x, 1).multiplicar(Monomio.de(y, 1)));
        Racional c = p.coeficiente(Monomio.de(y, 2));
        if (a.esCero() || b.esCero() || c.esCero()) {
            throw noAplica("Le falta el término en " + (a.esCero() ? x + "²" : b.esCero() ? "" + x + y : y + "²"));
        }
        BigInteger delta = b.num().pow(2).subtract(BigInteger.valueOf(4).multiply(a.num()).multiply(c.num()));
        String textoDelta = "Δ = " + Formato.numero(b) + "² − 4·" + Formato.numeroEnParentesis(a) + "·"
                + Formato.numeroEnParentesis(c) + " = " + delta;
        if (delta.signum() < 0) {
            throw noAplica(textoDelta + " < 0");
        }
        BigInteger s = Termino.raizEntera(delta, 2).orElseThrow(() -> noAplica(textoDelta + " no es un cuadrado perfecto"));
        Racional dosA = a.multiplicar(Racional.de(2));
        Racional r1 = b.negar().sumar(Racional.de(s)).dividir(dosA);
        Racional r2 = b.negar().restar(Racional.de(s)).dividir(dosA);
        Racional[] fila1 = {Racional.de(r1.den()), r1.negar().multiplicar(Racional.de(r1.den()))};
        Racional[] fila2 = {Racional.de(r2.den()), r2.negar().multiplicar(Racional.de(r2.den()))};
        if (fila1[0].compareTo(fila2[0]) < 0) {
            Racional[] t = fila1;
            fila1 = fila2;
            fila2 = t;
        }
        Termino x1 = new Termino(fila1[0], Monomio.de(x, 1));
        Termino x2 = new Termino(fila2[0], Monomio.de(x, 1));
        Termino y1 = new Termino(fila1[1], Monomio.de(y, 1));
        Termino y2 = new Termino(fila2[1], Monomio.de(y, 1));
        Termino cruce1 = x1.multiplicar(y2);
        Termino cruce2 = x2.multiplicar(y1);
        String verificacion = Formato.termino(x1) + "·" + parentesis(y2) + " + " + Formato.termino(x2) + "·"
                + parentesis(y1) + " = " + Formato.suma(List.of(cruce1, cruce2)) + " = "
                + Formato.termino(new Termino(b, Monomio.de(x, 1).multiplicar(Monomio.de(y, 1))));
        var aspa = new TrabajoIntermedio.AspaSimple(Formato.termino(x1) + "    " + Formato.termino(y1),
                Formato.termino(x2) + "    " + Formato.termino(y2), verificacion, x, fila1[0], fila1[1], fila2[0],
                fila2[1], y);
        List<Factor> factores = new ArrayList<>();
        Racional k = a.dividir(fila1[0].multiplicar(fila2[0]));
        if (!k.equals(Racional.UNO)) {
            factores.add(Factor.de(Polinomio.constante(k)));
        }
        factores.add(Factor.de(lineal(x, fila1[0], y, fila1[1])));
        factores.add(Factor.de(lineal(x, fila2[0], y, fila2[1])));
        return resultado("Es homogéneo de grado 2: con t = " + x + "/" + y + ", " + textoDelta + " = " + s
                + "², cuadrado perfecto", p, factores, aspa);
    }

    private static String parentesis(Termino t) {
        String s = Formato.termino(t);
        return t.coeficiente().signo() < 0 ? "(" + s + ")" : s;
    }

    static Polinomio lineal(char x, Racional a, char y, Racional c) {
        return Polinomio.termino(a, Monomio.de(x, 1)).sumar(Polinomio.termino(c, Monomio.de(y, 1)));
    }

    /** Sprint 9, parte D: orienta sin decir el nombre del método. */
    @Override
    public String pista() {
        return "Todos los términos tienen el mismo grado: míralo como un trinomio en x y en y, y cruza los productos de los extremos.";
    }
}
