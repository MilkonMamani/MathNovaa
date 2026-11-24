package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaExtendida;
import pe.mathnova.domain.factorizacion.Resolucion;

/**
 * Qué estructura tiene la factorización en un campo, en una frase para la comparación Q / R / C:
 * "Irreducible sobre los racionales", "Un factor lineal y un factor cuadrático irreducible", "Tres factores
 * lineales: una raíz real y dos complejas conjugadas". Se cuenta sobre el resultado del motor.
 */
public final class DescripcionCampo {

    private static final String[] MASCULINO = {"cero", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete",
            "ocho", "nueve", "diez"};
    private static final String[] FEMENINO = {"cero", "una", "dos", "tres", "cuatro", "cinco", "seis", "siete",
            "ocho", "nueve", "diez"};

    private DescripcionCampo() {
    }

    public static String de(Resolucion r, Campo campo) {
        if (r == null) {
            return null;
        }
        Map<Integer, Integer> grados = new TreeMap<>();
        int reales = 0;
        int complejas = 0;
        boolean repetidos = false;
        if (r.extension() != null && campo != Campo.Q) {
            if (r.extension().aproximada()) {
                return "Sin forma exacta con radicales: las raíces se aproximan (≈)";
            }
            for (FormaExtendida.Factor f : r.extension().resultado().factores()) {
                int g = f.polinomio().grado();
                grados.merge(g, f.multiplicidad(), Integer::sum);
                repetidos |= f.multiplicidad() > 1;
                if (g == 1) {
                    if (f.polinomio().coeficiente(0).esReal()) {
                        reales += f.multiplicidad();
                    } else {
                        complejas += f.multiplicidad();
                    }
                }
            }
        } else {
            for (Factor f : r.resultado().factores()) {
                int g = f.polinomio().esCero() ? 0 : f.polinomio().grado();
                grados.merge(g, f.multiplicidad(), Integer::sum);
                repetidos |= f.multiplicidad() > 1;
            }
        }
        grados.remove(0);
        int total = grados.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "Una constante: no hay factores";
        }
        if (total == 1) {
            int g = grados.keySet().iterator().next();
            if (g >= 2) {
                return "Irreducible sobre " + switch (campo) {
                    case Q -> "los racionales";
                    case R -> "los reales";
                    case C -> "los complejos";
                };
            }
            return "Un factor lineal";
        }
        List<String> partes = new ArrayList<>();
        grados.forEach((g, n) -> partes.add(numero(n, false) + " " + (n == 1 ? "factor " : "factores ") + nombre(g, n)));
        String frase = unir(partes);
        if (repetidos) {
            frase += " (contando multiplicidades)";
        }
        if (complejas > 0 && grados.keySet().equals(java.util.Set.of(1))) {
            frase += ": " + (reales == 0 ? "" : numero(reales, true) + (reales == 1 ? " raíz real y " : " raíces reales y "))
                    + numero(complejas, true) + (complejas == 1 ? " compleja" : " complejas conjugadas");
        }
        return Character.toUpperCase(frase.charAt(0)) + frase.substring(1);
    }

    private static String nombre(int grado, int n) {
        String s = n == 1 ? "" : "s";
        return switch (grado) {
            case 1 -> "lineal" + (n == 1 ? "" : "es");
            case 2 -> "cuadrático" + s + " irreducible" + (n == 1 ? "" : "s");
            default -> "de grado " + grado + " irreducible" + (n == 1 ? "" : "s");
        };
    }

    private static String numero(int n, boolean femenino) {
        String[] t = femenino ? FEMENINO : MASCULINO;
        return n < t.length ? t[n] : String.valueOf(n);
    }

    private static String unir(List<String> partes) {
        if (partes.size() == 1) {
            return partes.getFirst();
        }
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.getLast();
    }
}
