package pe.mathnova.domain.expresion;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Qué letras de una entrada son variables y cuáles son constantes literales (RN43, RN44).
 * e, i y π nunca aparecen aquí: son constantes con nombre propio (RN42).
 *
 * <ul>
 *   <li>RN43: en una operación la variable es la que indica la operación (la x de d/dx, de
 *       ∫(…, x) o de lim(x→…)); en ∂/∂x∂y y en ∬ son todas las que indica. Las demás letras
 *       son constantes: en d/dt(a t^2) la variable es t y la constante a.</li>
 *   <li>RN44: en una expresión sin operación se factoriza, y en factorización todas las letras son
 *       variables: a^2 - b^2 es un polinomio en a y b.</li>
 *   <li>Cuando hace falta una sola variable y la entrada no indica ninguna,
 *       {@link #conUnaVariable(Expresion)} usa x, o si no hay x la primera letra en orden alfabético.</li>
 * </ul>
 *
 * @param variables  en orden alfabético
 * @param constantes en orden alfabético
 */
public record VariablePrincipal(SortedSet<Character> variables, SortedSet<Character> constantes) {

    public VariablePrincipal {
        variables = Collections.unmodifiableSortedSet(new TreeSet<>(variables));
        constantes = Collections.unmodifiableSortedSet(new TreeSet<>(constantes));
    }

    /** RN43 para las operaciones y RN44 para las expresiones sin operación. */
    public static VariablePrincipal de(Expresion e) {
        SortedSet<Character> letras = Expresiones.variables(e);
        SortedSet<Character> indicadas = indicadas(e);
        return repartir(letras, indicadas == null ? letras : indicadas);
    }

    /**
     * RN43 cuando se necesita una sola variable: la que indica la operación o, si no indica ninguna,
     * x; si no hay x, la primera letra en orden alfabético.
     */
    public static VariablePrincipal conUnaVariable(Expresion e) {
        SortedSet<Character> letras = Expresiones.variables(e);
        SortedSet<Character> indicadas = indicadas(e);
        if (indicadas != null) {
            return repartir(letras, indicadas);
        }
        if (letras.isEmpty()) {
            return repartir(letras, letras);
        }
        char elegida = letras.contains('x') ? 'x' : letras.first();
        return repartir(letras, new TreeSet<>(Collections.singleton(elegida)));
    }

    /** Las variables que nombra la operación, o null si no es una operación. */
    private static SortedSet<Character> indicadas(Expresion e) {
        TreeSet<Character> vars = new TreeSet<>();
        switch (e) {
            case Limite l -> vars.add(l.variable());
            case Derivada d -> vars.add(d.variable());
            case DerivadaParcial d -> vars.addAll(d.variables());
            case Integral i -> vars.add(i.variable());
            case IntegralMultiple i -> i.rangos().forEach(r -> vars.add(r.variable()));
            case LimiteDoble l -> {
                vars.add(l.x());
                vars.add(l.y());
            }
            default -> {
                return null;
            }
        }
        return vars;
    }

    private static VariablePrincipal repartir(SortedSet<Character> letras, SortedSet<Character> variables) {
        TreeSet<Character> constantes = new TreeSet<>(letras);
        constantes.removeAll(variables);
        return new VariablePrincipal(variables, constantes);
    }
}
