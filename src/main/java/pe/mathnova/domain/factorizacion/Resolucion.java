package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

public record Resolucion(Expresion entrada, List<Paso> pasos, List<Paso> descartados, FormaFactorizada resultado,
                         List<Irreducibilidad> irreducibles, List<String> alternativas, String metodoForzado,
                         Extension extension) {

    public Resolucion {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(resultado, "resultado");
        pasos = List.copyOf(pasos);
        descartados = List.copyOf(descartados);
        irreducibles = List.copyOf(irreducibles);
        alternativas = List.copyOf(alternativas);
    }

    public Resolucion(Expresion entrada, List<Paso> pasos, List<Paso> descartados, FormaFactorizada resultado,
                      List<Irreducibilidad> irreducibles, List<String> alternativas, String metodoForzado) {
        this(entrada, pasos, descartados, resultado, irreducibles, alternativas, metodoForzado, null);
    }

    public Resolucion(Expresion entrada, List<Paso> pasos, List<Paso> descartados, FormaFactorizada resultado,
                      List<Irreducibilidad> irreducibles) {
        this(entrada, pasos, descartados, resultado, irreducibles, List.of(), null);
    }

    public Campo campo() {
        return extension == null ? Campo.Q : extension.campo();
    }

    public Resolucion conExtension(Extension e) {
        return new Resolucion(entrada, pasos, descartados, resultado, irreducibles, alternativas, metodoForzado, e);
    }

    public String texto() {
        if (extension == null) {
            return resultado.aTexto();
        }
        return extension.texto(pasos.isEmpty() && resultado.factores().size() == 1
                && resultado.factores().getFirst().multiplicidad() == 1);
    }
}
