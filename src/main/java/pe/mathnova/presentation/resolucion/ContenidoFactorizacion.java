package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.presentation.grafica.DatosGrafica;

/**
 * Factorización de un polinomio o de una expresión con funciones: la gráfica, el análisis (propiedades, raíces y
 * conjunto solución, ver {@link AnalisisFactorizacion}), los métodos y la comparación Q/R/C.
 *
 * <p>Con varias variables (Sprint 7) la gráfica muestra el aviso de siempre, Análisis tiene el grado
 * total, el grado en cada variable y si es homogéneo, y Exploración los métodos M alternativos.
 */
public record ContenidoFactorizacion(String latexSolucion, Optional<DatosGrafica> grafica,
                                     Optional<VistaPropiedades> propiedades, VistaExploracion exploracion,
                                     Optional<String> comparacion, List<SeccionAnalisis> secciones,
                                     Optional<AnalisisFactorizacion> analisisFactorizacion)
        implements ContenidoPestanas {

    public ContenidoFactorizacion {
        secciones = List.copyOf(secciones);
    }

    public ContenidoFactorizacion(String latexSolucion, Optional<DatosGrafica> grafica,
                                  Optional<VistaPropiedades> propiedades, VistaExploracion exploracion,
                                  Optional<String> comparacion, List<SeccionAnalisis> secciones) {
        this(latexSolucion, grafica, propiedades, exploracion, comparacion, secciones, Optional.empty());
    }

    public ContenidoFactorizacion(String latexSolucion, Optional<DatosGrafica> grafica,
                                  Optional<VistaPropiedades> propiedades, VistaExploracion exploracion,
                                  Optional<String> comparacion) {
        this(latexSolucion, grafica, propiedades, exploracion, comparacion, List.of());
    }

    public static ContenidoFactorizacion de(Resolucion r, VistaResolucion v, String texto, boolean codigos) {
        return new ContenidoFactorizacion(v.latexFormula(), DatosGrafica.de(r), VistaPropiedades.de(r),
                VistaExploracion.de(r, codigos), Optional.of(texto), grados(r), AnalisisFactorizacion.de(r));
    }

    public static ContenidoFactorizacion de(ResolucionFunciones r, VistaResolucion v, String texto, boolean codigos) {
        return new ContenidoFactorizacion(v.latexFormula(), DatosGrafica.de(r), Optional.empty(),
                VistaExploracion.de(r, codigos), Optional.of(texto));
    }

    /** Con dos o más letras: grado total, grado en cada letra y si es homogéneo. */
    static List<SeccionAnalisis> grados(Resolucion r) {
        Polinomio p;
        try {
            p = Polinomio.desde(r.entrada());
        } catch (RuntimeException e) {
            return List.of();
        }
        if (p.variables().size() < 2) {
            return List.of();
        }
        List<VistaPropiedades.Fila> filas = new ArrayList<>();
        filas.add(new VistaPropiedades.Fila("Grado total", String.valueOf(p.grado())));
        for (char v : p.variables()) {
            filas.add(new VistaPropiedades.Fila("Grado en " + v, String.valueOf(p.gradoEn(v))));
        }
        int g = p.grado();
        boolean homogeneo = p.terminos().keySet().stream().allMatch(m -> m.grado() == g);
        filas.add(new VistaPropiedades.Fila("¿Homogéneo?", homogeneo ? "sí, todos los términos son de grado " + g
                : "no, los términos tienen grados distintos"));
        filas.add(new VistaPropiedades.Fila("Términos", String.valueOf(p.terminos().size())));
        return List.of(new SeccionAnalisis("Polinomio en varias variables", filas, List.of(), List.of(), null,
                "La gráfica es para funciones de una variable."));
    }
}
