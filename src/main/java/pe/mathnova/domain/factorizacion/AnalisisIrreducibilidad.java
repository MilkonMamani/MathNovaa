package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

public final class AnalisisIrreducibilidad {

    static final String SIN_CERTIFICAR = "No se encontró una factorización en Q.";

    private static final BigInteger LIMITE = BigInteger.valueOf(1_000_000_000_000L);
    private static final int CANDIDATOS_VISIBLES = 12;

    private AnalisisIrreducibilidad() {
    }

    public static Irreducibilidad analizar(Polinomio p) {
        int grado = p.grado();
        boolean unaLetra = p.variables().size() == 1 && !Contenido.tieneFracciones(p);
        if (grado == 1) {
            return new Irreducibilidad(p, p.variables().size() >= 2 ? "A6" : "A4", "Es de grado 1", true);
        }
        if (p.variables().size() >= 2) {
            return enVarias(p);
        }
        if (grado == 2 && unaLetra) {
            Cuadratica q = Cuadratica.de(p);
            BigInteger delta = q.discriminante();
            String d = "Δ = " + Formato.numero(Racional.de(delta));
            if (delta.signum() < 0) {
                return new Irreducibilidad(p, "A1", d + " < 0", true);
            }
            if (Termino.raizEntera(delta, 2).isEmpty()) {
                return new Irreducibilidad(p, "A1", d + " no es un cuadrado perfecto", true);
            }
            return new Irreducibilidad(p, "A1", d + " es un cuadrado perfecto, pero ningún método lo partió", false);
        }
        if (grado == 3 && unaLetra) {
            return cubica(p);
        }
        if (unaLetra && grado > Kronecker.GRADO_MAXIMO) {
            return new Irreducibilidad(p, "A4", VerificacionIrreducibilidad.GRADO_ALTO, false);
        }
        if (unaLetra) {
            Kronecker.Respuesta r = Kronecker.buscar(Univariado.de(p).orElseThrow());
            if (r.irreducible()) {
                return new Irreducibilidad(p, "A4", VerificacionIrreducibilidad.IRREDUCIBLE, true);
            }
        }
        return new Irreducibilidad(p, "A4", SIN_CERTIFICAR, false);
    }

    private static Irreducibilidad enVarias(Polinomio p) {
        Optional<Multivariable.Reduccion> r = Multivariable.kronecker(p);
        if (r.isEmpty()) {
            return new Irreducibilidad(p, "A6", p.variables().size() > 3
                    ? "Con más de tres letras no se certifica" : "Con grado total mayor que 6 no se certifica", false);
        }
        Multivariable.Reduccion k = r.get();
        if (k.factor().isPresent()) {
            return new Irreducibilidad(p, "A6", "Con " + k.sustitucion() + " aparece el factor "
                    + Formato.polinomio(k.factor().get()) + ", así que todavía se puede factorizar", false);
        }
        String u = Formato.polinomio(k.enUnaLetra());
        if (k.irreducible()) {
            return new Irreducibilidad(p, "A6", "Reducción de Kronecker: con " + k.sustitucion() + " queda " + u
                    + ", y ninguna combinación de sus factores en Q regresa a un factor", true);
        }
        return new Irreducibilidad(p, "A6", "Con " + k.sustitucion() + " queda " + u
                + ", que no se pudo factorizar del todo: " + SIN_CERTIFICAR, false);
    }

    private static Irreducibilidad cubica(Polinomio p) {
        char v = p.variables().first();
        BigInteger a0 = p.constante().num().abs();
        BigInteger an = p.coeficiente(Monomio.de(v, 3)).num().abs();
        if (a0.signum() == 0) {
            return new Irreducibilidad(p, "A4", v + " = 0 es raíz", false);
        }
        if (a0.compareTo(LIMITE) > 0 || an.compareTo(LIMITE) > 0) {
            return new Irreducibilidad(p, "A4", SIN_CERTIFICAR, false);
        }
        TreeSet<Racional> candidatos = new TreeSet<>();
        for (long num : divisores(a0.longValueExact())) {
            for (long den : divisores(an.longValueExact())) {
                candidatos.add(Racional.de(num, den));
            }
        }
        for (Racional r : candidatos) {
            for (Racional x : List.of(r, r.negar())) {
                if (p.evaluar(Map.of(v, x)).esCero()) {
                    return new Irreducibilidad(p, "A4", v + " = " + Formato.numero(x)
                            + " es raíz, así que todavía se puede factorizar", false);
                }
            }
        }
        List<String> textos = new ArrayList<>();
        for (Racional r : candidatos) {
            if (textos.size() == CANDIDATOS_VISIBLES) {
                textos.add("…");
                break;
            }
            textos.add("±" + r);
        }
        return new Irreducibilidad(p, "A4", "Ninguno de los candidatos " + String.join(", ", textos)
                + " es raíz: no tiene raíces racionales", true);
    }

    private static List<Long> divisores(long n) {
        List<Long> chicos = new ArrayList<>();
        List<Long> grandes = new ArrayList<>();
        for (long d = 1; d * d <= n; d++) {
            if (n % d == 0) {
                chicos.add(d);
                if (d != n / d) {
                    grandes.addFirst(n / d);
                }
            }
        }
        chicos.addAll(grandes);
        return chicos;
    }
}
