package pe.mathnova.domain.calculo.derivadas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.Letras;

public record ResolucionDerivada(Derivada entrada, Expresion funcion, Expresion resultado, List<PasoLimite> pasos,
                                 char implicita, AnalisisDerivada analisis, boolean verificada) {

    public ResolucionDerivada {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(funcion, "funcion");
        Objects.requireNonNull(resultado, "resultado");
        pasos = List.copyOf(pasos);
    }

    public char variable() {
        return entrada.variable();
    }

    public int orden() {
        return entrada.orden();
    }

    public boolean esImplicita() {
        return implicita != 0;
    }

    public String nombre() {
        if (esImplicita()) {
            return "d" + implicita + "/d" + variable();
        }
        String primas = orden() <= 3 ? "'".repeat(orden()) : "⁽" + orden() + "⁾";
        return "f" + primas + "(" + variable() + ")";
    }

    public String texto() {
        String r = ImpresoraPizarra.imprimir(resultado);
        return esImplicita() ? nombre() + " = " + r : r;
    }

    public String latex() {
        String r = ImpresoraLatex.imprimir(resultado);
        if (esImplicita()) {
            return "\\frac{d" + Letras.latex(implicita).strip() + "}{d" + Letras.latex(variable()).strip() + "} = " + r;
        }
        return ImpresoraLatex.imprimir(entrada) + " = " + r;
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
