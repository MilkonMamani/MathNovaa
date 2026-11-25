package pe.mathnova.presentation.pizarra;

import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

public record ModeloRuffini(List<String> fallidos, List<String> raices, List<Tabla> tablas, List<String> faltantes) {

    public ModeloRuffini(List<String> fallidos, List<String> raices, List<Tabla> tablas) {
        this(fallidos, raices, tablas, List.of());
    }

    public record Celda(String texto, boolean relleno) {
    }

    public record Tabla(String raiz, List<Celda> arriba, List<String> medio, List<String> abajo, String residuo,
                        boolean residuoCero, List<String> origenes, String valor, String factor) {

        public Tabla(String raiz, List<Celda> arriba, List<String> medio, List<String> abajo, String residuo,
                     boolean residuoCero, List<String> origenes) {
            this(raiz, arriba, medio, abajo, residuo, residuoCero, origenes, raiz, null);
        }
    }

    static String factor(char v, Racional r) {
        if (r.esCero()) {
            return String.valueOf(v);
        }
        return "(" + v + (r.signo() > 0 ? " " + FormateadorTexto.MENOS + " " + FormateadorTexto.numero(r)
                : " + " + FormateadorTexto.numero(r.negar())) + ")";
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
        List<String> faltantes = new ArrayList<>();
        for (TrabajoIntermedio.TablaRuffini.Division d : t.divisiones()) {
            int grado = d.coeficientes().size() - 1;
            for (int i : d.rellenos()) {
                String termino = FormateadorTexto.potencia(v, grado - i);
                if (!faltantes.contains(termino)) {
                    faltantes.add(termino);
                }
            }
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
                    FormateadorTexto.numero(d.residuo()), d.residuo().esCero(), origenes,
                    FormateadorTexto.numero(d.raiz()), factor(v, d.raiz())));
        }
        return new ModeloRuffini(fallidos, raices, tablas, faltantes);
    }
}
