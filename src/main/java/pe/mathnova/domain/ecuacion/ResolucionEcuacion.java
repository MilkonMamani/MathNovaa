package pe.mathnova.domain.ecuacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.factorizacion.Resolucion;

/**
 * Una ecuación polinómica resuelta en Q (HU38, RF73).
 *
 * @param entrada       la ecuación tal como se escribió
 * @param variable      la incógnita
 * @param pasos         1. pasar todo a un lado (si hace falta), 2. factorizar, 3. igualar cada factor a
 *                      cero, 4. despejar
 * @param polinomio     P(x) − Q(x), el lado que queda igualado a cero
 * @param factorizacion la factorización de ese polinomio, con sus pasos de siempre; null si es constante
 * @param soluciones    raíces racionales de menor a mayor; una raíz repetida va una sola vez
 * @param sinSolucion   factores sin raíz en Q ("x^2 + 1")
 * @param tipo          si hay soluciones, si no hay ninguna o si se cumple para todo x
 */
public record ResolucionEcuacion(Ecuacion entrada, char variable, List<PasoEcuacion> pasos, Polinomio polinomio,
                                 Resolucion factorizacion, List<Solucion> soluciones, List<Polinomio> sinSolucion,
                                 Tipo tipo) {

    public enum Tipo {
        /** Hay una o más soluciones en Q. */
        CON_SOLUCIONES,
        /** Ninguna solución en Q (por ejemplo x^2 + 1 = 0, o 0 = 5). */
        SIN_SOLUCION,
        /** 0 = 0: se cumple para cualquier valor. */
        IDENTIDAD
    }

    public ResolucionEcuacion {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(polinomio, "polinomio");
        pasos = List.copyOf(pasos);
        soluciones = List.copyOf(soluciones);
        sinSolucion = List.copyOf(sinSolucion);
    }

    /** Una raíz y cuántas veces se repite. */
    public record Solucion(Racional valor, int multiplicidad) {

        /** "" si es simple; "doble", "triple"… si se repite. */
        public String nombreMultiplicidad() {
            return switch (multiplicidad) {
                case 1 -> "";
                case 2 -> "doble";
                case 3 -> "triple";
                case 4 -> "cuádruple";
                default -> "de multiplicidad " + multiplicidad;
            };
        }
    }

    /**
     * Un paso de la resolución.
     *
     * @param titulo  "Pasar todo a un lado", "Factorizar", "Igualar cada factor a cero", "Despejar"
     * @param detalle por qué se hace
     * @param lineas  lo que se escribe en la pizarra, una ecuación por línea, en texto de la sección 9
     */
    public record PasoEcuacion(Tipo tipo, String titulo, String detalle, List<String> lineas) {

        public enum Tipo { PASAR_A_UN_LADO, FACTORIZAR, IGUALAR_A_CERO, DESPEJAR }

        public PasoEcuacion {
            lineas = List.copyOf(lineas);
        }
    }

    /** "x₁ = 2, x₂ = 3", "x = 1 (doble)", "no tiene solución en Q". */
    public String resumen() {
        return switch (tipo) {
            case IDENTIDAD -> "Se cumple para cualquier valor de " + variable;
            case SIN_SOLUCION -> "No tiene solución en Q";
            case CON_SOLUCIONES -> String.join(", ", lineasDeSolucion(variable, soluciones));
        };
    }

    /** x₁ = 2, x₂ = 3; con una sola raíz, x = 1 (sin subíndice). */
    static List<String> lineasDeSolucion(char variable, List<Solucion> soluciones) {
        List<String> r = new java.util.ArrayList<>();
        for (int i = 0; i < soluciones.size(); i++) {
            Solucion s = soluciones.get(i);
            String nombre = soluciones.size() == 1 ? String.valueOf(variable) : variable + subindice(i + 1);
            String linea = nombre + " = " + s.valor();
            if (s.multiplicidad() > 1) {
                linea += " (" + s.nombreMultiplicidad() + ")";
            }
            r.add(linea);
        }
        return r;
    }

    private static String subindice(int n) {
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(n).toCharArray()) {
            sb.append((char) ('₀' + (c - '0')));
        }
        return sb.toString();
    }
}
