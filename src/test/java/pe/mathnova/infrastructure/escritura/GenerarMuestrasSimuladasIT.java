package pe.mathnova.infrastructure.escritura;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import pe.mathnova.domain.escritura.Muestra;
import pe.mathnova.domain.escritura.Trazo;

/**
 * Escribe reconocedor/pruebas/*.json con los trazos SIMULADOS de los casos A-E, para la autoprueba del
 * paquete ({@code MathNova.exe --mathnova.autoprueba=reconocedor\pruebas}). No son escritura de una persona.
 * Se corre con {@code -Dmathnova.generar.muestras=true}.
 */
@EnabledIfSystemProperty(named = "mathnova.generar.muestras", matches = "true")
class GenerarMuestrasSimuladasIT {

    @Test
    void generar() throws Exception {
        Path carpeta = Path.of("reconocedor", "pruebas");
        Files.createDirectories(carpeta);
        guardar(carpeta, "A", "SIMULADO A: 3x^2+2x-1", TrazosSimulados.casoA(45, 1));
        guardar(carpeta, "B", "SIMULADO B: integral de 2x dx", TrazosSimulados.casoB(45, 1));
        guardar(carpeta, "C", "SIMULADO C: integral de 0 a 1 de 2x dx", TrazosSimulados.casoC(45, 1));
        guardar(carpeta, "D", "SIMULADO D: (x+1)/(x-1)", TrazosSimulados.casoD(45, 1));
        guardar(carpeta, "E", "SIMULADO E: raiz de x^2+1", TrazosSimulados.casoE(45, 1));
    }

    private static void guardar(Path carpeta, String nombre, String etiqueta, List<Trazo> trazos) throws Exception {
        Muestra m = new Muestra(etiqueta, 520, 220, trazos);
        Path tmp = new ArchivoMuestras(carpeta).guardar(m);
        Files.move(tmp, carpeta.resolve(nombre + ".json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
