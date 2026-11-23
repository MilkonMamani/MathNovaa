package pe.mathnova.presentation.pizarra;

import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * Lo que dibuja {@link TablaRuffini}, ya como texto de pizarra: las fichas de los candidatos y las
 * tablas apiladas. No usa JavaFX, así que se prueba sin abrir la ventana.
 *
 * @param fallidos candidatos probados que no son raíz ("x = 1 ✗ residuo −2"), en orden
 * @param raices   las raíces encontradas ("x = 2 ✓")
 */
public record ModeloRuffini(List<String> fallidos, List<String> raices, List<Tabla> tablas) {

    /** Una celda de la fila de arriba; {@code relleno} si es un 0 de un término que faltaba. */
    public record Celda(String texto, boolean relleno) {
    }

    /**
     * Una tabla. Sus columnas quedan alineadas a la izquierda con las de la tabla de arriba.
     *
     * @param raiz      "x = 2"
     * @param arriba    fila de coeficientes
     * @param medio     fila del medio: una celda vacía y luego resultado anterior × raíz
     * @param abajo     fila de resultados sin el residuo (los coeficientes del cociente)
     * @param residuo   el último número de la fila de abajo
     * @param origenes  para cada celda del medio (desde la segunda), de dónde sale: "2·2"
     */
    public record Tabla(String raiz, List<Celda> arriba, List<String> medio, List<String> abajo, String residuo,
                        boolean residuoCero, List<String> origenes) {
    }

    public static ModeloRuffini de(TrabajoIntermedio.TablaRuffini t) {
        char v = t.variable();
        List<String> fallidos = new ArrayList<>();
        List<String> raices = new ArrayList<>();
        for (TrabajoIntermedio.TablaRuffini.Candidato c : t.candidatos()) {
            String x = v + " = " + FormateadorTexto.numero(c.valor());
            if (c.esRaiz()) {
                raices.add(x + " ✓");
            } else {
                fallidos.add(x + " ✗ residuo " + FormateadorTexto.numero(c.residuo()));
            }
        }
        if (raices.isEmpty()) {
            t.divisiones().stream().map(d -> v + " = " + FormateadorTexto.numero(d.raiz()) + " ✓")
                    .distinct().forEach(raices::add);
        }
        List<Tabla> tablas = new ArrayList<>();
        for (TrabajoIntermedio.TablaRuffini.Division d : t.divisiones()) {
            List<Celda> arriba = new ArrayList<>();
            for (int i = 0; i < d.coeficientes().size(); i++) {
                arriba.add(new Celda(FormateadorTexto.numero(d.coeficientes().get(i)), d.rellenos().contains(i)));
            }
            List<String> medio = new ArrayList<>();
            medio.add("");
            d.productos().forEach(p -> medio.add(FormateadorTexto.numero(p)));
            List<String> abajo = d.resultados().subList(0, d.resultados().size() - 1).stream()
                    .map(FormateadorTexto::numero).toList();
            List<String> origenes = new ArrayList<>();
            origenes.add("");
            for (int i = 1; i < d.coeficientes().size(); i++) {
                Racional anterior = d.resultados().get(i - 1);
                origenes.add(FormateadorTexto.numero(anterior) + "·" + FormateadorTexto.numeroEnParentesis(d.raiz()));
            }
            tablas.add(new Tabla(v + " = " + FormateadorTexto.numero(d.raiz()), arriba, medio, abajo,
                    FormateadorTexto.numero(d.residuo()), d.residuo().esCero(), origenes));
        }
        return new ModeloRuffini(fallidos, raices, tablas);
    }
}
