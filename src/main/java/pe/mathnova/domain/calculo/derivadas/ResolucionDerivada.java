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

/**
 * Una derivada resuelta (HU12, HU25).
 *
 * @param funcion   lo que se deriva; en una implícita, la ecuación
 * @param resultado la derivada de orden n ya simplificada; en una implícita, lo que vale y'
 * @param pasos     una regla por paso (D01…D09) con las internas anidadas, y al final "Simplificar"; en orden
 *                  superior, un paso por derivada; si se buscaron, los puntos críticos
 * @param implicita la letra que es función de la variable (D08), o 0
 * @param analisis  RF28 para f racional de una variable y orden 1, o null
 * @param verificada si el cociente incremental (h = 10⁻²⁰) coincide con la derivada en tres puntos; en una
 *                   implícita, si y' anula la derivada de los dos lados
 */
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

    /** "f'(x)", "f''(x)", "f⁽⁴⁾(x)" o "dy/dx". */
    public String nombre() {
        if (esImplicita()) {
            return "d" + implicita + "/d" + variable();
        }
        String primas = orden() <= 3 ? "'".repeat(orden()) : "⁽" + orden() + "⁾";
        return "f" + primas + "(" + variable() + ")";
    }

    /** "2x·sen x + x²·cos x", o "dy/dx = −x/y" en una implícita. */
    public String texto() {
        String r = ImpresoraPizarra.imprimir(resultado);
        return esImplicita() ? nombre() + " = " + r : r;
    }

    /** "\frac{d}{dx}\left(…\right) = …" o "\frac{dy}{dx} = −\frac{x}{y}". */
    public String latex() {
        String r = ImpresoraLatex.imprimir(resultado);
        if (esImplicita()) {
            return "\\frac{d" + Letras.latex(implicita).strip() + "}{d" + Letras.latex(variable()).strip() + "} = " + r;
        }
        return ImpresoraLatex.imprimir(entrada) + " = " + r;
    }

    /** Todos los códigos, con los de los pasos anidados. */
    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
