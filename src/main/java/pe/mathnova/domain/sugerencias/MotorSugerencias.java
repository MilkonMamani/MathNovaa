package pe.mathnova.domain.sugerencias;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Aplica las reglas RN15–RN19 a lo que se resolvió (RF21). RN20: nada que el sistema todavía no
 * resuelve se ofrece como disponible. Desde el Sprint 6 las derivadas (RN18) y las integrales (RN16, RN17,
 * RN18) también se resuelven.
 */
public final class MotorSugerencias {

    /** Las operaciones que el sistema ya resuelve. */
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

    /**
     * RN20 con el motor a la vista: una sugerencia solo queda disponible si {@code seResuelve} dice que su
     * plantilla se resuelve de verdad (una integral que ningún método resuelve queda desactivada).
     */
    public MotorSugerencias(java.util.function.Predicate<Sugerencia> seResuelve) {
        this(Reglas.todas(), seResuelve);
    }

    public MotorSugerencias(List<ReglaSugerencia> reglas, java.util.function.Predicate<Sugerencia> seResuelve) {
        this.reglas = List.copyOf(Objects.requireNonNull(reglas, "reglas"));
        this.seResuelve = Objects.requireNonNull(seResuelve, "seResuelve");
    }

    /** RN41 para una f de varias variables que no pasó por una factorización (una parcial, un gradiente). */
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
                continue; // una regla que falla no impide las demás
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
