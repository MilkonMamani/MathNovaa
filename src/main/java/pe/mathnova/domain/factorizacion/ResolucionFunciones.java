package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;

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

    public String formula() {
        return factorizable ? ImpresoraTexto.imprimir(resultado) : mensaje;
    }

    public String texto() {
        return restricciones.isEmpty() || !factorizable ? formula() : formula() + "; " + String.join(", ", restricciones);
    }
}
