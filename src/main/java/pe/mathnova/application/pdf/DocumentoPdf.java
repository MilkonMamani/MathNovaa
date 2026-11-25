package pe.mathnova.application.pdf;

import java.util.List;
import java.util.Objects;

public record DocumentoPdf(String verbo, String enunciadoLatex, String enunciadoTexto, String fecha, List<Paso> pasos,
                           String resultadoLatex, String resultadoTexto, List<String> restricciones,
                           Comprobacion comprobacion, byte[] graficaPng, double escalaLetra) {

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

    public record Comprobacion(String metodo, List<String> lineasLatex, boolean coincide, String mensaje) {

        public Comprobacion {
            lineasLatex = lineasLatex == null ? List.of() : List.copyOf(lineasLatex);
        }
    }

    public DocumentoPdf conGrafica(byte[] png) {
        return new DocumentoPdf(verbo, enunciadoLatex, enunciadoTexto, fecha, pasos, resultadoLatex, resultadoTexto,
                restricciones, comprobacion, png, escalaLetra);
    }

    public DocumentoPdf conEscala(double factor) {
        return new DocumentoPdf(verbo, enunciadoLatex, enunciadoTexto, fecha, pasos, resultadoLatex, resultadoTexto,
                restricciones, comprobacion, graficaPng, factor);
    }

    public long formulasDePasos() {
        return pasos.stream().filter(Paso::tieneFormula).count();
    }
}
