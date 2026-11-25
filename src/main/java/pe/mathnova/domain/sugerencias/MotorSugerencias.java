package pe.mathnova.domain.sugerencias;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MotorSugerencias {

    public static final Set<String> DISPONIBLES = Set.of("LIMITE", "ASINTOTAS", "INTEGRAR", "PUNTOS_CRITICOS",
            "DERIVADA_PARCIAL", "GRADIENTE", "EXTREMOS", "INTEGRAL_MULTIPLE");

    private final List<ReglaSugerencia> reglas;
    private final java.util.function.Predicate<Sugerencia> seResuelve;

    public MotorSugerencias() {
        this(Reglas.todas());
    }

    public MotorSugerencias(List<ReglaSugerencia> reglas) {
        this(reglas, s -> true);
    }

    public MotorSugerencias(java.util.function.Predicate<Sugerencia> seResuelve) {
        this(Reglas.todas(), seResuelve);
    }

    public MotorSugerencias(List<ReglaSugerencia> reglas, java.util.function.Predicate<Sugerencia> seResuelve) {
        this.reglas = List.copyOf(Objects.requireNonNull(reglas, "reglas"));
        this.seResuelve = Objects.requireNonNull(seResuelve, "seResuelve");
    }

    public static List<Sugerencia> paraVariasVariables(pe.mathnova.domain.expresion.Expresion f) {
        List<Sugerencia> r = new ArrayList<>();
        for (Sugerencia s : ParcialesDeVariasVariables.de(f)) {
            r.add(s.conDisponible(s.disponible() && DISPONIBLES.contains(s.operacion())));
        }
        return r;
    }

    public List<Sugerencia> sugerir(EjercicioResuelto resuelto) {
        List<Sugerencia> r = new ArrayList<>();
        for (ReglaSugerencia regla : reglas) {
            if (regla.variasVariables() != resuelto.variasVariables()) {
                continue;
            }
            List<Sugerencia> s;
            try {
                s = regla.aplica(resuelto);
            } catch (RuntimeException e) {
                continue;
            }
            for (Sugerencia x : s) {
                boolean disponible = x.disponible() && DISPONIBLES.contains(x.operacion());
                if (disponible) {
                    try {
                        disponible = seResuelve.test(x);
                    } catch (RuntimeException e) {
                        disponible = false;
                    }
                }
                r.add(x.conDisponible(disponible));
            }
        }
        return r;
    }
}
