package pe.mathnova.domain.calculo;

import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.calculo.derivadas.PasoDerivada;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

public final class Anidados {

    public static final String ECUACION = "EC";

    private Anidados() {
    }

    public static PasoLimite deDerivada(PasoDerivada p) {
        return deDerivada(p, null);
    }

    public static PasoLimite deDerivada(PasoDerivada p, String parte) {
        List<PasoLimite> hijos = new ArrayList<>();
        p.hijos().forEach(h -> hijos.add(deDerivada(h, null)));
        List<String> lineas = new ArrayList<>();
        lineas.add(p.formula());
        lineas.addAll(p.trabajo());
        String titulo = p.nombre() + (parte == null ? "" : " · " + parte);
        return new PasoLimite(p.codigo(), titulo, porQue(p), p.antes(), p.despues(),
                new TrabajoIntermedio.Agrupacion(lineas), hijos.isEmpty() ? null : "Reglas internas", hijos);
    }

    private static String porQue(PasoDerivada p) {
        return switch (p.codigo()) {
            case "D01" -> p.regla();
            case "D03" -> "Es una suma: la derivada de una suma es la suma de las derivadas, término a término";
            case "D04" -> p.regla().startsWith("(c·f)") ? p.regla()
                    : "Es un producto de factores que dependen de la variable: se usa la regla del producto";
            case "D05" -> "Es un cociente: se usa la regla del cociente";
            case "D06" -> "Es una función compuesta: se deriva la de afuera y se multiplica por la derivada de "
                    + "adentro (" + p.regla().replaceFirst("^.*, con ", "") + ")";
            case "D08" -> p.regla();
            case "D09" -> "La base y el exponente dependen de la variable: se toma ln, se deriva y se despeja";
            default -> "Se aplica " + p.regla();
        };
    }

    public static List<PasoLimite> deFactorizacion(Resolucion r, String parte) {
        List<PasoLimite> r2 = new ArrayList<>();
        r.pasos().forEach(p -> r2.add(deFactorizacion(p, parte)));
        return r2;
    }

    public static PasoLimite deFactorizacion(Paso p, String parte) {
        String titulo = Tecnicas.nombreCorto(p.codigoTecnica()) + (parte == null ? "" : " · " + parte);
        return new PasoLimite(p.codigoTecnica(), titulo, p.justificacion(), p.antes().aExpresion(),
                p.despuesComoForma().aExpresion(), p.trabajo());
    }

    public static List<PasoLimite> deEcuacion(ResolucionEcuacion r) {
        List<PasoLimite> pasos = new ArrayList<>();
        for (ResolucionEcuacion.PasoEcuacion p : r.pasos()) {
            List<PasoLimite> hijos = p.tipo() == ResolucionEcuacion.PasoEcuacion.Tipo.FACTORIZAR
                    && r.factorizacion() != null ? deFactorizacion(r.factorizacion(), null) : List.of();
            pasos.add(new PasoLimite(ECUACION, p.titulo(), p.detalle(), null, null,
                    new TrabajoIntermedio.Agrupacion(p.lineas()), hijos.isEmpty() ? null : "Métodos de factorización",
                    hijos));
        }
        return pasos;
    }
}
