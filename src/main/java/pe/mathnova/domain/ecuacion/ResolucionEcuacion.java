package pe.mathnova.domain.ecuacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.factorizacion.Resolucion;

public record ResolucionEcuacion(Ecuacion entrada, char variable, List<PasoEcuacion> pasos, Polinomio polinomio,
                                 Resolucion factorizacion, List<Solucion> soluciones, List<Polinomio> sinSolucion,
                                 Tipo tipo) {

    public enum Tipo {
        CON_SOLUCIONES,
        SIN_SOLUCION,
        IDENTIDAD
    }

    public ResolucionEcuacion {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(polinomio, "polinomio");
        pasos = List.copyOf(pasos);
        soluciones = List.copyOf(soluciones);
        sinSolucion = List.copyOf(sinSolucion);
    }

    public record Solucion(Racional valor, int multiplicidad) {

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

    public record PasoEcuacion(Tipo tipo, String titulo, String detalle, List<String> lineas) {

        public enum Tipo { PASAR_A_UN_LADO, FACTORIZAR, IGUALAR_A_CERO, DESPEJAR }

        public PasoEcuacion {
            lineas = List.copyOf(lineas);
        }
    }

    public String resumen() {
        return switch (tipo) {
            case IDENTIDAD -> "Se cumple para cualquier valor de " + variable;
            case SIN_SOLUCION -> "No tiene solución en Q";
            case CON_SOLUCIONES -> String.join(", ", lineasDeSolucion(variable, soluciones));
        };
    }

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
