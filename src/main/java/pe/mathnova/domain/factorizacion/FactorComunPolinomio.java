package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.factorizacion.MetodoFactorizacion.ResultadoMetodo;

public final class FactorComunPolinomio {

    public static final String CODIGO = "F03";

    private record Pieza(Polinomio base, int exponente, Expresion original) {
    }

    private record TerminoArbol(boolean negativo, List<Pieza> piezas, Expresion original) {
    }

    public Optional<ResultadoMetodo> detectar(Expresion e) {
        if (!(e instanceof Suma s)) {
            return Optional.empty();
        }
        List<TerminoArbol> terminos = new ArrayList<>();
        try {
            for (Expresion t : s.terminos()) {
                terminos.add(descomponer(t));
            }
            return buscar(e, terminos);
        } catch (NoEsPolinomioException | ArithmeticException noEs) {
            return Optional.empty();
        }
    }

    private static TerminoArbol descomponer(Expresion t) {
        boolean negativo = false;
        Expresion x = t;
        while (x instanceof Negacion(Expresion a)) {
            negativo = !negativo;
            x = a;
        }
        List<Pieza> piezas = new ArrayList<>();
        aplanar(x, piezas);
        return new TerminoArbol(negativo, piezas, t);
    }

    private static void aplanar(Expresion x, List<Pieza> piezas) {
        if (x instanceof Producto p) {
            p.factores().forEach(f -> aplanar(f, piezas));
        } else if (x instanceof Potencia(Expresion base, Constante k) && k.valor().esEntero()
                && k.valor().signo() > 0 && k.valor().num().bitLength() < 31) {
            piezas.add(new Pieza(Polinomio.desde(base), k.valor().num().intValueExact(), x));
        } else {
            piezas.add(new Pieza(Polinomio.desde(x), 1, x));
        }
    }

    private Optional<ResultadoMetodo> buscar(Expresion e, List<TerminoArbol> terminos) {
        for (Pieza candidata : terminos.getFirst().piezas()) {
            Polinomio base = candidata.base();
            if (base.terminos().size() < 2) {
                continue;
            }
            int minimo = Integer.MAX_VALUE;
            boolean enTodos = true;
            for (TerminoArbol t : terminos) {
                Optional<Pieza> igual = t.piezas().stream().filter(pz -> pz.base().equals(base)).findFirst();
                if (igual.isEmpty()) {
                    enTodos = false;
                    break;
                }
                minimo = Math.min(minimo, igual.get().exponente());
            }
            if (enTodos) {
                return Optional.of(factorizar(e, terminos, base, minimo));
            }
        }
        return Optional.empty();
    }

    private ResultadoMetodo factorizar(Expresion e, List<TerminoArbol> terminos, Polinomio base, int k) {
        Polinomio resto = Polinomio.CERO;
        List<String> grupos = new ArrayList<>();
        for (TerminoArbol t : terminos) {
            Polinomio producto = Polinomio.UNO;
            boolean usada = false;
            for (Pieza pz : t.piezas()) {
                if (!usada && pz.base().equals(base)) {
                    producto = producto.multiplicar(base.potencia(pz.exponente() - k));
                    usada = true;
                } else {
                    producto = producto.multiplicar(pz.base().potencia(pz.exponente()));
                }
            }
            resto = resto.sumar(t.negativo() ? producto.negar() : producto);
            grupos.add(Formato.desdeTexto(ImpresoraTexto.imprimir(t.original())));
        }
        String comun = Formato.parentesis(base) + (k > 1 ? Formato.superindice(k) : "");
        String lista = grupos.size() == 2
                ? grupos.get(0) + " y " + grupos.get(1)
                : String.join(", ", grupos.subList(0, grupos.size() - 1)) + " y " + grupos.getLast();
        String justificacion = "Los términos " + lista + " comparten " + comun;
        Polinomio antes = Polinomio.desde(e);
        List<Factor> factores = List.of(new Factor(base, k), Factor.de(resto));
        Paso paso = Paso.aplicado(CODIGO, justificacion, antes, factores, new TrabajoIntermedio.Agrupacion(grupos));
        return new ResultadoMetodo(factores, paso);
    }
}
