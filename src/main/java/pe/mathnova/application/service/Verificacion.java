package pe.mathnova.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaExtendida;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.VerificacionCampo;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;

/**
 * HU18: el resultado expandido paso a paso y si coincide con la entrada.
 *
 * @param resultado forma factorizada en texto, como "2(x - 1)^2"
 * @param productos cada multiplicación hecha para expandirla, como "(x - 1)^2 = x^2 - 2x + 1"
 * @param expandido el polinomio que queda al final
 * @param coincide  true si es igual a la entrada
 * @param numerica  true si se comparó evaluando con 40 cifras (radicales anidados, Parte E) y no expandiendo
 */
public record Verificacion(String resultado, List<String> productos, String expandido, boolean coincide,
                           boolean numerica) {

    /** "Verificado numéricamente con 40 cifras". */
    public static final String AYUDA_NUMERICA = "Verificado numéricamente con " + EvaluacionNumerica.CIFRAS + " cifras";

    public Verificacion {
        productos = List.copyOf(productos);
    }

    public Verificacion(String resultado, List<String> productos, String expandido, boolean coincide) {
        this(resultado, productos, expandido, coincide, false);
    }

    static Verificacion de(Resolucion r) {
        if (r.extension() != null && !r.extension().pasos().isEmpty()) {
            return enCampo(r);
        }
        FormaFactorizada forma = r.resultado();
        List<String> productos = new ArrayList<>();
        Polinomio acumulado = null;
        for (Factor f : forma.factores()) {
            Polinomio potencia = f.expandir();
            if (f.multiplicidad() > 1) {
                productos.add(agrupar(f.polinomio()) + "^" + f.multiplicidad() + " = " + potencia);
            }
            if (acumulado == null) {
                acumulado = potencia;
            } else {
                Polinomio producto = acumulado.multiplicar(potencia);
                productos.add(agrupar(acumulado) + " · " + agrupar(potencia) + " = " + producto);
                acumulado = producto;
            }
        }
        Racional c = forma.constante();
        Polinomio total;
        if (acumulado == null) {
            total = Polinomio.constante(c);
        } else if (c.equals(Racional.UNO)) {
            total = acumulado;
        } else {
            total = acumulado.multiplicar(c);
            productos.add((c.esEntero() ? c.toString() : "(" + c + ")") + " · " + agrupar(acumulado) + " = " + total);
        }
        boolean coincide = forma.expandir().equals(Polinomio.desde(r.entrada()));
        return new Verificacion(forma.aTexto(), productos, total.toString(), coincide);
    }

    /** HU18 para P01: arriba·(denominador original) = (numerador original)·abajo. */
    static Verificacion de(Simplificacion s) {
        Polinomio num = s.numerador().resultado().expandir();
        Polinomio den = s.denominador().resultado().expandir();
        Polinomio arriba = s.arriba().expandir();
        Polinomio abajo = s.abajo().expandir();
        Polinomio izquierda = arriba.multiplicar(den);
        Polinomio derecha = num.multiplicar(abajo);
        List<String> productos = List.of(
                agrupar(arriba) + " · " + agrupar(den) + " = " + izquierda,
                agrupar(num) + " · " + agrupar(abajo) + " = " + derecha);
        return new Verificacion(s.fraccion(), productos, izquierda.toString(), s.verifica());
    }

    private static String agrupar(Polinomio p) {
        return p.terminos().size() > 1 ? "(" + p + ")" : p.toString();
    }

    // ------------------------------------------------------------------ R y C (Parte E)

    /** Exacta si no hay radicales anidados: se expande factor por factor; si no, en cinco puntos. */
    static Verificacion enCampo(Resolucion r) {
        Polinomio entrada = Polinomio.desde(r.entrada());
        FormaExtendida forma = r.extension().resultado();
        VerificacionCampo v = VerificacionCampo.de(r);
        if (v.numerica()) {
            char x = entrada.variables().isEmpty() ? 'x' : entrada.variables().first();
            return new Verificacion(forma.aTexto(), puntos(entrada.aExpresion(), forma.aExpresion(), x),
                    entrada.toString(), v.coincide(), true);
        }
        List<String> productos = new ArrayList<>();
        PolinomioAlgebraico acumulado = null;
        for (FormaExtendida.Factor f : forma.factores()) {
            PolinomioAlgebraico potencia = f.polinomio().potencia(f.multiplicidad());
            if (f.multiplicidad() > 1) {
                productos.add(agrupar(f.polinomio()) + "^" + f.multiplicidad() + " = " + potencia);
            }
            if (acumulado == null) {
                acumulado = potencia;
            } else {
                PolinomioAlgebraico producto = acumulado.multiplicar(potencia);
                productos.add(agrupar(acumulado) + " · " + agrupar(potencia) + " = " + producto);
                acumulado = producto;
            }
        }
        Racional c = forma.constante();
        PolinomioAlgebraico total = acumulado == null ? PolinomioAlgebraico.constante('x', NumeroAlgebraico.de(c))
                : acumulado.multiplicar(NumeroAlgebraico.de(c));
        if (acumulado != null && !c.equals(Racional.UNO)) {
            productos.add((c.esEntero() ? c.toString() : "(" + c + ")") + " · " + agrupar(acumulado) + " = " + total);
        }
        return new Verificacion(forma.aTexto(), productos, total.toString(), v.coincide(), false);
    }

    /** Expresiones con funciones: entrada y resultado en los puntos donde ambas están definidas. */
    static Verificacion de(ResolucionFunciones r) {
        if (!r.factorizable()) {
            return new Verificacion(r.formula(), List.of(), "", false, false);
        }
        List<String> lineas = new ArrayList<>();
        int definidos = 0;
        boolean coincide = true;
        for (Racional p : PUNTOS_POSITIVOS) {
            try {
                Map<Character, ComplejoDecimal> x = Map.of('x', ComplejoDecimal.de(p, EvaluacionNumerica.MC));
                ComplejoDecimal a = EvaluacionNumerica.evaluar(r.entrada(), x, EvaluacionNumerica.MC);
                ComplejoDecimal b = EvaluacionNumerica.evaluar(r.resultado(), x, EvaluacionNumerica.MC);
                boolean igual = a.cercano(b, EvaluacionNumerica.CIFRAS_IGUALES, EvaluacionNumerica.MC);
                coincide &= igual;
                definidos++;
                lineas.add("x = " + p + ":  " + a.texto(12) + " ≈ " + b.texto(12) + (igual ? "  ✓" : "  ✗"));
            } catch (ArithmeticException e) {
                // fuera del dominio: el punto no cuenta
            }
        }
        lineas.addFirst("Se evalúan la entrada y el resultado con " + EvaluacionNumerica.CIFRAS + " cifras:");
        return new Verificacion(r.formula(), lineas, pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(r.entrada()),
                coincide && definidos >= 2, true);
    }

    /** Positivos para que ln x esté definido; ninguno es múltiplo de π/2. */
    private static final List<Racional> PUNTOS_POSITIVOS = List.of(Racional.de(1, 3), Racional.de(5, 7),
            Racional.de(13, 11), Racional.de(17, 9), Racional.de(29, 13));

    private static List<String> puntos(Expresion entrada, Expresion resultado, char x) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Hay radicales anidados: se evalúan la entrada y el resultado en cinco puntos con "
                + EvaluacionNumerica.CIFRAS + " cifras (deben coincidir en " + EvaluacionNumerica.CIFRAS_IGUALES + "):");
        for (Racional p : EvaluacionNumerica.PUNTOS) {
            try {
                Map<Character, ComplejoDecimal> v = Map.of(x, ComplejoDecimal.de(p, EvaluacionNumerica.MC));
                ComplejoDecimal a = EvaluacionNumerica.evaluar(entrada, v, EvaluacionNumerica.MC);
                ComplejoDecimal b = EvaluacionNumerica.evaluar(resultado, v, EvaluacionNumerica.MC);
                boolean igual = a.cercano(b, EvaluacionNumerica.CIFRAS_IGUALES, EvaluacionNumerica.MC);
                lineas.add(x + " = " + p + ":  " + a.texto(12) + " ≈ " + b.texto(12) + (igual ? "  ✓" : "  ✗"));
            } catch (ArithmeticException e) {
                lineas.add(x + " = " + p + ": no se pudo evaluar");
            }
        }
        return lineas;
    }

    private static String agrupar(PolinomioAlgebraico p) {
        return p.coeficientes().stream().filter(c -> !c.esCero()).count() > 1 ? "(" + p + ")" : p.toString();
    }
}
