package pe.mathnova.presentation.resolucion;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.integrales.MotorIntegrales;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.grafica.CurvaExpresion;
import pe.mathnova.presentation.grafica.DatosGrafica;
import pe.mathnova.presentation.grafica.FuncionReal;

public record ContenidoIntegral(String latexSolucion, Optional<DatosGrafica> grafica, List<SeccionAnalisis> secciones,
                                VistaExploracion exploracion) implements ContenidoPestanas {

    public static final int SIMPSON = 100;

    public ContenidoIntegral {
        secciones = List.copyOf(secciones);
    }

    public static ContenidoIntegral de(ResolucionIntegral r, List<CaminoCalculo> caminos, boolean codigos) {
        return new ContenidoIntegral(r.latex(), grafica(r), secciones(r),
                VistaExploracion.deCalculo(caminos, MotorIntegrales::nombre, codigos));
    }

    @Override
    public Optional<String> comparacion() {
        return Optional.empty();
    }

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    static Optional<DatosGrafica> grafica(ResolucionIntegral r) {
        char x = r.variable();
        Expresion f = r.integral().cuerpo();
        var letras = Expresiones.variables(f);
        if (letras.size() > 1 || !letras.isEmpty() && !letras.contains(x)) {
            return Optional.empty();
        }
        try {
            DatosGrafica.Calculo calculo;
            if (r.definida()) {
                MathContext mc = new MathContext(20);
                double a = EvaluacionNumerica.evaluar(r.integral().inferior(), Map.of(), mc).re().doubleValue();
                double b = EvaluacionNumerica.evaluar(r.integral().superior(), Map.of(), mc).re().doubleValue();
                String valor = r.resuelta() ? etiquetaValor(r) : "";
                calculo = new DatosGrafica.Calculo("f(" + x + ")", null, null, null, List.of(),
                        new DatosGrafica.Area(a, b, valor));
            } else if (r.resuelta()) {
                Expresion sinK = r.antiderivada();
                calculo = new DatosGrafica.Calculo("f(" + x + ")", new FuncionReal(new CurvaExpresion(sinK, x)),
                        "F(" + x + ") con C = 0", null, List.of(), null);
            } else {
                calculo = null;
            }
            return Optional.of(DatosGrafica.deCalculo(x, f, calculo));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String etiquetaValor(ResolucionIntegral r) {
        String k = r.coeficiente().equals(pe.mathnova.domain.algebra.Racional.UNO) ? ""
                : " (por " + FormateadorTexto.numero(r.coeficiente()) + ")";
        if (r.valorRacional() != null) {
            return "∫ = " + FormateadorTexto.numero(r.valorRacional()) + k;
        }
        return r.aproximado() == null ? "" : "∫ ≈ " + decimal(r.aproximado(), 4) + k;
    }

    private static String decimal(BigDecimal d, int cifras) {
        String s = d.setScale(cifras, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return s.startsWith("-") ? FormateadorTexto.MENOS + s.substring(1) : s;
    }

    static List<SeccionAnalisis> secciones(ResolucionIntegral r) {
        if (!r.resuelta()) {
            return List.of(new SeccionAnalisis("Sin resolver", List.of(), List.of(), List.of(), null, r.motivo()));
        }
        List<SeccionAnalisis> s = new ArrayList<>();
        ResolucionIntegral.Verificacion v = r.verificacion();
        String sinK = p(r.antiderivada());
        List<VistaPropiedades.Fila> filas = new ArrayList<>();
        filas.add(new VistaPropiedades.Fila("Resultado", r.definida() ? "F(" + r.variable() + ") = " + sinK
                : sinK + " + C"));
        filas.add(new VistaPropiedades.Fila("Derivada del resultado", v == null ? "—" : p(v.derivada())));
        filas.add(new VistaPropiedades.Fila("Integrando", p(r.integral().cuerpo())
                + (r.coeficiente().equals(pe.mathnova.domain.algebra.Racional.UNO) ? ""
                : " (por " + FormateadorTexto.numero(r.coeficiente()) + ")")));
        String coincide = v == null ? "no se pudo derivar" : !v.coincide() ? "no" : v.exacta()
                ? "sí, exacto" : "sí, numérico en cinco puntos";
        filas.add(new VistaPropiedades.Fila("¿Coincide?", coincide));
        s.add(new SeccionAnalisis("Verificación: (F)' = integrando", filas, List.of(), List.of(), null, null));
        if (r.definida()) {
            Optional<BigDecimal> simpson = MotorIntegrales.simpson(r, SIMPSON);
            List<VistaPropiedades.Fila> f2 = new ArrayList<>();
            String exacto = r.texto() + (r.aproximado() == null ? "" : " ≈ " + decimal(r.aproximado(), 10));
            f2.add(new VistaPropiedades.Fila("Valor exacto (Barrow)", exacto));
            if (simpson.isPresent()) {
                f2.add(new VistaPropiedades.Fila("Simpson (n = " + SIMPSON + ")", decimal(simpson.get(), 10)));
                if (r.aproximado() != null) {
                    BigDecimal dif = simpson.get().subtract(r.aproximado()).abs();
                    f2.add(new VistaPropiedades.Fila("Diferencia", dif.compareTo(BigDecimal.ONE.movePointLeft(12)) < 0
                            ? "menor que 10⁻¹²" : dif.round(new MathContext(3)).toString()));
                }
            }
            s.add(new SeccionAnalisis("Aproximación numérica", f2, List.of(), List.of(), null,
                    simpson.isPresent() ? "La regla de Simpson es una aproximación; el valor exacto es el de Barrow."
                            : "No se pudo aproximar: el integrando no se evalúa en todo el intervalo."));
        }
        return s;
    }
}
