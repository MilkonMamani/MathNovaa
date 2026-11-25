package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

public final class FactorComun extends MetodoBase {

    private final boolean fraccionario;

    private FactorComun(boolean fraccionario) {
        super(fraccionario ? "F02" : "F01");
        this.fraccionario = fraccionario;
    }

    public static FactorComun monomio() {
        return new FactorComun(false);
    }

    public static FactorComun fraccionario() {
        return new FactorComun(true);
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        if (p.terminos().size() < 2) {
            throw noAplica("Es un solo término");
        }
        boolean hayFracciones = Contenido.tieneFracciones(p);
        if (hayFracciones && !fraccionario) {
            throw noAplica("Hay coeficientes fraccionarios: corresponde F02");
        }
        if (!hayFracciones && fraccionario) {
            throw noAplica("Todos los coeficientes son enteros: corresponde F01");
        }
        Racional c = Contenido.de(p);
        Monomio m = Contenido.monomioComun(p);
        boolean negativo = c.signo() < 0;
        Racional abs = negativo ? c.negar() : c;

        String coeficientes = fraccionario ? textoContenido(p, abs) : textoMcd(p, abs);
        String letras = textoLetras(p, m);
        if (abs.equals(Racional.UNO) && m.equals(Monomio.UNO) && !negativo) {
            throw noAplica(coeficientes + " y " + letras);
        }
        String justificacion = negativo
                ? coeficientes + ", " + letras + " y el coeficiente principal es negativo: se extrae "
                        + Formato.termino(new Termino(c, m))
                : coeficientes + " y " + letras;

        List<Factor> factores = new ArrayList<>();
        if (!c.equals(Racional.UNO)) {
            factores.add(Factor.de(Polinomio.constante(c)));
        }
        if (!m.equals(Monomio.UNO)) {
            factores.add(Factor.de(Polinomio.termino(Racional.UNO, m)));
        }
        factores.add(Factor.de(Contenido.dividir(p, c, m)));
        return resultado(justificacion, p, factores, null);
    }

    private static String textoMcd(Polinomio p, Racional mcd) {
        List<BigInteger> valores = p.terminos().values().stream().map(r -> r.num().abs()).toList();
        return "MCD(" + Formato.lista(valores) + ") = " + mcd;
    }

    private static String textoContenido(Polinomio p, Racional contenido) {
        List<BigInteger> numeradores = p.terminos().values().stream().map(r -> r.num().abs()).toList();
        List<BigInteger> denominadores = p.terminos().values().stream().map(Racional::den).toList();
        return "Contenido = MCD(" + Formato.lista(numeradores) + ")/MCM(" + Formato.lista(denominadores)
                + ") = " + contenido;
    }

    private static String textoLetras(Polinomio p, Monomio comun) {
        SortedSet<Character> letras = p.variables();
        if (comun.equals(Monomio.UNO)) {
            return letras.size() == 1
                    ? "no todos los términos tienen " + letras.first()
                    : "ninguna letra está en todos los términos";
        }
        return letras.size() == 1
                ? "la menor potencia de " + letras.first() + " es " + Formato.monomio(comun)
                : "el monomio común es " + Formato.monomio(comun);
    }

    @Override
    public String pista() {
        return codigo().equals("F02")
                ? "Mira los denominadores: ¿puedes sacar una fracción que deje todos los coeficientes enteros?"
                : "Fíjate si todos los términos comparten un número o una letra.";
    }
}
