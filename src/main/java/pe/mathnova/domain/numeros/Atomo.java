package pe.mathnova.domain.numeros;

import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Raiz;

/**
 * Un número real que no cabe en la forma racional × radicales de enteros: un radical anidado como
 * √(1 + 4√2) o ∛(3 + √5), o un valor trigonométrico de Cardano como cos(⅓·arccos(…)). Se guarda la
 * expresión tal como sale y se compara por su texto; las cuentas con átomos son formales y la
 * verificación de lo que los usa es numérica (Parte E).
 *
 * @param clave     el texto de la expresión; identifica al átomo
 * @param indice    n si átomoⁿ = radicando (un radical anidado); 0 si no hay tal relación
 * @param radicando lo que queda dentro de la raíz; null si indice es 0
 */
public record Atomo(String clave, Expresion expresion, int indice, NumeroAlgebraico radicando)
        implements Comparable<Atomo> {

    public Atomo {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(expresion, "expresion");
        if (indice != 0 && (indice < 2 || radicando == null)) {
            throw new IllegalArgumentException("Un radical anidado necesita índice ≥ 2 y radicando");
        }
    }

    /** ⁿ√z con z real positivo (n par) o real (n impar), sin simplificar. */
    static Atomo raiz(int n, NumeroAlgebraico z) {
        Expresion e = new Raiz(n, z.aExpresion());
        return new Atomo(ImpresoraTexto.imprimir(e), e, n, z);
    }

    /** Un valor que solo se conoce por su expresión, como 2cos(⅓·arccos(√3/2)). */
    public static Atomo simbolico(Expresion e) {
        return new Atomo(ImpresoraTexto.imprimir(e), e, 0, null);
    }

    @Override
    public int compareTo(Atomo o) {
        return clave.compareTo(o.clave);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Atomo a && a.clave.equals(clave);
    }

    @Override
    public int hashCode() {
        return clave.hashCode();
    }

    @Override
    public String toString() {
        return clave;
    }
}
