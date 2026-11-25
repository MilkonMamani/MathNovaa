package pe.mathnova.domain.numeros;

import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Raiz;

public record Atomo(String clave, Expresion expresion, int indice, NumeroAlgebraico radicando)
        implements Comparable<Atomo> {

    public Atomo {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(expresion, "expresion");
        if (indice != 0 && (indice < 2 || radicando == null)) {
            throw new IllegalArgumentException("Un radical anidado necesita índice ≥ 2 y radicando");
        }
    }

    static Atomo raiz(int n, NumeroAlgebraico z) {
        Expresion e = new Raiz(n, z.aExpresion());
        return new Atomo(ImpresoraTexto.imprimir(e), e, n, z);
    }

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
