package pe.mathnova.domain.calculo;

import java.util.Objects;

public record CaminoCalculo(String codigo, String nombre, Estado estado, String texto) {

    public enum Estado { USADO, SIRVE, NO_APLICA }

    public CaminoCalculo {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(estado, "estado");
    }
}
