package pe.mathnova.presentation.resolucion;

import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.presentation.grafica.DatosGrafica;

/**
 * Simplificar una fracción de polinomios: la gráfica con sus huecos, propiedades y asíntotas en
 * Análisis, y en Exploración lo usado en el numerador y el denominador más la comparación Q/R/C (la
 * fracción simplificada con arriba y abajo factorizados en cada campo).
 */
public record ContenidoFraccion(String latexSolucion, Optional<DatosGrafica> grafica,
                                Optional<VistaPropiedades> propiedades, Optional<Asintotas> asintotas,
                                VistaExploracion exploracion, Optional<String> comparacion)
        implements ContenidoPestanas {

    public static ContenidoFraccion de(Simplificacion s, VistaResolucion v, Optional<Asintotas> asintotas,
                                       String texto, boolean codigos) {
        Optional<DatosGrafica> grafica = DatosGrafica.de(s);
        return new ContenidoFraccion(v.latexFormula(), grafica, grafica.flatMap(VistaPropiedades::de), asintotas,
                VistaExploracion.de(List.of(s.numerador(), s.denominador()), codigos), Optional.of(texto));
    }
}
