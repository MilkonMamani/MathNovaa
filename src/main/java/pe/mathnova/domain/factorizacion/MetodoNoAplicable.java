package pe.mathnova.domain.factorizacion;

import java.util.Objects;

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
