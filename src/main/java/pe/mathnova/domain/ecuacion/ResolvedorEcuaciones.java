package pe.mathnova.domain.ecuacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion.PasoEcuacion;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion.Solucion;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;

/**
 * Resuelve en Q una ecuación polinómica de una incógnita (HU38, RF73):
 * <ol>
 *   <li>"Pasar todo a un lado": P(x) − Q(x) = 0, solo si Q no es 0.</li>
 *   <li>"Factorizar", con los pasos de siempre del orquestador.</li>
 *   <li>"Igualar cada factor a cero".</li>
 *   <li>"Despejar": las raíces racionales de menor a mayor; una raíz doble se muestra una vez,
 *       marcada "doble". Un factor sin raíz en Q se informa: "x^2 + 1 = 0 no tiene solución en Q".</li>
 * </ol>
 */
public final class ResolvedorEcuaciones {

    private final OrquestadorFactorizacion orquestador;

    public ResolvedorEcuaciones(OrquestadorFactorizacion orquestador) {
        this.orquestador = orquestador;
    }

    /**
     * @throws NoEsPolinomioException si algún lado no es un polinomio, o si hay más de una incógnita
     */
    public ResolucionEcuacion resolver(Ecuacion ecuacion) {
        return resolver(ecuacion, null);
    }

    /**
     * @param forzado el método con que empieza la factorización (HU08), o null en automático
     * @throws pe.mathnova.domain.factorizacion.MetodoNoAplicable si el método forzado no aplica
     */
    public ResolucionEcuacion resolver(Ecuacion ecuacion, String forzado) {
        Polinomio izquierda = Polinomio.desde(ecuacion.izquierda());
        Polinomio derecha = Polinomio.desde(ecuacion.derecha());
        Polinomio p = izquierda.restar(derecha);

        SortedSet<Character> letras = new TreeSet<>(izquierda.variables());
        letras.addAll(derecha.variables());
        if (letras.size() > 1) {
            throw new NoEsPolinomioException("Por ahora se resuelven ecuaciones con una sola incógnita; esta tiene "
                    + letras.size() + ": " + letras);
        }
        char x = letras.isEmpty() ? 'x' : letras.first();

        List<PasoEcuacion> pasos = new ArrayList<>();
        boolean derechaEsCero = derecha.esCero();
        Expresion aFactorizar = ecuacion.izquierda();
        if (!derechaEsCero) {
            pasos.add(new PasoEcuacion(PasoEcuacion.Tipo.PASAR_A_UN_LADO, "Pasar todo a un lado",
                    "Se resta " + derecha + " a los dos lados para que la ecuación quede igualada a cero.",
                    lineasDePasar(texto(ecuacion.izquierda()) + " - " + restado(derecha) + " = 0", p + " = 0")));
            aFactorizar = p.aExpresion();
        }

        if (p.esConstante()) {
            boolean identidad = p.esCero();
            pasos.add(new PasoEcuacion(PasoEcuacion.Tipo.DESPEJAR, "Despejar",
                    identidad ? "Queda 0 = 0: la igualdad se cumple para cualquier valor de " + x + "."
                            : "Queda " + p + " = 0, que es falso: ningún valor de " + x + " la cumple.",
                    List.of(p + " = 0")));
            return new ResolucionEcuacion(ecuacion, x, pasos, p, null, List.of(), List.of(),
                    identidad ? ResolucionEcuacion.Tipo.IDENTIDAD : ResolucionEcuacion.Tipo.SIN_SOLUCION);
        }

        Resolucion factorizacion = orquestador.resolver(aFactorizar, Campo.Q, forzado);
        FormaFactorizada forma = factorizacion.resultado();
        pasos.add(new PasoEcuacion(PasoEcuacion.Tipo.FACTORIZAR, "Factorizar",
                "Se factoriza " + p + " con los métodos de siempre.",
                List.of(forma.aTexto() + " = 0")));

        // Igualar cada factor a cero.
        List<String> igualados = new ArrayList<>();
        for (Factor f : forma.factores()) {
            String linea = f.polinomio() + " = 0";
            if (f.multiplicidad() > 1) {
                linea += " (" + new Solucion(Racional.CERO, f.multiplicidad()).nombreMultiplicidad() + ")";
            }
            igualados.add(linea);
        }
        String porque = forma.constante().equals(Racional.UNO)
                ? "Un producto es cero solo si alguno de sus factores es cero."
                : "Un producto es cero solo si alguno de sus factores es cero; la constante " + forma.constante()
                        + " no puede ser cero.";
        pasos.add(new PasoEcuacion(PasoEcuacion.Tipo.IGUALAR_A_CERO, "Igualar cada factor a cero", porque, igualados));

        // Despejar.
        List<Solucion> soluciones = new ArrayList<>();
        List<Polinomio> sinSolucion = new ArrayList<>();
        List<String> despejes = new ArrayList<>();
        for (Factor f : forma.factores()) {
            Polinomio factor = f.polinomio();
            if (factor.grado() == 1) {
                Racional a = factor.coeficiente(Monomio.de(x, 1));
                Racional b = factor.constante();
                Racional raiz = b.negar().dividir(a);
                soluciones.add(new Solucion(raiz, f.multiplicidad()));
                if (!b.esCero() || !a.equals(Racional.UNO)) {
                    despejes.add(factor + " = 0  →  " + x + " = " + raiz);
                }
            } else {
                sinSolucion.add(factor);
                despejes.add(factor + " = 0 no tiene solución en Q");
            }
        }
        soluciones.sort(Comparator.comparing(Solucion::valor));
        despejes.addAll(ResolucionEcuacion.lineasDeSolucion(x, soluciones));
        String detalle = soluciones.isEmpty()
                ? "Ningún factor tiene raíces racionales."
                : "Las soluciones van de menor a mayor" + (soluciones.stream().anyMatch(s -> s.multiplicidad() > 1)
                        ? "; una raíz repetida se escribe una sola vez." : ".");
        pasos.add(new PasoEcuacion(PasoEcuacion.Tipo.DESPEJAR, "Despejar", detalle, despejes));

        return new ResolucionEcuacion(ecuacion, x, pasos, p, factorizacion, soluciones, sinSolucion,
                soluciones.isEmpty() ? ResolucionEcuacion.Tipo.SIN_SOLUCION : ResolucionEcuacion.Tipo.CON_SOLUCIONES);
    }

    private static List<String> lineasDePasar(String restando, String reducido) {
        return restando.equals(reducido) ? List.of(reducido) : List.of(restando, reducido);
    }

    /** "x" o "(3x + 2)": entre paréntesis si tiene más de un término o empieza con signo. */
    private static String restado(Polinomio q) {
        String t = q.toString();
        return q.terminos().size() > 1 || t.startsWith("-") ? "(" + t + ")" : t;
    }

    private static String texto(Expresion e) {
        return pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(e);
    }
}
