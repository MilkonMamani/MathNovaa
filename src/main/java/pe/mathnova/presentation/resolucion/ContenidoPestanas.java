package pe.mathnova.presentation.resolucion;

import java.util.Optional;

import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.presentation.grafica.DatosGrafica;

/**
 * Qué muestran las pestañas Gráfica, Análisis y Exploración para un tipo de resolución (Sprint 5).
 * Cada tipo decide lo suyo en su implementación, sin JavaFX; el controlador solo lo dibuja.
 *
 * <ul>
 *   <li>{@link ContenidoFactorizacion}: polinomios y expresiones con funciones.</li>
 *   <li>{@link ContenidoFraccion}: simplificar una fracción de polinomios.</li>
 *   <li>{@link ContenidoEcuacion}: ecuaciones polinómicas.</li>
 *   <li>{@link ContenidoLimite}: límites.</li>
 *   <li>{@link ContenidoDerivada} y {@link ContenidoIntegral}: derivadas e integrales (Sprint 6).</li>
 *   <li>{@link ContenidoParciales}: parciales, gradiente, derivada direccional y regla de la cadena (Sprint 7).</li>
 *   <li>{@link ContenidoLimiteDoble}, {@link ContenidoExtremos} y {@link ContenidoIntegralMultiple} (Sprint 8).</li>
 * </ul>
 */
public sealed interface ContenidoPestanas
        permits ContenidoFactorizacion, ContenidoFraccion, ContenidoEcuacion, ContenidoLimite, ContenidoDerivada,
        ContenidoIntegral, ContenidoParciales, ContenidoLimiteDoble, ContenidoExtremos, ContenidoIntegralMultiple {

    // ---------------------------------------------------------------- Gráfica

    /** Lo que va grande en la tarjeta Solución, en LaTeX. */
    String latexSolucion();

    /** HU35; vacío si la función no es de una variable. */
    Optional<DatosGrafica> grafica();

    /**
     * Sprint 8: las curvas de nivel de f(x, y) (o la región de una integral múltiple); si está, la Gráfica la
     * dibuja en lugar de {@link #grafica()}.
     */
    default Optional<pe.mathnova.presentation.grafica.DatosCurvasNivel> curvasNivel() {
        return Optional.empty();
    }

    // ---------------------------------------------------------------- Análisis

    /** RF66: propiedades y recta de signos; también van en el panel plegable de la derecha. */
    default Optional<VistaPropiedades> propiedades() {
        return Optional.empty();
    }

    /**
     * La factorización de un polinomio de una variable: propiedades, raíces y conjunto solución. Si está, va en
     * lugar de {@link #propiedades()} y la Pizarra no repite las propiedades debajo.
     */
    default Optional<AnalisisFactorizacion> analisisFactorizacion() {
        return Optional.empty();
    }

    /** La tabla de valores de un límite, que va primero en Análisis. */
    default Optional<TablaLimite> tabla() {
        return Optional.empty();
    }

    /** Sprint 6: secciones propias (puntos críticos, crecimiento, verificación, Simpson…). */
    default java.util.List<SeccionAnalisis> secciones() {
        return java.util.List.of();
    }

    /** RF26, después de lo demás. */
    default Optional<Asintotas> asintotas() {
        return Optional.empty();
    }

    // ---------------------------------------------------------------- Exploración

    VistaExploracion exploracion();

    /**
     * El texto (sección 9) que se compara en Q, R y C, o vacío si la comparación no se muestra (en un
     * límite no tiene sentido).
     */
    Optional<String> comparacion();
}
