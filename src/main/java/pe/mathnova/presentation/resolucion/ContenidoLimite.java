package pe.mathnova.presentation.resolucion;

import java.util.Optional;

import pe.mathnova.application.service.LimiteService;
import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.grafica.DatosGrafica;

public record ContenidoLimite(String latexSolucion, Optional<DatosGrafica> grafica, Optional<TablaLimite> tabla,
                              Optional<Asintotas> asintotas, VistaExploracion exploracion)
        implements ContenidoPestanas {

    public static ContenidoLimite de(ResolucionLimite r, LimiteService servicio, boolean codigos) {
        String conclusion = switch (r.resultado()) {
            case ResultadoLimite.Finito f -> "Los valores de f se acercan a " + f.texto() + ".";
            case ResultadoLimite.Infinito i -> "Los valores de f crecen sin cota (" + i.texto() + ").";
            case ResultadoLimite.NoExiste n -> "Por cada lado los valores van a lugares distintos.";
            case ResultadoLimite.NoCalculado n -> null;
        };
        Optional<TablaLimite> tabla;
        try {
            tabla = Optional.of(TablaLimite.de(servicio.tabla(r), r.resultado(),
                    conclusion == null ? null : FormateadorTexto.pizarraConNegativos(conclusion)));
        } catch (RuntimeException e) {
            tabla = Optional.empty();
        }
        return new ContenidoLimite(latex(r), DatosGrafica.de(r, servicio.fraccion(r)), tabla, servicio.asintotas(r),
                VistaExploracion.deLimite(servicio.caminos(r), codigos));
    }

    static String latex(ResolucionLimite r) {
        String completo = ImpresoraLatex.imprimir(r.entrada());
        String lim = prefijo(completo);
        String f = lim == null ? completo : lim + " f(" + r.variable() + ")";
        return r.resultado() instanceof ResultadoLimite.NoCalculado ? f : f + " = " + r.resultado().latex();
    }

    private static String prefijo(String latex) {
        String inicio = "\\lim_{";
        if (!latex.startsWith(inicio)) {
            return null;
        }
        int nivel = 1;
        for (int i = inicio.length(); i < latex.length(); i++) {
            char c = latex.charAt(i);
            if (c == '{') {
                nivel++;
            } else if (c == '}' && --nivel == 0) {
                return latex.substring(0, i + 1);
            }
        }
        return null;
    }

    @Override
    public Optional<String> comparacion() {
        return Optional.empty();
    }
}
