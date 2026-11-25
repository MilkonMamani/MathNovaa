package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble;
import pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.domain.sugerencias.Sugerencia;
import pe.mathnova.presentation.grafica.DatosCurvasNivel;
import pe.mathnova.presentation.grafica.MarchingSquares;
import pe.mathnova.presentation.grafica.VentanaGrafica;

class PestanasSprint8Test {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService SERVICIO = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static Map<String, String> filas(SeccionAnalisis s) {
        return s.filas().stream().collect(Collectors.toMap(VistaPropiedades.Fila::nombre, VistaPropiedades.Fila::valor,
                (a, b) -> a, java.util.LinkedHashMap::new));
    }

    @Test
    void lasCurvasDeNivelDeUnaParaboloideSonCirculos() {
        VentanaGrafica v = new VentanaGrafica(-2, 2, -2, 2);
        double[][] m = MarchingSquares.malla(Parser.parse("x^2 + y^2"), 'x', 'y', v);
        assertEquals(MarchingSquares.LADO + 1, m.length);
        List<double[]> s = MarchingSquares.segmentos(m, 1, v);
        assertFalse(s.isEmpty());
        for (double[] seg : s) {
            assertEquals(1, Math.hypot(seg[0], seg[1]), 0.02);
            assertEquals(1, Math.hypot(seg[2], seg[3]), 0.02);
        }
        double[] rango = MarchingSquares.rango(m);
        assertEquals(0, rango[0], 1e-9);
        assertEquals(8, rango[1], 1e-9);
        double[] c = MarchingSquares.niveles(rango[0], rango[1], 9);
        assertEquals(9, c.length);
        assertEquals(0.8, c[0], 1e-9);
        assertEquals(7.2, c[8], 1e-9);
    }

    @Test
    void lasCeldasFueraDelDominioSeSaltan() {
        VentanaGrafica v = new VentanaGrafica(-1, 1, -1, 1);
        double[][] m = MarchingSquares.malla(Parser.parse("ln(x)"), 'x', 'y', v);
        for (double[] seg : MarchingSquares.segmentos(m, -0.5, v)) {
            assertTrue(seg[0] > 0 && seg[2] > 0);
        }
    }

    @Test
    void limiteDobleConSusTrayectorias() {
        ResolucionLimiteDoble r = SERVICIO.resolverLimiteDoble("lim((x,y)→(0,0), x^2 y/(x^4 + y^2))", f -> { });
        ContenidoLimiteDoble c = ContenidoLimiteDoble.de(r);
        DatosCurvasNivel d = c.curvasNivel().orElseThrow();
        assertAll(
                () -> assertTrue(c.grafica().isEmpty()),
                () -> assertNotNull(d.funcion()),
                () -> assertTrue(d.curvas().stream().anyMatch(k -> k.rotulo().equals("y = x²: 1/2")),
                        d.curvas().stream().map(DatosCurvasNivel.Curva::rotulo).toList().toString()),
                () -> assertTrue(d.curvas().stream().anyMatch(k -> k.rotulo().equals("y = x: 0"))),
                () -> assertTrue(d.curvas().stream().allMatch(k -> k.estilo() == DatosCurvasNivel.Estilo.TRAYECTORIA)),
                () -> assertEquals("Trayectorias", c.secciones().getFirst().titulo()),
                () -> assertEquals("1/2", filas(c.secciones().getFirst()).get("y = x²")));
        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals(VistaResolucion.Etiqueta.NO_EXISTE, v.etiqueta());
        assertTrue(v.subtitulo().contains("Límite por trayectorias"), v.subtitulo());
    }

    @Test
    void laRectaConPendienteSeDibujaConSuValor() {
        ContenidoLimiteDoble c = ContenidoLimiteDoble.de(SERVICIO.resolverLimiteDoble(
                "lim((x,y)→(0,0), xy/(x^2 + y^2))", f -> { }));
        List<String> rotulos = c.curvasNivel().orElseThrow().curvas().stream().map(DatosCurvasNivel.Curva::rotulo)
                .toList();
        assertTrue(rotulos.contains("y = x: 1/2"), rotulos.toString());
        assertTrue(rotulos.contains("y = −x: −1/2"), rotulos.toString());
        assertEquals("m/(1 + m²) (depende de m)", filas(c.secciones().getFirst()).get("y = mx"));
    }

    @Test
    void laFormaPolarVaEnAnalisis() {
        ResolucionLimiteDoble r = SERVICIO.resolverLimiteDoble("lim((x,y)→(0,0), x^2 y/(x^2 + y^2))", f -> { });
        ContenidoLimiteDoble c = ContenidoLimiteDoble.de(r);
        SeccionAnalisis polar = c.secciones().stream().filter(s -> s.titulo().equals("Forma polar")).findFirst()
                .orElseThrow();
        assertEquals("r·cos²θ·sen θ", filas(polar).get("f en polares"));
        assertTrue(filas(polar).get("Cota").contains("≤ r"));
        assertEquals(VistaResolucion.Etiqueta.CALCULADO, VistaResolucion.de(r, false).etiqueta());
    }

    @Test
    void extremosConSusPuntosMarcados() {
        ResolucionExtremos r = SERVICIO.resolverExtremos("extremos(x^3 + y^3 - 3xy)", f -> { });
        ContenidoExtremos c = ContenidoExtremos.de(r);
        DatosCurvasNivel d = c.curvasNivel().orElseThrow();
        assertAll(
                () -> assertEquals(2, d.marcas().size()),
                () -> assertEquals(DatosCurvasNivel.TipoMarca.SILLA, d.marcas().get(0).tipo()),
                () -> assertEquals(DatosCurvasNivel.TipoMarca.MINIMO, d.marcas().get(1).tipo()),
                () -> assertTrue(d.curvas().isEmpty()),
                () -> assertEquals("D = −9,  fxx = 0:  punto de silla", filas(c.secciones().getFirst()).get("(0, 0)")),
                () -> assertEquals("D = 27,  fxx = 6:  mínimo local", filas(c.secciones().getFirst()).get("(1, 1)")),
                () -> assertEquals("Matriz hessiana", c.secciones().get(1).titulo()));
    }

    @Test
    void lagrangeDibujaLaRestriccionYLaCurvaDeNivelDelOptimo() {
        ResolucionExtremos r = SERVICIO.resolverExtremos("extremos(xy; x + y = 10)", f -> { });
        DatosCurvasNivel d = ContenidoExtremos.de(r).curvasNivel().orElseThrow();
        assertAll(
                () -> assertEquals(DatosCurvasNivel.TipoMarca.MAXIMO, d.marcas().getFirst().tipo()),
                () -> assertTrue(d.curvas().stream().anyMatch(k -> k.estilo() == DatosCurvasNivel.Estilo.RESTRICCION
                        && k.forma() == DatosCurvasNivel.Forma.IMPLICITA)),
                () -> assertTrue(d.curvas().stream().anyMatch(k -> k.rotulo().equals("f = 25"))),
                () -> assertTrue(d.ventana().contiene(5, 5)));
        assertEquals("λ = 5,  f = 25:  máximo",
                filas(ContenidoExtremos.de(r).secciones().getFirst()).get("(5, 5)"));
    }

    @Test
    void integralDobleConSuRegionYSimpson() {
        ResolucionIntegralMultiple r = SERVICIO.resolverIntegralMultiple("∬(x + y, y, 0, x, x, 0, 1)", null, f -> { });
        ContenidoIntegralMultiple c = ContenidoIntegralMultiple.de(r, SERVICIO.caminos(r), false);
        DatosCurvasNivel d = c.curvasNivel().orElseThrow();
        Map<String, String> valor = filas(c.secciones().getFirst());
        assertAll(
                () -> assertInstanceOf(DatosCurvasNivel.RegionCartesiana.class, d.region()),
                () -> assertTrue(((DatosCurvasNivel.RegionCartesiana) d.region()).interiorEsY()),
                () -> assertTrue(d.curvas().stream().anyMatch(k -> k.rotulo().equals("y = x"))),
                () -> assertTrue(d.curvas().stream().anyMatch(k -> k.rotulo().equals("x = 1"))),
                () -> assertEquals("1/2", valor.get("Valor exacto")),
                () -> assertEquals("≈ 0.5", valor.get("Decimal")),
                () -> assertEquals("≈ 0.5", valor.get("Simpson 2D (n = 40)")),
                () -> assertTrue(c.secciones().getFirst().aviso().contains("coincide")));
        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta());
        assertEquals("Integral doble · Integral iterada", v.subtitulo());
    }

    @Test
    void enPolaresLaRegionEsDelPlanoXY() {
        ResolucionIntegralMultiple r = SERVICIO.resolverIntegralMultiple(
                "∬(x^2 + y^2, r, 0, 1, θ, 0, 2π; coord=polares)", null, f -> { });
        ContenidoIntegralMultiple c = ContenidoIntegralMultiple.de(r, SERVICIO.caminos(r), false);
        DatosCurvasNivel d = c.curvasNivel().orElseThrow();
        assertInstanceOf(DatosCurvasNivel.RegionPolar.class, d.region());
        assertEquals('x', d.x());
        assertTrue(d.curvas().stream().anyMatch(k -> k.rotulo().equals("r = 1")));
        assertEquals("r", filas(c.secciones().getFirst()).get("Jacobiano"));
    }

    @Test
    void exploracionConCambioDeOrdenYCoordenadas() {
        ResolucionIntegralMultiple r = SERVICIO.resolverIntegralMultiple(
                "∬(x^2 + y^2, y, -sqrt(1 - x^2), sqrt(1 - x^2), x, -1, 1)", null, f -> { });
        VistaExploracion x = ContenidoIntegralMultiple.de(r, SERVICIO.caminos(r), true).exploracion();
        assertTrue(x.probables());
        VistaExploracion.Tarjeta polares = x.tarjetas().stream().filter(t -> t.codigo().equals("POLARES"))
                .findFirst().orElseThrow();
        assertAll(
                () -> assertEquals(VistaExploracion.Estado.USADO, x.tarjetas().getFirst().estado()),
                () -> assertEquals("V09 · Integral iterada", x.tarjetas().getFirst().nombre()),
                () -> assertEquals(VistaExploracion.Estado.PROBAR, polares.estado()),
                () -> assertEquals("V10 · Coordenadas polares", polares.nombre()),
                () -> assertTrue(polares.vistaPrevia().contains("π/2"), polares.vistaPrevia()));
        ResolucionIntegralMultiple p = SERVICIO.resolverIntegralMultiple(
                "∬(x^2 + y^2, y, -sqrt(1 - x^2), sqrt(1 - x^2), x, -1, 1)", "POLARES", f -> { });
        assertEquals("π/2", p.texto());
    }

    @Test
    void laReglaDeLaCadenaNoTieneCurvasYElGradienteLlevaSuFlecha() {
        var g = SERVICIO.resolverParciales("grad(x^2 + y^2, (1, 2))", f -> { });
        DatosCurvasNivel d = ContenidoParciales.de(g).curvasNivel().orElseThrow();
        DatosCurvasNivel.Flecha f = d.flechas().getFirst();
        assertAll(
                () -> assertEquals(1, f.x(), 1e-12),
                () -> assertEquals(2, f.y(), 1e-12),
                () -> assertEquals(2.0, f.dy() / f.dx(), 1e-9),
                () -> assertEquals("∇f(1, 2)", f.rotulo()));
        assertTrue(ContenidoParciales.de(SERVICIO.resolverParciales("∂/∂x(x^2 y)", f2 -> { })).curvasNivel()
                .orElseThrow().flechas().isEmpty());
        assertEquals(Optional.empty(), ContenidoParciales.de(SERVICIO.resolverParciales(
                "d/dt(x^2 y); x = t^2, y = sen t", f2 -> { })).curvasNivel());
    }

    @Test
    void lasSugerenciasDeDosVariablesQuedanActivas() {
        List<Sugerencia> s = FACTORIZACION.sugerencias(FACTORIZACION.factorizar("x^2 - y^2"));
        Sugerencia extremos = s.stream().filter(x -> x.titulo().equals("Puntos críticos y extremos")).findFirst()
                .orElseThrow();
        Sugerencia doble = s.stream().filter(x -> x.titulo().equals("Integral doble")).findFirst().orElseThrow();
        assertAll(
                () -> assertTrue(extremos.disponible()),
                () -> assertEquals("extremos(x^2 - y^2)", extremos.plantilla()),
                () -> assertTrue(SERVICIO.decidir(extremos.plantilla()).resoluble()),
                () -> assertTrue(doble.disponible()),
                () -> assertTrue(SERVICIO.decidir(doble.plantilla()).resoluble()));
    }

    @Test
    void resultadoEnVivo() {
        assertEquals(Optional.of("No existe"), SERVICIO.enVivo("lim((x,y)→(0,0), xy/(x^2 + y^2))"));
        assertEquals(Optional.of("= 3"), SERVICIO.enVivo("lim((x,y)→(1,2), x^2 + xy)"));
        assertEquals(Optional.of("(1, 0) mínimo local"), SERVICIO.enVivo("extremos(x^2 + y^2 - 2x)"));
        assertEquals(Optional.of("= 9"), SERVICIO.enVivo("∬(x y, x, 0, 2, y, 0, 3)"));
        assertTrue(SERVICIO.decidir("d/dt(x^2 y); x = t^2, y = sen t").resoluble());
    }
}
