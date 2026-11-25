package pe.mathnova.application.service;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.stream.Collectors;

public sealed interface AnalisisExpresion {

    record Valida(String latex, SortedSet<Character> variables, SortedSet<Character> constantes, Tipo tipo,
                  OptionalInt grado, Optional<String> operacion) implements AnalisisExpresion {

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

    record Invalida(String mensaje, int posicion) implements AnalisisExpresion {
    }

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
