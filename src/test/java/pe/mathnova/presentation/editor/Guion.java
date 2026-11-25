package pe.mathnova.presentation.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.DoubleUnaryOperator;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple;
import pe.mathnova.domain.calculo.parciales.ResolucionParcial;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.grafica.DatosCurvasNivel;

final class Guion {

    static final Path FIXTURES = Path.of("src", "test", "resources", "latex");

    record Salida(Object resolucion, String pantalla, List<DatosCurvasNivel> curvas) {
    }

    @FunctionalInterface
    interface Chequeo {
        void comprobar(Salida s);
    }

    record Caso(String id, String nombre, String campo, List<String> pasos, String texto, String latexCon,
                String aviso, Chequeo chequeo) {

        @Override
        public String toString() {
            return nombre + (campo.equals("Q") ? "" : " en " + campo);
        }

        Caso en(String c) {
            return new Caso(id + "-" + c.toLowerCase(), nombre, c, pasos, texto, latexCon, aviso, chequeo);
        }

        Caso conLatex(String fragmento) {
            return new Caso(id, nombre, campo, pasos, texto, fragmento, aviso, chequeo);
        }

        String latex() {
            Path p = FIXTURES.resolve(id + ".tex");
            if (Files.exists(p)) {
                try {
                    return Files.readString(p, StandardCharsets.UTF_8).strip();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            try (InputStream in = Guion.class.getResourceAsStream("/latex/" + id + ".tex")) {
                return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        void guardar(String latex) {
            try {
                Files.createDirectories(FIXTURES);
                Files.writeString(FIXTURES.resolve(id + ".tex"), latex + "\n", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static Caso caso(String id, String nombre, String texto, Chequeo chequeo, String... pasos) {
        return new Caso(id, nombre, "Q", List.of(pasos), texto, null, null, chequeo);
    }

    static Caso error(String id, String nombre, String texto, String aviso, String... pasos) {
        return new Caso(id, nombre, "Q", List.of(pasos), texto, null, aviso, null);
    }

    static List<Caso> casos() {
        Caso x4mas1 = caso("x4-mas-1", "x^4 + 1", "x^4 + 1", null, "x^4+1");
        Caso dosVariables = caso("dos-variables", "x^2 + 3xy + 2y^2 + 4x + 5y + 3", "x^2 + 3xy + 2y^2 + 4x + 5y + 3",
                factores("(x + y + 1)(x + 2y + 3)"), "x^2+3xy+2y^2+4x+5y+3");
        return List.of(
                caso("integral-por-3", "3∫₃⁶ 3x dx", "3·∫(3x, x, 3, 6)", definida(Racional.de(243, 2)),
                        "3", "[∫]", "3", "[→]", "6", "[→]", "3x"),
                error("letra-enie", "3ñ + 1", "3ñ + 1", "'ñ' en la posición 2", "3ñ+1"),
                caso("cubica-1", "x^3 - 2x^2 - x + 2", "x^3 - 2x^2 - x + 2", factores("(x + 1)(x - 1)(x - 2)"),
                        "x^3-2x^2-x+2"),
                caso("cubica-2", "2x^3 - 3x^2 - 3x + 2", "2x^3 - 3x^2 - 3x + 2", factores("(x + 1)(2x - 1)(x - 2)"),
                        "2x^3-3x^2-3x+2"),
                caso("cuartica", "x^4 + 5x^3 + 11x^2 + 14x + 4", "x^4 + 5x^3 + 11x^2 + 14x + 4",
                        factores("(x^2 + 3x + 1)(x^2 + 2x + 4)"), "x^4+5x^3+11x^2+14x+4"),
                caso("sophie-germain", "x^4 + 4", "x^4 + 4", factores("(x^2 - 2x + 2)(x^2 + 2x + 2)"), "x^4+4"),
                new Caso(x4mas1.id(), x4mas1.nombre(), "Q", x4mas1.pasos(), x4mas1.texto(), null, null, irreducible()),
                new Caso(x4mas1.id(), x4mas1.nombre(), "Q", x4mas1.pasos(), x4mas1.texto(), null, null,
                        factores("(x^2 - √2·x + 1)(x^2 + √2·x + 1)")).en("R"),
                new Caso(x4mas1.id(), x4mas1.nombre(), "Q", x4mas1.pasos(), x4mas1.texto(), null, null,
                        linealesComplejos(4)).en("C"),
                caso("ecuacion", "x^2 - 5x + 6 = 0", "x^2 - 5x + 6 = 0", soluciones(2, 3), "x^2-5x+6=0"),
                caso("exponencial-teclas", "e^(2x) - 3e^x + 2 con la tecla e^□", "e^(2x) - 3e^x + 2",
                        funciones("(e^x - 1)(e^x - 2)"), "[e^□]", "2x", "[→]", "-3", "[e^□]", "x", "[→]", "+2"),
                caso("exponencial-a-mano", "e^(2x) - 3e^x + 2 escrito a mano", "e^(2x) - 3e^x + 2",
                        funciones("(e^x - 1)(e^x - 2)"), "e^(2x)-3e^x+2"),
                caso("logaritmo", "ln^2 x - 4", "ln^2(x) - 4",
                        funciones("(ln(x) - 2)(ln(x) + 2)").y(contiene("x > 0")), "ln^2 x-4"),
                caso("limite-1", "lim x→2 (x² − 4)/(x − 2)", "lim(x→2, (x^2 - 4)/(x - 2))", limite(Racional.de(4)),
                        "[lím]", "2", "[→]", "[□/□]", "x^2-4", "[→]", "x-2"),
                caso("limite-2", "lim x→0 (eˣ − 1 − x)/x²", "lim(x→0, (e^x - 1 - x)/x^2)", limite(Racional.de(1, 2)),
                        "[lím]", "0", "[→]", "[□/□]", "[e^□]", "x", "[→]", "-1-x", "[→]", "x^2"),
                caso("limite-3", "lim x→2 1/(x − 2)", "lim(x→2, 1/(x - 2))", limiteLaterales(),
                        "[lím]", "2", "[→]", "[□/□]", "1", "[→]", "x-2"),
                caso("derivada", "d/dx(x² sen x)", "d/dx(x^2·sen(x))",
                        derivada(x -> 2 * x * Math.sin(x) + x * x * Math.cos(x)),
                        "[d/dx]", "x", "[□²]", "[sen]", "x"),
                caso("integral-racional", "∫ (3x + 5)/((x − 1)(x + 2)) dx", "∫((3x + 5)/((x - 1)(x + 2)), x)",
                        primitiva(x -> 8.0 / 3 * Math.log(Math.abs(x - 1)) + 1.0 / 3 * Math.log(Math.abs(x + 2)), 2, 5),
                        "[∫]", "[→]", "[→]", "[□/□]", "3x+5", "[→]", "(x-1)(x+2)"),
                caso("integral-partes", "∫ eˣ sen x dx", "∫(e^x·sen(x), x)",
                        primitiva(x -> Math.exp(x) / 2 * (Math.sin(x) - Math.cos(x)), -1, 2),
                        "[∫]", "[→]", "[→]", "[e^□]", "x", "[→]", "[sen]", "x"),
                caso("integral-raiz-tecla", "∫ √(1 − x²) dx con la tecla √", "∫(√(1 - x^2), x)", raizDeUnoMenosX2(),
                        "[∫]", "[→]", "[→]", "[√□]", "1-x^2"),
                caso("integral-raiz-mano", "∫ √(1 − x²) dx con √ escrito a mano", "∫(√(1 - x^2), x)", raizDeUnoMenosX2(),
                        "[∫]", "[→]", "[→]", "√1-x^2"),
                caso("integral-raiz-parentesis", "∫ √((1 − x²)) dx con paréntesis dentro de la raíz",
                        "∫(√(1 - x^2), x)", raizDeUnoMenosX2(), "[∫]", "[→]", "[→]", "[√□]", "(1-x^2)"),
                caso("integral-impar", "∫₋₁¹ (x³ − x) dx", "∫(x^3 - x, x, -1, 1)", definida(Racional.CERO),
                        "[∫]", "-1", "[→]", "1", "[→]", "x^3-x"),
                dosVariables,
                dosVariables.en("R"),
                dosVariables.en("C"),
                caso("limite-doble", "lim((x,y)→(0,0), x²y/(x⁴ + y²))", "lim((x, y)→(0, 0), x^2y/(x^4 + y^2))",
                        limiteDobleNoExiste(),
                        "[lím(x,y)]", "0", "[→]", "0", "[→]", "[→]", "[□/□]", "x^2y", "[→]", "x^4+y^2"),
                caso("extremos-tecla", "extremos(x³ + y³ − 3xy) con la tecla extr", "extremos(x^3 + y^3 - 3xy)",
                        extremos(Map.of(List.of(0, 0), ResolucionExtremos.Clase.SILLA,
                                List.of(1, 1), ResolucionExtremos.Clase.MINIMO)),
                        "[extr]", "x^3+y^3-3xy").conLatex("\\operatorname{extremos}"),
                caso("extremos-a-mano", "extremos(x³ + y³ − 3xy) escrito a mano", "extremos(x^3 + y^3 - 3xy)",
                        extremos(Map.of(List.of(0, 0), ResolucionExtremos.Clase.SILLA,
                                List.of(1, 1), ResolucionExtremos.Clase.MINIMO)),
                        "extremos(x^3+y^3-3xy)").conLatex("\\operatorname{extremos}"),
                caso("lagrange", "extremos(xy; x + y = 10)", "extremos(xy; x + y = 10)",
                        extremos(Map.of(List.of(5, 5), ResolucionExtremos.Clase.MAXIMO_RESTRINGIDO)).y(contiene("25")),
                        "[extr]", "xy;x+y=10"),
                caso("gradiente-a-mano", "grad(x²y) escrito a mano", "grad(x^2y)", gradiente(), "grad(x^2y)")
                        .conLatex("\\operatorname{grad}"),
                caso("integral-doble", "∬(e^(x²), x, y, 1, y, 0, 1) con la tecla ∬", "∬(e^(x^2), x, y, 1, y, 0, 1)",
                        multiple((Math.E - 1) / 2),
                        "[∬]", "0", "[→]", "1", "[→]", "y", "[→]", "1", "[→]", "[e^□]", "x", "[□²]"),
                new Caso("abel-ruffini", "x^5 - x - 1", "R", List.of("x^5-x-1"), "x^5 - x - 1", null, null,
                        contiene("1.1673").y(contiene("Abel–Ruffini"))),
                new Caso("casus-irreducibilis", "x^3 - 3x + 1", "R", List.of("x^3-3x+1"), "x^3 - 3x + 1", null, null,
                        tresRaicesReales()),
                caso("no-polinomica", "x + sen x", "x + sen(x)", noFactorizable("no es polinómica en una sola función (RN32)"),
                        "x+", "[sen]", "x"));
    }

    interface Combinable extends Chequeo {
        default Chequeo y(Chequeo otro) {
            return s -> {
                comprobar(s);
                otro.comprobar(s);
            };
        }
    }

    private static Combinable c(Chequeo ch) {
        return ch::comprobar;
    }

    static Combinable contiene(String fragmento) {
        return c(s -> assertTrue(s.pantalla().contains(fragmento),
                () -> "en la pantalla falta \"" + fragmento + "\": " + s.pantalla()));
    }

    static Combinable factores(String esperado) {
        return c(s -> {
            Resolucion res = assertInstanceOf(Resolucion.class, s.resolucion());
            assertEquals(mismosFactores(esperado), mismosFactores(res.texto()), "dio " + res.texto());
        });
    }

    static Combinable irreducible() {
        return c(s -> {
            Resolucion res = assertInstanceOf(Resolucion.class, s.resolucion());
            assertEquals(1, res.resultado().factores().size(), res.texto());
            assertTrue(s.pantalla().contains("no se puede factorizar en Q"), s.pantalla());
        });
    }

    static Combinable linealesComplejos(int n) {
        return c(s -> {
            Resolucion res = assertInstanceOf(Resolucion.class, s.resolucion());
            Set<String> f = mismosFactores(res.texto());
            assertEquals(n, f.size(), res.texto());
            f.forEach(x -> assertTrue(x.contains("i"), "sin i: " + x));
        });
    }

    static Combinable tresRaicesReales() {
        return c(s -> {
            Resolucion res = assertInstanceOf(Resolucion.class, s.resolucion());
            assertEquals(3, mismosFactores(res.texto()).size(), res.texto());
            assertFalse(res.texto().matches(".*(^|[^a-z])i([^a-z]|$).*"), "tiene i: " + res.texto());
        });
    }

    static Combinable funciones(String esperado) {
        return c(s -> {
            ResolucionFunciones res = assertInstanceOf(ResolucionFunciones.class, s.resolucion());
            assertTrue(res.factorizable(), res.mensaje());
            assertEquals(mismosFactores(esperado), mismosFactores(res.formula()), "dio " + res.formula());
        });
    }

    static Combinable noFactorizable(String motivo) {
        return c(s -> {
            ResolucionFunciones res = assertInstanceOf(ResolucionFunciones.class, s.resolucion());
            assertFalse(res.factorizable());
            assertTrue(s.pantalla().contains(motivo), () -> "falta \"" + motivo + "\": " + s.pantalla());
        });
    }

    static Combinable soluciones(int... valores) {
        return c(s -> {
            ResolucionEcuacion res = assertInstanceOf(ResolucionEcuacion.class, s.resolucion());
            Set<Racional> esperadas = new java.util.HashSet<>();
            for (int v : valores) {
                esperadas.add(Racional.de(v));
            }
            Set<Racional> obtenidas = new java.util.HashSet<>();
            res.soluciones().forEach(x -> obtenidas.add(x.valor()));
            assertEquals(esperadas, obtenidas);
        });
    }

    static Combinable limite(Racional esperado) {
        return c(s -> {
            ResolucionLimite res = assertInstanceOf(ResolucionLimite.class, s.resolucion());
            ResultadoLimite.Finito f = assertInstanceOf(ResultadoLimite.Finito.class, res.resultado());
            assertTrue(f.valor().esRacional(), f.texto());
            assertEquals(esperado, f.valor().aRacional());
        });
    }

    static Combinable limiteLaterales() {
        return c(s -> {
            ResolucionLimite res = assertInstanceOf(ResolucionLimite.class, s.resolucion());
            ResultadoLimite.NoExiste n = assertInstanceOf(ResultadoLimite.NoExiste.class, res.resultado());
            assertTrue(n.motivo().contains("−∞") && n.motivo().contains("+∞"), n.motivo());
        });
    }

    static Combinable derivada(DoubleUnaryOperator esperada) {
        return c(s -> {
            ResolucionDerivada res = assertInstanceOf(ResolucionDerivada.class, s.resolucion());
            for (double x : new double[] {-1.3, 0.4, 2.2}) {
                assertEquals(esperada.applyAsDouble(x), valor(res.resultado(), x), 1e-9, "en x = " + x + ": " + res.texto());
            }
        });
    }

    static Combinable raizDeUnoMenosX2() {
        return primitiva(x -> 0.5 * (Math.asin(x) + x * Math.sqrt(1 - x * x)), -0.5, 0.7);
    }

    static Combinable primitiva(DoubleUnaryOperator esperada, double a, double b) {
        return c(s -> {
            ResolucionIntegral res = assertInstanceOf(ResolucionIntegral.class, s.resolucion());
            assertTrue(res.resuelta(), () -> "no se resolvió: " + res.motivo());
            assertFalse(res.definida());
            double[] puntos = {a, (2 * a + b) / 3, (a + 2 * b) / 3, b};
            for (int i = 1; i < puntos.length; i++) {
                double esperadoDif = esperada.applyAsDouble(puntos[i]) - esperada.applyAsDouble(puntos[0]);
                double obtenidoDif = valor(res.antiderivada(), puntos[i]) - valor(res.antiderivada(), puntos[0]);
                assertEquals(esperadoDif, obtenidoDif, 1e-9, res.texto());
            }
        });
    }

    static Combinable definida(Racional esperado) {
        return c(s -> {
            ResolucionIntegral res = assertInstanceOf(ResolucionIntegral.class, s.resolucion());
            assertTrue(res.resuelta(), () -> "no se resolvió: " + res.motivo());
            assertTrue(res.definida());
            assertEquals(esperado, res.valorRacional(), res.texto());
        });
    }

    static Combinable limiteDobleNoExiste() {
        return c(s -> {
            ResolucionLimiteDoble res = assertInstanceOf(ResolucionLimiteDoble.class, s.resolucion());
            assertEquals(ResolucionLimiteDoble.Estado.NO_EXISTE, res.estado(), res.texto());
            Map<String, String> valores = new java.util.LinkedHashMap<>();
            res.trayectorias().forEach(t -> valores.put(t.nombre(), t.texto()));
            assertEquals("0", valores.get("y = mx"), valores::toString);
            assertEquals("1/2", valores.get("y = x²"), valores::toString);
            assertTrue(s.pantalla().contains("y = mx"), s.pantalla());
            assertTrue(s.pantalla().contains("y = x²") || s.pantalla().contains("y = x^{2}"), s.pantalla());
            assertTrue(s.pantalla().contains("el límite no existe (RN38)"), s.pantalla());
            assertEquals(1, s.curvas().size(), "no se dibujaron las curvas de nivel");
            long trayectorias = s.curvas().getFirst().curvas().stream()
                    .filter(k -> k.estilo() == DatosCurvasNivel.Estilo.TRAYECTORIA).count();
            assertTrue(trayectorias >= 2, "trayectorias dibujadas: " + trayectorias);
        });
    }

    static Combinable extremos(Map<List<Integer>, ResolucionExtremos.Clase> esperados) {
        return c(s -> {
            ResolucionExtremos res = assertInstanceOf(ResolucionExtremos.class, s.resolucion());
            Map<List<Integer>, ResolucionExtremos.Clase> obtenidos = new java.util.HashMap<>();
            for (ResolucionExtremos.Punto p : res.puntos()) {
                List<Integer> coords = new ArrayList<>();
                p.coordenadas().forEach(q -> coords.add(q.esEntero() ? q.num().intValueExact() : Integer.MIN_VALUE));
                obtenidos.put(coords, p.clase());
            }
            assertEquals(esperados, obtenidos, res.texto());
        });
    }

    static Combinable gradiente() {
        return c(s -> assertInstanceOf(ResolucionParcial.class, s.resolucion()));
    }

    static Combinable multiple(double esperado) {
        return c(s -> {
            ResolucionIntegralMultiple res = assertInstanceOf(ResolucionIntegralMultiple.class, s.resolucion());
            assertTrue(res.resuelta(), res.texto());
            assertEquals(esperado, valor(res.valor(), 0), 1e-12, res.texto());
        });
    }

    private static final MathContext MC = new MathContext(30);

    private static double valor(Expresion e, double x) {
        ComplejoDecimal z = EvaluacionNumerica.evaluar(e, Map.of('x', ComplejoDecimal.real(new java.math.BigDecimal(x))), MC);
        return z.re().doubleValue();
    }

    static Set<String> mismosFactores(String formula) {
        String s = formula.replace("·", "").replace("−", "-");
        Set<String> f = new TreeSet<>();
        int nivel = 0;
        int desde = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') {
                if (nivel == 0) {
                    desde = i;
                }
                nivel++;
            } else if (ch == ')') {
                nivel--;
                if (nivel == 0) {
                    String factor = s.substring(desde, i + 1);
                    if (desde == 0 || !Character.isLetter(s.charAt(desde - 1))) {
                        f.add(Parser.parse(factor).toString());
                    }
                }
            }
        }
        return f;
    }

    private Guion() {
    }
}
