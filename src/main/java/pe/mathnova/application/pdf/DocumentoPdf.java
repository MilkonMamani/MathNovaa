package pe.mathnova.application.pdf;

import java.util.List;
import java.util.Objects;

/**
 * Lo que va en el PDF de una resolución (Sprint 9, parte C), en el orden en que se imprime: el verbo y el
 * enunciado, los pasos, el resultado en su recuadro, la comprobación y, si se pidió, la gráfica. Las fórmulas
 * van en LaTeX para que el exportador las dibuje en vectores; cada una trae también su texto, por si el LaTeX
 * no se puede dibujar.
 *
 * @param verbo          "Factorizar", "Calcular límite"…
 * @param fecha          la del encabezado, ya escrita: "24 de noviembre de 2026"
 * @param restricciones  "x ≠ 2"…; vacía si no hay
 * @param comprobacion   null si esta operación no se comprueba
 * @param graficaPng     la gráfica en PNG; null si no se pidió "Incluir gráfica"
 * @param escalaLetra    el tamaño de letra de Configuración: 1; 1,2 o 1,4 (Sprint 9, parte E)
 */
public record DocumentoPdf(String verbo, String enunciadoLatex, String enunciadoTexto, String fecha, List<Paso> pasos,
                           String resultadoLatex, String resultadoTexto, List<String> restricciones,
                           Comprobacion comprobacion, byte[] graficaPng, double escalaLetra) {

    /** Con la letra normal. */
    public DocumentoPdf(String verbo, String enunciadoLatex, String enunciadoTexto, String fecha, List<Paso> pasos,
                        String resultadoLatex, String resultadoTexto, List<String> restricciones,
                        Comprobacion comprobacion, byte[] graficaPng) {
        this(verbo, enunciadoLatex, enunciadoTexto, fecha, pasos, resultadoLatex, resultadoTexto, restricciones,
                comprobacion, graficaPng, 1);
    }

    public DocumentoPdf {
        escalaLetra = escalaLetra > 0 ? escalaLetra : 1;
        Objects.requireNonNull(verbo, "verbo");
        Objects.requireNonNull(fecha, "fecha");
        pasos = List.copyOf(pasos);
        restricciones = restricciones == null ? List.of() : List.copyOf(restricciones);
    }

    /**
     * Un paso: "2.1 → Diferencia de cuadrados", la fórmula y la explicación.
     *
     * @param numero      "1", "2", o "2.1" si está dentro de otro
     * @param nivel       0 arriba, 1 anidado…: cuánto se sangra
     * @param latex       la fórmula del paso; vacía si el paso no tiene
     * @param explicacion por qué se hace; puede ser vacía
     */
    public record Paso(String numero, int nivel, String titulo, String latex, String explicacion) {

        public Paso {
            Objects.requireNonNull(numero, "numero");
            Objects.requireNonNull(titulo, "titulo");
            latex = latex == null ? "" : latex;
            explicacion = explicacion == null ? "" : explicacion;
        }

        public boolean tieneFormula() {
            return !latex.isBlank();
        }
    }

    /**
     * La comprobación (HU18): cómo se hizo, los renglones en LaTeX y si coincidió.
     *
     * @param metodo  "Por expansión", "Derivando el resultado"…
     * @param mensaje "Coincide con la entrada"…
     */
    public record Comprobacion(String metodo, List<String> lineasLatex, boolean coincide, String mensaje) {

        public Comprobacion {
            lineasLatex = lineasLatex == null ? List.of() : List.copyOf(lineasLatex);
        }
    }

    /** El mismo documento con la gráfica (o sin ella, con null). */
    public DocumentoPdf conGrafica(byte[] png) {
        return new DocumentoPdf(verbo, enunciadoLatex, enunciadoTexto, fecha, pasos, resultadoLatex, resultadoTexto,
                restricciones, comprobacion, png, escalaLetra);
    }

    /** El mismo documento con otro tamaño de letra. */
    public DocumentoPdf conEscala(double factor) {
        return new DocumentoPdf(verbo, enunciadoLatex, enunciadoTexto, fecha, pasos, resultadoLatex, resultadoTexto,
                restricciones, comprobacion, graficaPng, factor);
    }

    /** Las fórmulas de los pasos (los que tienen). */
    public long formulasDePasos() {
        return pasos.stream().filter(Paso::tieneFormula).count();
    }
}
