package pe.mathnova.domain.calculo.limites;

import java.util.Objects;

import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.numeros.NumeroAlgebraico;

public sealed interface ResultadoLimite {

    String NO_CALCULADO = "No se pudo calcular con los métodos disponibles";

    String texto();

    String latex();

    record Finito(NumeroAlgebraico valor) implements ResultadoLimite {
        public Finito {
            Objects.requireNonNull(valor, "valor");
        }

        @Override
        public String texto() {
            return ImpresoraTexto.imprimir(valor.aExpresion());
        }

        @Override
        public String latex() {
            return ImpresoraLatex.imprimir(valor.aExpresion());
        }
    }

    record Infinito(boolean positivo) implements ResultadoLimite {
        @Override
        public String texto() {
            return positivo ? "+∞" : "−∞";
        }

        @Override
        public String latex() {
            return positivo ? "+\\infty" : "-\\infty";
        }
    }

    record NoExiste(String motivo) implements ResultadoLimite {
        public NoExiste {
            Objects.requireNonNull(motivo, "motivo");
        }

        @Override
        public String texto() {
            return "no existe";
        }

        @Override
        public String latex() {
            return "\\text{no existe}";
        }
    }

    record NoCalculado(String motivo) implements ResultadoLimite {
        public NoCalculado {
            Objects.requireNonNull(motivo, "motivo");
        }

        @Override
        public String texto() {
            return "sin calcular";
        }

        @Override
        public String latex() {
            return "\\text{sin calcular}";
        }
    }
}
