package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.analisis.AnalisisPolinomio;
import pe.mathnova.domain.analisis.AnalisisPolinomio.Punto;
import pe.mathnova.domain.analisis.AnalisisPolinomio.Raiz;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.grafica.DatosGrafica;

/**
 * La sección "Propiedades de la función" (RF66), como texto de pizarra y sin JavaFX: una tabla
 * pequeña y la tabla de signos como recta.
 *
 * @param filas       "Grado" → "4", "Raíces racionales" → "x = −1, x = 1/2…"
 * @param marcas      las raíces sobre la recta, de menor a mayor: "−1", "1/2", "1", "2"
 * @param signos      +1 o −1 en cada tramo (uno más que marcas); vacío si no hay tabla de signos
 * @param avisoSignos por qué no hay tabla de signos, o null
 */
public record VistaPropiedades(List<Fila> filas, List<String> marcas, List<Integer> signos, String avisoSignos) {

    public record Fila(String nombre, String valor) {
    }

    public VistaPropiedades {
        filas = List.copyOf(filas);
        marcas = List.copyOf(marcas);
        signos = List.copyOf(signos);
    }

    /** Vacío si el polinomio tiene más de una letra. */
    public static Optional<VistaPropiedades> de(Resolucion r) {
        return enQ(r).map(v -> r.extension() == null ? v : conCampo(v, r));
    }

    /**
     * En R o C se agregan las raíces del campo: las exactas (√2, 1 + 2i) de los factores lineales y,
     * si se aproximaron (A5), las aproximadas con ≈.
     */
    private static VistaPropiedades conCampo(VistaPropiedades v, Resolucion r) {
        var ext = r.extension();
        List<Fila> filas = new ArrayList<>(v.filas());
        String nombre = ext.campo() == pe.mathnova.domain.factorizacion.Campo.R ? "Raíces reales" : "Raíces en C";
        if (ext.aproximada()) {
            filas.add(new Fila(nombre, FormateadorTexto.pizarraConNegativos(
                    String.join(", ", ext.aproximadas().stream().map(t -> "x ≈ " + t).toList()))));
        } else {
            List<String> raices = new ArrayList<>();
            for (var f : ext.resultado().factores()) {
                if (f.polinomio().grado() == 1) {
                    var raiz = f.polinomio().coeficiente(0).negar().dividir(f.polinomio().principal());
                    String m = f.multiplicidad() == 1 ? "" : " (" + multiplicidad(f.multiplicidad()) + ")";
                    raices.add(FormateadorTexto.pizarraConNegativos("x = " + raiz) + m);
                }
            }
            filas.add(new Fila(nombre, raices.isEmpty() ? "no tiene" : String.join(", ", raices)));
        }
        return new VistaPropiedades(filas, v.marcas(), v.signos(), v.avisoSignos());
    }

    private static Optional<VistaPropiedades> enQ(Resolucion r) {
        return AnalisisPolinomio.de(r).map(a -> {
            String x = String.valueOf(a.variable());
            List<Fila> filas = new ArrayList<>();
            filas.add(new Fila("Grado", String.valueOf(a.grado())));
            filas.add(new Fila("Coeficiente principal", FormateadorTexto.numero(a.coeficientePrincipal())));
            filas.add(new Fila("Raíces racionales", raices(x, a.raices())));
            filas.add(new Fila("Corte con el eje y", "(0, " + FormateadorTexto.numero(a.corteY()) + ")"));
            a.vertice().ifPresent(v -> filas.add(new Fila("Vértice", punto(v))));
            if (!a.signosCompletos()) {
                return new VistaPropiedades(filas, List.of(), List.of(),
                        "La tabla de signos no se muestra: hay factores con raíces que no son racionales.");
            }
            return new VistaPropiedades(filas,
                    a.raices().stream().map(z -> FormateadorTexto.numero(z.valor())).toList(),
                    a.signos().stream().map(AnalisisPolinomio.Tramo::signo).toList(), null);
        });
    }

    /** Para una fracción: raíces, asíntotas, huecos y corte con el eje y; sin tabla de signos. */
    public static Optional<VistaPropiedades> de(DatosGrafica d) {
        String x = String.valueOf(d.variable());
        List<Fila> filas = new ArrayList<>();
        filas.add(new Fila("Raíces racionales", raices(x, d.raices())));
        filas.add(new Fila("Asíntotas verticales", d.asintotas().isEmpty() ? "no tiene"
                : String.join(", ", d.asintotas().stream().map(a -> x + " = " + FormateadorTexto.numero(a)).toList())));
        if (!d.huecos().isEmpty()) {
            filas.add(new Fila("Huecos", String.join(", ", d.huecos().stream().map(VistaPropiedades::punto).toList())));
        }
        filas.add(new Fila("Corte con el eje y", d.corteY().map(c -> "(0, " + FormateadorTexto.numero(c) + ")")
                .orElse("no tiene: no está definida en 0")));
        return Optional.of(new VistaPropiedades(filas, List.of(), List.of(), null));
    }

    /** "x = −1, x = 1 (doble)" o "no tiene". */
    private static String raices(String x, List<Raiz> raices) {
        if (raices.isEmpty()) {
            return "no tiene";
        }
        return String.join(", ", raices.stream().map(r -> x + " = " + FormateadorTexto.numero(r.valor())
                + (r.multiplicidad() == 1 ? "" : " (" + multiplicidad(r.multiplicidad()) + ")")).toList());
    }

    public static String multiplicidad(int n) {
        return switch (n) {
            case 2 -> "doble";
            case 3 -> "triple";
            default -> "multiplicidad " + n;
        };
    }

    /** "(5/2, −1/4)": exacto, como fracción o entero. */
    public static String punto(Punto p) {
        return "(" + FormateadorTexto.numero(p.x()) + ", " + FormateadorTexto.numero(p.y()) + ")";
    }
}
