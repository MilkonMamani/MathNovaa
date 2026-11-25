package pe.mathnova.domain.calculo.integrales;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaFactorizada;

final class FraccionesParciales {

    private FraccionesParciales() {
    }

    record Division(Polinomio cociente, Polinomio resto) {
    }

    static Division dividir(Polinomio a, Polinomio b, char x) {
        Polinomio q = Polinomio.CERO;
        Polinomio r = a;
        int m = b.gradoEn(x);
        Racional principal = b.coeficiente(Monomio.de(x, m));
        int vueltas = 0;
        while (!r.esCero() && r.gradoEn(x) >= m && vueltas++ < 200) {
            int n = r.gradoEn(x);
            Racional c = r.coeficiente(Monomio.de(x, n)).dividir(principal);
            Polinomio t = Polinomio.termino(c, Monomio.de(x, n - m));
            q = q.sumar(t);
            r = r.restar(t.multiplicar(b));
        }
        return new Division(q, r);
    }

    record Sumando(Polinomio factor, int potencia, boolean cuadratico, List<String> letras) {
    }

    record Descomposicion(List<Sumando> sumandos, String planteo, String identidad, List<String[]> ecuaciones,
                          Map<String, Racional> valores, List<String> integrales, Expresion integral) {
    }

    static Optional<Descomposicion> descomponer(Polinomio numerador, FormaFactorizada forma, char x) {
        List<Factor> factores = new ArrayList<>(forma.factores().stream()
                .filter(f -> f.polinomio().gradoEn(x) > 0).toList());
        for (Factor f : factores) {
            int g = f.polinomio().gradoEn(x);
            if (g > 2 || g == 2 && (f.multiplicidad() > 1 || discriminante(f.polinomio(), x).signo() >= 0)) {
                return Optional.empty();
            }
        }
        factores.sort(Comparator.comparingInt((Factor f) -> f.polinomio().gradoEn(x))
                .thenComparing(f -> f.polinomio().constante().dividir(f.polinomio().coeficiente(
                        Monomio.de(x, f.polinomio().gradoEn(x))))));
        List<Sumando> sumandos = new ArrayList<>();
        char letra = 'A';
        for (Factor f : factores) {
            boolean cuad = f.polinomio().gradoEn(x) == 2;
            for (int j = 1; j <= (cuad ? 1 : f.multiplicidad()); j++) {
                List<String> letras = cuad ? List.of(String.valueOf(letra++), String.valueOf(letra++))
                        : List.of(String.valueOf(letra++));
                sumandos.add(new Sumando(f.polinomio(), j, cuad, letras));
            }
        }
        Polinomio dSinConstante = Polinomio.UNO;
        for (Factor f : factores) {
            dSinConstante = dSinConstante.multiplicar(f.polinomio().potencia(f.multiplicidad()));
        }
        Polinomio izquierda = numerador.multiplicar(forma.constante().inverso());
        int n = dSinConstante.gradoEn(x);
        List<String> nombres = new ArrayList<>();
        List<Polinomio> columnas = new ArrayList<>();
        List<String> identidad = new ArrayList<>();
        for (Sumando s : sumandos) {
            Polinomio resto = dividir(dSinConstante, s.factor().potencia(s.potencia()), x).cociente();
            String otros = resto.equals(Polinomio.UNO) ? "" : envolverProducto(resto, x);
            if (s.cuadratico()) {
                nombres.add(s.letras().get(0));
                columnas.add(resto.multiplicar(Polinomio.variable(x)));
                nombres.add(s.letras().get(1));
                columnas.add(resto);
                identidad.add("(" + s.letras().get(0) + x + " + " + s.letras().get(1) + ")" + otros);
            } else {
                nombres.add(s.letras().getFirst());
                columnas.add(resto);
                identidad.add(s.letras().getFirst() + otros);
            }
        }
        int m = nombres.size();
        Racional[][] a = new Racional[n][m + 1];
        List<String[]> ecuaciones = new ArrayList<>();
        for (int fila = 0; fila < n; fila++) {
            int grado = n - 1 - fila;
            StringBuilder lado = new StringBuilder();
            for (int k = 0; k < m; k++) {
                Racional c = columnas.get(k).coeficiente(Monomio.de(x, grado));
                a[fila][k] = c;
                agregarTermino(lado, c, nombres.get(k));
            }
            a[fila][m] = izquierda.coeficiente(Monomio.de(x, grado));
            ecuaciones.add(new String[] {grado == 0 ? "1" : ImpresoraPizarra.imprimir(
                    Simplificador.potencia(new Variable(x), Constante.de(grado))),
                    (lado.isEmpty() ? "0" : lado.toString()) + " = " + ImpresoraPizarra.numero(a[fila][m])});
        }
        Optional<Racional[]> sol = resolver(a, m);
        if (sol.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Racional> valores = new TreeMap<>();
        for (int k = 0; k < m; k++) {
            valores.put(nombres.get(k), sol.get()[k]);
        }
        List<String> planteo = new ArrayList<>();
        List<String> integrales = new ArrayList<>();
        List<Expresion> partes = new ArrayList<>();
        for (Sumando s : sumandos) {
            String den = denominador(s, x);
            if (s.cuadratico()) {
                planteo.add("(" + s.letras().get(0) + x + " + " + s.letras().get(1) + ")/" + den);
                Racional b = valores.get(s.letras().get(0));
                Racional c = valores.get(s.letras().get(1));
                Expresion i = cuadratico(b, c, s.factor(), x);
                partes.add(i);
                integrales.add("∫(" + numeroLineal(b, c, x) + ")/" + den + " d" + x + " = " + pizarra(i));
            } else {
                planteo.add(s.letras().getFirst() + "/" + den);
                Racional c = valores.get(s.letras().getFirst());
                Expresion i = lineal(c, s.factor(), s.potencia(), x);
                partes.add(i);
                integrales.add("∫" + parentesisSiFraccion(c) + "/" + den + " d" + x + " = " + pizarra(i));
            }
        }
        Expresion total = Simplificador.suma(partes);
        String id = pizarra(Simplificador.simplificar(izquierda.aExpresion())) + " = " + String.join(" + ", identidad);
        return Optional.of(new Descomposicion(sumandos, String.join(" + ", planteo), id, ecuaciones, valores,
                integrales, total));
    }

    private static String pizarra(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private static Racional discriminante(Polinomio q, char x) {
        Racional a = q.coeficiente(Monomio.de(x, 2));
        Racional b = q.coeficiente(Monomio.de(x, 1));
        Racional c = q.constante();
        return b.multiplicar(b).restar(Racional.de(4).multiplicar(a).multiplicar(c));
    }

    private static String texto(Polinomio p) {
        return pizarra(Simplificador.simplificar(p.aExpresion()));
    }

    private static String denominador(Sumando s, char x) {
        String base = s.factor().equals(Polinomio.variable(x)) ? String.valueOf(x) : "(" + texto(s.factor()) + ")";
        return s.potencia() == 1 ? base : base + ImpresoraPizarra.imprimir(
                Simplificador.potencia(new Variable('z'), Constante.de(s.potencia()))).substring(1);
    }

    private static String envolverProducto(Polinomio p, char x) {
        if (p.equals(Polinomio.variable(x))) {
            return String.valueOf(x);
        }
        return "(" + texto(p) + ")";
    }

    private static void agregarTermino(StringBuilder sb, Racional c, String letra) {
        if (c.esCero()) {
            return;
        }
        boolean negativo = c.signo() < 0;
        Racional abs = negativo ? c.negar() : c;
        String coef = abs.equals(Racional.UNO) ? "" : abs.esEntero() ? abs.toString() : "(" + abs + ")";
        if (sb.isEmpty()) {
            sb.append(negativo ? ImpresoraPizarra.MENOS : "").append(coef).append(letra);
        } else {
            sb.append(negativo ? " " + ImpresoraPizarra.MENOS + " " : " + ").append(coef).append(letra);
        }
    }

    private static String parentesisSiFraccion(Racional r) {
        String s = ImpresoraPizarra.numero(r);
        return r.esEntero() && r.signo() >= 0 ? s : "(" + s + ")";
    }

    private static String numeroLineal(Racional b, Racional c, char x) {
        return pizarra(Simplificador.simplificar(Polinomio.termino(b, Monomio.de(x, 1))
                .sumar(Polinomio.constante(c)).aExpresion()));
    }

    private static Optional<Racional[]> resolver(Racional[][] a, int m) {
        int n = a.length;
        int fila = 0;
        int[] pivoteDe = new int[m];
        java.util.Arrays.fill(pivoteDe, -1);
        for (int col = 0; col < m && fila < n; col++) {
            int p = -1;
            for (int i = fila; i < n; i++) {
                if (!a[i][col].esCero()) {
                    p = i;
                    break;
                }
            }
            if (p < 0) {
                continue;
            }
            Racional[] t = a[p];
            a[p] = a[fila];
            a[fila] = t;
            Racional piv = a[fila][col];
            for (int j = 0; j <= m; j++) {
                a[fila][j] = a[fila][j].dividir(piv);
            }
            for (int i = 0; i < n; i++) {
                if (i != fila && !a[i][col].esCero()) {
                    Racional f = a[i][col];
                    for (int j = 0; j <= m; j++) {
                        a[i][j] = a[i][j].restar(f.multiplicar(a[fila][j]));
                    }
                }
            }
            pivoteDe[col] = fila;
            fila++;
        }
        Racional[] r = new Racional[m];
        for (int col = 0; col < m; col++) {
            if (pivoteDe[col] < 0) {
                return Optional.empty();
            }
            r[col] = a[pivoteDe[col]][m];
        }
        for (int i = fila; i < n; i++) {
            if (!a[i][m].esCero()) {
                return Optional.empty();
            }
        }
        return Optional.of(r);
    }

    static Expresion lineal(Racional a, Polinomio p, int j, char x) {
        Racional alfa = p.coeficiente(Monomio.de(x, 1));
        Expresion base = Simplificador.simplificar(p.aExpresion());
        if (j == 1) {
            return Simplificador.producto(List.of(new Constante(a.dividir(alfa)), TablaIntegrales.ln(base)));
        }
        Racional c = a.negar().dividir(alfa.multiplicar(Racional.de(j - 1)));
        return Simplificador.cociente(new Constante(c), Simplificador.potencia(base, Constante.de(j - 1)));
    }

    private static Expresion argumentoArctan(Racional dosAlfa, Racional beta, Racional delta, Expresion raiz, char x) {
        if (!(raiz instanceof Constante s)) {
            return Simplificador.simplificar(Simplificador.cociente(Simplificador.simplificar(
                    Polinomio.termino(dosAlfa, Monomio.de(x, 1)).sumar(Polinomio.constante(beta)).aExpresion()), raiz));
        }
        Racional a = dosAlfa.dividir(s.valor());
        Racional b = beta.dividir(s.valor());
        java.math.BigInteger d = a.den().multiply(b.den()).divide(a.den().gcd(b.den()));
        Racional ea = a.multiplicar(Racional.de(d));
        Racional eb = b.multiplicar(Racional.de(d));
        java.math.BigInteger g = ea.num().gcd(eb.num()).gcd(d);
        if (g.signum() > 0 && !g.equals(java.math.BigInteger.ONE)) {
            ea = ea.dividir(Racional.de(g));
            eb = eb.dividir(Racional.de(g));
            d = d.divide(g);
        }
        Expresion numerador = Simplificador.simplificar(Polinomio.termino(ea, Monomio.de(x, 1))
                .sumar(Polinomio.constante(eb)).aExpresion());
        return d.equals(java.math.BigInteger.ONE) ? numerador : new pe.mathnova.domain.expresion.Cociente(numerador,
                new Constante(Racional.de(d)));
    }

    static Expresion cuadratico(Racional b, Racional c, Polinomio q, char x) {
        Racional alfa = q.coeficiente(Monomio.de(x, 2));
        Racional beta = q.coeficiente(Monomio.de(x, 1));
        Racional gamma = q.constante();
        Racional dosAlfa = alfa.multiplicar(Racional.de(2));
        Expresion base = Simplificador.simplificar(q.aExpresion());
        List<Expresion> partes = new ArrayList<>();
        Racional k1 = b.dividir(dosAlfa);
        if (!k1.esCero()) {
            partes.add(Simplificador.producto(List.of(new Constante(k1), TablaIntegrales.ln(base))));
        }
        Racional k2 = c.restar(b.multiplicar(beta).dividir(dosAlfa));
        if (!k2.esCero()) {
            Racional delta = Racional.de(4).multiplicar(alfa).multiplicar(gamma).restar(beta.multiplicar(beta));
            Expresion raiz = Simplificador.raiz(2, new Constante(delta));
            Expresion argumento = argumentoArctan(dosAlfa, beta, delta, raiz, x);
            Expresion factor = Simplificador.cociente(new Constante(k2.multiplicar(Racional.de(2))), raiz);
            partes.add(Simplificador.simplificar(Simplificador.producto(List.of(factor,
                    new Funcion(TipoFuncion.ARCTAN, argumento)))));
        }
        return partes.isEmpty() ? Constante.de(0) : Simplificador.suma(partes);
    }
}
