package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.parser.Parser;

public record Simplificacion(Expresion entrada, Resolucion numerador, Resolucion denominador,
                             FormaFactorizada arriba, FormaFactorizada abajo, List<Factor> cancelados,
                             List<String> restricciones, String justificacion, String division) {

    public Simplificacion {
        Objects.requireNonNull(entrada, "entrada");
        cancelados = List.copyOf(cancelados);
        restricciones = List.copyOf(restricciones);
    }

    public String fraccion() {
        String num = textoArriba();
        if (abajo.factores().isEmpty()) {
            return num;
        }
        String den = abajo.aTexto();
        boolean producto = abajo.factores().size() > 1;
        boolean suma = abajo.factores().size() == 1 && abajo.factores().getFirst().multiplicidad() == 1
                && abajo.factores().getFirst().polinomio().terminos().size() > 1;
        if (producto || suma) {
            den = suma ? "(" + den + ")" : "(" + den + ")";
        }
        return num + "/" + den;
    }

    private String textoArriba() {
        String t = arriba.aTexto();
        boolean unaSuma = arriba.factores().size() == 1 && arriba.factores().getFirst().multiplicidad() == 1
                && arriba.factores().getFirst().polinomio().terminos().size() > 1
                && arriba.constante().equals(pe.mathnova.domain.algebra.Racional.UNO);
        return unaSuma && !abajo.factores().isEmpty() ? "(" + t + ")" : t;
    }

    public String texto() {
        return restricciones.isEmpty() ? fraccion() : fraccion() + "; " + String.join(", ", restricciones);
    }

    public Expresion aExpresion() {
        Expresion num = arriba.factores().isEmpty() ? Parser.parse(arriba.aTexto()) : arriba.aExpresion();
        if (abajo.factores().isEmpty()) {
            return num;
        }
        boolean negativo = num instanceof Negacion;
        Expresion cuerpo = negativo ? ((Negacion) num).argumento() : num;
        Expresion c = new Cociente(cuerpo, abajo.aExpresion());
        return negativo ? new Negacion(c) : c;
    }

    public boolean verifica() {
        Polinomio num = numerador.resultado().expandir();
        Polinomio den = denominador.resultado().expandir();
        return arriba.expandir().multiplicar(den).equals(num.multiplicar(abajo.expandir()));
    }
}
