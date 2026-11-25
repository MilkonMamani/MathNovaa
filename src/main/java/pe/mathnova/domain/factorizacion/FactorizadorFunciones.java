package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.parser.Parser;

public final class FactorizadorFunciones {

    static final String NO_POLINOMICA = "No se puede factorizar: no es polinómica en una sola función (RN32)";

    private final OrquestadorFactorizacion motor;

    public FactorizadorFunciones(OrquestadorFactorizacion motor) {
        this.motor = motor;
    }

    public static boolean tieneFunciones(Expresion e) {
        return SustitucionFunciones.tieneFunciones(e);
    }

    public ResolucionFunciones resolver(Expresion entrada) {
        List<String> restricciones = restricciones(entrada);
        List<String> identidades = new ArrayList<>();

        Expresion e = sumaAProducto(entrada, identidades);
        e = anguloDoble(e, identidades);

        Map<String, Character> letras = new LinkedHashMap<>();
        Polinomio p;
        try {
            p = Polinomio.desde(SustitucionFunciones.aLetras(e, letras));
        } catch (NoEsPolinomioException | ArithmeticException ex) {
            return ResolucionFunciones.noFactorizable(entrada, NO_POLINOMICA, restricciones);
        }
        Map<Character, Expresion> funciones = new HashMap<>();
        letras.forEach((clave, letra) -> funciones.put(letra, Parser.parse(clave)));

        Polinomio pitagorica = pitagoricaHaciaUnTermino(p, letras, funciones, identidades);
        if (pitagorica != null) {
            p = pitagorica;
        }

        Resolucion r = motor.resolver(p.aExpresion(), Campo.Q);
        FormaFactorizada forma = quitarSenoMasCoseno(r.resultado(), letras, identidades);
        boolean letraSola = p.variables().size() == 1;
        if (r.pasos().isEmpty() && identidades.isEmpty()) {
            String mensaje = letraSola && !p.variables().contains('x')
                    ? "No se puede factorizar: " + p + " es irreducible en Q (con " + definicion(p, funciones) + ")"
                    : NO_POLINOMICA;
            return ResolucionFunciones.noFactorizable(entrada, mensaje, restricciones);
        }
        Expresion resultado = SustitucionFunciones.deLetras(forma.aExpresion(), funciones);
        String codigo = codigo(r, p, funciones, identidades);
        var sustitucion = new TrabajoIntermedio.Sustitucion(nombres(p, funciones), definicion(p, funciones),
                p.toString(), forma.aTexto(), ImpresoraTexto.imprimir(resultado));
        return new ResolucionFunciones(entrada, true, null, codigo, r, funciones, resultado, identidades, sustitucion,
                restricciones);
    }

    private static String codigo(Resolucion r, Polinomio p, Map<Character, Expresion> funciones, List<String> identidades) {
        if (!identidades.isEmpty()) {
            return "T04";
        }
        boolean variasLetras = p.variables().size() > 1;
        if (variasLetras && !r.pasos().isEmpty() && Set.of("F01", "F02", "M01").contains(r.pasos().getFirst().codigoTecnica())) {
            return "T01";
        }
        boolean exponencial = false;
        boolean logaritmo = false;
        for (Expresion f : funciones.values()) {
            if (f instanceof Potencia) {
                exponencial = true;
            } else if (f instanceof Funcion fn && (fn.tipo() == TipoFuncion.LN || fn.tipo() == TipoFuncion.LOG)) {
                logaritmo = true;
            } else if (f instanceof Funcion fn && fn.tipo() == TipoFuncion.EXP) {
                exponencial = true;
            }
        }
        return exponencial ? "T02" : logaritmo ? "T05" : "T03";
    }

    private static String nombres(Polinomio p, Map<Character, Expresion> funciones) {
        List<String> l = new ArrayList<>();
        for (char c : p.variables()) {
            if (funciones.containsKey(c)) {
                l.add(String.valueOf(c));
            }
        }
        return String.join(", ", l);
    }

    private static String definicion(Polinomio p, Map<Character, Expresion> funciones) {
        List<String> l = new ArrayList<>();
        for (char c : p.variables()) {
            Expresion f = funciones.get(c);
            if (f != null) {
                l.add(c + " = " + ImpresoraTexto.imprimir(f));
            }
        }
        return String.join(", ", l);
    }

    private static Expresion anguloDoble(Expresion e, List<String> identidades) {
        Set<String> sueltas = new LinkedHashSet<>();
        SustitucionFunciones.recorrer(e, n -> {
            if (n instanceof Funcion f && mitad(f.argumento()) == null) {
                sueltas.add(f.tipo().name());
            }
        });
        boolean enSenos = sueltas.contains("SEN") && !sueltas.contains("COS");
        return SustitucionFunciones.transformar(e, n -> {
            if (n instanceof Funcion f && (f.tipo() == TipoFuncion.SEN || f.tipo() == TipoFuncion.COS)) {
                Expresion a = mitad(f.argumento());
                if (a == null) {
                    return n;
                }
                Funcion sen = new Funcion(TipoFuncion.SEN, a);
                Funcion cos = new Funcion(TipoFuncion.COS, a);
                Expresion r;
                if (f.tipo() == TipoFuncion.SEN) {
                    r = new Producto(Constante.de(2), sen, cos);
                } else if (enSenos) {
                    r = new Suma(Constante.de(1), new Negacion(new Producto(Constante.de(2), new Potencia(sen, Constante.de(2)))));
                } else {
                    r = new Suma(new Producto(Constante.de(2), new Potencia(cos, Constante.de(2))), new Negacion(Constante.de(1)));
                }
                identidades.add(ImpresoraTexto.imprimir(f) + " = " + ImpresoraTexto.imprimir(r));
                return r;
            }
            return n;
        });
    }

    private static Expresion mitad(Expresion e) {
        if (e instanceof Producto(List<Expresion> fs) && fs.size() == 2 && fs.getFirst() instanceof Constante c
                && c.valor().equals(Racional.de(2))) {
            return fs.get(1);
        }
        return null;
    }

    private static Expresion sumaAProducto(Expresion e, List<String> identidades) {
        if (!(e instanceof Suma(List<Expresion> ts)) || ts.size() != 2) {
            return e;
        }
        Object[] a = trig(ts.get(0));
        Object[] b = trig(ts.get(1));
        if (a == null || b == null || a[0] != b[0] || (boolean) a[2]) {
            return e;
        }
        long m = (long) a[1];
        long n = (long) b[1];
        boolean resta = (boolean) b[2];
        if (m == n || Math.max(m, n) == 2 * Math.min(m, n)) {
            return e;
        }
        Expresion x = new Variable('x');
        Expresion semisuma = argumento((m + n), 2, x);
        Expresion semidif = argumento(m - n, 2, x);
        TipoFuncion t = (TipoFuncion) a[0];
        Expresion r;
        if (t == TipoFuncion.SEN) {
            r = resta ? new Producto(Constante.de(2), new Funcion(TipoFuncion.COS, semisuma), new Funcion(TipoFuncion.SEN, semidif))
                    : new Producto(Constante.de(2), new Funcion(TipoFuncion.SEN, semisuma), new Funcion(TipoFuncion.COS, semidif));
        } else {
            r = resta ? new Negacion(new Producto(Constante.de(2), new Funcion(TipoFuncion.SEN, semisuma), new Funcion(TipoFuncion.SEN, semidif)))
                    : new Producto(Constante.de(2), new Funcion(TipoFuncion.COS, semisuma), new Funcion(TipoFuncion.COS, semidif));
        }
        identidades.add(ImpresoraTexto.imprimir(e) + " = " + ImpresoraTexto.imprimir(r));
        return r;
    }

    private static Object[] trig(Expresion t) {
        boolean negativo = false;
        if (t instanceof Negacion(Expresion arg)) {
            negativo = true;
            t = arg;
        }
        if (!(t instanceof Funcion f) || f.tipo() != TipoFuncion.SEN && f.tipo() != TipoFuncion.COS) {
            return null;
        }
        long k;
        if (f.argumento() instanceof Variable v && v.nombre() == 'x') {
            k = 1;
        } else if (f.argumento() instanceof Producto(List<Expresion> fs) && fs.size() == 2
                && fs.getFirst() instanceof Constante c && c.valor().esEntero() && c.valor().signo() > 0
                && fs.get(1) instanceof Variable v && v.nombre() == 'x') {
            k = c.valor().num().longValueExact();
        } else {
            return null;
        }
        return new Object[] {f.tipo(), k, negativo};
    }

    private static Expresion argumento(long k, long d, Expresion x) {
        Racional c = Racional.de(k, d);
        if (c.equals(Racional.UNO)) {
            return x;
        }
        return c.esEntero() ? new Producto(new Constante(c), x) : new Producto(new Constante(c), x);
    }

    private record Par(char sen, char cos) {
    }

    private static List<Par> pares(Map<String, Character> letras, Map<Character, Expresion> funciones, boolean crear) {
        List<Par> r = new ArrayList<>();
        for (var e : new ArrayList<>(letras.entrySet())) {
            if (!(funciones.get(e.getValue()) instanceof Funcion f)) {
                continue;
            }
            if (f.tipo() != TipoFuncion.SEN && f.tipo() != TipoFuncion.COS) {
                continue;
            }
            TipoFuncion otro = f.tipo() == TipoFuncion.SEN ? TipoFuncion.COS : TipoFuncion.SEN;
            Funcion g = new Funcion(otro, f.argumento());
            String clave = ImpresoraTexto.imprimir(g);
            Character l = letras.get(clave);
            if (l == null && crear) {
                l = ((Variable) SustitucionFunciones.aLetras(g, letras)).nombre();
                funciones.put(l, g);
            }
            if (l != null) {
                Par par = f.tipo() == TipoFuncion.SEN ? new Par(e.getValue(), l) : new Par(l, e.getValue());
                if (!r.contains(par)) {
                    r.add(par);
                }
            }
        }
        return r;
    }

    private Polinomio pitagoricaHaciaUnTermino(Polinomio p, Map<String, Character> letras,
                                               Map<Character, Expresion> funciones, List<String> identidades) {
        if (p.terminos().size() < 2) {
            return null;
        }
        Map<String, Character> copiaLetras = new LinkedHashMap<>(letras);
        Map<Character, Expresion> copiaFunciones = new HashMap<>(funciones);
        for (Par par : pares(copiaLetras, copiaFunciones, true)) {
            for (boolean cosACero : new boolean[] {true, false}) {
                char quitar = cosACero ? par.cos() : par.sen();
                char poner = cosACero ? par.sen() : par.cos();
                if (p.gradoEn(quitar) < 2) {
                    continue;
                }
                Polinomio q = reemplazarCuadrado(p, quitar, poner);
                if (q.terminos().size() == 1 && q.variables().size() <= 1) {
                    letras.clear();
                    letras.putAll(copiaLetras);
                    funciones.clear();
                    funciones.putAll(copiaFunciones);
                    identidades.add(ImpresoraTexto.imprimir(funciones.get(quitar)) + "^2 = 1 - "
                            + ImpresoraTexto.imprimir(funciones.get(poner)) + "^2");
                    return q;
                }
            }
        }
        return null;
    }

    private static Polinomio reemplazarCuadrado(Polinomio p, char quitar, char poner) {
        Polinomio uno = Polinomio.UNO;
        Polinomio cambio = uno.restar(Polinomio.variable(poner).potencia(2));
        Polinomio r = Polinomio.CERO;
        for (var t : p.terminos().entrySet()) {
            int k = t.getKey().exponente(quitar);
            int par = k - k % 2;
            Monomio resto = par == 0 ? t.getKey() : t.getKey().dividir(Monomio.de(quitar, par));
            r = r.sumar(Polinomio.termino(t.getValue(), resto).multiplicar(cambio.potencia(k / 2)));
        }
        return r;
    }

    private static FormaFactorizada quitarSenoMasCoseno(FormaFactorizada f, Map<String, Character> letras,
                                                       List<String> identidades) {
        Map<Character, Expresion> funciones = new HashMap<>();
        letras.forEach((clave, letra) -> funciones.put(letra, Parser.parse(clave)));
        List<Par> pares = pares(new LinkedHashMap<>(letras), funciones, false);
        if (pares.isEmpty()) {
            return f;
        }
        List<Factor> quedan = new ArrayList<>();
        boolean cambio = false;
        for (Factor x : f.factores()) {
            boolean uno = false;
            for (Par par : pares) {
                Polinomio s2c2 = Polinomio.variable(par.sen()).potencia(2).sumar(Polinomio.variable(par.cos()).potencia(2));
                if (x.polinomio().equals(s2c2)) {
                    uno = true;
                    identidades.add(ImpresoraTexto.imprimir(funciones.get(par.sen())) + "^2 + "
                            + ImpresoraTexto.imprimir(funciones.get(par.cos())) + "^2 = 1");
                }
            }
            if (uno) {
                cambio = true;
            } else {
                quedan.add(x);
            }
        }
        return cambio ? FormaFactorizada.normalizada(f.constante(), quedan) : f;
    }

    static List<String> restricciones(Expresion e) {
        Set<String> r = new LinkedHashSet<>();
        SustitucionFunciones.recorrer(e, n -> {
            if (n instanceof Funcion f) {
                String a = ImpresoraTexto.imprimir(f.argumento());
                switch (f.tipo()) {
                    case LN, LOG -> r.add(a + " > 0");
                    case TAN, SEC -> r.add(a + " ≠ π/2 + kπ");
                    case COT, CSC -> r.add(a + " ≠ kπ");
                    case SQRT -> r.add(a + " ≥ 0");
                    case ARCSEN, ARCCOS -> r.add("-1 ≤ " + a + " ≤ 1");
                    default -> {
                    }
                }
            }
        });
        return List.copyOf(r);
    }
}
