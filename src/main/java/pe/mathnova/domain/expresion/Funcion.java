package pe.mathnova.domain.expresion;

import java.util.Objects;

/**
 * Llamada a función. Solo LOG usa base (10 si se escribe "log x", 2 si se escribe "log_2 x");
 * para las demás la base es 0.
 */
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

    /** Función sin base; para LOG usa base 10. */
    public Funcion(TipoFuncion tipo, Expresion argumento) {
        this(tipo, argumento, tipo == TipoFuncion.LOG ? 10 : 0);
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
