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

/**
 * Lo que miran las reglas de sugerencias: un polinomio de una letra ya factorizado (en Q, R o C), una
 * fracción de polinomios de una letra ya simplificada, o una expresión con funciones ya factorizada.
 * Exactamente uno de {@code factorizacion}, {@code fraccion} y {@code funciones} no es null. Las
 * reglas no miran el campo: corren igual en Q, R y C.
 */
public record EjercicioResuelto(Expresion entrada, char variable, Resolucion factorizacion, Simplificacion fraccion,
                                ResolucionFunciones funciones) {

    /** Vacío si es una constante; con varias letras solo la ven las reglas de varias variables. */
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
        // Sprint 7: con dos o más letras solo corren las reglas de varias variables (RN41)
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

    /** Una expresión con funciones de x (HU23); vacío si no se pudo factorizar. */
    public static Optional<EjercicioResuelto> de(ResolucionFunciones r) {
        if (!r.factorizable()) {
            return Optional.empty();
        }
        return Optional.of(new EjercicioResuelto(r.entrada(), 'x', null, null, r));
    }

    /** Dos o más letras (RN41). */
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

    /** La entrada en la sintaxis de la sección 9, para las plantillas. */
    public String texto() {
        return ImpresoraTexto.imprimir(entrada);
    }

    public Polinomio polinomio() {
        return Polinomio.desde(entrada);
    }

    /** Grado de lo que queda arriba después de simplificar (solo fracciones). */
    int gradoArriba() {
        Polinomio p = fraccion.arriba().expandir();
        return p.esCero() ? -1 : p.gradoEn(variable);
    }

    /** Grado de lo que queda abajo después de simplificar (0 si se canceló todo). */
    int gradoAbajo() {
        return fraccion.abajo().expandir().gradoEn(variable);
    }
}
