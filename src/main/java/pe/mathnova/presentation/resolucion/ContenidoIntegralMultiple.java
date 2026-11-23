package pe.mathnova.presentation.resolucion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.multiples.MotorIntegralesMultiples;
import pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.Rango;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.grafica.CurvaExpresion;
import pe.mathnova.presentation.grafica.DatosCurvasNivel;
import pe.mathnova.presentation.grafica.DatosGrafica;
import pe.mathnova.presentation.grafica.VentanaGrafica;

/**
 * Una integral doble o triple (Sprint 8, HU31).
 * <ul>
 *   <li>Gráfica: la región de integración en el plano, sombreada, con las curvas que la limitan rotuladas (en polares,
 *       la región en el plano xy; en una triple, la región de las dos variables de afuera).</li>
 *   <li>Análisis: el valor exacto, su decimal y la comprobación con Simpson en 2D o 3D.</li>
 *   <li>Exploración: la iterada, el cambio de orden y el cambio de coordenadas, con "Probar".</li>
 * </ul>
 */
public record ContenidoIntegralMultiple(String latexSolucion, List<SeccionAnalisis> secciones, DatosCurvasNivel curvas,
                                        VistaExploracion exploracion) implements ContenidoPestanas {

    /** Subintervalos por variable de la regla de Simpson. */
    static final int SIMPSON_DOBLE = 40;
    static final int SIMPSON_TRIPLE = 16;

    public ContenidoIntegralMultiple {
        secciones = List.copyOf(secciones);
    }

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    public static ContenidoIntegralMultiple de(ResolucionIntegralMultiple r, List<CaminoCalculo> caminos,
                                               boolean codigos) {
        return new ContenidoIntegralMultiple(r.latex(), secciones(r), curvas(r), exploracion(caminos, codigos));
    }

    @Override
    public Optional<DatosGrafica> grafica() {
        return Optional.empty();
    }

    @Override
    public Optional<DatosCurvasNivel> curvasNivel() {
        return Optional.ofNullable(curvas);
    }

    @Override
    public Optional<String> comparacion() {
        return Optional.empty();
    }

    // ------------------------------------------------------------------ Análisis

    private static String decimal(BigDecimal d) {
        return FormateadorTexto.pizarraConNegativos(d.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
                .toPlainString());
    }

    static List<SeccionAnalisis> secciones(ResolucionIntegralMultiple r) {
        List<VistaPropiedades.Fila> filas = new ArrayList<>();
        if (!r.resuelta()) {
            filas.add(new VistaPropiedades.Fila("Resultado", r.motivo()));
            return List.of(new SeccionAnalisis("Valor", filas));
        }
        filas.add(new VistaPropiedades.Fila("Valor exacto", p(r.valor())));
        Optional<BigDecimal> d = MotorIntegralesMultiples.decimal(r);
        d.ifPresent(x -> filas.add(new VistaPropiedades.Fila("Decimal", "≈ " + decimal(x))));
        int n = r.efectiva().esDoble() ? SIMPSON_DOBLE : SIMPSON_TRIPLE;
        Optional<BigDecimal> s = MotorIntegralesMultiples.simpson(r, n);
        String aviso = null;
        if (s.isPresent()) {
            filas.add(new VistaPropiedades.Fila("Simpson " + (r.efectiva().esDoble() ? "2D" : "3D") + " (n = " + n + ")",
                    "≈ " + decimal(s.get())));
            if (d.isPresent()) {
                BigDecimal dif = s.get().subtract(d.get()).abs();
                filas.add(new VistaPropiedades.Fila("Diferencia", decimal(dif)));
                boolean coincide = dif.compareTo(new BigDecimal("1e-4").multiply(d.get().abs().max(BigDecimal.ONE))) < 0;
                aviso = coincide ? "La comprobación numérica coincide con el valor exacto."
                        : "La comprobación numérica no coincide: revisa el resultado.";
            }
        }
        if (r.jacobiano() != null) {
            filas.add(new VistaPropiedades.Fila("Jacobiano", p(r.jacobiano())));
        }
        return List.of(new SeccionAnalisis("Valor", filas, List.of(), List.of(), null, aviso));
    }

    // ------------------------------------------------------------------ Exploración

    static VistaExploracion exploracion(List<CaminoCalculo> caminos, boolean codigos) {
        List<VistaExploracion.Tarjeta> t = new ArrayList<>();
        for (CaminoCalculo c : caminos) {
            String tecnica = c.codigo().equals(MotorIntegralesMultiples.ITERADA)
                    || c.codigo().equals(MotorIntegralesMultiples.ORDEN) ? "V09" : "V10";
            String nombre = codigos ? tecnica + " · " + c.nombre() : c.nombre();
            String texto = c.texto() == null ? null : FormateadorTexto.pizarraConNegativos(c.texto());
            t.add(switch (c.estado()) {
                case USADO -> new VistaExploracion.Tarjeta(c.codigo(), nombre, c.nombre(), VistaExploracion.Estado.USADO,
                        null, texto, VistaExploracion.Accion.RESOLVER_CON);
                case SIRVE -> new VistaExploracion.Tarjeta(c.codigo(), nombre, c.nombre(),
                        VistaExploracion.Estado.PROBAR, null, texto, VistaExploracion.Accion.RESOLVER_CON);
                case NO_APLICA -> new VistaExploracion.Tarjeta(c.codigo(), nombre, c.nombre(),
                        VistaExploracion.Estado.NO_APLICA, texto, null, VistaExploracion.Accion.RESOLVER_CON);
            });
        }
        return new VistaExploracion(t, true);
    }

    // ------------------------------------------------------------------ Gráfica

    private static double d(Expresion e, Map<Character, Double> v) {
        return CurvaExpresion.evaluar(e, v);
    }

    static DatosCurvasNivel curvas(ResolucionIntegralMultiple r) {
        IntegralMultiple in = r.efectiva();
        try {
            return switch (in.coordenadas()) {
                case CARTESIANAS -> cartesiana(in);
                case POLARES -> polar(in.rangos().get(0), in.rangos().get(1));
                case CILINDRICAS -> polar(in.rangos().get(1), in.rangos().get(2));
                case ESFERICAS -> esferica(in);
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static DatosCurvasNivel cartesiana(IntegralMultiple in) {
        int k = in.esDoble() ? 0 : 1;
        Rango adentro = in.rangos().get(k);
        Rango afuera = in.rangos().get(k + 1);
        char v1 = adentro.variable();
        char v2 = afuera.variable();
        char horizontal = v1 == 'x' ? 'x' : v2 == 'x' ? 'x' : v2;
        boolean interiorEsY = v1 != horizontal;
        double a = d(afuera.inferior(), Map.of());
        double b = d(afuera.superior(), Map.of());
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return null;
        }
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= 50; i++) {
            double t = a + (b - a) * i / 50;
            for (Expresion g : List.of(adentro.inferior(), adentro.superior())) {
                double y = d(g, Map.of(v2, t));
                if (Double.isFinite(y)) {
                    lo = Math.min(lo, y);
                    hi = Math.max(hi, y);
                }
            }
        }
        if (lo > hi) {
            return null;
        }
        DatosCurvasNivel.Forma forma = interiorEsY ? DatosCurvasNivel.Forma.Y_DE_X : DatosCurvasNivel.Forma.X_DE_Y;
        DatosCurvasNivel.Forma recta = interiorEsY ? DatosCurvasNivel.Forma.X_DE_Y : DatosCurvasNivel.Forma.Y_DE_X;
        List<DatosCurvasNivel.Curva> curvas = new ArrayList<>();
        curvas.add(new DatosCurvasNivel.Curva(forma, adentro.inferior(), a, b, v1 + " = " + p(adentro.inferior()),
                DatosCurvasNivel.Estilo.BORDE));
        curvas.add(new DatosCurvasNivel.Curva(forma, adentro.superior(), a, b, v1 + " = " + p(adentro.superior()),
                DatosCurvasNivel.Estilo.BORDE));
        for (Expresion borde : List.of(afuera.inferior(), afuera.superior())) {
            double t = d(borde, Map.of());
            double g1 = d(adentro.inferior(), Map.of(v2, t));
            double g2 = d(adentro.superior(), Map.of(v2, t));
            if (Math.abs(g1 - g2) > 1e-9) {
                curvas.add(new DatosCurvasNivel.Curva(recta, borde, Math.min(g1, g2), Math.max(g1, g2),
                        v2 + " = " + p(borde), DatosCurvasNivel.Estilo.BORDE));
            }
        }
        VentanaGrafica v = interiorEsY ? ventana(a, b, lo, hi) : ventana(lo, hi, a, b);
        char vertical = horizontal == v1 ? v2 : v1;
        return new DatosCurvasNivel(null, horizontal, vertical, v, curvas, List.of(), List.of(),
                new DatosCurvasNivel.RegionCartesiana(interiorEsY, a, b, adentro.inferior(), adentro.superior(), v2),
                null);
    }

    private static VentanaGrafica ventana(double x0, double x1, double y0, double y1) {
        double mx = Math.max(0.5, (x1 - x0) * 0.25);
        double my = Math.max(0.5, (y1 - y0) * 0.25);
        return new VentanaGrafica(Math.min(x0, 0) - mx, Math.max(x1, 0) + mx, Math.min(y0, 0) - my, Math.max(y1, 0) + my);
    }

    private static DatosCurvasNivel polar(Rango radio, Rango angulo) {
        double a = d(angulo.inferior(), Map.of());
        double b = d(angulo.superior(), Map.of());
        double rmax = 0;
        for (int i = 0; i <= 60; i++) {
            double t = a + (b - a) * i / 60;
            rmax = Math.max(rmax, Math.abs(d(radio.superior(), Map.of('θ', t))));
        }
        if (!Double.isFinite(rmax) || rmax == 0) {
            return null;
        }
        List<DatosCurvasNivel.Curva> curvas = new ArrayList<>();
        curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.POLAR, radio.superior(), a, b,
                "r = " + p(radio.superior()), DatosCurvasNivel.Estilo.BORDE));
        if (!(radio.inferior() instanceof Constante c && c.valor().esCero())) {
            curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.POLAR, radio.inferior(), a, b,
                    "r = " + p(radio.inferior()), DatosCurvasNivel.Estilo.BORDE));
        }
        if (b - a < 2 * Math.PI - 1e-9) {
            curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.RAYO, null, a, rmax,
                    "θ = " + p(angulo.inferior()), DatosCurvasNivel.Estilo.BORDE));
            curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.RAYO, null, b, rmax,
                    "θ = " + p(angulo.superior()), DatosCurvasNivel.Estilo.BORDE));
        }
        double m = rmax * 1.3;
        return new DatosCurvasNivel(null, 'x', 'y', new VentanaGrafica(-m, m, -m, m), curvas, List.of(), List.of(),
                new DatosCurvasNivel.RegionPolar(a, b, radio.inferior(), radio.superior()), null);
    }

    /** La proyección de la región esférica sobre el plano xy: un disco de radio ρ máximo. */
    private static DatosCurvasNivel esferica(IntegralMultiple in) {
        Rango rho = in.rangos().get(0);
        Rango theta = in.rangos().get(2);
        Expresion rmax = rho.superior();
        return polar(new Rango('r', rho.inferior(), rmax), theta);
    }
}
