package pe.mathnova.domain.calculo.limites;

import java.util.Objects;

import pe.mathnova.domain.numeros.NumeroAlgebraico;

/**
 * Lo que da una expresión al reemplazar la variable por el punto (o por ±∞): un número exacto, ±∞,
 * k/0, una indeterminación o algo que no está definido. Es la "evaluación directa" de L01.
 */
public sealed interface Valor {

    /** Un número real exacto: racional, con radicales o con e, π y valores de funciones. */
    record Numero(NumeroAlgebraico valor) implements Valor {
        public Numero {
            Objects.requireNonNull(valor, "valor");
        }

        public boolean esCero() {
            return valor.esCero() || !valor.exacto() && valor.signo() == 0;
        }
    }

    /** +∞ (signo 1) o −∞ (signo −1). */
    record Infinito(int signo) implements Valor {
        public Infinito {
            if (signo != 1 && signo != -1) {
                throw new IllegalArgumentException("El signo de ∞ es 1 o −1");
            }
        }
    }

    /** k/0 con k ≠ 0: crece sin cota, pero el signo depende del lado (L05). */
    record SobreCero(NumeroAlgebraico k) implements Valor {
        public SobreCero {
            Objects.requireNonNull(k, "k");
        }
    }

    /** Una forma indeterminada en la raíz de la expresión. */
    record Indeterminado(Indeterminacion tipo) implements Valor {
        public Indeterminado {
            Objects.requireNonNull(tipo, "tipo");
        }
    }

    /**
     * Algo que la evaluación directa no resuelve: una función fuera de su dominio, una
     * indeterminación dentro de una parte de la expresión o una oscilación.
     *
     * @param noExiste el límite no existe (sen x cuando x → ∞); si es false, solo no se pudo evaluar
     */
    record Indefinido(String motivo, boolean noExiste) implements Valor {
        public Indefinido {
            Objects.requireNonNull(motivo, "motivo");
        }
    }
}
