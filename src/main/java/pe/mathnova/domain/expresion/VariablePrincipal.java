package pe.mathnova.domain.expresion;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public record VariablePrincipal(SortedSet<Character> variables, SortedSet<Character> constantes) {

    public VariablePrincipal {
        variables = Collections.unmodifiableSortedSet(new TreeSet<>(variables));
        constantes = Collections.unmodifiableSortedSet(new TreeSet<>(constantes));
    }

    public static VariablePrincipal de(Expresion e) {
        SortedSet<Character> letras = Expresiones.variables(e);
        SortedSet<Character> indicadas = indicadas(e);
        return repartir(letras, indicadas == null ? letras : indicadas);
    }

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
