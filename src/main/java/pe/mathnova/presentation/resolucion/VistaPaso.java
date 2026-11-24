package pe.mathnova.presentation.resolucion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * Una fila del procedimiento: un paso del motor, varios pasos de Ruffini seguidos presentados como
 * uno, o, en una fracción, la cancelación (P01) y la división (P02). En un límite, un paso puede
 * llevar pasos anidados (la factorización dentro de L02, las derivadas dentro de L07), que se
 * muestran plegados bajo {@code grupo}.
 *
 * @param codigo  código interno del método; no se muestra salvo que venga en el título
 * @param titulo  nombre corto ("Ruffini (2 raíces)", "Agrupación · numerador"), con el código delante
 *                solo si Configuración lo pide
 * @param ayuda   nombre largo del método, para el tooltip
 * @param trabajo el dibujo de pizarra, o null
 * @param grupo   el rótulo de los pasos anidados ("Factorizar numerador y denominador"), o null
 * @param hijos   los pasos anidados
 * @param desarrollo los renglones de pizarra de un paso largo (Cardano) en lugar del trabajo; null si no
 */
public record VistaPaso(int orden, String codigo, String titulo, String ayuda, String justificacion,
                        Expresion antes, TrabajoIntermedio trabajo, Expresion despues, String grupo,
                        List<VistaPaso> hijos, Desarrollo desarrollo) {

    public VistaPaso {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(justificacion, "justificacion");
        hijos = List.copyOf(hijos);
    }

    public VistaPaso(int orden, String codigo, String titulo, String ayuda, String justificacion,
                     Expresion antes, TrabajoIntermedio trabajo, Expresion despues, String grupo,
                     List<VistaPaso> hijos) {
        this(orden, codigo, titulo, ayuda, justificacion, antes, trabajo, despues, grupo, hijos, null);
    }

    /** El mismo paso dibujado con {@code d}. */
    public VistaPaso conDesarrollo(Desarrollo d) {
        return new VistaPaso(orden, codigo, titulo, ayuda, justificacion, antes, trabajo, despues, grupo, hijos, d);
    }

    public VistaPaso(int orden, String codigo, String titulo, String ayuda, String justificacion,
                     Expresion antes, TrabajoIntermedio trabajo, Expresion despues) {
        this(orden, codigo, titulo, ayuda, justificacion, antes, trabajo, despues, null, List.of());
    }
}
