package pe.mathnova.domain.calculo.derivadas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

/**
 * Una regla de derivación aplicada (RF27): D01 constante, D02 potencia, D03 suma, D04 producto,
 * D05 cociente, D06 cadena, D07 función trascendente, D08 derivación implícita, D09 derivación
 * logarítmica.
 *
 * @param codigo   "D01"…"D09", como en la tabla tecnica de V2
 * @param regla    la regla con sus valores: "(sen u)' = cos u · u' con u = 3x"
 * @param antes    lo que se deriva
 * @param despues  su derivada, ya simplificada
 * @param formula  la regla en general, para Trabajo: "(u·v)' = u'·v + u·v'"
 * @param trabajo  qué es u y v en este caso y sus derivadas: "u = x², v = sen x", "u' = 2x, v' = cos x"
 * @param hijos    las reglas que se aplicaron dentro de esta (la cadena dentro del producto)
 */
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

    /** Nombre de la regla, igual al del catálogo de V2. */
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

    /** Este código y los de sus reglas internas, en orden. */
    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        r.add(codigo);
        hijos.forEach(h -> r.addAll(h.codigos()));
        return r;
    }
}
