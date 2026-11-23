package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.parser.Parser;

/**
 * constante × factores. {@link #normalizada} la deja en la forma final de RN12: factores
 * primitivos con coeficiente principal positivo, repetidos agrupados como potencia y ordenados
 * (primero las letras solas, luego por grado y, a igual grado, por raíz de menor a mayor).
 */
public record FormaFactorizada(Racional constante, List<Factor> factores) {

    public FormaFactorizada {
        Objects.requireNonNull(constante, "constante");
        factores = List.copyOf(factores);
    }

    /** RN12. Las constantes y los contenidos de los factores pasan a la constante. */
    public static FormaFactorizada normalizada(Racional constante, List<Factor> factores) {
        Racional c = constante;
        Map<Polinomio, Integer> agrupados = new LinkedHashMap<>();
        for (Factor f : factores) {
            Polinomio p = f.polinomio();
            int m = f.multiplicidad();
            if (p.esConstante()) {
                c = c.multiplicar(p.constante().potencia(m));
            } else if (p.terminos().size() == 1) {
                var t = p.terminos().entrySet().iterator().next();
                c = c.multiplicar(t.getValue().potencia(m));
                t.getKey().exponentes().forEach((letra, e) ->
                        agrupados.merge(Polinomio.variable(letra), e * m, Integer::sum));
            } else {
                Racional contenido = Contenido.de(p);
                c = c.multiplicar(contenido.potencia(m));
                agrupados.merge(Contenido.dividir(p, contenido, Monomio.UNO), m, Integer::sum);
            }
        }
        if (c.esCero()) {
            return new FormaFactorizada(Racional.CERO, List.of());
        }
        List<Factor> lista = new ArrayList<>();
        agrupados.forEach((p, m) -> lista.add(new Factor(p, m)));
        lista.sort(ORDEN);
        return new FormaFactorizada(c, lista);
    }

    private static final Comparator<Factor> ORDEN = Comparator
            .comparingInt((Factor f) -> f.polinomio().terminos().size() == 1 ? 0 : 1)
            .thenComparingInt(f -> f.polinomio().grado())
            .thenComparing(f -> raiz(f.polinomio()), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(f -> f.polinomio().toString());

    /** Raíz de un factor ax + b en una sola letra: -b/a. null en otro caso. */
    private static Racional raiz(Polinomio p) {
        if (p.grado() != 1 || p.variables().size() != 1) {
            return null;
        }
        char v = p.variables().first();
        return p.constante().negar().dividir(p.coeficiente(Monomio.de(v, 1)));
    }

    /** constante × Π factor^multiplicidad (HU18). */
    public Polinomio expandir() {
        Polinomio r = Polinomio.constante(constante);
        for (Factor f : factores) {
            r = r.multiplicar(f.expandir());
        }
        return r;
    }

    /**
     * Texto en el formato del banco: "2x^2(x^2 - 2)", "(-1/5)(3x - 5)", "-(x + 6)(x - 6)",
     * "(4x + 9)^2". Se vuelve a leer con el parser.
     */
    public String aTexto() {
        if (factores.isEmpty()) {
            return constante.toString();
        }
        StringBuilder sb = new StringBuilder();
        if (constante.equals(Racional.UNO.negar())) {
            sb.append('-');
        } else if (!constante.equals(Racional.UNO)) {
            sb.append(constante.esEntero() ? constante.toString() : "(" + constante + ")");
        }
        boolean solo = factores.size() == 1 && factores.getFirst().multiplicidad() == 1 && sb.isEmpty();
        for (Factor f : factores) {
            Polinomio p = f.polinomio();
            boolean monomioSimple = p.terminos().size() == 1 && Contenido.principal(p).equals(Racional.UNO);
            String texto = p.toString();
            if (monomioSimple && (p.variables().size() == 1 || f.multiplicidad() == 1)) {
                sb.append(texto);
            } else if (solo) {
                sb.append(texto);
            } else {
                sb.append('(').append(texto).append(')');
            }
            if (f.multiplicidad() > 1) {
                sb.append('^').append(f.multiplicidad());
            }
        }
        return sb.toString();
    }

    /** Árbol de aTexto(), para dibujarlo con ImpresoraLatex. */
    public Expresion aExpresion() {
        return Parser.parse(aTexto());
    }
}
