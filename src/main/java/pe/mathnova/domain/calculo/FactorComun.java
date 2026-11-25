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

public final class FactorComun {

    public static final String SIMPLIFICAR = "P01";

    private FactorComun() {
    }

    public record Cancelacion(PasoLimite paso, Expresion simplificada, List<String> restricciones) {
    }

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

    public static List<String> restricciones(List<PasoLimite> pasos) {
        return pasos.stream().filter(p -> SIMPLIFICAR.equals(p.codigo()))
                .filter(p -> p.trabajo() instanceof TrabajoIntermedio.Agrupacion).findFirst()
                .map(p -> ((TrabajoIntermedio.Agrupacion) p.trabajo()).grupos().stream()
                        .filter(l -> l.contains("≠") && !l.contains("=")).toList())
                .orElse(List.of());
    }

    public static List<PasoLimite> hijos(Simplificacion s) {
        List<PasoLimite> hijos = new ArrayList<>();
        s.numerador().pasos().forEach(p -> hijos.add(deFactorizacion(p, "numerador")));
        s.denominador().pasos().forEach(p -> hijos.add(deFactorizacion(p, "denominador")));
        return hijos;
    }

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

    private static String pizarra(String restriccion) {
        return restriccion.replace("-", ImpresoraPizarra.MENOS);
    }
}
