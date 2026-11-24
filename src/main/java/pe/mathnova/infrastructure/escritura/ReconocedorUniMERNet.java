package pe.mathnova.infrastructure.escritura;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.Trazo;

/**
 * El reconocedor real de la pestaña "Escribir": UniMERNet Small en un proceso local, solo CPU y sin red.
 * Se activa con {@code mathnova.reconocedor=unimernet}.
 *
 * <p>Los trazos se dibujan en una imagen ({@link ImagenTrazos}) y se mandan al proceso persistente
 * ({@link ProcesoInferencia}); la respuesta es el LaTeX tal como lo lee el modelo, sin retoques: el
 * adaptador al editor está en {@code LatexReconocido}.
 *
 * <p>Dónde está el reconocedor (la carpeta con {@code python/}, {@code servidor/} y {@code modelo/}):
 * {@code mathnova.unimernet.carpeta} si se puso; si no, junto al ejecutable instalado
 * ({@code <instalación>/reconocedor}); si no, {@code ./reconocedor} (desarrollo).
 */
@Component
@ConditionalOnProperty(name = "mathnova.reconocedor", havingValue = "unimernet")
public class ReconocedorUniMERNet implements ReconocedorEscritura, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReconocedorUniMERNet.class);

    private final Path carpeta;
    private final ProcesoInferencia proceso;

    @Autowired
    public ReconocedorUniMERNet(@Value("${mathnova.unimernet.carpeta:}") String carpeta,
                                @Value("${mathnova.unimernet.hilos:0}") int hilos,
                                @Value("${mathnova.unimernet.espera-carga-s:240}") long esperaCarga,
                                @Value("${mathnova.unimernet.espera-lectura-s:90}") long esperaLectura) {
        this.carpeta = ubicar(carpeta);
        Path python = this.carpeta.resolve("python").resolve("python.exe");
        if (!Files.isRegularFile(python)) {
            python = this.carpeta.resolve("python").resolve("bin").resolve("python3"); // no Windows (desarrollo)
        }
        List<String> comando = new ArrayList<>(List.of(python.toString(), "-I", "-u",
                this.carpeta.resolve("servidor").resolve("servidor_unimernet.py").toString(),
                "--modelo", this.carpeta.resolve("modelo").resolve("unimernet_small").toString()));
        if (hilos > 0) {
            comando.add("--hilos");
            comando.add(String.valueOf(hilos));
        }
        Path registro = Path.of(System.getProperty("user.home"), ".mathnova", "reconocedor.log");
        this.proceso = new ProcesoInferencia(comando, this.carpeta, Duration.ofSeconds(esperaCarga),
                Duration.ofSeconds(esperaLectura), registro);
        LOG.info("Reconocedor UniMERNet en {}", this.carpeta);
        // Por si la JVM termina sin cerrar Spring (no debería): el proceso no queda huérfano.
        Runtime.getRuntime().addShutdownHook(new Thread(proceso::close, "reconocedor-cierre"));
    }

    /** Para las pruebas: con un comando cualquiera. */
    ReconocedorUniMERNet(Path carpeta, ProcesoInferencia proceso) {
        this.carpeta = carpeta;
        this.proceso = proceso;
    }

    static Path ubicar(String configurada) {
        if (configurada != null && !configurada.isBlank()) {
            return Path.of(configurada).toAbsolutePath().normalize();
        }
        // jpackage pone la ruta del .exe en jpackage.app-path: <instalación>/MathNova.exe
        String app = System.getProperty("jpackage.app-path");
        if (app != null && !app.isBlank()) {
            Path junto = Path.of(app).toAbsolutePath().getParent().resolve("reconocedor");
            if (Files.isDirectory(junto)) {
                return junto;
            }
        }
        return Path.of(System.getProperty("user.dir"), "reconocedor").toAbsolutePath().normalize();
    }

    public Path carpeta() {
        return carpeta;
    }

    @Override
    public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
        byte[] png = ImagenTrazos.png(trazos);
        if (png == null) {
            return new ResultadoReconocimiento("", List.of());
        }
        try {
            return new ResultadoReconocimiento(proceso.reconocer(png), List.of());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lectura cancelada", e);
        } catch (IOException e) {
            LOG.warn("No se pudo reconocer la escritura: {}", e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** El LaTeX de una imagen PNG cualquiera (pruebas con imágenes tipográficas). */
    String reconocerPng(byte[] png) throws IOException, InterruptedException {
        return proceso.reconocer(png);
    }

    /** Milisegundos que tardó en cargar el modelo; -1 si no cargó. */
    public long cargaMs() {
        return proceso.cargaMs();
    }

    @Override
    public void preparar() {
        try {
            proceso.iniciar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            LOG.warn("No se pudo preparar el reconocedor: {}", e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean listo() {
        return proceso.listo();
    }

    /** PID del proceso de inferencia (para medir memoria); -1 si no está en marcha. */
    public long pid() {
        return proceso.pid();
    }

    @Override
    public void destroy() {
        proceso.close();
    }
}
