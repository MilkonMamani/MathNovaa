package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * F01 (coeficientes enteros) y F02 (coeficientes fraccionarios): saca el contenido y la menor
 * potencia de cada letra. Si el coeficiente principal es negativo, el signo sale junto con el
 * MCD (RN05, RN06): −x² + 36 = −(x² − 36).
 */
public final class FactorComun extends MetodoBase {

    private final boolean fraccionario;

    private FactorComun(boolean fraccionario) {
        super(fraccionario ? "F02" : "F01");
        this.fraccionario = fraccionario;
    }

    /** F01. */
    public static FactorComun monomio() {
        return new FactorComun(false);
    }

    /** F02. */
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

    /** "MCD(6, 9) = 3". */
    private static String textoMcd(Polinomio p, Racional mcd) {
        List<BigInteger> valores = p.terminos().values().stream().map(r -> r.num().abs()).toList();
        return "MCD(" + Formato.lista(valores) + ") = " + mcd;
    }

    /** "Contenido = MCD(3, 1)/MCM(5, 1) = 1/5". */
    private static String textoContenido(Polinomio p, Racional contenido) {
        List<BigInteger> numeradores = p.terminos().values().stream().map(r -> r.num().abs()).toList();
        List<BigInteger> denominadores = p.terminos().values().stream().map(Racional::den).toList();
        return "Contenido = MCD(" + Formato.lista(numeradores) + ")/MCM(" + Formato.lista(denominadores)
                + ") = " + contenido;
    }

    /** "la menor potencia de x es x²", "no todos los términos tienen x". */
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
}
