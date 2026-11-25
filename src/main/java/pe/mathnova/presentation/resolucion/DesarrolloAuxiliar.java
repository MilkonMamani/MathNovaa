package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.pizarra.Pizarra;

/**
 * El desarrollo de una factorización auxiliar (la del numerador de un límite, la del denominador de una
 * integral): los métodos que usó el motor, uno detrás de otro, con sus renglones matemáticos.
 *
 * <p>{@link #filas()} cuenta las filas matemáticas del procedimiento para decidir si se muestra completo (1 a 3)
 * o plegado detrás de "Ver desarrollo" (4 o más). Se cuentan las fórmulas (también la que da la factorización
 * de cada método) y las filas de las tablas de Ruffini; no se cuentan los títulos ni las notas. La cuenta sale de
 * los datos, no del ancho: una fórmula partida en dos líneas sigue siendo una fila.
 */
final class DesarrolloAuxiliar {

    /** Hasta esta cantidad de filas el desarrollo va a la vista; desde una más, plegado. */
    static final int MAXIMO_A_LA_VISTA = 3;

    private final List<Node> nodos = new ArrayList<>();
    private int filas;

    DesarrolloAuxiliar(List<VistaPaso> metodos, double maximo, double minimo) {
        boolean varios = metodos.size() > 1;
        for (VistaPaso h : metodos) {
            if (varios) {
                Label nombre = new Label(ReorganizarPasos.metodo(h.titulo()));
                nombre.getStyleClass().add("pizarra-desarrollo-metodo");
                nombre.setWrapText(true);
                nombre.setMinWidth(0);
                nodos.add(nombre);
            }
            agregar(h, maximo, minimo);
        }
    }

    /** Las filas matemáticas (ver la clase). */
    int filas() {
        return filas;
    }

    /** Va plegado detrás de "Ver desarrollo". */
    boolean largo() {
        return filas > MAXIMO_A_LA_VISTA;
    }

    List<Node> nodos() {
        return List.copyOf(nodos);
    }

    /** Las filas de un método, sin dibujarlo (pruebas y decisión A/B). */
    static int filas(VistaPaso h) {
        TrabajoIntermedio t = h.trabajo();
        int conclusion = h.antes() != null && h.despues() != null ? 1 : 0;
        if (t instanceof TrabajoIntermedio.TablaRuffini r) {
            int completo = r.divisiones().getFirst().rellenos().isEmpty() ? 0 : 1;
            // el polinomio completo, las tres filas de cada tabla y su cociente, y la factorización
            return completo + 4 * r.divisiones().size() + conclusion;
        }
        List<String> lineas = Pizarra.lineas(t);
        if (lineas != null) {
            long formulas = lineas.stream().flatMap(l -> Igualdades.Renglon.partidos(l).stream())
                    .filter(x -> x.latex() != null).count();
            return (int) formulas + conclusion;
        }
        if (t == null) {
            return conclusion;
        }
        return switch (t) {
            case TrabajoIntermedio.AspaSimple a -> 2 + 3 + (a.segunda() == 0 ? 3 : conclusion);
            case TrabajoIntermedio.AspaDoble a -> 2 + 3 + conclusion;
            case TrabajoIntermedio.AspaDobleEspecial a -> 2 + 3 + conclusion;
            case TrabajoIntermedio.Sustitucion s -> 4 + conclusion;
            case TrabajoIntermedio.SistemaCoeficientes s -> 2 + s.ecuaciones().size() + s.solucion().size()
                    + conclusion;
            default -> 1 + conclusion;
        };
    }

    private void agregar(VistaPaso h, double maximo, double minimo) {
        filas += filas(h);
        String antes = h.antes() == null ? null : ImpresoraLatex.imprimir(h.antes());
        String despues = h.despues() == null ? null : ImpresoraLatex.imprimir(h.despues());
        String conclusion = antes != null && despues != null ? antes + " = " + despues : null;
        TrabajoIntermedio t = h.trabajo();
        if (t instanceof TrabajoIntermedio.TablaRuffini r) {
            ruffini(r, maximo, minimo);
            if (conclusion != null) {
                nodos.add(new Igualdades(List.of(Igualdades.Renglon.deLatex(conclusion)), maximo, minimo));
            }
            return;
        }
        List<Igualdades.Renglon> renglones = RenglonesPaso.renglones(h, RenglonesPaso.Marco.GENERAL, false);
        if (renglones != null) {
            List<Igualdades.Renglon> todos = new ArrayList<>(renglones);
            if (conclusion != null) {
                todos.add(Igualdades.Renglon.deLatex(conclusion));
            }
            if (!todos.isEmpty()) {
                nodos.add(new Igualdades(todos, maximo, minimo));
            }
            return;
        }
        if (t != null) {
            nodos.add(Pizarra.de(t));
        }
        if (conclusion != null && !RenglonesPaso.dibujoConcluye(t)) {
            nodos.add(new Igualdades(List.of(Igualdades.Renglon.deLatex(conclusion)), maximo, minimo));
        }
    }

    /**
     * Ruffini: el polinomio completo con los ceros que faltan (x³ + 0x² − 3x − 2), la tabla, el residuo 0 y el
     * cociente que forman los coeficientes de abajo.
     */
    private void ruffini(TrabajoIntermedio.TablaRuffini r, double maximo, double minimo) {
        char x = r.variable();
        TrabajoIntermedio.TablaRuffini.Division primera = r.divisiones().getFirst();
        if (!primera.rellenos().isEmpty()) {
            nodos.add(new Igualdades(List.of(Igualdades.Renglon.deLatex(polinomio(primera.coeficientes(), x, true))),
                    maximo, minimo));
            int grado = primera.coeficientes().size() - 1;
            List<String> faltan = primera.rellenos().stream().map(k -> String.valueOf(grado - k)).toList();
            nodos.add(nota(faltan.size() == 1
                    ? "Se coloca 0 porque falta el término de grado " + faltan.getFirst() + "."
                    : "Se colocan ceros porque faltan los términos de grado " + String.join(", ", faltan) + "."));
        }
        // La tabla ya dice "Residuo 0 → (x − 2) es factor"; aquí, solo la división que funcionó.
        nodos.add(new pe.mathnova.presentation.pizarra.TablaRuffini(r).sinCandidatos());
        for (TrabajoIntermedio.TablaRuffini.Division d : r.divisiones()) {
            List<Racional> cociente = d.resultados().subList(0, d.resultados().size() - 1);
            String coefs = String.join(", ", cociente.stream().map(DesarrolloAuxiliar::numero).toList());
            nodos.add(nota("Los coeficientes " + coefs + " forman el cociente:"));
            nodos.add(new Igualdades(List.of(Igualdades.Renglon.deLatex(polinomio(cociente, x, false))), maximo,
                    minimo));
        }
    }

    /** "x³ + 0x² − 3x − 2" (con ceros) o "x² + 2x + 1", en LaTeX; los coeficientes de mayor a menor grado. */
    static String polinomio(List<Racional> coeficientes, char x, boolean conCeros) {
        StringBuilder sb = new StringBuilder();
        int n = coeficientes.size() - 1;
        for (int i = 0; i <= n; i++) {
            Racional c = coeficientes.get(i);
            int g = n - i;
            if (c.esCero() && !conCeros) {
                continue;
            }
            boolean negativo = c.signo() < 0;
            Racional abs = negativo ? c.negar() : c;
            String letra = g == 0 ? "" : g == 1 ? String.valueOf(x) : x + "^{" + g + "}";
            String coef = abs.equals(Racional.UNO) && !letra.isEmpty() ? ""
                    : ImpresoraLatex.imprimir(new Constante(abs));
            if (sb.isEmpty()) {
                sb.append(negativo ? "-" : "");
            } else {
                sb.append(negativo ? " - " : " + ");
            }
            sb.append(coef).append(letra);
        }
        return sb.isEmpty() ? "0" : sb.toString();
    }

    private static String numero(Racional r) {
        String s = r.den().equals(java.math.BigInteger.ONE) ? r.num().toString() : r.num() + "/" + r.den();
        return s.startsWith("-") ? "−" + s.substring(1) : s;
    }

    private static Label nota(String texto) {
        Label l = new Label(texto);
        l.setWrapText(true);
        l.setMinWidth(0);
        l.getStyleClass().add("pizarra-explicacion");
        return l;
    }

    /** Todo el desarrollo en una columna (con la sangría y la línea azul, si va plegado). */
    VBox caja() {
        VBox c = new VBox(8);
        c.setMinWidth(0);
        c.getChildren().addAll(nodos);
        return c;
    }
}
