package pe.mathnova.infrastructure.escritura;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.Trazo;

/**
 * Lugar para el motor real de escritura a mano, que debe funcionar sin conexión. Se activa con
 * {@code mathnova.reconocedor=sin-conexion}.
 *
 * <p>TODO(escritura a mano): conectar un motor real. Opciones evaluadas:
 * <ul>
 *   <li>Un modelo entrenado con CROHME (por ejemplo BTTR o CoMER) exportado a ONNX y ejecutado con
 *       ONNX Runtime para Java (com.microsoft.onnxruntime:onnxruntime). Los trazos (x, y, t) se
 *       normalizan a la altura del modelo y se rasterizan o se pasan como secuencia, según el modelo.
 *       Las probabilidades por símbolo del decodificador dan {@code confianza}; por debajo de un umbral
 *       el símbolo va como {@code SimboloDudoso} con {@code TablaConfusiones.alternativas}.</li>
 *   <li>Un SDK comercial que funcione sin conexión (MyScript iink u otro), si la licencia lo permite.</li>
 * </ul>
 * Las dependencias del motor se agregan recién cuando se elija uno; hasta entonces este bean avisa que
 * no hay motor.
 */
@Component
@ConditionalOnProperty(name = "mathnova.reconocedor", havingValue = "sin-conexion")
public class ReconocedorSinConexion implements ReconocedorEscritura {

    @Override
    public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
        throw new UnsupportedOperationException("Todavía no hay un motor de escritura a mano instalado");
    }
}
