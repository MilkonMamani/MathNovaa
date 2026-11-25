package pe.mathnova.presentation.resolucion;

import java.util.Optional;

import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.presentation.grafica.DatosGrafica;

public record ContenidoEcuacion(String latexSolucion, Optional<DatosGrafica> grafica,
                                Optional<VistaPropiedades> propiedades, VistaExploracion exploracion,
                                Optional<String> comparacion) implements ContenidoPestanas {

    public static ContenidoEcuacion de(ResolucionEcuacion r, VistaResolucion v, boolean codigos) {
        Resolucion f = r.factorizacion();
        if (f == null) {
            return new ContenidoEcuacion(v.latexFormula(), Optional.empty(), Optional.empty(),
                    VistaExploracion.vacia(), Optional.empty());
        }
        return new ContenidoEcuacion(v.latexFormula(), DatosGrafica.de(f), VistaPropiedades.de(f),
                VistaExploracion.de(f, codigos), Optional.of(ImpresoraTexto.imprimir(f.entrada())));
    }
}
