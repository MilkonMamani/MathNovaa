package pe.mathnova.infrastructure.escritura;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class ProcesoInferencia implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcesoInferencia.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final List<String> comando;
    private final Path carpeta;
    private final Duration esperaCarga;
    private final Duration esperaLectura;
    private final Path registro;

    private final Semaphore libre = new Semaphore(1);
    private final Map<Long, CompletableFuture<String>> pendientes = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    private Process proceso;
    private Writer entrada;
    private CompletableFuture<JsonNode> listo;
    private volatile long cargaMs = -1;
    private volatile boolean cerrado;

    public ProcesoInferencia(List<String> comando, Path carpeta, Duration esperaCarga, Duration esperaLectura,
                             Path registro) {
        this.comando = List.copyOf(comando);
        this.carpeta = carpeta;
        this.esperaCarga = esperaCarga;
        this.esperaLectura = esperaLectura;
        this.registro = registro;
    }

    public synchronized boolean listo() {
        return proceso != null && proceso.isAlive() && listo != null && listo.isDone() && !listo.isCompletedExceptionally();
    }

    public long cargaMs() {
        return cargaMs;
    }

    public synchronized long pid() {
        return proceso != null && proceso.isAlive() ? proceso.pid() : -1;
    }

    public void iniciar() throws IOException, InterruptedException {
        CompletableFuture<JsonNode> espera;
        synchronized (this) {
            if (cerrado) {
                throw new IOException("MathNova se está cerrando");
            }
            if (proceso == null || !proceso.isAlive()) {
                arrancar();
            }
            espera = listo;
        }
        try {
            JsonNode r = espera.get(esperaCarga.toMillis(), TimeUnit.MILLISECONDS);
            if (!r.path("listo").asBoolean(false)) {
                detener();
                throw new IOException("El reconocedor no pudo cargar el modelo: " + r.path("error").asString(""));
            }
        } catch (ExecutionException e) {
            detener();
            throw new IOException("El reconocedor se cerró al cargar el modelo", e.getCause());
        } catch (TimeoutException e) {
            detener();
            throw new IOException("El reconocedor tardó más de " + esperaCarga.toSeconds() + " s en cargar el modelo");
        }
    }

    public String reconocer(byte[] png) throws IOException, InterruptedException {
        iniciar();
        libre.acquire();
        long id = ids.incrementAndGet();
        CompletableFuture<String> respuesta = new CompletableFuture<>();
        respuesta.whenComplete((r, e) -> libre.release());
        pendientes.put(id, respuesta);
        try {
            ObjectNode pedido = JSON.createObjectNode();
            pedido.put("id", id);
            pedido.put("imagen", Base64.getEncoder().encodeToString(png));
            escribir(JSON.writeValueAsString(pedido));
        } catch (IOException | RuntimeException e) {
            pendientes.remove(id);
            respuesta.completeExceptionally(e);
            detener();
            throw new IOException("No se pudo enviar la imagen al reconocedor", e);
        }
        try {
            return respuesta.get(esperaLectura.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            throw c instanceof IOException io ? io : new IOException(c.getMessage(), c);
        } catch (TimeoutException e) {
            LOG.warn("El reconocedor no respondió en {} s; se reinicia", esperaLectura.toSeconds());
            detener();
            throw new IOException("El reconocedor no respondió a tiempo");
        }
    }

    private synchronized void escribir(String linea) throws IOException {
        if (entrada == null) {
            throw new IOException("El reconocedor no está en marcha");
        }
        entrada.write(linea);
        entrada.write('\n');
        entrada.flush();
    }

    private void arrancar() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(comando).directory(carpeta.toFile());
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("PYTHONUTF8", "1");
        pb.environment().put("PYTHONDONTWRITEBYTECODE", "1");
        pb.environment().remove("PYTHONPATH");
        pb.environment().remove("PYTHONHOME");
        if (registro != null) {
            Files.createDirectories(registro.getParent());
            pb.redirectError(ProcessBuilder.Redirect.appendTo(registro.toFile()));
        }
        LOG.info("Arrancando el reconocedor: {}", comando);
        long inicio = System.nanoTime();
        Process p = pb.start();
        proceso = p;
        entrada = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8));
        CompletableFuture<JsonNode> esteListo = new CompletableFuture<>();
        listo = esteListo;
        Thread lector = new Thread(() -> leer(p, esteListo, inicio), "reconocedor-salida");
        lector.setDaemon(true);
        lector.start();
        if (registro == null) {
            Thread errores = new Thread(() -> volcarErrores(p), "reconocedor-errores");
            errores.setDaemon(true);
            errores.start();
        }
    }

    private void leer(Process p, CompletableFuture<JsonNode> esteListo, long inicio) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                JsonNode n;
                try {
                    n = JSON.readTree(linea);
                } catch (RuntimeException e) {
                    LOG.debug("Línea del reconocedor que no es JSON: {}", linea);
                    continue;
                }
                if (n.has("listo")) {
                    if (n.path("listo").asBoolean(false)) {
                        cargaMs = n.path("carga_ms").asLong((System.nanoTime() - inicio) / 1_000_000L);
                        LOG.info("Reconocedor listo en {} ms (proceso, {} ms desde el arranque): {}", cargaMs,
                                (System.nanoTime() - inicio) / 1_000_000L, n.path("version").asString(""));
                    }
                    esteListo.complete(n);
                    continue;
                }
                CompletableFuture<String> f = pendientes.remove(n.path("id").asLong(-1));
                if (f == null) {
                    continue;
                }
                if (n.has("latex")) {
                    LOG.info("Reconocido en {} ms: {}", n.path("ms").asLong(), n.path("latex").asString(""));
                    f.complete(n.path("latex").asString(""));
                } else {
                    f.completeExceptionally(new IOException("El reconocedor falló: " + n.path("error").asString("")));
                }
            }
        } catch (IOException e) {
            LOG.debug("Se cortó la salida del reconocedor", e);
        }
        IOException fin = new IOException("El reconocedor se cerró"
                + (p.isAlive() ? "" : " (código " + codigo(p) + ")"));
        esteListo.completeExceptionally(fin);
        pendientes.values().forEach(f -> f.completeExceptionally(fin));
        pendientes.clear();
        synchronized (this) {
            if (proceso == p) {
                proceso = null;
                entrada = null;
            }
        }
    }

    private static String codigo(Process p) {
        try {
            return String.valueOf(p.waitFor(2, TimeUnit.SECONDS) ? p.exitValue() : "?");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "?";
        }
    }

    private static void volcarErrores(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                LOG.debug("[reconocedor] {}", linea);
            }
        } catch (IOException e) {
        }
    }

    public synchronized void detener() {
        Process p = proceso;
        proceso = null;
        if (entrada != null) {
            try {
                entrada.close();
            } catch (IOException e) {
            }
            entrada = null;
        }
        if (p == null) {
            return;
        }
        try {
            if (!p.waitFor(3, TimeUnit.SECONDS)) {
                p.descendants().forEach(ProcessHandle::destroyForcibly);
                p.destroyForcibly();
                p.waitFor(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            p.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        LOG.info("Reconocedor detenido");
    }

    @Override
    public void close() {
        cerrado = true;
        detener();
    }

    void anotar(String texto) {
        if (registro == null) {
            return;
        }
        try {
            Files.createDirectories(registro.getParent());
            Files.writeString(registro, texto + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.debug("No se pudo escribir en {}", registro, e);
        }
    }
}
