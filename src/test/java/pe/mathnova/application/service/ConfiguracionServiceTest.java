package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.application.service.ConfiguracionService.ModoPizarra;
import pe.mathnova.application.service.ConfiguracionService.TamanoLetra;
import pe.mathnova.application.service.ConfiguracionService.Tema;
import pe.mathnova.domain.factorizacion.Campo;

/**
 * Sprint 9, parte E: la tabla configuracion. Valores por defecto, guardar y leer cada clave (también por su
 * propiedad observable) y que "Borrar todo el historial" no toque los ejemplos. Base de prueba de target/; cada
 * prueba se deshace al terminar, y las propiedades vuelven a su valor por defecto.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
@Transactional
class ConfiguracionServiceTest {

    private static final List<String> CLAVES = List.of(ConfiguracionService.CAMPO_POR_DEFECTO,
            ConfiguracionService.MODO_PIZARRA, ConfiguracionService.MOSTRAR_EXPLICACIONES,
            ConfiguracionService.MOSTRAR_CODIGOS, ConfiguracionService.TAMANO_LETRA, ConfiguracionService.TEMA);

    @Autowired
    private ConfiguracionService configuracion;
    @Autowired
    private HistorialService historial;
    @Autowired
    private ResolverService resolver;
    @Autowired
    private PracticaService practica;
    @Autowired
    private JdbcTemplate jdbc;

    /** El servicio es uno para todas las pruebas: sus propiedades vuelven a lo de fábrica. */
    @AfterEach
    void restaurar() {
        configuracion.guardarCampoPorDefecto(Campo.Q);
        configuracion.guardarModoPizarra(ModoPizarra.TODOS);
        configuracion.guardarMostrarExplicaciones(false);
        configuracion.guardarMostrarCodigos(false);
        configuracion.guardarTamanoLetra(TamanoLetra.NORMAL);
        configuracion.guardarTema(Tema.CLARO);
    }

    private String valor(String clave) {
        return jdbc.queryForObject("SELECT valor FROM configuracion WHERE clave = ?", String.class, clave);
    }

    @Test
    void sinNadaGuardadoValenLosDeFabrica() {
        jdbc.update("DELETE FROM configuracion WHERE clave IN (?, ?, ?, ?, ?, ?)", CLAVES.toArray());
        assertEquals(Campo.Q, configuracion.campoPorDefecto(), "RN27");
        assertEquals(ModoPizarra.TODOS, configuracion.modoPizarra());
        assertFalse(configuracion.mostrarExplicaciones());
        assertFalse(configuracion.mostrarCodigos(), "RNF09: apagado por defecto");
        assertEquals(TamanoLetra.NORMAL, configuracion.tamanoLetra());
        assertEquals(Tema.CLARO, configuracion.tema());
        assertTrue(configuracion.animacionesActivas());

        // Al arrancar se crean las claves que faltan, con esos valores
        configuracion.crearClavesFaltantes();
        assertEquals(Map.of(ConfiguracionService.CAMPO_POR_DEFECTO, "Q", ConfiguracionService.MODO_PIZARRA, "todos",
                ConfiguracionService.MOSTRAR_EXPLICACIONES, "false", ConfiguracionService.MOSTRAR_CODIGOS, "false",
                ConfiguracionService.TAMANO_LETRA, "normal", ConfiguracionService.TEMA, "claro"),
                Map.of(CLAVES.get(0), valor(CLAVES.get(0)), CLAVES.get(1), valor(CLAVES.get(1)), CLAVES.get(2),
                        valor(CLAVES.get(2)), CLAVES.get(3), valor(CLAVES.get(3)), CLAVES.get(4), valor(CLAVES.get(4)),
                        CLAVES.get(5), valor(CLAVES.get(5))));
    }

    @Test
    void unValorQueNoSeEntiendeEsElDeFabrica() {
        jdbc.update("UPDATE configuracion SET valor = 'Z' WHERE clave = ?", ConfiguracionService.CAMPO_POR_DEFECTO);
        jdbc.update("UPDATE configuracion SET valor = 'enorme' WHERE clave = ?", ConfiguracionService.TAMANO_LETRA);
        assertEquals(Campo.Q, configuracion.campoPorDefecto());
        assertEquals(TamanoLetra.NORMAL, configuracion.tamanoLetra());
    }

    @Test
    void campoPorDefecto() {
        configuracion.guardarCampoPorDefecto(Campo.C);
        assertEquals("C", valor(ConfiguracionService.CAMPO_POR_DEFECTO));
        assertEquals(Campo.C, configuracion.campoPorDefecto());
        assertEquals(Campo.C, configuracion.campoProperty().get());
        configuracion.guardarCampoPorDefecto(Campo.R);
        assertEquals(Campo.R, configuracion.campoPorDefecto());
    }

    @Test
    void comoSeAbreLaPizarra() {
        configuracion.guardarModoPizarra(ModoPizarra.PASO_A_PASO);
        assertEquals("paso", valor(ConfiguracionService.MODO_PIZARRA));
        assertEquals(ModoPizarra.PASO_A_PASO, configuracion.modoPizarra());
        assertEquals(ModoPizarra.PASO_A_PASO, configuracion.modoProperty().get());
    }

    @Test
    void mostrarExplicaciones() {
        configuracion.guardarMostrarExplicaciones(true);
        assertEquals("true", valor(ConfiguracionService.MOSTRAR_EXPLICACIONES));
        assertTrue(configuracion.mostrarExplicaciones());
        assertTrue(configuracion.explicacionesProperty().get());
    }

    @Test
    void mostrarCodigos() {
        configuracion.guardarMostrarCodigos(true);
        assertEquals("true", valor(ConfiguracionService.MOSTRAR_CODIGOS));
        assertTrue(configuracion.mostrarCodigos());
        assertTrue(configuracion.codigosProperty().get());
    }

    @Test
    void tamanoDeLetra() {
        configuracion.guardarTamanoLetra(TamanoLetra.MUY_GRANDE);
        assertEquals("muy_grande", valor(ConfiguracionService.TAMANO_LETRA));
        assertEquals(TamanoLetra.MUY_GRANDE, configuracion.tamanoLetra());
        assertEquals(TamanoLetra.MUY_GRANDE, configuracion.letraProperty().get());
        assertEquals(1.4, configuracion.tamanoLetra().factor());
    }

    @Test
    void tema() {
        configuracion.guardarTema(Tema.OSCURO);
        assertEquals("oscuro", valor(ConfiguracionService.TEMA));
        assertEquals(Tema.OSCURO, configuracion.tema());
        assertEquals(Tema.OSCURO, configuracion.temaProperty().get());
    }

    @Test
    void lasPropiedadesAvisanAlCambiar() {
        List<Object> vistos = new java.util.ArrayList<>();
        configuracion.temaProperty().addListener((o, a, b) -> vistos.add(b));
        configuracion.letraProperty().addListener((o, a, b) -> vistos.add(b));
        configuracion.guardarTema(Tema.OSCURO);
        configuracion.guardarTamanoLetra(TamanoLetra.GRANDE);
        assertEquals(List.of(Tema.OSCURO, TamanoLetra.GRANDE), vistos);
    }

    @Test
    void borrarElHistorialNoTocaLosEjemplos() {
        resolver.resolverYGuardar("x^3 - 2x^2 - x + 2", Campo.Q, null);
        resolver.resolverYGuardar("lim(x→2, (x^2 - 4)/(x - 2))", Campo.Q, null);
        PracticaService.Practica p = practica.iniciar("2x^3 - 3x^2 - 3x + 2", Campo.Q);
        practica.responder(p.sesionId(), 1, practica.correcta(p.sesionId(), 1));
        int ejemplos = jdbc.queryForObject("SELECT COUNT(*) FROM ejemplo", Integer.class);
        int tecnicas = jdbc.queryForObject("SELECT COUNT(*) FROM tecnica", Integer.class);
        int ajustes = jdbc.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class);
        assertTrue(historial.ejerciciosGuardados() >= 3);
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM paso", Integer.class) > 0);
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM intento_paso", Integer.class) > 0);

        historial.borrarTodo();

        for (String tabla : List.of("ejercicio", "ejercicio_tema", "resolucion", "paso", "parametro_resolucion",
                "restriccion_dominio", "sugerencia", "sesion_practica", "intento_paso")) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class), tabla);
        }
        assertEquals(0, historial.ejerciciosGuardados());
        assertEquals(ejemplos, jdbc.queryForObject("SELECT COUNT(*) FROM ejemplo", Integer.class), "los ejemplos quedan");
        assertEquals(33, ejemplos);
        assertEquals(tecnicas, jdbc.queryForObject("SELECT COUNT(*) FROM tecnica", Integer.class));
        assertEquals(ajustes, jdbc.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class));
    }

    @Test
    void laRutaDeLaBaseSeMuestraCorta() {
        assertEquals("~/.mathnova/mathnova.db",
                InformacionService.rutaCorta("jdbc:sqlite:C:\\Users\\ana/.mathnova/mathnova.db", "C:\\Users\\ana"));
        assertEquals("target/prueba-mathnova.db", InformacionService.rutaCorta("jdbc:sqlite:target/prueba-mathnova.db",
                "C:\\Users\\ana"));
    }
}
