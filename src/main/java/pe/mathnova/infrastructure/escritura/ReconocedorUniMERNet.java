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
            python = this.carpeta.resolve("python").resolve("bin").resolve("python3");
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
        Runtime.getRuntime().addShutdownHook(new Thread(proceso::close, "reconocedor-cierre"));
    }

    ReconocedorUniMERNet(Path carpeta, ProcesoInferencia proceso) {
        this.carpeta = carpeta;
        this.proceso = proceso;
    }

    static Path ubicar(String configurada) {
        if (configurada != null && !configurada.isBlank()) {
            return Path.of(configurada).toAbsolutePath().normalize();
        }
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

    public static final String FALTA = "Falta el reconocedor de escritura";

    static void verificar(Path carpeta) {
        List<String> faltan = new ArrayList<>();
        for (String f : List.of("python/python.exe", "servidor/servidor_unimernet.py",
                "modelo/unimernet_small/unimernet_small.pth", "modelo/unimernet_small/tokenizer.json")) {
            if (!Files.isRegularFile(carpeta.resolve(f))) {
                faltan.add(f.replace('/', java.io.File.separatorChar));
            }
        }
        if (!faltan.isEmpty()) {
            throw new IllegalStateException(FALTA + " en " + carpeta + " (no está: " + String.join(", ", faltan)
                    + "). Copia la carpeta \"reconocedor\" del paquete de MathNova o ejecuta"
                    + " reconocedor\\preparar\\preparar-reconocedor.ps1 con internet.");
        }
    }

    @Override
    public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
        verificar(carpeta);
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

    String reconocerPng(byte[] png) throws IOException, InterruptedException {
        return proceso.reconocer(png);
    }

    public long cargaMs() {
        return proceso.cargaMs();
    }

    @Override
    public void preparar() {
        verificar(carpeta);
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

    public long pid() {
        return proceso.pid();
    }

    @Override
    public void destroy() {
        proceso.close();
    }
}
