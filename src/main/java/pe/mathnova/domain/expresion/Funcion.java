package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Funcion(TipoFuncion tipo, Expresion argumento, int base) implements Expresion {

    public Funcion {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(argumento, "argumento");
        if (tipo == TipoFuncion.LOG && base < 2) {
            throw new IllegalArgumentException("La base del logaritmo debe ser un entero mayor que 1");
        }
        if (tipo != TipoFuncion.LOG && base != 0) {
            throw new IllegalArgumentException("Solo el logaritmo lleva base");
        }
    }

    public Funcion(TipoFuncion tipo, Expresion argumento) {
        this(tipo, argumento, tipo == TipoFuncion.LOG ? 10 : 0);
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
