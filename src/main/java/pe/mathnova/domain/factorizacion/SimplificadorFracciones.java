package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Negacion;

/**
 * P01 (HU09): simplificar una fracción algebraica. Se factorizan numerador y denominador con el
 * motor, se cancelan los factores comunes y se listan las restricciones x ≠ … del denominador
 * original (RN14). Si lo que queda es impropio (grado de arriba ≥ grado de abajo), P02 muestra
 * además la división: cociente + resto/divisor.
 */
public final class SimplificadorFracciones {

    private final OrquestadorFactorizacion motor;

    public SimplificadorFracciones(OrquestadorFactorizacion motor) {
        this.motor = Objects.requireNonNull(motor, "motor");
    }

    /** Un cociente de polinomios con denominador no constante (también −(a/b)). */
    public static boolean esFraccion(Expresion e) {
        Cociente c = cociente(e);
        if (c == null) {
            return false;
        }
        try {
            Polinomio.desde(c.numerador());
            return !Polinomio.desde(c.denominador()).esConstante();
        } catch (NoEsPolinomioException | ArithmeticException noEs) {
            return false;
        }
    }

    private static Cociente cociente(Expresion e) {
        Expresion x = e;
        while (x instanceof Negacion n) {
            x = n.argumento();
        }
        return x instanceof Cociente c ? c : null;
    }

    private static boolean negativa(Expresion e) {
        boolean neg = false;
        Expresion x = e;
        while (x instanceof Negacion n) {
            neg = !neg;
            x = n.argumento();
        }
        return neg;
    }

    /** @throws IllegalArgumentException si no es una fracción de polinomios */
    public Simplificacion simplificar(Expresion e) {
        if (!esFraccion(e)) {
            throw new IllegalArgumentException("No es una fracción de polinomios");
        }
        Cociente c = cociente(e);
        Resolucion num = motor.resolver(c.numerador(), Campo.Q);
        Resolucion den = motor.resolver(c.denominador(), Campo.Q);
        FormaFactorizada fn = num.resultado();
        FormaFactorizada fd = den.resultado();
        if (fd.constante().esCero()) {
            throw new ArithmeticException("El denominador es cero");
        }

        Map<Polinomio, Integer> arriba = multiconjunto(fn);
        Map<Polinomio, Integer> abajo = multiconjunto(fd);
        List<Factor> cancelados = new ArrayList<>();
        for (Polinomio f : new ArrayList<>(arriba.keySet())) {
            int comun = Math.min(arriba.get(f), abajo.getOrDefault(f, 0));
            if (comun > 0) {
                cancelados.add(new Factor(f, comun));
                arriba.merge(f, -comun, Integer::sum);
                abajo.merge(f, -comun, Integer::sum);
            }
        }
        arriba.values().removeIf(m -> m == 0);
        abajo.values().removeIf(m -> m == 0);

        Racional k = fn.constante().dividir(fd.constante());
        if (negativa(e)) {
            k = k.negar();
        }
        FormaFactorizada formaArriba = FormaFactorizada.normalizada(k, lista(arriba));
        FormaFactorizada formaAbajo = FormaFactorizada.normalizada(Racional.UNO, lista(abajo));

        String justificacion = cancelados.isEmpty()
                ? "Numerador y denominador no tienen factores comunes: la fracción ya está simplificada"
                : "Se cancela" + (cancelados.size() > 1 ? "n " : " ") + texto(cancelados)
                        + ", que está arriba y abajo";
        return new Simplificacion(e, num, den, formaArriba, formaAbajo, cancelados, restricciones(fd),
                justificacion, division(formaArriba, formaAbajo));
    }

    private static Map<Polinomio, Integer> multiconjunto(FormaFactorizada f) {
        Map<Polinomio, Integer> m = new LinkedHashMap<>();
        f.factores().forEach(x -> m.merge(x.polinomio(), x.multiplicidad(), Integer::sum));
        return m;
    }

    private static List<Factor> lista(Map<Polinomio, Integer> m) {
        List<Factor> r = new ArrayList<>();
        m.forEach((p, k) -> r.add(new Factor(p, k)));
        return r;
    }

    /** "(x − 2)" o "(x − 2) y (x + 1)²". */
    private static String texto(List<Factor> fs) {
        List<String> t = fs.stream().map(f -> Formato.parentesis(f.polinomio())
                + (f.multiplicidad() > 1 ? Formato.superindice(f.multiplicidad()) : "")).toList();
        return t.size() == 1 ? t.getFirst() : String.join(", ", t.subList(0, t.size() - 1)) + " y " + t.getLast();
    }

    /**
     * RN14: cada factor lineal (qx − p) del denominador original da x ≠ p/q; los de grado mayor
     * con raíces reales posibles quedan como "f ≠ 0". En el formato del banco: "x ≠ -3".
     */
    static List<String> restricciones(FormaFactorizada denominador) {
        List<Racional> valores = new ArrayList<>();
        List<String> otras = new ArrayList<>();
        char letra = 'x';
        for (Factor f : denominador.factores()) {
            Polinomio p = f.polinomio();
            if (p.variables().size() != 1) {
                otras.add(p + " ≠ 0");
                continue;
            }
            letra = p.variables().first();
            if (p.grado() == 1) {
                Racional a = p.coeficiente(Monomio.de(letra, 1));
                valores.add(p.constante().negar().dividir(a));
            } else if (p.grado() == 2 && Cuadratica.de(p).discriminante().signum() < 0) {
                continue; // nunca se anula en los reales
            } else {
                otras.add(p + " ≠ 0");
            }
        }
        valores.sort(Comparator.naturalOrder());
        List<String> r = new ArrayList<>();
        char v = letra;
        valores.stream().distinct().forEach(x -> r.add(v + " ≠ " + x));
        r.addAll(otras);
        return r;
    }

    /** P02: si arriba tiene grado ≥ abajo, "cociente + resto/divisor"; null si no hace falta. */
    private static String division(FormaFactorizada arriba, FormaFactorizada abajo) {
        if (abajo.factores().isEmpty()) {
            return null;
        }
        Polinomio n = arriba.expandir();
        Polinomio d = abajo.expandir();
        if (n.variables().size() > 1 || d.variables().size() > 1 || n.grado() < d.grado()) {
            return null;
        }
        Univariado un = Univariado.de(n).orElseThrow();
        Univariado ud = Univariado.de(d).orElseThrow();
        if (un.variable() != ud.variable() && !n.esConstante()) {
            return null;
        }
        Univariado[] qr = un.dividir(ud);
        // En el formato del parser, para poder dibujarlo: "x + 1/(x + 1)".
        String cociente = qr[0].aPolinomio().toString();
        if (qr[1].esCero()) {
            return cociente;
        }
        Polinomio resto = qr[1].aPolinomio();
        String signo = " + ";
        if (Contenido.principal(resto).signo() < 0) {
            signo = " - ";
            resto = resto.negar();
        }
        String r = resto.terminos().size() > 1 ? "(" + resto + ")" : resto.toString();
        String inicio = qr[0].esCero() ? (signo.equals(" - ") ? "-" : "") : cociente + signo;
        return inicio + r + "/(" + d + ")";
    }
}
