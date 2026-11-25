package pe.mathnova.domain.calculo.limites;

import java.util.Objects;

import pe.mathnova.domain.numeros.NumeroAlgebraico;

public sealed interface Valor {

    record Numero(NumeroAlgebraico valor) implements Valor {
        public Numero {
            Objects.requireNonNull(valor, "valor");
        }

        public boolean esCero() {
            return valor.esCero() || !valor.exacto() && valor.signo() == 0;
        }
    }

    record Infinito(int signo) implements Valor {
        public Infinito {
            if (signo != 1 && signo != -1) {
                throw new IllegalArgumentException("El signo de ∞ es 1 o −1");
            }
        }
    }

    record SobreCero(NumeroAlgebraico k) implements Valor {
        public SobreCero {
            Objects.requireNonNull(k, "k");
        }
    }

    record Indeterminado(Indeterminacion tipo) implements Valor {
        public Indeterminado {
            Objects.requireNonNull(tipo, "tipo");
        }
    }

    record Indefinido(String motivo, boolean noExiste) implements Valor {
        public Indefinido {
            Objects.requireNonNull(motivo, "motivo");
        }
    }
}
