package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.calculo.parciales.ResolucionParcial;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.sugerencias.Sugerencia;
import pe.mathnova.presentation.pizarra.ModeloAspa;

class PestanasSprint7Test {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService SERVICIO = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static ContenidoFactorizacion factorizacion(String texto) {
        Resolucion r = FACTORIZACION.factorizar(texto);
        VistaResolucion v = VistaResolucion.de(r, FACTORIZACION.verificar(r), false);
        return ContenidoFactorizacion.de(r, v, texto, false);
    }

    private static ResolucionParcial parciales(String texto) {
        return SERVICIO.resolverParciales(texto, f -> { });
    }

    private static Map<String, String> filas(SeccionAnalisis s) {
        return s.filas().stream().collect(Collectors.toMap(VistaPropiedades.Fila::nombre, VistaPropiedades.Fila::valor,
                (a, b) -> a, java.util.LinkedHashMap::new));
    }

    @Test
    void integralPorPartesMuestraIlateEnExploracion() {
        ResolucionIntegral r = SERVICIO.resolverIntegral("∫(x ln x, x)", null, f -> { });
        ContenidoIntegral c = ContenidoIntegral.de(r, SERVICIO.caminos(r), false);
        VistaExploracion.Tarjeta usado = c.exploracion().tarjetas().getFirst();
        assertAll(
                () -> assertEquals("I03", usado.codigo()),
                () -> assertEquals(VistaExploracion.Estado.USADO, usado.estado()),
                () -> assertTrue(usado.vistaPrevia() != null && usado.vistaPrevia().startsWith("ILATE: u = ln x"),
                        String.valueOf(usado.vistaPrevia())),
                () -> assertTrue(c.exploracion().tarjetas().stream()
                        .filter(t -> t.estado() == VistaExploracion.Estado.NO_APLICA)
                        .allMatch(t -> t.motivo() != null && !t.motivo().contains("próxima versión"))),
                () -> assertTrue(c.grafica().isPresent(), "la gráfica es la de siempre"),
                () -> assertEquals("sí, numérico en cinco puntos", c.secciones().getFirst().filas().get(3).valor()));
        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta());
        assertTrue(v.subtitulo().endsWith("Por partes (ILATE)"), v.subtitulo());
    }

    @Test
    void factorizacionMultivariable() {
        ContenidoFactorizacion c = factorizacion("x^3 - 7xy^2 + 6y^3");
        Map<String, String> f = filas(c.secciones().getFirst());
        assertAll(
                () -> assertTrue(c.grafica().isEmpty(), "gráfica: el aviso de una variable"),
                () -> assertTrue(c.propiedades().isEmpty()),
                () -> assertEquals("3", f.get("Grado total")),
                () -> assertEquals("3", f.get("Grado en x")),
                () -> assertEquals("3", f.get("Grado en y")),
                () -> assertTrue(f.get("¿Homogéneo?").startsWith("sí"), f.get("¿Homogéneo?")),
                () -> assertEquals("M06", c.exploracion().tarjetas().getFirst().codigo()),
                () -> assertTrue(c.exploracion().tarjetas().stream().allMatch(t -> t.codigo().startsWith("M")
                        || t.codigo().startsWith("F")), c.exploracion().tarjetas()::toString));
        assertTrue(filas(factorizacion("x^2 + xy + 2x + 2y").secciones().getFirst()).get("¿Homogéneo?").startsWith("no"));
        assertTrue(factorizacion("x^2 - 5x + 6").secciones().isEmpty(), "con una letra no cambia nada");
    }

    @Test
    void exploracionConMetodosAlternativos() {
        ContenidoFactorizacion c = factorizacion("x^3 - x^2y - xy^2 + y^3");
        List<String> codigos = c.exploracion().tarjetas().stream().map(VistaExploracion.Tarjeta::codigo).toList();
        assertEquals("M07", codigos.getFirst());
        assertTrue(codigos.contains("M02"), codigos.toString());
        assertTrue(c.exploracion().tarjetas().stream().anyMatch(t -> t.codigo().equals("M06")
                && t.estado() == VistaExploracion.Estado.NO_APLICA), codigos.toString());
    }

    @Test
    void aspaSimpleHomogeneaConLaSegundaLetra() {
        Resolucion r = FACTORIZACION.factorizar("2x^2 + 5xy - 3y^2");
        ModeloAspa m = ModeloAspa.de((TrabajoIntermedio.AspaSimple) r.pasos().getFirst().trabajo());
        assertEquals(List.of("2x", "x"), m.izquierda());
        assertEquals(List.of("−y", "3y"), m.derecha());
        assertEquals("6xy − xy = 5xy ✓", m.suma());
    }

    @Test
    void parcialesConHessiana() {
        ResolucionParcial r = parciales("grad(x^3 y + y^2)");
        ContenidoParciales c = ContenidoParciales.de(r);
        assertAll(
                () -> assertTrue(c.grafica().isEmpty(), "gráfica: el aviso de una variable"),
                () -> assertTrue(c.exploracion().tarjetas().isEmpty(), "exploración vacía"),
                () -> assertEquals(List.of("Dominio de f", "Parciales de primer orden", "Parciales de segundo orden",
                        "Matriz hessiana"), c.secciones().stream().map(SeccionAnalisis::titulo).toList()),
                () -> assertEquals(List.of("∂f/∂x", "∂f/∂y"), c.secciones().get(1).filas().stream()
                        .map(VistaPropiedades.Fila::nombre).toList()),
                () -> assertEquals(List.of("∂²f/∂x²", "∂²f/∂y∂x", "∂²f/∂x∂y", "∂²f/∂y²"), c.secciones().get(2).filas()
                        .stream().map(VistaPropiedades.Fila::nombre).toList()),
                () -> assertNotNull(c.parciales()),
                () -> assertEquals(2, c.parciales().hessiana().size()),
                () -> assertTrue(c.secciones().get(3).aviso().startsWith("Es simétrica")));
        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta());
        assertEquals("Gradiente de f(x, y)", v.subtitulo());
        assertEquals("Dominio: Todos los (x, y) del plano.", v.notas().getFirst());
    }

    @Test
    void elDominioVaEnAnalisis() {
        ContenidoParciales c = ContenidoParciales.de(parciales("∂/∂x(ln(x + y))"));
        assertEquals("Los (x, y) con x + y > 0", filas(c.secciones().getFirst()).get("Dominio"));
    }

    @Test
    void cadenaConSuArbolEnLosPasos() {
        ResolucionParcial r = parciales("d/dt(x^2 y); x = t^2, y = sen t");
        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals("Regla de la cadena", v.subtitulo());
        assertTrue(v.pasos().getFirst().trabajo() instanceof TrabajoIntermedio.ArbolCadena);
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta());
    }

    @Test
    void sugerenciasDeVariasVariables() {
        List<Sugerencia> s = FACTORIZACION.sugerencias(FACTORIZACION.factorizar("x^2 - 4y^2"));
        assertEquals(List.of("Derivadas parciales", "Gradiente", "Puntos críticos y extremos", "Integral doble"),
                s.stream().map(Sugerencia::titulo).toList());
        assertEquals(List.of(true, true, true, true), s.stream().map(Sugerencia::disponible).toList());
        assertTrue(s.stream().allMatch(x -> x.regla().equals("RN41")));
        assertEquals("∂/∂x(x^2 - 4y^2)", s.getFirst().plantilla());
        assertTrue(SERVICIO.decidir(s.get(1).plantilla()).resoluble(), "la plantilla del gradiente se resuelve");
    }

    @Test
    void conUnaLetraNoHaySugerenciasDeVariasVariables() {
        List<Sugerencia> s = FACTORIZACION.sugerencias(FACTORIZACION.factorizar("x^2 - 5x + 6"));
        assertFalse(s.isEmpty());
        assertTrue(s.stream().noneMatch(x -> x.regla().equals("RN41")));
    }
}
