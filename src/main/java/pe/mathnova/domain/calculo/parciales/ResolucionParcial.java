package pe.mathnova.domain.calculo.parciales;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;

public record ResolucionParcial(Expresion entrada, Tipo tipo, Expresion funcion, List<Character> variables,
                                Expresion resultado, List<Expresion> vector, List<PasoLimite> pasos,
                                boolean verificada, List<String> dominio) {

    public enum Tipo {
        PARCIAL("V01"), GRADIENTE("V02"), DIRECCIONAL("V03"), CADENA("V04");

        private final String codigo;

        Tipo(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    public ResolucionParcial {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(funcion, "funcion");
        variables = List.copyOf(variables);
        vector = List.copyOf(vector);
        pasos = List.copyOf(pasos);
        dominio = List.copyOf(dominio);
    }

    public String texto() {
        if (tipo == Tipo.GRADIENTE) {
            return "(" + String.join(", ", vector.stream().map(ImpresoraPizarra::imprimir).toList()) + ")";
        }
        return ImpresoraPizarra.imprimir(resultado);
    }

    public String latex() {
        String derecha = tipo == Tipo.GRADIENTE
                ? "\\left(" + String.join(", ", vector.stream().map(ImpresoraLatex::imprimir).toList()) + "\\right)"
                : ImpresoraLatex.imprimir(resultado);
        return ImpresoraLatex.imprimir(entrada) + " = " + derecha;
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
