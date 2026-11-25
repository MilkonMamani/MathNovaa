package pe.mathnova.domain.escritura;

import java.util.ArrayList;
import java.util.List;

public record ResultadoReconocimiento(String latex, List<SimboloDudoso> dudosos) {

    public ResultadoReconocimiento {
        dudosos = List.copyOf(dudosos);
    }

    public ResultadoReconocimiento elegir(SimboloDudoso dudoso, String alternativa) {
        if (!dudosos.contains(dudoso)) {
            throw new IllegalArgumentException("El símbolo no es de este resultado: " + dudoso);
        }
        int inicio = dudoso.indice();
        int fin = inicio + dudoso.elegido().length();
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
