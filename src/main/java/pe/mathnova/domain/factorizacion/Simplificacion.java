package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.parser.Parser;

/**
 * P01 (HU09): una fracción simplificada.
 *
 * @param numerador        factorización del numerador original
 * @param denominador      factorización del denominador original
 * @param arriba           lo que queda arriba (con la constante de la fracción)
 * @param abajo            lo que queda abajo (constante 1); sin factores si se canceló todo
 * @param cancelados       factores comunes que se cancelaron, con su multiplicidad
 * @param restricciones    RN14: los valores que anulan el denominador original, "x ≠ -3"
 * @param justificacion    qué se canceló
 * @param division         P02 si lo que queda es impropio: "x + 1/(x + 1)" (formato del parser); null si no
 */
public record Simplificacion(Expresion entrada, Resolucion numerador, Resolucion denominador,
                             FormaFactorizada arriba, FormaFactorizada abajo, List<Factor> cancelados,
                             List<String> restricciones, String justificacion, String division) {

    public Simplificacion {
        Objects.requireNonNull(entrada, "entrada");
        cancelados = List.copyOf(cancelados);
        restricciones = List.copyOf(restricciones);
    }

    /** La fracción en el formato del banco, sin las restricciones: "(x + 2)/(x + 3)", "3/(x + 2)". */
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

    /** "(x + 2)/(x + 3); x ≠ -3, x ≠ 2". */
    public String texto() {
        return restricciones.isEmpty() ? fraccion() : fraccion() + "; " + String.join(", ", restricciones);
    }

    /** El árbol de la fracción simplificada, para dibujarla con ImpresoraLatex. */
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

    /** HU18 para fracciones: arriba·(denominador original) = (numerador original)·abajo. */
    public boolean verifica() {
        Polinomio num = numerador.resultado().expandir();
        Polinomio den = denominador.resultado().expandir();
        return arriba.expandir().multiplicar(den).equals(num.multiplicar(abajo.expandir()));
    }
}
