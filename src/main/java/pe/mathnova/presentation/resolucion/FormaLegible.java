package pe.mathnova.presentation.resolucion;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.CubicaCardano;
import pe.mathnova.domain.factorizacion.Extension;
import pe.mathnova.domain.factorizacion.FormaExtendida;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;

/**
 * La presentación legible de una factorización en R o C que pasó por Cardano (F27, Δ &gt; 0). La raíz real,
 * con sus radicales anidados, se define una vez como r y se reutiliza:
 *
 * <pre>
 *   R:  (x − r)[3x² + (8 + 3r)x + (3r² + 8r − 1)]
 *   C:  3(x − r)(x − z)(x − z̄),  z, z̄ = (−(8 + 3r) ± i√(27r² + 48r − 76))/6
 * </pre>
 *
 * Todo sale de los datos de la cúbica ({@link CubicaCardano}); las abreviaturas son solo de presentación: el
 * cociente en r es exacto y el valor de Cardano de r se despliega con "Ver valor exacto de r".
 */
public final class FormaLegible {

    static final String VER_R = "Ver valor exacto de r";
    private static final MathContext MC = EvaluacionNumerica.MC;

    private final Campo campo;
    private final CubicaCardano c;
    private final char x;
    private final char r;
    private final Racional total;
    private final Racional prefijo;
    private final List<FormaExtendida.Factor> resto;
    private final Polinomio entrada;
    private final boolean entera;

    private FormaLegible(Campo campo, CubicaCardano c, Racional total, List<FormaExtendida.Factor> resto,
                         Polinomio entrada) {
        this.campo = campo;
        this.c = c;
        this.x = c.variable();
        this.r = x == 'r' ? 's' : 'r';
        this.total = total;
        this.prefijo = total.dividir(c.a3());
        this.resto = List.copyOf(resto);
        this.entrada = entrada;
        this.entera = resto.isEmpty() && prefijo.equals(Racional.UNO);
    }

    /**
     * La forma legible si la resolución está en R o C, tiene un solo paso de Cardano con Δ &gt; 0 y sus piezas
     * están en el resultado; si no, vacío (se muestra como siempre).
     */
    public static Optional<FormaLegible> de(Resolucion res) {
        if (res == null || res.extension() == null || res.extension().aproximada()) {
            return Optional.empty();
        }
        Extension ext = res.extension();
        List<Extension.Paso> cardano = ext.pasos().stream().filter(p -> "F27".equals(p.codigoTecnica())).toList();
        if (cardano.size() != 1 || cardano.getFirst().antes() == null) {
            return Optional.empty();
        }
        try {
            Optional<CubicaCardano> cc = CubicaCardano.de(Polinomio.desde(cardano.getFirst().antes()));
            if (cc.isEmpty()) {
                return Optional.empty();
            }
            CubicaCardano c = cc.get();
            FormaExtendida forma = ext.resultado();
            List<FormaExtendida.Factor> fs = forma.factores();
            PolinomioAlgebraico lineal = PolinomioAlgebraico.lineal(c.variable(), c.raiz());
            int k = -1;
            for (int i = 0; i < fs.size(); i++) {
                if (fs.get(i).polinomio().equals(lineal) && fs.get(i).multiplicidad() == 1) {
                    k = i;
                    break;
                }
            }
            int piezas = ext.campo() == Campo.R ? 2 : 3;
            if (k < 0 || k + piezas > fs.size()) {
                return Optional.empty();
            }
            for (int i = k + 1; i < k + piezas; i++) {
                int grado = ext.campo() == Campo.R ? 2 : 1;
                if (fs.get(i).polinomio().grado() != grado || fs.get(i).multiplicidad() != 1) {
                    return Optional.empty();
                }
            }
            List<FormaExtendida.Factor> resto = new ArrayList<>(fs.subList(0, k));
            resto.addAll(fs.subList(k + piezas, fs.size()));
            return Optional.of(new FormaLegible(ext.campo(), c, forma.constante(), resto, Polinomio.desde(res.entrada())));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public Campo campo() {
        return campo;
    }

    public CubicaCardano cubica() {
        return c;
    }

    // ------------------------------------------------------------------ el resultado

    /** El resultado con los símbolos: lo que va en el bloque celeste y en la tarjeta Solución. */
    public String latex() {
        return campo == Campo.R ? latexR() : latexC();
    }

    private String latexR() {
        return coef(prefijo) + latexResto() + xr() + corchete();
    }

    private String latexC() {
        return coef(total) + latexResto() + xr() + xz();
    }

    /** Debajo del resultado: qué es r (y z) y el control para ver su valor exacto. */
    public List<Desarrollo.Renglon> pie() {
        List<Desarrollo.Renglon> l = new ArrayList<>();
        l.add(new Desarrollo.Texto("donde r es la raíz real de " + p() + (entera ? ":" : ",")));
        if (!entera) {
            l.add(new Desarrollo.Formula(p() + " = " + latex(c.polinomio())));
        }
        l.add(definicionR(null));
        if (campo == Campo.C) {
            l.add(new Desarrollo.Texto("y z, z̄ son las raíces complejas conjugadas del factor cuadrático:"));
            l.add(new Desarrollo.Formula("z,\\ \\bar{z} = " + zFraccion()));
        }
        return l;
    }

    /** La frase de la tarjeta Solución, en lugar de los radicales anidados. */
    public List<String> notas() {
        String rr = "r ≈ " + aproximacion() + " es la raíz real de " + (entera ? "P" : "P(" + x + ")")
                + " (fórmula de Cardano).";
        return List.of(campo == Campo.R
                ? rr + " El factor cuadrático no se factoriza en R: su discriminante es negativo."
                : rr + " z y z̄ son las raíces complejas conjugadas del factor cuadrático.");
    }

    // ------------------------------------------------------------------ el paso de Cardano

    /**
     * El paso de Cardano en orden: la sustitución y la reducida, el discriminante y su signo, la raíz real r
     * y la factorización en el campo elegido; una operación por renglón.
     */
    public Desarrollo desarrollo() {
        List<Desarrollo.Renglon> l = new ArrayList<>();
        if (!entera) {
            l.add(new Desarrollo.Texto("Se llama " + p() + " al factor cúbico:"));
            l.add(new Desarrollo.Formula(p() + " = " + latex(c.polinomio())));
        }
        Racional h = c.h();
        l.add(new Desarrollo.Texto(h.esCero() ? "Ya no tiene término en x²; se escribe x = t:"
                : "Sustitución para quitar el término en x²:"));
        l.add(new Desarrollo.Formula(x + " = t" + sumando(Polinomio.constante(h.negar()), "", false)));
        l.add(new Desarrollo.Texto(c.a3().equals(Racional.UNO) ? "Ecuación cúbica reducida:"
                : "Ecuación cúbica reducida (dividiendo entre " + c.a3() + "):"));
        l.add(new Desarrollo.Formula("t^{3}" + sumando(Polinomio.constante(c.p()), "t", false)
                + sumando(Polinomio.constante(c.q()), "", false) + " = 0"));
        l.add(new Desarrollo.Formula("p = " + racional(c.p()) + ",\\quad q = " + racional(c.q())));

        l.add(new Desarrollo.Texto("Discriminante:"));
        l.add(new Desarrollo.Formula("\\Delta = \\left(\\frac{q}{2}\\right)^{2} + \\left(\\frac{p}{3}\\right)^{3} = "
                + racional(c.delta()) + " > 0"));
        l.add(new Desarrollo.Texto("Como Δ > 0, hay una raíz real y dos raíces complejas conjugadas."));

        l.add(new Desarrollo.Texto("La raíz real se llama r:"));
        l.add(definicionR(null));

        l.add(new Desarrollo.Texto("Al dividir " + p() + " entre (x − r) queda un factor cuadrático:"));
        l.add(new Desarrollo.Formula(p() + " = " + xr() + corchete()));
        Polinomio d = c.discriminanteCociente(r);
        l.add(new Desarrollo.Texto(campo == Campo.R
                ? "Ese factor no se factoriza en R porque su discriminante es negativo:"
                : "Su discriminante es negativo, así que sus raíces son complejas conjugadas:"));
        l.add(new Desarrollo.Formula("\\Delta_{2} = " + polinomio(d, false) + " \\approx "
                + redondeo(evaluar(d), 2) + " < 0"));
        if (campo == Campo.C) {
            l.add(new Desarrollo.Formula("z,\\ \\bar{z} = " + zFraccion()));
            l.add(new Desarrollo.Texto("Factorización en C:"));
            l.add(new Desarrollo.Formula(p() + " = " + coef(c.a3()) + xr() + xz()));
        }
        if (!entera) {
            l.add(new Desarrollo.Texto("Factorización completa en " + campo + ":"));
            l.add(new Desarrollo.Formula(latex()));
        }
        return new Desarrollo(l);
    }

    private Desarrollo.Definicion definicionR(String texto) {
        Racional h = c.h();
        String uv = "r = u + v" + sumando(Polinomio.constante(h.negar()), "", false);
        List<String> detalle = List.of(
                "u = \\sqrt[3]{-\\frac{q}{2} + \\sqrt{\\Delta}} = " + ImpresoraLatex.imprimir(c.u().aExpresion()),
                "v = \\sqrt[3]{-\\frac{q}{2} - \\sqrt{\\Delta}} = " + ImpresoraLatex.imprimir(c.v().aExpresion()),
                "r = " + ImpresoraLatex.imprimir(c.raiz().aExpresion()));
        return new Desarrollo.Definicion(texto, uv + " \\approx " + aproximacion(), VER_R, detalle);
    }

    // ------------------------------------------------------------------ comprobación

    /**
     * La comprobación algebraica: del resultado a la entrada, con expresiones completas. Vacía si hay otros
     * factores además de la cúbica (entonces se muestra la evaluación numérica).
     */
    public List<CadenaIgualdades.Eslabon> cadena() {
        if (!resto.isEmpty()) {
            return List.of();
        }
        List<CadenaIgualdades.Eslabon> l = new ArrayList<>();
        l.add(new CadenaIgualdades.Eslabon(latex()));
        if (campo == Campo.C) {
            l.add(new CadenaIgualdades.Eslabon(latexR(), "z y z̄ son las raíces del factor cuadrático:",
                    coef(c.a3()) + xz() + " = " + cuadratico()));
        }
        Polinomio rc = c.residuoSinConstante(r);
        String menosRc = " - \\left(" + polinomio(rc, false) + "\\right)";
        // (x − r)·Q(x): cada potencia de x con lo que aporta x·Q y lo que aporta −r·Q, sin simplificar
        List<Termino> x2 = new ArrayList<>(terminos(c.coeficienteLineal(r), true));
        x2.add(new Termino(c.a3().negar(), 1));
        List<Termino> x1 = new ArrayList<>(terminos(c.coeficienteConstante(r), false));
        for (Termino t : terminos(c.coeficienteLineal(r).multiplicar(Polinomio.variable(r)), false)) {
            x1.add(new Termino(t.coef().negar(), t.exp()));
        }
        String distribuido = coefMonomio(c.a3(), x + "^{3}") + " + \\left(" + terminosLatex(x2) + "\\right)" + x + "^{2}"
                + " + \\left(" + terminosLatex(x1) + "\\right)" + x + menosRc;
        l.add(new CadenaIgualdades.Eslabon(envolver(distribuido),
                "Se multiplica (x − r) por cada término y se agrupa por potencias de x:", null));
        Polinomio sinConstante = c.polinomio().restar(Polinomio.constante(c.a0()));
        l.add(new CadenaIgualdades.Eslabon(envolver(latex(sinConstante) + menosRc), "Se reducen los paréntesis:", null));
        l.add(new CadenaIgualdades.Eslabon(envolver(latex(c.polinomio())), "Como r es raíz de P, P(r) = 0:",
                polinomio(rc, false) + " = " + racional(c.a0().negar())));
        if (!prefijo.equals(Racional.UNO)) {
            l.add(new CadenaIgualdades.Eslabon(latex(entrada)));
        }
        return l;
    }

    /** Lo que de verdad se calculó: (x − r)·Q = P(x) − P(r) con polinomios en x y r, y que k·P es la entrada. */
    public boolean algebraExacta() {
        return resto.isEmpty() && c.divisionExacta(r) && c.polinomio().multiplicar(prefijo).equals(entrada);
    }

    /** El valor de Cardano de r anula P con 40 cifras. */
    public boolean raizComprobada() {
        return c.raizComprobada();
    }

    // ------------------------------------------------------------------ piezas

    private String p() {
        return "P(" + x + ")";
    }

    private String xr() {
        return "\\left(" + x + " - " + r + "\\right)";
    }

    private String xz() {
        return "\\left(" + x + " - z\\right)\\left(" + x + " - \\bar{z}\\right)";
    }

    /** a₃x² + (a₂ + a₃r)x + (a₃r² + a₂r + a₁), sin corchetes. */
    private String cuadratico() {
        return sumando(Polinomio.constante(c.a3()), x + "^{2}", true) + sumando(c.coeficienteLineal(r), String.valueOf(x),
                false, true) + sumando(c.coeficienteConstante(r), "", false);
    }

    private String corchete() {
        return "\\left[" + cuadratico() + "\\right]";
    }

    /** (−(a₂ + a₃r) ± i√(−Δ₂))/(2a₃), con el denominador positivo. */
    private String zFraccion() {
        Polinomio b = c.coeficienteLineal(r);
        Racional dos = c.a3().multiplicar(Racional.de(2));
        boolean positivo = dos.signo() > 0;
        String menosB = terminos(b, true).size() == 1
                ? polinomio(positivo ? b.negar() : b, true)
                : (positivo ? "-" : "") + "\\left(" + polinomio(b, true) + "\\right)";
        String num = menosB + " \\pm i\\sqrt{" + polinomio(c.discriminanteCociente(r).negar(), false) + "}";
        Racional den = positivo ? dos : dos.negar();
        return den.equals(Racional.UNO) ? num : "\\frac{" + num + "}{" + racional(den) + "}";
    }

    private String envolver(String interior) {
        if (prefijo.equals(Racional.UNO)) {
            return interior;
        }
        return coef(prefijo) + "\\left[" + interior + "\\right]";
    }

    private String latexResto() {
        StringBuilder sb = new StringBuilder();
        for (FormaExtendida.Factor f : resto) {
            PolinomioAlgebraico q = f.polinomio();
            boolean monomio = q.coeficientes().stream().filter(k -> !k.esCero()).count() == 1;
            String t = ImpresoraLatex.imprimir(q.aExpresion());
            sb.append(monomio ? t : "\\left(" + t + "\\right)");
            if (f.multiplicidad() > 1) {
                sb.append("^{").append(f.multiplicidad()).append('}');
            }
        }
        return sb.toString();
    }

    String aproximacion() {
        return redondeo(c.valor(MC), 4);
    }

    private BigDecimal evaluar(Polinomio enR) {
        BigDecimal rv = c.valor(MC);
        BigDecimal s = BigDecimal.ZERO;
        for (Termino t : terminos(enR, false)) {
            s = s.add(decimal(t.coef()).multiply(rv.pow(t.exp(), MC), MC), MC);
        }
        return s;
    }

    private static String redondeo(BigDecimal v, int decimales) {
        return v.setScale(decimales, RoundingMode.HALF_EVEN).toPlainString();
    }

    private static BigDecimal decimal(Racional q) {
        return new BigDecimal(q.num()).divide(new BigDecimal(q.den()), MC);
    }

    // ------------------------------------------------------------------ LaTeX de polinomios en r

    private record Termino(Racional coef, int exp) {
    }

    private List<Termino> terminos(Polinomio p, boolean ascendente) {
        List<Termino> l = new ArrayList<>();
        for (var e : p.terminos().entrySet()) {
            Monomio m = e.getKey();
            l.add(new Termino(e.getValue(), m.grado()));
        }
        l.sort((a, b) -> ascendente ? Integer.compare(a.exp(), b.exp()) : Integer.compare(b.exp(), a.exp()));
        return l;
    }

    private String polinomio(Polinomio p, boolean ascendente) {
        List<Termino> l = terminos(p, ascendente);
        return l.isEmpty() ? "0" : terminosLatex(l);
    }

    private String terminosLatex(List<Termino> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            Termino t = l.get(i);
            boolean negativo = t.coef().signo() < 0;
            if (i == 0) {
                sb.append(negativo ? "-" : "");
            } else {
                sb.append(negativo ? " - " : " + ");
            }
            sb.append(cuerpo(t));
        }
        return sb.toString();
    }

    private String cuerpo(Termino t) {
        Racional a = t.coef().signo() < 0 ? t.coef().negar() : t.coef();
        if (t.exp() == 0) {
            return racional(a);
        }
        String letra = t.exp() == 1 ? String.valueOf(r) : r + "^{" + t.exp() + "}";
        return (a.equals(Racional.UNO) ? "" : racional(a)) + letra;
    }

    private String sumando(Polinomio coefR, String parteX, boolean primero) {
        return sumando(coefR, parteX, primero, false);
    }

    /** "+ (8 + 3r)x", "− x", "+ 3rx"; vacío si el coeficiente es 0. */
    private String sumando(Polinomio coefR, String parteX, boolean primero, boolean ascendente) {
        List<Termino> l = terminos(coefR, ascendente);
        if (l.isEmpty()) {
            return "";
        }
        if (l.size() > 1) {
            return (primero ? "" : " + ") + "\\left(" + terminosLatex(l) + "\\right)" + parteX;
        }
        Termino t = l.getFirst();
        boolean negativo = t.coef().signo() < 0;
        String signo = primero ? (negativo ? "-" : "") : (negativo ? " - " : " + ");
        String cuerpo = cuerpo(t);
        if (!parteX.isEmpty() && t.exp() == 0 && cuerpo.equals("1")) {
            cuerpo = "";
        }
        return signo + cuerpo + parteX;
    }

    private static String coefMonomio(Racional a, String parteX) {
        return coef(a) + parteX;
    }

    /** El coeficiente delante de un producto: nada si es 1, "−" si es −1. */
    private static String coef(Racional k) {
        if (k.equals(Racional.UNO)) {
            return "";
        }
        if (k.equals(Racional.UNO.negar())) {
            return "-";
        }
        return racional(k);
    }

    static String racional(Racional q) {
        boolean negativo = q.signo() < 0;
        Racional a = negativo ? q.negar() : q;
        String cuerpo = a.esEntero() ? a.num().toString() : "\\frac{" + a.num() + "}{" + a.den() + "}";
        return (negativo ? "-" : "") + cuerpo;
    }

    private static String latex(Polinomio p) {
        return ImpresoraLatex.imprimir(p.aExpresion());
    }
}
