package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pe.mathnova.domain.calculo.limites.CaminosLimite;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * La pestaña "Exploración" (Parte D): los métodos como tarjetas "Usado", "Probar" o "No aplica", y
 * la comparación Q/R/C (R y C llegan en el Sprint 4). Sin JavaFX.
 *
 * @param probables si "Probar" vuelve a resolver con ese método (no en una fracción)
 */
public record VistaExploracion(List<Tarjeta> tarjetas, boolean probables) {

    public enum Estado {
        USADO("Usado"), PROBAR("Probar"), NO_APLICA("No aplica");

        private final String texto;

        Estado(String texto) {
            this.texto = texto;
        }

        public String texto() {
            return texto;
        }
    }

    /** Qué hace "Probar". */
    public enum Accion {
        /** Vuelve a resolver empezando por ese método. */
        RESOLVER_CON,
        /** Abre la pestaña Análisis (la tabla de valores de un límite). */
        ABRIR_ANALISIS
    }

    /**
     * @param nombre      nombre corto, con el código delante solo si Configuración lo pide
     * @param ayuda       nombre largo, para el tooltip
     * @param motivo      por qué no aplica; null en los demás
     * @param vistaPrevia una línea con lo que da ese camino ("(2x)/1 en x = 2 → 4"), o null
     */
    public record Tarjeta(String codigo, String nombre, String ayuda, Estado estado, String motivo,
                          String vistaPrevia, Accion accion) {

        public Tarjeta(String codigo, String nombre, String ayuda, Estado estado, String motivo) {
            this(codigo, nombre, ayuda, estado, motivo, null, Accion.RESOLVER_CON);
        }
    }

    public VistaExploracion {
        tarjetas = List.copyOf(tarjetas);
    }

    public static VistaExploracion vacia() {
        return new VistaExploracion(List.of(), false);
    }

    /** De una factorización (o de la factorización dentro de una ecuación). */
    public static VistaExploracion de(Resolucion r, boolean codigos) {
        return new VistaExploracion(tarjetas(List.of(r), r.alternativas(), codigos), true);
    }

    /** HU23: la técnica T y lo usado en u; "Probar" no aplica (el método se fuerza sobre polinomios). */
    public static VistaExploracion de(pe.mathnova.domain.factorizacion.ResolucionFunciones r, boolean codigos) {
        if (!r.factorizable()) {
            return vacia();
        }
        List<Tarjeta> t = new ArrayList<>();
        t.add(tarjeta(r.codigo(), Estado.USADO, null, codigos));
        for (Tarjeta x : tarjetas(List.of(r.enLetras()), List.of(), codigos)) {
            if (!x.codigo().equals(r.codigo())) {
                t.add(x);
            }
        }
        return new VistaExploracion(t, false);
    }

    /**
     * Sprint 5, de un límite: el método usado ("Usado"), los otros caminos que llegan al mismo valor
     * ("Probar", con su vista previa) y los que no aplican con su motivo.
     */
    public static VistaExploracion deLimite(List<CaminosLimite.Camino> caminos, boolean codigos) {
        List<Tarjeta> t = new ArrayList<>();
        for (CaminosLimite.Camino c : caminos) {
            boolean tabla = c.codigo().equals(CaminosLimite.TABLA);
            String nombre = tabla ? "Tabla de valores" : VistaResolucion.nombre(c.codigo(), codigos);
            String ayuda = tabla ? "Evaluar f cada vez más cerca del punto (pestaña Análisis)" : Tecnicas.nombre(c.codigo());
            String texto = c.texto() == null ? null : FormateadorTexto.pizarraConNegativos(c.texto());
            t.add(switch (c.estado()) {
                case USADO -> new Tarjeta(c.codigo(), nombre, ayuda, Estado.USADO, null, null, Accion.RESOLVER_CON);
                case SIRVE -> new Tarjeta(c.codigo(), nombre, ayuda, Estado.PROBAR, null, texto,
                        tabla ? Accion.ABRIR_ANALISIS : Accion.RESOLVER_CON);
                case NO_APLICA -> new Tarjeta(c.codigo(), nombre, ayuda, Estado.NO_APLICA, texto, null,
                        Accion.RESOLVER_CON);
            });
        }
        return new VistaExploracion(t, true);
    }

    /**
     * Sprint 6, de una derivada o una integral: el camino usado, los otros que también sirven (con lo que
     * dan, y "Probar" vuelve a resolver empezando por ahí) y los que no aplican con su motivo.
     *
     * @param nombres el nombre de cada código (los de derivadas e integrales no están en Tecnicas)
     */
    public static VistaExploracion deCalculo(List<pe.mathnova.domain.calculo.CaminoCalculo> caminos,
                                             java.util.function.Function<String, String> nombres, boolean codigos) {
        List<Tarjeta> t = new ArrayList<>();
        for (pe.mathnova.domain.calculo.CaminoCalculo c : caminos) {
            String nombre = codigos ? c.codigo() + " · " + c.nombre() : c.nombre();
            String ayuda = nombres.apply(c.codigo());
            String texto = c.texto() == null ? null : FormateadorTexto.pizarraConNegativos(c.texto());
            t.add(switch (c.estado()) {
                // Sprint 7: en por partes, "Usado" muestra la elección de u por ILATE
                case USADO -> new Tarjeta(c.codigo(), nombre, ayuda, Estado.USADO, null, texto, Accion.RESOLVER_CON);
                case SIRVE -> new Tarjeta(c.codigo(), nombre, ayuda, Estado.PROBAR, null, texto, Accion.RESOLVER_CON);
                case NO_APLICA -> new Tarjeta(c.codigo(), nombre, ayuda, Estado.NO_APLICA, texto, null,
                        Accion.RESOLVER_CON);
            });
        }
        return new VistaExploracion(t, true);
    }

    /** De una fracción: lo usado y lo que no aplicó en el numerador y el denominador. */
    public static VistaExploracion de(List<Resolucion> partes, boolean codigos) {
        return new VistaExploracion(tarjetas(partes, List.of(), codigos), false);
    }

    private static List<Tarjeta> tarjetas(List<Resolucion> partes, List<String> alternativas, boolean codigos) {
        Set<String> usados = new LinkedHashSet<>();
        Map<String, String> noAplican = new LinkedHashMap<>();
        for (Resolucion r : partes) {
            r.pasos().forEach(p -> usados.add(p.codigoTecnica()));
            if (r.extension() != null) {
                // "Pasar a R/C" no es un método: solo F21–F30 y A5.
                r.extension().pasos().stream().map(p -> p.codigoTecnica()).filter(c -> !c.startsWith("P"))
                        .forEach(usados::add);
            }
            for (Paso d : r.descartados()) {
                noAplican.putIfAbsent(d.codigoTecnica(), d.motivoDescarte());
            }
        }
        List<Tarjeta> t = new ArrayList<>();
        for (String c : usados) {
            t.add(tarjeta(c, Estado.USADO, null, codigos));
        }
        for (String c : alternativas) {
            if (!usados.contains(c)) {
                t.add(tarjeta(c, Estado.PROBAR, null, codigos));
            }
        }
        noAplican.forEach((c, motivo) -> {
            if (!usados.contains(c) && !alternativas.contains(c)) {
                String m = FormateadorTexto.pizarra(codigos ? motivo : FormateadorTexto.sinCodigos(motivo));
                t.add(tarjeta(c, Estado.NO_APLICA, m, codigos));
            }
        });
        return t;
    }

    private static Tarjeta tarjeta(String codigo, Estado estado, String motivo, boolean codigos) {
        return new Tarjeta(codigo, VistaResolucion.nombre(codigo, codigos), Tecnicas.nombre(codigo), estado, motivo);
    }
}
