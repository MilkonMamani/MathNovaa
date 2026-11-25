package pe.mathnova.presentation.resolucion;

import java.util.List;
import java.util.Objects;

public record SeccionAnalisis(String titulo, List<VistaPropiedades.Fila> filas, List<String> marcas,
                              List<Integer> signos, String rotuloSignos, String aviso) {

    public SeccionAnalisis {
        Objects.requireNonNull(titulo, "titulo");
        filas = List.copyOf(filas);
        marcas = List.copyOf(marcas);
        signos = List.copyOf(signos);
    }

    public SeccionAnalisis(String titulo, List<VistaPropiedades.Fila> filas) {
        this(titulo, filas, List.of(), List.of(), null, null);
    }
}
