package pe.mathnova.domain.calculo.derivadas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

public record PasoDerivada(String codigo, String regla, Expresion antes, Expresion despues, String formula,
                           List<String> trabajo, List<PasoDerivada> hijos) {

    public PasoDerivada {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(regla, "regla");
        Objects.requireNonNull(antes, "antes");
        Objects.requireNonNull(despues, "despues");
        Objects.requireNonNull(formula, "formula");
        trabajo = List.copyOf(trabajo);
        hijos = List.copyOf(hijos);
    }

    public PasoDerivada(String codigo, String regla, Expresion antes, Expresion despues) {
        this(codigo, regla, antes, despues, regla, List.of(), List.of());
    }

    public String nombre() {
        return nombre(codigo);
    }

    public static String nombre(String codigo) {
        return switch (codigo) {
            case "D01" -> "Derivada de una constante";
            case "D02" -> "Regla de la potencia";
            case "D03" -> "Regla de la suma";
            case "D04" -> "Regla del producto";
            case "D05" -> "Regla del cociente";
            case "D06" -> "Regla de la cadena";
            case "D07" -> "Derivada de función trascendente";
            case "D08" -> "Derivación implícita";
            case "D09" -> "Derivación logarítmica";
            default -> codigo;
        };
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        r.add(codigo);
        hijos.forEach(h -> r.addAll(h.codigos()));
        return r;
    }
}
