package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

final class AspaDobleVariables extends MetodoBase {

    AspaDobleVariables() {
        super("M05");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 2 && p.grado() == 2 && p.terminos().size() >= 4 && !Multivariable.homogeneo(p);
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        if (Contenido.tieneFracciones(p)) {
            throw noAplica("Tiene coeficientes fraccionarios");
        }
        char x = Multivariable.principal(p);
        char y = Multivariable.segunda(p);
        Monomio mx = Monomio.de(x, 1);
        Monomio my = Monomio.de(y, 1);
        BigInteger a = p.coeficiente(mx.potencia(2)).num();
        BigInteger b = p.coeficiente(mx.multiplicar(my)).num();
        BigInteger c = p.coeficiente(my.potencia(2)).num();
        BigInteger d = p.coeficiente(mx).num();
        BigInteger e = p.coeficiente(my).num();
        BigInteger f = p.constante().num();
        if (a.signum() == 0 || c.signum() == 0 || f.signum() == 0) {
            throw noAplica("El aspa doble necesita los tres extremos: " + x + "², " + y + "² y el término independiente");
        }
        for (BigInteger a1 : Multivariable.divisores(a)) {
            BigInteger a2 = a.divide(a1);
            for (BigInteger c1 : conSigno(Multivariable.divisores(c))) {
                BigInteger c2 = c.divide(c1);
                if (!a1.multiply(c2).add(a2.multiply(c1)).equals(b)) {
                    continue;
                }
                for (BigInteger f1 : conSigno(Multivariable.divisores(f))) {
                    BigInteger f2 = f.divide(f1);
                    if (a1.multiply(f2).add(a2.multiply(f1)).equals(d) && c1.multiply(f2).add(c2.multiply(f1)).equals(e)) {
                        return armar(p, x, y, a1, c1, f1, a2, c2, f2);
                    }
                }
            }
        }
        throw noAplica("Ninguna elección de los extremos cumple las tres aspas (" + x + y + ", " + y + " y " + x + ")");
    }

    private static List<BigInteger> conSigno(List<BigInteger> ds) {
        List<BigInteger> r = new ArrayList<>();
        for (BigInteger q : ds) {
            r.add(q);
            r.add(q.negate());
        }
        return r;
    }

    private ResultadoMetodo armar(Polinomio p, char x, char y, BigInteger a1, BigInteger c1, BigInteger f1,
                                  BigInteger a2, BigInteger c2, BigInteger f2) {
        Polinomio uno = factor(x, y, a1, c1, f1);
        Polinomio dos = factor(x, y, a2, c2, f2);
        var trabajo = new TrabajoIntermedio.AspaDoble(x, y, Racional.de(a1), Racional.de(c1), Racional.de(f1),
                Racional.de(a2), Racional.de(c2), Racional.de(f2));
        return resultado("Es de grado 2 en " + x + " y " + y + " con seis términos posibles: las aspas " + x + y + ", "
                + y + " y " + x + " se verifican con (" + Formato.polinomio(uno) + ") y (" + Formato.polinomio(dos) + ")",
                p, List.of(Factor.de(uno), Factor.de(dos)), trabajo);
    }

    private static Polinomio factor(char x, char y, BigInteger a, BigInteger c, BigInteger f) {
        return Polinomio.termino(Racional.de(a), Monomio.de(x, 1)).sumar(Polinomio.termino(Racional.de(c), Monomio.de(y, 1)))
                .sumar(Polinomio.constante(Racional.de(f)));
    }

    @Override
    public String pista() {
        return "Hay términos en x², xy, y², x, y y un número: prueba a cruzar los productos dos veces.";
    }
}
