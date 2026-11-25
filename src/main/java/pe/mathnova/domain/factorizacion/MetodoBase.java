package pe.mathnova.domain.factorizacion;

import java.util.List;

import pe.mathnova.domain.algebra.Polinomio;

abstract class MetodoBase implements MetodoFactorizacion {

    private final String codigo;

    MetodoBase(String codigo) {
        this.codigo = codigo;
    }

    @Override
    public final String codigo() {
        return codigo;
    }

    @Override
    public final String nombre() {
        return Tecnicas.nombre(codigo);
    }

    @Override
    public final Aplicabilidad puedeAplicarse(Polinomio p, Campo c) {
        try {
            intentar(p);
            return Aplicabilidad.si();
        } catch (NoAplica e) {
            return Aplicabilidad.no(e.getMessage());
        }
    }

    @Override
    public final ResultadoMetodo aplicar(Polinomio p, Campo c) {
        try {
            return intentar(p);
        } catch (NoAplica e) {
            throw new IllegalArgumentException(codigo + " no aplica: " + e.getMessage(), e);
        }
    }

    abstract ResultadoMetodo intentar(Polinomio p);

    ResultadoMetodo resultado(String justificacion, Polinomio antes, List<Factor> factores,
                              TrabajoIntermedio trabajo) {
        return new ResultadoMetodo(factores, Paso.aplicado(codigo, justificacion, antes, factores, trabajo));
    }

    static final class NoAplica extends RuntimeException {
        NoAplica(String motivo) {
            super(motivo, null, false, false);
        }
    }

    static NoAplica noAplica(String motivo) {
        return new NoAplica(motivo);
    }
}
