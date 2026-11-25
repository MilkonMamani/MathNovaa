package pe.mathnova.infrastructure.escritura;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.Trazo;

@Component
@ConditionalOnProperty(name = "mathnova.reconocedor", havingValue = "sin-conexion")
public class ReconocedorSinConexion implements ReconocedorEscritura {

    @Override
    public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
        throw new UnsupportedOperationException("Todavía no hay un motor de escritura a mano instalado");
    }
}
