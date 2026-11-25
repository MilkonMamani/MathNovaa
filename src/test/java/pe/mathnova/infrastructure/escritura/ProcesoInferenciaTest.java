package pe.mathnova.infrastructure.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class ProcesoInferenciaTest {

    private static ProcesoInferencia proceso(String modo) throws Exception {
        String java = ProcessHandle.current().info().command().orElse("java");
        Path fuente = Path.of(ProcesoInferenciaTest.class.getResource("/escritura/ServidorFalso.java").toURI());
        return new ProcesoInferencia(List.of(java, fuente.toString(), modo), fuente.getParent(),
                Duration.ofSeconds(60), Duration.ofSeconds(20), null);
    }

    @Test
    void cargaUnaVezYAtiendeVariasImagenesConElMismoProceso() throws Exception {
        try (ProcesoInferencia p = proceso("normal")) {
            assertFalse(p.listo());
            assertEquals("img4", p.reconocer(new byte[] {1, 2, 3}));
            long pid = p.pid();
            assertTrue(p.listo());
            assertEquals("img8", p.reconocer(new byte[] {1, 2, 3, 4, 5}));
            assertEquals(pid, p.pid(), "no se volvió a arrancar");
        }
    }

    @Test
    void unaLecturaAbandonadaNoSeMezclaConLaSiguiente() throws Exception {
        try (ProcesoInferencia p = proceso("lento")) {
            p.iniciar();
            AtomicReference<Throwable> fin = new AtomicReference<>();
            Thread vieja = new Thread(() -> {
                try {
                    p.reconocer(new byte[] {1, 2, 3});
                } catch (Throwable e) {
                    fin.set(e);
                }
            });
            vieja.start();
            Thread.sleep(150);
            vieja.interrupt();
            vieja.join();
            assertTrue(fin.get() instanceof InterruptedException, String.valueOf(fin.get()));
            assertEquals("img8", p.reconocer(new byte[] {1, 2, 3, 4, 5}));
        }
    }

    @Test
    void siElProcesoMuereLaLecturaFallaYElSiguientePedidoLoRearranca() throws Exception {
        try (ProcesoInferencia p = proceso("morir")) {
            p.iniciar();
            long pid = p.pid();
            assertThrows(IOException.class, () -> p.reconocer(new byte[] {1}));
            p.iniciar();
            assertTrue(p.listo());
            assertTrue(p.pid() != pid);
        }
    }

    @Test
    void unModeloQueNoCargaSeAvisa() throws Exception {
        try (ProcesoInferencia p = proceso("roto")) {
            IOException e = assertThrows(IOException.class, p::iniciar);
            assertTrue(e.getMessage().contains("falta el modelo"), e.getMessage());
        }
    }

    @Test
    void alCerrarTerminaElProceso() throws Exception {
        ProcesoInferencia p = proceso("normal");
        p.iniciar();
        ProcessHandle h = ProcessHandle.of(p.pid()).orElseThrow();
        p.close();
        h.onExit().get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertFalse(h.isAlive());
        assertThrows(IOException.class, p::iniciar, "cerrado no vuelve a arrancar");
    }
}
