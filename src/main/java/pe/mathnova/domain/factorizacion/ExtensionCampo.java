package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.numeros.Atomo;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.DeMoivre;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;
import pe.mathnova.domain.numeros.RaicesAproximadas;
import pe.mathnova.domain.parser.Parser;

final class ExtensionCampo {

    private static final Racional MEDIO = Racional.de(1, 2);

    private final Campo campo;
    private final List<Extension.Paso> pasos = new ArrayList<>();
    private final List<String> notas = new ArrayList<>();
    private final List<String> aproximadas = new ArrayList<>();

    private ExtensionCampo(Campo campo) {
        this.campo = campo;
    }

    static Extension extender(Resolucion q, Campo campo) {
        if (campo == Campo.Q) {
            throw new IllegalArgumentException("La extensión es para R o C");
        }
        return new ExtensionCampo(campo).ejecutar(q);
    }

    private Extension ejecutar(Resolucion q) {
        FormaFactorizada fq = q.resultado();
        Polinomio entrada = Polinomio.desde(q.entrada());
        Racional constante = fq.constante();
        List<FormaExtendida.Factor> finales = new ArrayList<>();

        Map<Polinomio, List<PolinomioAlgebraico>> binomio = esBinomio(entrada) && entrada.grado() >= (campo == Campo.C ? 3 : 4)
                ? binomio(entrada, fq.factores().stream().map(Factor::polinomio)
                        .filter(p -> p.grado() >= 2).toList(), true)
                : null;
        List<Polinomio> tratados = new ArrayList<>();
        List<String> motivos = new ArrayList<>();
        for (Factor f : fq.factores()) {
            Polinomio p = f.polinomio();
            int m = f.multiplicidad();
            List<PolinomioAlgebraico> piezas = null;
            if (p.variables().size() == 1 && p.grado() >= 2) {
                int antes = pasos.size();
                piezas = binomio != null ? binomio.get(p) : partir(p);
                if (piezas != null || pasos.size() > antes) {
                    tratados.add(p);
                    motivos.add(motivo(p, piezas != null));
                }
            }
            if (piezas == null) {
                finales.add(new FormaExtendida.Factor(PolinomioAlgebraico.desde(p), m));
                continue;
            }
            constante = constante.multiplicar(principal(p).potencia(m));
            for (PolinomioAlgebraico pieza : piezas) {
                finales.add(new FormaExtendida.Factor(pieza, m));
            }
        }
        FormaExtendida resultado = new FormaExtendida(constante, finales);
        if (binomio != null && !pasos.isEmpty()) {
            Extension.Paso p = pasos.getFirst();
            pasos.set(0, new Extension.Paso(0, p.codigoTecnica(), p.justificacion(), p.antes(),
                    resultado.aExpresion(), p.trabajo()));
        }
        List<Extension.Paso> todos = new ArrayList<>();
        if (!pasos.isEmpty()) {
            String c = campo == Campo.R ? "R" : "C";
            String justificacion = motivos.isEmpty()
                    ? "Primero se factoriza en Q (RN27); ahora se parten en " + c + " los factores irreducibles en Q."
                    : String.join(" ", motivos);
            todos.add(new Extension.Paso(0, "P" + c, justificacion, fq.aExpresion(), aPartir(tratados, fq), null));
        }
        todos.addAll(pasos);
        List<Extension.Paso> numerados = new ArrayList<>();
        int orden = q.pasos().size() + 1;
        for (Extension.Paso p : todos) {
            numerados.add(new Extension.Paso(orden++, p.codigoTecnica(), p.justificacion(), p.antes(), p.despues(),
                    p.trabajo()));
        }
        return new Extension(campo, numerados, resultado, notas, aproximadas);
    }

    private List<PolinomioAlgebraico> partir(Polinomio p) {
        int g = p.grado();
        try {
            if (g == 2) {
                return cuadratica(p);
            }
            if (esBinomio(p)) {
                if (g == 3 && campo == Campo.R) {
                    return cubos(p);
                }
                Map<Polinomio, List<PolinomioAlgebraico>> r = binomio(p, List.of(p), false);
                if (r != null) {
                    return r.get(p);
                }
            }
            if (g == 3) {
                return cardano(p);
            }
            if (g == 4) {
                List<PolinomioAlgebraico> r = ferrari(p);
                if (r != null) {
                    return r;
                }
            }
        } catch (ArithmeticException e) {
        }
        return aproximar(p);
    }

    private List<PolinomioAlgebraico> cuadratica(Polinomio p) {
        char x = p.variables().first();
        Racional a = coef(p, 2);
        Racional b = coef(p, 1);
        Racional c = coef(p, 0);
        Racional delta = b.multiplicar(b).restar(Racional.de(4).multiplicar(a).multiplicar(c));
        if (delta.signo() < 0 && campo == Campo.R) {
            notas.add(p + " no se factoriza en R porque Δ = " + delta + " < 0.");
            return null;
        }
        NumeroAlgebraico raizDelta = NumeroAlgebraico.raiz(2, delta);
        NumeroAlgebraico menosB = NumeroAlgebraico.de(b.negar());
        Racional dosA = a.multiplicar(Racional.de(2));
        NumeroAlgebraico r1 = menosB.sumar(raizDelta).dividir(dosA);
        NumeroAlgebraico r2 = menosB.restar(raizDelta).dividir(dosA);
        List<PolinomioAlgebraico> piezas = List.of(PolinomioAlgebraico.lineal(x, r1), PolinomioAlgebraico.lineal(x, r2));
        Expresion despues = new FormaExtendida(a, piezas.stream().map(f -> new FormaExtendida.Factor(f, 1)).toList())
                .aExpresion();
        String pt = p.toString();
        String normal = a.equals(Racional.UNO) ? "" : pt + " = " + a + "(" + Polinomio.desde(Parser.parse(pt))
                .multiplicar(a.inverso()) + ") y ";
        if (delta.signo() > 0) {
            if (b.esCero()) {
                paso("F21", normal + "x^2 - " + c.negar().dividir(a) + " es una diferencia de cuadrados con radicales: "
                        + c.negar().dividir(a) + " = (" + r1 + ")^2.", p, despues,
                        new TrabajoIntermedio.Comparacion("a^2 - b^2 = (a - b)(a + b) con a = " + x + " y b = " + r1));
            } else if (a.equals(Racional.UNO) && b.num().testBit(0) == false && b.esEntero()) {
                Racional h = b.multiplicar(MEDIO);
                Racional k = h.multiplicar(h).restar(c);
                String cuadrado = "(" + Polinomio.desde(Parser.parse(x + " + " + h)) + ")^2";
                String completado = cuadrado + " - " + k;
                paso("F22", "Se completa el cuadrado: se suma y se resta (" + h + ")^2 = " + h.multiplicar(h) + ".", p,
                        Parser.parse(completado), new TrabajoIntermedio.Agrupacion(List.of(
                                pt + " = (" + Polinomio.desde(Parser.parse(cuadrado)) + ") - " + h.multiplicar(h) + " + " + c,
                                "= " + completado)));
                NumeroAlgebraico rk = NumeroAlgebraico.raiz(2, k);
                paso("F21", completado + " es una diferencia de cuadrados: " + k + " = (" + rk + ")^2.", p, despues,
                        new TrabajoIntermedio.Comparacion("a^2 - b^2 = (a - b)(a + b) con a = "
                                + Polinomio.desde(Parser.parse(x + " + " + h)) + " y b = " + rk));
            } else {
                paso("F23", "Fórmula general con Δ = " + delta + " > 0 (no es un cuadrado: las raíces llevan radicales).",
                        p, despues, formulaGeneral(a, b, c, delta, raizDelta, r1, r2));
            }
            return piezas;
        }
        if (b.esCero()) {
            NumeroAlgebraico k = NumeroAlgebraico.raiz(2, c.dividir(a));
            paso("F24", normal + "x^2 + " + c.dividir(a) + " es una suma de cuadrados; en C, con i^2 = -1, es la diferencia "
                    + "x^2 - (" + k + "·i)^2.", p, despues,
                    new TrabajoIntermedio.Comparacion("a^2 + b^2 = (a - b·i)(a + b·i) con a = " + x + " y b = " + k));
        } else {
            paso("F25", "Δ = " + delta + " < 0: en C la raíz de Δ es imaginaria, √(" + delta + ") = " + raizDelta + ".", p,
                    despues, formulaGeneral(a, b, c, delta, raizDelta, r1, r2));
        }
        return piezas;
    }

    private static TrabajoIntermedio formulaGeneral(Racional a, Racional b, Racional c, Racional delta,
                                                    NumeroAlgebraico raizDelta, NumeroAlgebraico r1, NumeroAlgebraico r2) {
        return new TrabajoIntermedio.Agrupacion(List.of(
                "Δ = b^2 - 4ac = (" + b + ")^2 - 4·" + paren(a) + "·" + paren(c) + " = " + delta,
                "√Δ = √(" + delta + ") = " + raizDelta,
                "x = (-b ± √Δ)/(2a) = (" + b.negar() + " ± " + NumeroAlgebraico.raiz(2, delta).toString() + ")/" + paren(a.multiplicar(Racional.de(2))),
                "x₁ = " + r1 + ",  x₂ = " + r2));
    }

    private List<PolinomioAlgebraico> cuadraticaAlgebraica(PolinomioAlgebraico q) {
        char x = q.variable();
        PolinomioAlgebraico m = q.monico();
        NumeroAlgebraico b = m.coeficiente(1);
        NumeroAlgebraico c = m.coeficiente(0);
        NumeroAlgebraico delta = b.multiplicar(b).restar(c.multiplicar(Racional.de(4)));
        boolean real = delta.esReal();
        boolean negativo = real && delta.signo() < 0;
        if (negativo && campo == Campo.R) {
            notas.add(q + " no se factoriza en R porque Δ = " + delta + " < 0.");
            return null;
        }
        NumeroAlgebraico raiz = delta.raizCuadrada();
        NumeroAlgebraico r1 = b.negar().sumar(raiz).multiplicar(MEDIO);
        NumeroAlgebraico r2 = b.negar().restar(raiz).multiplicar(MEDIO);
        List<PolinomioAlgebraico> piezas = List.of(PolinomioAlgebraico.lineal(x, r1), PolinomioAlgebraico.lineal(x, r2));
        paso(real && !negativo ? "F23" : "F25", "Fórmula general en " + q + " con Δ = " + delta
                        + (negativo ? " < 0." : "."), q,
                forma(Racional.UNO, piezas), new TrabajoIntermedio.Agrupacion(List.of(
                        "Δ = (" + b + ")^2 - 4·(" + c + ") = " + delta,
                        "√Δ = " + raiz,
                        "x₁ = " + r1 + ",  x₂ = " + r2)));
        return piezas;
    }

    private List<PolinomioAlgebraico> cubos(Polinomio p) {
        char x = p.variables().first();
        Racional a = coef(p, 3);
        Racional d = coef(p, 0);
        NumeroAlgebraico r = NumeroAlgebraico.raiz(3, d.negar().dividir(a));
        PolinomioAlgebraico lineal = PolinomioAlgebraico.lineal(x, r);
        PolinomioAlgebraico cuadratico = PolinomioAlgebraico.cuadratico(x, r, r.multiplicar(r));
        List<PolinomioAlgebraico> piezas = List.of(lineal, cuadratico);
        boolean suma = d.signo() > 0;
        NumeroAlgebraico b = suma ? r.negar() : r;
        String identidad = suma ? "a^3 + b^3 = (a + b)(a^2 - ab + b^2)" : "a^3 - b^3 = (a - b)(a^2 + ab + b^2)";
        paso("F26", (suma ? "Suma" : "Diferencia") + " de cubos con radicales: " + d.dividir(a).toString().replace("-", "")
                        + " = (" + b + ")^3.", p, forma(a, piezas),
                new TrabajoIntermedio.Comparacion(identidad + " con a = " + x + " y b = " + b));
        notas.add(cuadratico + " no se factoriza en R porque Δ = " + r.multiplicar(r).multiplicar(Racional.de(-3)) + " < 0.");
        return piezas;
    }

    private Map<Polinomio, List<PolinomioAlgebraico>> binomio(Polinomio binomio, List<Polinomio> factores,
                                                               boolean entrada) {
        if (factores.isEmpty()) {
            return null;
        }
        char x = binomio.variables().first();
        int n = binomio.grado();
        Racional a = coef(binomio, n);
        Racional cte = coef(binomio, 0).negar().dividir(a);
        Optional<List<DeMoivre.Raiz>> raices = DeMoivre.raices(n, cte);
        if (raices.isEmpty()) {
            return null;
        }
        Map<Polinomio, List<PolinomioAlgebraico>> r = new LinkedHashMap<>();
        List<String> lineas = new ArrayList<>();
        Racional modulo = cte.signo() < 0 ? cte.negar() : cte;
        String rho = NumeroAlgebraico.raiz(n, modulo).toString();
        if (campo == Campo.C) {
            String theta = cte.signo() < 0 ? "π" : "0";
            lineas.add(x + "^" + n + " = " + cte + " = " + modulo + "(cos " + theta + " + i·sen " + theta + ")");
            lineas.add(x + "ₖ = " + rho + "·(cos((" + theta + " + 2kπ)/" + n + ") + i·sen((" + theta + " + 2kπ)/" + n
                    + ")),  k = 0, …, " + (n - 1));
            for (DeMoivre.Raiz z : raices.get()) {
                lineas.add(x + sub(z.k()) + " = " + rho + "·(cos " + DeMoivre.angulo(z.angulo()) + " + i·sen "
                        + DeMoivre.angulo(z.angulo()) + ") = " + z.valor());
            }
        } else {
            lineas.add("Cada par de raíces conjugadas z, z̄ da (x - z)(x - z̄) = x^2 - 2·Re(z)·x + |z|^2.");
        }
        boolean cambia = false;
        for (Polinomio f : factores) {
            PolinomioAlgebraico fa = PolinomioAlgebraico.desde(f);
            List<NumeroAlgebraico> suyas = raices.get().stream().map(DeMoivre.Raiz::valor)
                    .filter(z -> fa.evaluar(z).esCero()).toList();
            if (suyas.size() != f.grado()) {
                return null;
            }
            List<PolinomioAlgebraico> piezas = new ArrayList<>();
            for (NumeroAlgebraico z : suyas) {
                if (campo == Campo.C || z.esReal()) {
                    piezas.add(PolinomioAlgebraico.lineal(x, z));
                } else if (z.parteImaginaria().signo() > 0) {
                    NumeroAlgebraico re = z.parteReal();
                    NumeroAlgebraico im = z.parteImaginaria();
                    PolinomioAlgebraico q = PolinomioAlgebraico.cuadratico(x, re.multiplicar(Racional.de(-2)),
                            re.multiplicar(re).sumar(im.multiplicar(im)));
                    piezas.add(q);
                    lineas.add("z = " + z + ":  " + q);
                }
            }
            if (piezas.size() == 1 && piezas.getFirst().equals(fa.monico())) {
                r.put(f, null);
                if (f.grado() == 2) {
                    notas.add(f + " no se factoriza en R: sus raíces son complejas conjugadas.");
                }
                continue;
            }
            cambia = true;
            r.put(f, piezas);
        }
        if (!cambia) {
            return r;
        }
        String codigo = campo == Campo.C ? "F30" : "F29";
        String justificacion = campo == Campo.C
                ? "Las raíces de " + x + "^" + n + " = " + cte + " salen de la forma polar con la fórmula de De Moivre."
                : "Las raíces de " + binomio + " no reales vienen en pares conjugados; cada par da un cuadrático real (RN28).";
        List<PolinomioAlgebraico> todas = new ArrayList<>();
        r.values().stream().filter(l -> l != null).forEach(todas::addAll);
        paso(codigo, justificacion, binomio, entrada ? null : forma(a, todas), new TrabajoIntermedio.Agrupacion(lineas));
        return r;
    }

    private List<PolinomioAlgebraico> cardano(Polinomio p) {
        char x = p.variables().first();
        Racional a3 = coef(p, 3);
        Racional a = coef(p, 2).dividir(a3);
        Racional b = coef(p, 1).dividir(a3);
        Racional c = coef(p, 0).dividir(a3);
        Racional h = a.dividir(Racional.de(3));
        Racional pp = b.restar(a.multiplicar(a).dividir(Racional.de(3)));
        Racional qq = Racional.de(2).multiplicar(a.potencia(3)).dividir(Racional.de(27))
                .restar(a.multiplicar(b).dividir(Racional.de(3))).sumar(c);
        Racional delta = qq.multiplicar(qq).dividir(Racional.de(4)).sumar(pp.potencia(3).dividir(Racional.de(27)));
        List<String> lineas = new ArrayList<>();
        if (!h.esCero()) {
            lineas.add("Con " + x + " = t - " + paren(h) + ": t^3 + (" + pp + ")t + (" + qq + ")");
        }
        lineas.add("p = " + pp + ",  q = " + qq);
        NumeroAlgebraico menosH = NumeroAlgebraico.de(h.negar());
        List<PolinomioAlgebraico> piezas = new ArrayList<>();
        if (delta.signo() > 0) {
            lineas.add("Δ = (q/2)^2 + (p/3)^3 = " + delta + " > 0: una raíz real y dos complejas conjugadas");
            NumeroAlgebraico sd = NumeroAlgebraico.raiz(2, delta);
            NumeroAlgebraico mq = NumeroAlgebraico.de(qq.negar().multiplicar(MEDIO));
            NumeroAlgebraico u = mq.sumar(sd).raizCubicaReal();
            NumeroAlgebraico v = mq.restar(sd).raizCubicaReal();
            lineas.add("u = ∛(-q/2 + √Δ) = " + u + ",  v = ∛(-q/2 - √Δ) = " + v);
            NumeroAlgebraico x1 = u.sumar(v).sumar(menosH);
            lineas.add(x + "₁ = u + v" + (h.esCero() ? "" : " - " + paren(h)) + " = " + x1);
            piezas.add(PolinomioAlgebraico.lineal(x, x1));
            if (campo == Campo.R) {
                NumeroAlgebraico bb = x1.sumar(NumeroAlgebraico.de(a));
                NumeroAlgebraico cc = x1.multiplicar(x1).sumar(x1.multiplicar(a)).sumar(NumeroAlgebraico.de(b));
                PolinomioAlgebraico cuadratico = PolinomioAlgebraico.cuadratico(x, bb, cc);
                piezas.add(cuadratico);
                lineas.add("Al dividir entre (" + x + " - " + x1 + "): " + cuadratico);
                notas.add(cuadratico + " no se factoriza en R: su Δ es negativo (las otras dos raíces son complejas).");
            } else {
                NumeroAlgebraico re = u.sumar(v).multiplicar(Racional.de(-1, 2)).sumar(menosH);
                NumeroAlgebraico im = u.restar(v).multiplicar(NumeroAlgebraico.raiz(2, 3)).multiplicar(MEDIO)
                        .multiplicar(NumeroAlgebraico.I);
                NumeroAlgebraico x2 = re.sumar(im);
                NumeroAlgebraico x3 = re.restar(im);
                lineas.add(x + "₂, " + x + "₃ = -(u + v)/2 ± (√3/2)(u - v)·i: " + x2 + ",  " + x3);
                piezas.add(PolinomioAlgebraico.lineal(x, x2));
                piezas.add(PolinomioAlgebraico.lineal(x, x3));
            }
        } else {
            lineas.add("Δ = (q/2)^2 + (p/3)^3 = " + delta + " < 0: casus irreducibilis");
            lineas.add("Las tres raíces son reales e irracionales, pero la fórmula de Cardano las da con raíces "
                    + "cúbicas de números complejos. Se escriben en forma trigonométrica:");
            NumeroAlgebraico m = NumeroAlgebraico.raiz(2, pp.negar().dividir(Racional.de(3))).multiplicar(Racional.de(2));
            NumeroAlgebraico arg = NumeroAlgebraico.raiz(2, Racional.de(-3).dividir(pp))
                    .multiplicar(Racional.de(3).multiplicar(qq).dividir(Racional.de(2).multiplicar(pp)));
            lineas.add("tₖ = 2√(-p/3)·cos(1/3·arccos((3q/(2p))·√(-3/p)) - 2kπ/3),  k = 0, 1, 2");
            Expresion arccos = new Producto(new Constante(Racional.de(1, 3)), new Funcion(TipoFuncion.ARCCOS, arg.aExpresion()));
            for (int k = 0; k < 3; k++) {
                Expresion angulo = k == 0 ? arccos : new Suma(arccos, new Negacion(new Cociente(
                        new Producto(Constante.de(2L * k), new ConstanteEspecial(ConstanteEspecial.Tipo.PI)), Constante.de(3))));
                NumeroAlgebraico cos = NumeroAlgebraico.de(Atomo.simbolico(new Funcion(TipoFuncion.COS, angulo)));
                NumeroAlgebraico xk = m.multiplicar(cos).sumar(menosH);
                lineas.add(x + sub(k + 1) + " = " + xk);
                piezas.add(PolinomioAlgebraico.lineal(x, xk));
            }
        }
        paso("F27", "No tiene raíces racionales: se usa la fórmula de Cardano para " + p + ".", p, forma(a3, piezas),
                new TrabajoIntermedio.Agrupacion(lineas));
        return piezas;
    }

    private List<PolinomioAlgebraico> ferrari(Polinomio p) {
        char x = p.variables().first();
        PolinomioAlgebraico f = PolinomioAlgebraico.desde(p).monico();
        Racional h = f.coeficiente(3).aRacional().dividir(Racional.de(4));
        PolinomioAlgebraico g = f.trasladar(NumeroAlgebraico.de(h.negar()));
        Racional pp = g.coeficiente(2).aRacional();
        Racional qq = g.coeficiente(1).aRacional();
        Racional rr = g.coeficiente(0).aRacional();
        List<String> lineas = new ArrayList<>();
        if (!h.esCero()) {
            lineas.add("Con " + x + " = y - " + paren(h) + ": y^4 + (" + pp + ")y^2 + (" + qq + ")y + (" + rr + ")");
        }
        char y = 'y';
        PolinomioAlgebraico q1;
        PolinomioAlgebraico q2;
        if (qq.esCero()) {
            Racional d = pp.multiplicar(pp).restar(Racional.de(4).multiplicar(rr));
            if (d.signo() >= 0) {
                NumeroAlgebraico sd = NumeroAlgebraico.raiz(2, d);
                NumeroAlgebraico z1 = NumeroAlgebraico.de(pp.negar()).sumar(sd).multiplicar(MEDIO);
                NumeroAlgebraico z2 = NumeroAlgebraico.de(pp.negar()).restar(sd).multiplicar(MEDIO);
                lineas.add("q = 0: z = y^2 cumple z^2 + (" + pp + ")z + (" + rr + ") = 0, z = " + z1 + ", " + z2);
                q1 = PolinomioAlgebraico.cuadratico(y, NumeroAlgebraico.CERO, z1.negar());
                q2 = PolinomioAlgebraico.cuadratico(y, NumeroAlgebraico.CERO, z2.negar());
            } else {
                NumeroAlgebraico sr = NumeroAlgebraico.raiz(2, rr);
                NumeroAlgebraico s = sr.multiplicar(Racional.de(2)).restar(NumeroAlgebraico.de(pp)).raizCuadrada();
                lineas.add("q = 0: (y^2 + √r)^2 - (2√r - p)y^2 con √r = " + sr + " y √(2√r - p) = " + s);
                q1 = PolinomioAlgebraico.cuadratico(y, s.negar(), sr);
                q2 = PolinomioAlgebraico.cuadratico(y, s, sr);
            }
        } else {
            Racional c2 = pp.multiplicar(Racional.de(2));
            Racional c1 = pp.multiplicar(pp).restar(rr.multiplicar(Racional.de(4)));
            Racional c0 = qq.multiplicar(qq).negar();
            lineas.add("Cúbica resolvente: S^3 + (" + c2 + ")S^2 + (" + c1 + ")S + (" + c0 + ") = 0");
            Univariado res = Univariado.de('S', List.of(c0, c1, c2, Racional.UNO));
            NumeroAlgebraico sS = null;
            Optional<List<Racional>> candidatos = RaicesRacionales.candidatos(res);
            if (candidatos.isPresent()) {
                for (Racional k : candidatos.get()) {
                    if (k.signo() > 0 && res.evaluar(k).esCero()) {
                        sS = NumeroAlgebraico.de(k);
                        break;
                    }
                }
            }
            if (sS == null) {
                return null;
            }
            lineas.add("S = " + sS);
            NumeroAlgebraico s = sS.raizCuadrada();
            NumeroAlgebraico qs = NumeroAlgebraico.de(qq).dividir(s);
            NumeroAlgebraico base = sS.sumar(NumeroAlgebraico.de(pp));
            NumeroAlgebraico t = base.restar(qs).multiplicar(MEDIO);
            NumeroAlgebraico u = base.sumar(qs).multiplicar(MEDIO);
            q1 = PolinomioAlgebraico.cuadratico(y, s, t);
            q2 = PolinomioAlgebraico.cuadratico(y, s.negar(), u);
        }
        PolinomioAlgebraico x1 = enX(q1.trasladar(NumeroAlgebraico.de(h)), x);
        PolinomioAlgebraico x2 = enX(q2.trasladar(NumeroAlgebraico.de(h)), x);
        lineas.add(p + " = (" + x1 + ")(" + x2 + ")");
        Racional a = coef(p, 4);
        paso("F28", "No se parte en Q: el método de Ferrari lo escribe como producto de dos cuadráticos.", p,
                forma(a, List.of(x1, x2)), new TrabajoIntermedio.Agrupacion(lineas));
        List<PolinomioAlgebraico> piezas = new ArrayList<>();
        for (PolinomioAlgebraico q : List.of(x1, x2)) {
            List<PolinomioAlgebraico> r = cuadraticaAlgebraica(q);
            piezas.addAll(r == null ? List.of(q) : r);
        }
        return piezas;
    }

    private static PolinomioAlgebraico enX(PolinomioAlgebraico p, char x) {
        return PolinomioAlgebraico.de(x, p.coeficientes());
    }

    private List<PolinomioAlgebraico> aproximar(Polinomio p) {
        char x = p.variables().first();
        List<Racional> c = new ArrayList<>();
        for (int k = 0; k <= p.grado(); k++) {
            c.add(coef(p, k));
        }
        List<ComplejoDecimal> raices = RaicesAproximadas.raices(c);
        List<String> lineas = new ArrayList<>();
        lineas.add("Grado " + p.grado() + (p.grado() >= 5
                ? ": por el teorema de Abel–Ruffini no hay una fórmula general con radicales."
                : ": no se encontró una forma con radicales."));
        lineas.add("Durand-Kerner y Newton con " + RaicesAproximadas.PRECISION + " cifras:");
        int i = 1;
        List<String> textos = new ArrayList<>();
        for (ComplejoDecimal z : raices) {
            boolean real = z.esReal(RaicesAproximadas.PRECISION - 20);
            if (campo == Campo.R && !real) {
                continue;
            }
            String t = RaicesAproximadas.texto(z);
            textos.add(t);
            lineas.add(x + sub(i++) + " ≈ " + t);
        }
        aproximadas.addAll(textos);
        paso("A5", p + " es irreducible en Q y sus raíces se aproximan (≈).", p, null,
                new TrabajoIntermedio.Agrupacion(lineas));
        notas.add(p + " se deja como está: sus raíces solo se conocen aproximadas (Abel–Ruffini).");
        return null;
    }

    private String motivo(Polinomio p, boolean partido) {
        return motivoConPuntos(p, partido).replaceAll("(\\d)·(?=[i√])", "$1");
    }

    private String motivoConPuntos(Polinomio p, boolean partido) {
        String c = campo.name();
        if (p.grado() == 2 && partido) {
            Racional a = coef(p, 2);
            Racional b = coef(p, 1);
            Racional k = coef(p, 0);
            Racional delta = b.multiplicar(b).restar(Racional.de(4).multiplicar(a).multiplicar(k));
            if (delta.signo() > 0) {
                String noQ = p + " no se factoriza en Q porque Δ = " + delta + " no es un cuadrado perfecto.";
                if (b.esCero()) {
                    Racional r = k.negar().dividir(a);
                    return noQ + " En " + c + " sí, porque " + r + " = (" + NumeroAlgebraico.raiz(2, r) + ")^2.";
                }
                return noQ + " En " + c + " sí, porque Δ > 0: √" + delta + " = " + NumeroAlgebraico.raiz(2, delta) + ".";
            }
            NumeroAlgebraico raizDelta = NumeroAlgebraico.raiz(2, delta);
            NumeroAlgebraico menosB = NumeroAlgebraico.de(b.negar());
            Racional dosA = a.multiplicar(Racional.de(2));
            NumeroAlgebraico r1 = menosB.sumar(raizDelta).dividir(dosA);
            NumeroAlgebraico r2 = menosB.restar(raizDelta).dividir(dosA);
            String raices = b.esCero() ? "±" + r1 : r1 + " y " + r2;
            return p + " no tiene raíces reales (Δ = " + delta + " < 0). En C sí: " + raices + ".";
        }
        String noQ = p + " es irreducible en Q: no tiene raíces racionales ni se parte con coeficientes racionales.";
        if (!partido) {
            return noQ + " En " + c + " tampoco hay una fórmula con radicales: sus raíces se aproximan.";
        }
        return noQ + (campo == Campo.R
                ? " En R sí se parte: tiene raíces reales que se escriben con radicales."
                : " En C sí se parte: sus raíces complejas se escriben con radicales e i.");
    }

    private static Expresion aPartir(List<Polinomio> tratados, FormaFactorizada fq) {
        if (tratados.isEmpty()) {
            return fq.aExpresion();
        }
        if (tratados.size() == 1) {
            return tratados.getFirst().aExpresion();
        }
        return new Producto(tratados.stream().map(Polinomio::aExpresion).toList());
    }

    private void paso(String codigo, String justificacion, Object antes, Expresion despues, TrabajoIntermedio trabajo) {
        Expresion a = switch (antes) {
            case Polinomio p -> p.aExpresion();
            case PolinomioAlgebraico p -> p.aExpresion();
            case null, default -> null;
        };
        pasos.add(new Extension.Paso(0, codigo, justificacion, a, despues, trabajo));
    }

    private static Expresion forma(Racional constante, List<PolinomioAlgebraico> piezas) {
        return new FormaExtendida(constante, piezas.stream().map(f -> new FormaExtendida.Factor(f, 1)).toList())
                .aExpresion();
    }

    private static boolean esBinomio(Polinomio p) {
        return p.variables().size() == 1 && p.terminos().size() == 2 && !p.constante().esCero();
    }

    private static Racional coef(Polinomio p, int k) {
        char x = p.variables().first();
        return p.coeficiente(k == 0 ? Monomio.UNO : Monomio.de(x, k));
    }

    private static Racional principal(Polinomio p) {
        return coef(p, p.grado());
    }

    private static String sub(int n) {
        StringBuilder sb = new StringBuilder();
        for (char c : Integer.toString(n).toCharArray()) {
            sb.append((char) ('₀' + (c - '0')));
        }
        return sb.toString();
    }

    private static String paren(Racional r) {
        return r.signo() < 0 || !r.esEntero() ? "(" + r + ")" : r.toString();
    }
}
