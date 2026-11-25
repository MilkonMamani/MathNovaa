package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.numeros.NumeroAlgebraico;

public record Asintotas(char variable, List<NumeroAlgebraico> verticales, Optional<Racional> horizontal,
                        Optional<Recta> oblicua, List<String> notas) {

    public record Recta(Racional pendiente, Racional ordenada) {

        public String texto(char x) {
            Polinomio p = Polinomio.termino(pendiente, Monomio.de(x, 1)).sumar(Polinomio.constante(ordenada));
            return "y = " + p;
        }
    }

    public Asintotas {
        verticales = List.copyOf(verticales);
        notas = List.copyOf(notas);
    }

    public List<String> textoVerticales() {
        return verticales.stream().map(v -> variable + " = " + ImpresoraTexto.imprimir(v.aExpresion())).toList();
    }

    public Optional<String> textoHorizontal() {
        return horizontal.map(h -> "y = " + h);
    }

    public Optional<String> textoOblicua() {
        return oblicua.map(r -> r.texto(variable));
    }

    public boolean ninguna() {
        return verticales.isEmpty() && horizontal.isEmpty() && oblicua.isEmpty();
    }

    public static Optional<Asintotas> de(Simplificacion s) {
        Polinomio arriba = s.arriba().expandir();
        Polinomio abajo = s.abajo().expandir();
        Set<Character> letras = new TreeSet<>(s.numerador().resultado().expandir().variables());
        letras.addAll(s.denominador().resultado().expandir().variables());
        if (letras.size() != 1) {
            return Optional.empty();
        }
        char x = letras.iterator().next();

        List<NumeroAlgebraico> verticales = new ArrayList<>();
        List<String> notas = new ArrayList<>();
        for (Factor f : s.abajo().factores()) {
            Polinomio p = f.polinomio();
            int g = p.gradoEn(x);
            Racional a = p.coeficiente(Monomio.de(x, g));
            if (g == 1) {
                verticales.add(NumeroAlgebraico.de(p.constante().negar().dividir(a)));
            } else if (g == 2) {
                Racional b = p.coeficiente(Monomio.de(x, 1));
                Racional c = p.constante();
                Racional delta = b.multiplicar(b).restar(Racional.de(4).multiplicar(a).multiplicar(c));
                if (delta.signo() > 0) {
                    NumeroAlgebraico raiz = NumeroAlgebraico.raiz(2, delta);
                    NumeroAlgebraico menosB = NumeroAlgebraico.de(b.negar());
                    Racional dosA = Racional.de(2).multiplicar(a);
                    verticales.add(menosB.sumar(raiz).dividir(dosA));
                    verticales.add(menosB.restar(raiz).dividir(dosA));
                }
            } else if (g > 2) {
                notas.add("Las raíces de " + p + " no se calcularon de forma exacta");
            }
        }
        verticales = verticales.stream().distinct().sorted(Comparator.comparing(v -> v, NumeroAlgebraico::comparar))
                .toList();

        Optional<Racional> horizontal = Optional.empty();
        Optional<Recta> oblicua = Optional.empty();
        if (!abajo.esConstante()) {
            int n = arriba.esCero() ? -1 : arriba.gradoEn(x);
            int m = abajo.gradoEn(x);
            if (n < m) {
                horizontal = Optional.of(Racional.CERO);
            } else if (n == m) {
                horizontal = Optional.of(arriba.coeficiente(Monomio.de(x, n)).dividir(abajo.coeficiente(Monomio.de(x, m))));
            } else if (n == m + 1) {
                Racional[] q = cociente(arriba, abajo, x);
                oblicua = Optional.of(new Recta(q[1], q[0]));
            }
        }
        return Optional.of(new Asintotas(x, verticales, horizontal, oblicua, notas));
    }

    private static Racional[] cociente(Polinomio n, Polinomio d, char x) {
        int gn = n.gradoEn(x);
        int gd = d.gradoEn(x);
        Racional[] resto = new Racional[gn + 1];
        for (int i = 0; i <= gn; i++) {
            resto[i] = n.coeficiente(Monomio.de(x, i));
        }
        Racional principal = d.coeficiente(Monomio.de(x, gd));
        Racional[] q = new Racional[Math.max(gn - gd + 1, 2)];
        java.util.Arrays.fill(q, Racional.CERO);
        for (int k = gn - gd; k >= 0; k--) {
            Racional c = resto[k + gd].dividir(principal);
            q[k] = c;
            for (int j = 0; j <= gd; j++) {
                resto[k + j] = resto[k + j].restar(c.multiplicar(d.coeficiente(Monomio.de(x, j))));
            }
        }
        return q;
    }
}
