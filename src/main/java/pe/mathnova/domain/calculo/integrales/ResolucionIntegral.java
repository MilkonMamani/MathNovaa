package pe.mathnova.domain.calculo.integrales;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.Integral;

/**
 * Una integral resuelta (HU13, RF29–RF32).
 *
 * @param entrada      lo que se escribió: ∫(…) o k·∫(…)
 * @param integral     la integral sin el coeficiente
 * @param coeficiente  el número de delante (1 si no hay)
 * @param antiderivada F(x) sin + C (ya multiplicada por el coeficiente); null si ningún método aplicó
 * @param valor        en una definida, F(b) − F(a) exacto (por el coeficiente); null si no
 * @param aproximado   el valor decimal de una definida, o null
 * @param metodo       I01, I02, EXP o I06 (con P02 si hubo división); null si ninguno aplicó
 * @param verificacion derivada de F = integrando; null si no hay F
 * @param motivo       por qué no se resolvió, o null
 */
public record ResolucionIntegral(Expresion entrada, Integral integral, Racional coeficiente, Expresion antiderivada,
                                 Expresion valor, BigDecimal aproximado, List<PasoLimite> pasos, String metodo,
                                 Verificacion verificacion, String motivo) {

    /**
     * @param coincide si la derivada de F da el integrando
     * @param exacta   true si se comprobó exacto (fracciones de polinomios o la misma forma); false si fue
     *                 numérico en cinco puntos
     * @param derivada F′(x)
     */
    public record Verificacion(boolean coincide, boolean exacta, Expresion derivada) {
    }

    public ResolucionIntegral {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(integral, "integral");
        Objects.requireNonNull(coeficiente, "coeficiente");
        pasos = List.copyOf(pasos);
    }

    public char variable() {
        return integral.variable();
    }

    public boolean resuelta() {
        return antiderivada != null && (!integral.esDefinida() || valor != null);
    }

    public boolean definida() {
        return integral.esDefinida();
    }

    /** "x³ − 2x² + 5x + C", "1/3", o el motivo. */
    public String texto() {
        if (!resuelta()) {
            return motivo;
        }
        return definida() ? ImpresoraPizarra.imprimir(valor) : ImpresoraPizarra.imprimir(antiderivada) + " + C";
    }

    /** "\int … \, dx = … + C". */
    public String latex() {
        String izq = ImpresoraLatex.imprimir(entrada);
        if (!resuelta()) {
            return izq;
        }
        return izq + " = " + (definida() ? ImpresoraLatex.imprimir(valor) : ImpresoraLatex.imprimir(antiderivada) + " + C");
    }

    /** El valor de una definida si es racional (para "ans" y el resultado en vivo). */
    public Racional valorRacional() {
        return valor instanceof Constante c ? c.valor() : null;
    }

    public boolean verificada() {
        return verificacion != null && verificacion.coincide();
    }

    /** Todos los códigos, con los de los pasos anidados. */
    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
