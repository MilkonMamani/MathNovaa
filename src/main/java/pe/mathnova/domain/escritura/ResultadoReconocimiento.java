package pe.mathnova.domain.escritura;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que entendió el reconocedor: la expresión en LaTeX y los símbolos de los que no está seguro.
 *
 * @param dudosos ordenados por {@link SimboloDudoso#indice()}
 */
public record ResultadoReconocimiento(String latex, List<SimboloDudoso> dudosos) {

    public ResultadoReconocimiento {
        dudosos = List.copyOf(dudosos);
    }

    /**
     * El estudiante eligió {@code alternativa} para {@code dudoso}: el LaTeX cambia en ese lugar, el
     * símbolo deja de ser dudoso y los que vienen después se corren lo que haga falta.
     */
    public ResultadoReconocimiento elegir(SimboloDudoso dudoso, String alternativa) {
        if (!dudosos.contains(dudoso)) {
            throw new IllegalArgumentException("El símbolo no es de este resultado: " + dudoso);
        }
        int inicio = dudoso.indice();
        int fin = inicio + dudoso.elegido().length();
        // "\times" seguido de una letra se leería como otro comando: se separa con un espacio.
        String nuevo = alternativa;
        if (nuevo.startsWith("\\") && fin < latex.length() && Character.isLetter(latex.charAt(fin))) {
            nuevo += " ";
        }
        String latexNuevo = latex.substring(0, inicio) + nuevo + latex.substring(fin);
        int corrimiento = nuevo.length() - dudoso.elegido().length();
        List<SimboloDudoso> quedan = new ArrayList<>();
        for (SimboloDudoso d : dudosos) {
            if (d.equals(dudoso)) {
                continue;
            }
            quedan.add(d.indice() > inicio
                    ? new SimboloDudoso(d.indice() + corrimiento, d.elegido(), d.alternativas(), d.confianza())
                    : d);
        }
        return new ResultadoReconocimiento(latexNuevo, quedan);
    }
}
