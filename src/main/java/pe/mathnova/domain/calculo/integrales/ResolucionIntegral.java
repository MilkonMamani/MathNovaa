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

public record ResolucionIntegral(Expresion entrada, Integral integral, Racional coeficiente, Expresion antiderivada,
                                 Expresion valor, BigDecimal aproximado, List<PasoLimite> pasos, String metodo,
                                 Verificacion verificacion, String motivo) {

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

    public String texto() {
        if (!resuelta()) {
            return motivo;
        }
        return definida() ? ImpresoraPizarra.imprimir(valor) : ImpresoraPizarra.imprimir(antiderivada) + " + C";
    }

    public String latex() {
        String izq = ImpresoraLatex.imprimir(entrada);
        if (!resuelta()) {
            return izq;
        }
        return izq + " = " + (definida() ? ImpresoraLatex.imprimir(valor) : ImpresoraLatex.imprimir(antiderivada) + " + C");
    }

    public Racional valorRacional() {
        return valor instanceof Constante c ? c.valor() : null;
    }

    public boolean verificada() {
        return verificacion != null && verificacion.coincide();
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
