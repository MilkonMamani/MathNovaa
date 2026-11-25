package pe.mathnova.domain.expresion;

final class Precedencia {

    static final int SUMA = 1;
    static final int SIGNO = 2;
    static final int PRODUCTO = 3;
    static final int POTENCIA = 4;
    static final int ATOMO = 5;

    private Precedencia() {
    }

    static int de(Expresion e) {
        return switch (e) {
            case Suma s -> SUMA;
            case Negacion n -> SIGNO;
            case Producto p -> PRODUCTO;
            case Cociente c -> PRODUCTO;
            case Potencia p -> POTENCIA;
            case Constante c -> c.valor().signo() < 0 ? SIGNO : c.valor().esEntero() ? ATOMO : PRODUCTO;
            case Variable v -> ATOMO;
            case ConstanteEspecial c -> ATOMO;
            case Raiz r -> ATOMO;
            case Funcion f -> ATOMO;
            case Limite l -> SUMA;
            case Derivada d -> SUMA;
            case DerivadaParcial d -> SUMA;
            case Integral i -> SUMA;
            case IntegralMultiple i -> SUMA;
            case Ecuacion ec -> SUMA;
            case Gradiente g -> SUMA;
            case Direccional d -> SUMA;
            case LimiteDoble l -> SUMA;
            case Extremos x -> SUMA;
        };
    }
}
