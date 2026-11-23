package pe.mathnova.domain.parser;

/**
 * Pieza léxica. La posición es base 1 en el texto original, antes de normalizar.
 *
 * @param texto para FUNCION es el nombre canónico ("sen", "arctan"); para CONSTANTE es "e", "i" o "π";
 *              para INTEGRAL es "∫", "∬" o "∭"; para DERIVADA y PARCIAL es la forma normalizada
 *              ("d2/dx2", "∂2/∂x∂y", sin "^"); para la flecha es el operador "→"
 */
public record Token(Tipo tipo, String texto, int posicion) {

    public enum Tipo {
        NUMERO, VARIABLE, CONSTANTE, FUNCION, OPERADOR,
        /** "lim". */
        LIMITE,
        /** "d/dx", "d2/dx2". */
        DERIVADA,
        /** "∂/∂x", "∂2/∂x∂y". */
        PARCIAL,
        /** "∫", "int", "∬", "∭". */
        INTEGRAL,
        /** "∞" o "inf". */
        INFINITO,
        /** "grad": el gradiente ∇f (Sprint 7). */
        GRADIENTE,
        /** "Du": la derivada direccional (Sprint 7). */
        DIRECCIONAL,
        /** "extremos": puntos críticos y extremos (Sprint 8). */
        EXTREMOS,
        /** "coord", "polares", "cilindricas" o "esfericas": el sistema de una integral múltiple (Sprint 8). */
        COORDENADAS,
        FIN
    }

    public boolean esOperador(char c) {
        return tipo == Tipo.OPERADOR && texto.length() == 1 && texto.charAt(0) == c;
    }

    /** Verdadero para lim, d/dx, ∂/∂x, las integrales, grad, Du y extremos. */
    public boolean esOperacion() {
        return tipo == Tipo.LIMITE || tipo == Tipo.DERIVADA || tipo == Tipo.PARCIAL || tipo == Tipo.INTEGRAL
                || tipo == Tipo.GRADIENTE || tipo == Tipo.DIRECCIONAL || tipo == Tipo.EXTREMOS;
    }

    /** Cómo se nombra el token en un mensaje de error: el fin, o el texto entre comillas. */
    String describir() {
        return tipo == Tipo.FIN ? "el fin" : "\"" + texto + "\"";
    }
}
