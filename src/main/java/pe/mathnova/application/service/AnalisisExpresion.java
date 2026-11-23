package pe.mathnova.application.service;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.stream.Collectors;

/** Lo que la pantalla de entrada necesita saber de lo que se escribió (HU02, HU03, HU34). */
public sealed interface AnalisisExpresion {

    /**
     * Entrada bien escrita: LaTeX para la vista previa y la línea de información.
     *
     * @param variables  letras que son variables (RN43, RN44)
     * @param constantes letras que hacen de constante en una operación (la a de d/dt(a t^2))
     * @param tipo       tipo de la expresión o, en una operación, de su cuerpo
     * @param operacion  "límite cuando x → 2", "derivada respecto de t"…; vacío si no es una operación
     */
    record Valida(String latex, SortedSet<Character> variables, SortedSet<Character> constantes, Tipo tipo,
                  OptionalInt grado, Optional<String> operacion) implements AnalisisExpresion {

        /**
         * "Variables: x · Tipo: polinomio · Grado 3", o para una operación
         * "Operación: límite cuando x → 2 · Variable: x · Constantes: a".
         */
        public String resumen() {
            if (operacion.isPresent()) {
                String texto = "Operación: " + operacion.get()
                        + " · " + (variables.size() == 1 ? "Variable: " : "Variables: ") + lista(variables);
                if (!constantes.isEmpty()) {
                    texto += " · Constantes: " + lista(constantes);
                }
                return texto;
            }
            String texto = "Variables: " + lista(variables) + " · Tipo: " + tipo.etiqueta();
            if (grado.isPresent()) {
                texto += " · Grado " + grado.getAsInt();
            }
            return texto;
        }

        private static String lista(SortedSet<Character> letras) {
            return letras.isEmpty()
                    ? "ninguna"
                    : letras.stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
    }

    /**
     * Entrada mal escrita (RF04). La posición es base 1; 0 significa que el error no
     * apunta a un carácter concreto.
     */
    record Invalida(String mensaje, int posicion) implements AnalisisExpresion {
    }

    /** Campo vacío: no hay nada que mostrar. */
    record Vacia() implements AnalisisExpresion {
    }

    enum Tipo {
        POLINOMIO("polinomio"),
        TRASCENDENTE("trascendente"),
        ALGEBRAICA("algebraica");

        private final String etiqueta;

        Tipo(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }
}
