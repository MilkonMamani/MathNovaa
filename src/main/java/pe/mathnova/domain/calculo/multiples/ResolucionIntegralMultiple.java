package pe.mathnova.domain.calculo.multiples;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.IntegralMultiple;

public record ResolucionIntegralMultiple(IntegralMultiple entrada, IntegralMultiple efectiva, Expresion integrando,
                                         Expresion jacobiano, Expresion valor, boolean invertida,
                                         List<PasoLimite> pasos, String motivo) {

    public ResolucionIntegralMultiple {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(efectiva, "efectiva");
        pasos = List.copyOf(pasos);
    }

    public boolean resuelta() {
        return valor != null;
    }

    public IntegralMultiple.Coordenadas coordenadas() {
        return efectiva.coordenadas();
    }

    public String texto() {
        return resuelta() ? ImpresoraPizarra.imprimir(valor) : motivo;
    }

    public String latex() {
        String izq = ImpresoraLatex.imprimir(entrada);
        return resuelta() ? izq + " = " + ImpresoraLatex.imprimir(valor) : izq;
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
