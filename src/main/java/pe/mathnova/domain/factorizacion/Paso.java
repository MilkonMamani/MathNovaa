package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * Un método aplicado (HU04) o probado sin éxito (HU05).
 *
 * @param orden          1, 2, 3… dentro de su lista (pasos o descartados)
 * @param codigoTecnica  "F01"…"F13"
 * @param justificacion  por qué aplica, con valores concretos; en un descartado es el motivo
 * @param antes          el polinomio sobre el que se aplicó
 * @param despues        los factores que deja, en el orden en que salen; vacío en un descartado
 * @param trabajo        trabajo intermedio, o null
 * @param motivoDescarte por qué no aplicó; null si se aplicó
 */
public record Paso(int orden, String codigoTecnica, String justificacion, Polinomio antes, List<Factor> despues,
                   TrabajoIntermedio trabajo, boolean descartado, String motivoDescarte) {

    public Paso {
        Objects.requireNonNull(codigoTecnica, "codigoTecnica");
        Objects.requireNonNull(justificacion, "justificacion");
        Objects.requireNonNull(antes, "antes");
        despues = List.copyOf(despues);
        if (descartado && motivoDescarte == null) {
            throw new IllegalArgumentException("Un paso descartado necesita su motivo");
        }
        if (!descartado && despues.isEmpty()) {
            throw new IllegalArgumentException("Un paso aplicado necesita sus factores");
        }
    }

    static Paso aplicado(String codigo, String justificacion, Polinomio antes, List<Factor> despues,
                         TrabajoIntermedio trabajo) {
        return new Paso(0, codigo, justificacion, antes, despues, trabajo, false, null);
    }

    static Paso descartado(String codigo, Polinomio antes, String motivo) {
        return new Paso(0, codigo, motivo, antes, List.of(), null, true, motivo);
    }

    Paso conOrden(int nuevo) {
        return new Paso(nuevo, codigoTecnica, justificacion, antes, despues, trabajo, descartado, motivoDescarte);
    }

    /** Nombre de la técnica ("Agrupación de términos"). */
    public String nombreTecnica() {
        return Tecnicas.nombre(codigoTecnica);
    }

    /**
     * Los factores tal como salen del método, sin reordenar: "2(x^2 - 5x + 6)", "(x - 2)(x^2 - 1)".
     * Null en un descartado.
     */
    public FormaFactorizada despuesComoForma() {
        if (descartado) {
            return null;
        }
        Racional c = Racional.UNO;
        List<Factor> resto = new java.util.ArrayList<>();
        for (Factor f : despues) {
            if (f.polinomio().esConstante()) {
                c = c.multiplicar(f.polinomio().constante().potencia(f.multiplicidad()));
            } else {
                resto.add(f);
            }
        }
        return new FormaFactorizada(c, resto);
    }
}
