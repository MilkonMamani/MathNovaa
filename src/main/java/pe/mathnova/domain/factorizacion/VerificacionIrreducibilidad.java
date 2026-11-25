package pe.mathnova.domain.factorizacion;

import java.util.List;

import pe.mathnova.domain.algebra.Polinomio;

public final class VerificacionIrreducibilidad extends MetodoBase {

    static final String IRREDUCIBLE = "No se puede factorizar en Q: no tiene raíces racionales y no se parte "
            + "en factores de grado 2 o más.";
    static final String GRADO_ALTO = "Grado demasiado alto para certificar que no se puede factorizar.";

    public VerificacionIrreducibilidad() {
        super("A4");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() >= 4;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        if (u.grado() > Kronecker.GRADO_MAXIMO) {
            throw noAplica(GRADO_ALTO);
        }
        Kronecker.Respuesta r = Kronecker.buscar(u);
        if (r.factor().isEmpty()) {
            throw noAplica(r.completa() ? IRREDUCIBLE : AnalisisIrreducibilidad.SIN_CERTIFICAR);
        }
        Univariado g = r.factor().get();
        Univariado q = u.cocienteExacto(g).orElseThrow();
        String justificacion = "Probando los valores del polinomio en " + (g.grado() + 1) + " puntos (método de Kronecker), "
                + Formato.parentesis(g.aPolinomio()) + " lo divide exactamente";
        var trabajo = new TrabajoIntermedio.Agrupacion(List.of(
                Formato.polinomio(p) + " = " + Formato.parentesis(g.aPolinomio()) + Formato.parentesis(q.aPolinomio())));
        return resultado(justificacion, p, List.of(Factor.de(g.aPolinomio()), Factor.de(q.aPolinomio())), trabajo);
    }

    @Override
    public String pista() {
        return "¿Queda algún factor que se pueda partir, o ya ningún método funciona con ninguno?";
    }
}
