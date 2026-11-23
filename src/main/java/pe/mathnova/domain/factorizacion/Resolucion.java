package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

/**
 * Factorización completa de una entrada.
 *
 * @param pasos         métodos aplicados, en el orden en que se aplicaron (HU04)
 * @param descartados   métodos probados que no aplicaron, con su motivo (HU05)
 * @param resultado     forma final RN12
 * @param irreducibles  por qué cada factor final ya no se factoriza en Q (HU07)
 * @param alternativas  otros métodos que también aplican a la entrada, además del primero que se usó
 * @param metodoForzado el código del método forzado (HU08), o null en automático
 * @param extension     en R o C, lo que se hace después de Q (RN27); null en Q
 */
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

    /** Q si no hay extensión. */
    public Campo campo() {
        return extension == null ? Campo.Q : extension.campo();
    }

    /** La misma resolución con lo hecho en R o C. */
    public Resolucion conExtension(Extension e) {
        return new Resolucion(entrada, pasos, descartados, resultado, irreducibles, alternativas, metodoForzado, e);
    }

    /** El resultado como texto: en Q la forma de RN12; en R o C la forma del campo (con ≈ si se aproximó). */
    public String texto() {
        if (extension == null) {
            return resultado.aTexto();
        }
        return extension.texto(pasos.isEmpty() && resultado.factores().size() == 1
                && resultado.factores().getFirst().multiplicidad() == 1);
    }
}
