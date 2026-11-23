package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;

/**
 * Factorización de una expresión con funciones (sección 2.12, T01–T05).
 *
 * @param factorizable  false si no es polinómica en sus funciones o no se parte
 * @param mensaje       por qué no se factoriza; null si se factorizó
 * @param codigo        "T01"…"T05"; null si no se factorizó
 * @param enLetras      la factorización del polinomio en las letras (u = eˣ…); null si no se factorizó
 * @param funciones     letra → función que reemplaza
 * @param resultado     la forma final con las funciones de vuelta; null si no se factorizó
 * @param identidades   las identidades usadas (T04), como "sen(2x) = 2sen(x)·cos(x)"
 * @param sustitucion   los tres renglones: sustituir, factorizar en u y deshacer
 * @param restricciones de dominio (RF46, RN34): "x > 0", "x ≠ π/2 + kπ"
 */
public record ResolucionFunciones(Expresion entrada, boolean factorizable, String mensaje, String codigo,
                                  Resolucion enLetras, Map<Character, Expresion> funciones, Expresion resultado,
                                  List<String> identidades, TrabajoIntermedio.Sustitucion sustitucion,
                                  List<String> restricciones) {

    public ResolucionFunciones {
        Objects.requireNonNull(entrada, "entrada");
        funciones = funciones == null ? Map.of() : Map.copyOf(funciones);
        identidades = List.copyOf(identidades);
        restricciones = List.copyOf(restricciones);
    }

    static ResolucionFunciones noFactorizable(Expresion entrada, String mensaje, List<String> restricciones) {
        return new ResolucionFunciones(entrada, false, mensaje, null, null, null, null, List.of(), null, restricciones);
    }

    /** La forma final sin restricciones: "(e^x - 1)·(e^x - 2)"; el mensaje si no se factorizó. */
    public String formula() {
        return factorizable ? ImpresoraTexto.imprimir(resultado) : mensaje;
    }

    /** Como las fracciones: "ln(x)·(x - 1); x > 0". */
    public String texto() {
        return restricciones.isEmpty() || !factorizable ? formula() : formula() + "; " + String.join(", ", restricciones);
    }
}
