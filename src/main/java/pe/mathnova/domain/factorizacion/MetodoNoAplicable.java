package pe.mathnova.domain.factorizacion;

import java.util.Objects;

/**
 * HU08: se forzó un método que no aplica a la expresión. No es un error: la pantalla muestra el
 * motivo ("x² + 4 es una suma, no una diferencia").
 */
public final class MetodoNoAplicable extends RuntimeException {

    private final String codigo;
    private final String motivo;

    public MetodoNoAplicable(String codigo, String motivo) {
        super(Tecnicas.nombre(codigo) + " no aplica: " + motivo);
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.motivo = Objects.requireNonNull(motivo, "motivo");
    }

    public String codigo() {
        return codigo;
    }

    public String motivo() {
        return motivo;
    }
}
