package pe.mathnova.domain.expresion;

public sealed interface Expresion
        permits Constante, Variable, ConstanteEspecial, Suma, Producto, Cociente,
                Potencia, Negacion, Raiz, Funcion,
                Limite, Derivada, DerivadaParcial, Integral, IntegralMultiple, Ecuacion, Gradiente, Direccional,
                LimiteDoble, Extremos {

    <R> R aceptar(Visitante<R> visitante);
}
