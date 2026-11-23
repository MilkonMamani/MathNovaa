package pe.mathnova.domain.factorizacion;

import pe.mathnova.domain.algebra.Polinomio;

/**
 * Un método de una letra que también sirve en varias (F01, F05, F07, F04…), con el código de la
 * sección 2.13: M01 factor común, M03 productos notables, M02 agrupación. Hace la misma cuenta; solo
 * cambia el código del paso.
 */
final class EnVariasVariables implements MetodoFactorizacion {

    private final String codigo;
    private final MetodoFactorizacion base;

    EnVariasVariables(String codigo, MetodoFactorizacion base) {
        this.codigo = codigo;
        this.base = base;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public String nombre() {
        return Tecnicas.nombre(codigo);
    }

    /** El método de una letra que hace la cuenta ("F05"). */
    String codigoBase() {
        return base.codigo();
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return base.esCandidato(p);
    }

    @Override
    public Aplicabilidad puedeAplicarse(Polinomio p, Campo c) {
        return base.puedeAplicarse(p, c);
    }

    @Override
    public ResultadoMetodo aplicar(Polinomio p, Campo c) {
        ResultadoMetodo r = base.aplicar(p, c);
        Paso x = r.paso();
        String justificacion = base.codigo().equals("F01") || base.codigo().equals("F02") ? x.justificacion()
                : base.nombre() + ": " + x.justificacion();
        return new ResultadoMetodo(r.factores(), new Paso(x.orden(), codigo, justificacion, x.antes(), x.despues(),
                x.trabajo(), x.descartado(), x.motivoDescarte()));
    }
}
