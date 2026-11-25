package pe.mathnova.domain.sugerencias;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.Simplificacion;

public record EjercicioResuelto(Expresion entrada, char variable, Resolucion factorizacion, Simplificacion fraccion,
                                ResolucionFunciones funciones) {

    public static Optional<EjercicioResuelto> de(Resolucion r) {
        Polinomio p;
        try {
            p = Polinomio.desde(r.entrada());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (p.variables().isEmpty()) {
            return Optional.empty();
        }
        char x = p.variables().contains('x') ? 'x' : p.variables().first();
        return Optional.of(new EjercicioResuelto(r.entrada(), x, r, null, null));
    }

    public static Optional<EjercicioResuelto> de(Simplificacion s) {
        Set<Character> letras = new TreeSet<>(s.numerador().resultado().expandir().variables());
        letras.addAll(s.denominador().resultado().expandir().variables());
        if (letras.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(new EjercicioResuelto(s.entrada(), letras.iterator().next(), null, s, null));
    }

    public static Optional<EjercicioResuelto> de(ResolucionFunciones r) {
        if (!r.factorizable()) {
            return Optional.empty();
        }
        return Optional.of(new EjercicioResuelto(r.entrada(), 'x', null, null, r));
    }

    public boolean variasVariables() {
        return pe.mathnova.domain.expresion.Expresiones.variables(entrada).size() >= 2;
    }

    public boolean esPolinomio() {
        return factorizacion != null;
    }

    public boolean esFraccion() {
        return fraccion != null;
    }

    public boolean esFunciones() {
        return funciones != null;
    }

    public String texto() {
        return ImpresoraTexto.imprimir(entrada);
    }

    public Polinomio polinomio() {
        return Polinomio.desde(entrada);
    }

    int gradoArriba() {
        Polinomio p = fraccion.arriba().expandir();
        return p.esCero() ? -1 : p.gradoEn(variable);
    }

    int gradoAbajo() {
        return fraccion.abajo().expandir().gradoEn(variable);
    }
}
