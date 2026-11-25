package pe.mathnova.presentation.resolucion;

import java.util.Optional;

import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.presentation.grafica.DatosGrafica;

public sealed interface ContenidoPestanas
        permits ContenidoFactorizacion, ContenidoFraccion, ContenidoEcuacion, ContenidoLimite, ContenidoDerivada,
        ContenidoIntegral, ContenidoParciales, ContenidoLimiteDoble, ContenidoExtremos, ContenidoIntegralMultiple {

    String latexSolucion();

    Optional<DatosGrafica> grafica();

    default Optional<pe.mathnova.presentation.grafica.DatosCurvasNivel> curvasNivel() {
        return Optional.empty();
    }

    default Optional<VistaPropiedades> propiedades() {
        return Optional.empty();
    }

    default Optional<AnalisisFactorizacion> analisisFactorizacion() {
        return Optional.empty();
    }

    default Optional<TablaLimite> tabla() {
        return Optional.empty();
    }

    default java.util.List<SeccionAnalisis> secciones() {
        return java.util.List.of();
    }

    default Optional<Asintotas> asintotas() {
        return Optional.empty();
    }

    VistaExploracion exploracion();

    Optional<String> comparacion();
}
