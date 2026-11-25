package pe.mathnova.domain.calculo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.expresion.ImpresoraPizarra;

/**
 * "Factorizar y simplificar" antes de la operación principal (límite, derivada o integral) de un cociente de
 * polinomios: si el numerador y el denominador comparten un factor de grado mayor que cero, se factorizan con el
 * orquestador (en Q, exacto), se cancela el factor y la operación sigue sobre la fracción simplificada. La
 * cancelación vale donde el denominador original no se anula; esas restricciones viajan con el paso.
 *
 * <p>Se mira la expresión tal como la escribió el alumno, antes de cualquier simplificación interna que borre el
 * factor o la restricción. Si no hay factor común, no hay paso: factorizar por sí solo no justifica uno.
 */
public final class FactorComun {

    /** Derivadas e integrales: "Simplificar fracción" (P01) con la factorización anidada. */
    public static final String SIMPLIFICAR = "P01";

    private FactorComun() {
    }

    /**
     * El paso que factoriza y cancela, y lo que queda.
     *
     * @param paso          con los métodos de factorización como hijos ("Ruffini · numerador"…)
     * @param simplificada  la fracción sin el factor común
     * @param restricciones los valores que anulan el denominador original: "x ≠ 3"
     */
    public record Cancelacion(PasoLimite paso, Expresion simplificada, List<String> restricciones) {
    }

    /** La simplificación de f si es un cociente de polinomios con un factor común; vacío si no. */
    public static Optional<Simplificacion> buscar(Expresion f, OrquestadorFactorizacion orquestador) {
        if (!SimplificadorFracciones.esFraccion(f)) {
            return Optional.empty();
        }
        try {
            Simplificacion s = new SimplificadorFracciones(orquestador).simplificar(f);
            return s.cancelados().isEmpty() ? Optional.empty() : Optional.of(s);
        } catch (RuntimeException noSe) {
            return Optional.empty();
        }
    }

    /**
     * Para una derivada o una integral: el paso {@link #SIMPLIFICAR} con los métodos del numerador y del
     * denominador como hijos.
     */
    public static Optional<Cancelacion> cancelar(Expresion f, OrquestadorFactorizacion orquestador) {
        return buscar(f, orquestador).map(s -> {
            Expresion g = s.aExpresion();
            List<String> restricciones = s.restricciones().stream().map(FactorComun::pizarra).toList();
            List<String> lineas = new ArrayList<>();
            lineas.add(ImpresoraPizarra.imprimir(f) + " = " + factorizada(s));
            lineas.add("= " + ImpresoraPizarra.imprimir(g));
            if (!restricciones.isEmpty()) {
                lineas.add(String.join(", ", restricciones));
            }
            String porque = "El numerador y el denominador comparten un factor: se factorizan y se cancela. "
                    + s.justificacion() + "; la fracción que queda vale lo mismo"
                    + (restricciones.isEmpty() ? "" : " para " + String.join(" y ", restricciones));
            PasoLimite paso = new PasoLimite(SIMPLIFICAR, "Factorizar y simplificar", porque, f, g,
                    new TrabajoIntermedio.Agrupacion(lineas), "Factorizar numerador y denominador", hijos(s));
            return new Cancelacion(paso, g, restricciones);
        });
    }

    /**
     * Dónde vale el resultado si se simplificó un factor común antes de la operación: "x ≠ 3" (del paso
     * {@link #SIMPLIFICAR}); vacío si no se canceló nada.
     */
    public static List<String> restricciones(List<PasoLimite> pasos) {
        return pasos.stream().filter(p -> SIMPLIFICAR.equals(p.codigo()))
                .filter(p -> p.trabajo() instanceof TrabajoIntermedio.Agrupacion).findFirst()
                .map(p -> ((TrabajoIntermedio.Agrupacion) p.trabajo()).grupos().stream()
                        .filter(l -> l.contains("≠") && !l.contains("=")).toList())
                .orElse(List.of());
    }

    /** Los métodos de factorización, con la parte que factoriza cada uno: "Ruffini · numerador". */
    public static List<PasoLimite> hijos(Simplificacion s) {
        List<PasoLimite> hijos = new ArrayList<>();
        s.numerador().pasos().forEach(p -> hijos.add(deFactorizacion(p, "numerador")));
        s.denominador().pasos().forEach(p -> hijos.add(deFactorizacion(p, "denominador")));
        return hijos;
    }

    /** "((x + 3)(x − 3))/(x − 3)": el numerador y el denominador factorizados, en el formato del parser. */
    public static String factorizada(Simplificacion s) {
        return envolver(s.numerador().resultado().aTexto()) + "/" + envolver(s.denominador().resultado().aTexto());
    }

    private static String envolver(String t) {
        return t.startsWith("(") && t.endsWith(")") && t.indexOf(')') == t.length() - 1 ? t : "(" + t + ")";
    }

    private static PasoLimite deFactorizacion(Paso p, String parte) {
        return new PasoLimite(p.codigoTecnica(), Tecnicas.nombreCorto(p.codigoTecnica()) + " · " + parte,
                p.justificacion(), p.antes().aExpresion(), p.despuesComoForma().aExpresion(), p.trabajo());
    }

    /** "x ≠ -3" → "x ≠ −3". */
    private static String pizarra(String restriccion) {
        return restriccion.replace("-", ImpresoraPizarra.MENOS);
    }
}
