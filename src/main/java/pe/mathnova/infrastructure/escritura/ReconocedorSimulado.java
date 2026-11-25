package pe.mathnova.infrastructure.escritura;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.SimboloDudoso;
import pe.mathnova.domain.escritura.TablaConfusiones;
import pe.mathnova.domain.escritura.Trazo;

@Component
@ConditionalOnProperty(name = "mathnova.reconocedor", havingValue = "simulado", matchIfMissing = true)
public class ReconocedorSimulado implements ReconocedorEscritura {

    public static final String LATEX = "x^3-2x^2-x+2";
    static final int INDICE_DUDOSO = LATEX.indexOf("-x+") + 1;

    private final long pausaMs;

    @Autowired
    public ReconocedorSimulado() {
        this(600);
    }

    public ReconocedorSimulado(long pausaMs) {
        this.pausaMs = pausaMs;
    }

    @Override
    public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
        if (pausaMs > 0) {
            try {
                Thread.sleep(pausaMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Lectura cancelada", e);
            }
        }
        if (trazos.isEmpty()) {
            return new ResultadoReconocimiento("", List.of());
        }
        return new ResultadoReconocimiento(LATEX, List.of(
                new SimboloDudoso(INDICE_DUDOSO, "x", TablaConfusiones.alternativas("x"), 0.55)));
    }
}
