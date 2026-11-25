package pe.mathnova.domain.escritura;

import java.util.List;

public record SimboloDudoso(int indice, String elegido, List<String> alternativas, double confianza) {

    public SimboloDudoso {
        alternativas = List.copyOf(alternativas);
    }
}
