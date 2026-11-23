package pe.mathnova.presentation.controller;

/**
 * En qué está la pantalla "Resolver ejercicio". Todos los paneles se dibujan a partir de este estado
 * (ver {@link PantallaResolucion}); ninguno se actualiza por su cuenta.
 */
public enum EstadoResolucion {
    /** El editor está vacío. */
    VACIO,
    /** Hay una expresión que todavía no se resolvió (o cambió después de resolverla). */
    ESCRIBIENDO,
    /** Queda una casilla obligatoria vacía. */
    INCOMPLETO,
    /** La expresión tiene un error o no se puede resolver. */
    ERROR,
    /** "Resolver" está calculando. */
    CALCULANDO,
    /** Lo que se ve en los paneles es el resultado de lo que está en el editor. */
    RESUELTO
}
