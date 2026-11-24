package pe.mathnova.infrastructure.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
