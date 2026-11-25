package pe.mathnova.infrastructure.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import pe.mathnova.domain.escritura.ReconocedorEscritura;

/** Spring arma el reconocedor real con mathnova.reconocedor=unimernet, sin arrancar el proceso. */
class ReconocedorUniMERNetContextoTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ReconocedorUniMERNet.class, ReconocedorSimulado.class);

    @Test
    void conUnimernetSeCreaElReconocedorRealSinArrancarElProceso() {
        contexto.withPropertyValues("mathnova.reconocedor=unimernet",
                        "mathnova.unimernet.carpeta=C:/Program Files/MathNova/reconocedor")
                .run(c -> {
                    ReconocedorEscritura r = c.getBean(ReconocedorEscritura.class);
                    assertTrue(r instanceof ReconocedorUniMERNet);
                    ReconocedorUniMERNet u = (ReconocedorUniMERNet) r;
                    assertEquals(Path.of("C:/Program Files/MathNova/reconocedor").toAbsolutePath().normalize(), u.carpeta());
                    assertFalse(u.listo(), "el modelo se carga recién al usar Escribir");
                    assertEquals(-1, u.pid());
                });
    }

    @Test
    void porDefectoEnLasPruebasSigueElSimulado() {
        contexto.withPropertyValues("mathnova.reconocedor=simulado")
                .run(c -> assertTrue(c.getBean(ReconocedorEscritura.class) instanceof ReconocedorSimulado));
    }

    @Test
    void sinLosArchivosDelReconocedorDiceQueFaltaYComoConseguirlo(@org.junit.jupiter.api.io.TempDir Path vacia) {
        // Lo que tiene quien clona el repositorio: el código, pero no python\ ni modelo\ (no van en Git).
        ReconocedorUniMERNet r = new ReconocedorUniMERNet(vacia.toString(), 0, 10, 10);
        IllegalStateException e = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, r::preparar);
        assertTrue(e.getMessage().startsWith(ReconocedorUniMERNet.FALTA), e.getMessage());
        assertTrue(e.getMessage().contains("python.exe") && e.getMessage().contains("unimernet_small.pth"), e.getMessage());
        assertTrue(e.getMessage().contains("preparar-reconocedor.ps1"), e.getMessage());
        assertThrows(IllegalStateException.class, () -> r.reconocer(java.util.List.of()));
        assertEquals(-1, r.pid(), "no intentó arrancar nada");
    }
}
