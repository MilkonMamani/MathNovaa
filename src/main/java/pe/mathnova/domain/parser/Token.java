package pe.mathnova.domain.parser;

public record Token(Tipo tipo, String texto, int posicion) {

    public enum Tipo {
        NUMERO, VARIABLE, CONSTANTE, FUNCION, OPERADOR,
        LIMITE,
        DERIVADA,
        PARCIAL,
        INTEGRAL,
        INFINITO,
        GRADIENTE,
        DIRECCIONAL,
        EXTREMOS,
        COORDENADAS,
        FIN
    }

    public boolean esOperador(char c) {
        return tipo == Tipo.OPERADOR && texto.length() == 1 && texto.charAt(0) == c;
    }

    public boolean esOperacion() {
        return tipo == Tipo.LIMITE || tipo == Tipo.DERIVADA || tipo == Tipo.PARCIAL || tipo == Tipo.INTEGRAL
                || tipo == Tipo.GRADIENTE || tipo == Tipo.DIRECCIONAL || tipo == Tipo.EXTREMOS;
    }

    String describir() {
        return tipo == Tipo.FIN ? "el fin" : "\"" + texto + "\"";
    }
}
