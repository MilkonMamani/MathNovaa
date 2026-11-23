package pe.mathnova.domain.expresion;

/**
 * Árbol de sintaxis de una expresión. Inmutable; cada nodo es un record.
 * Las restas se guardan como Suma con un término Negacion.
 * Las operaciones (Limite, Derivada, DerivadaParcial, Integral, IntegralMultiple) solo aparecen
 * en la raíz del árbol: el parser no las deja dentro de otra expresión. Ecuacion (HU38) también va
 * solo en la raíz.
 */
public sealed interface Expresion
        permits Constante, Variable, ConstanteEspecial, Suma, Producto, Cociente,
                Potencia, Negacion, Raiz, Funcion,
                Limite, Derivada, DerivadaParcial, Integral, IntegralMultiple, Ecuacion, Gradiente, Direccional,
                LimiteDoble, Extremos {

    <R> R aceptar(Visitante<R> visitante);
}
