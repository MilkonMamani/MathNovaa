package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

public final class Reciproco extends MetodoBase {

    public Reciproco() {
        super("F18");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() >= 4 && p.grado() % 2 == 0;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        int grado = u.grado();
        if (grado < 4 || grado % 2 != 0) {
            throw noAplica("Es de grado " + grado + "; el método del recíproco necesita grado par desde 4");
        }
        if (u.coef(0).esCero() || !u.enteros()) {
            throw noAplica("No tiene término independiente");
        }
        List<String> coefs = new ArrayList<>();
        for (int i = grado; i >= 0; i--) {
            coefs.add(Formato.numero(u.coef(i)));
            if (!u.coef(i).equals(u.coef(grado - i))) {
                throw noAplica("Los coeficientes " + String.join(", ", deMayorAMenor(u))
                        + " no se leen igual de ida y de vuelta: no es recíproco");
            }
        }
        int m = grado / 2;
        char x = u.variable();
        char z = x == 'z' ? 'w' : 'z';

        List<Univariado> t = new ArrayList<>();
        t.add(Univariado.de(z, List.of(Racional.de(2))));
        t.add(Univariado.de(z, List.of(Racional.CERO, Racional.UNO)));
        Univariado zeta = t.get(1);
        for (int k = 2; k <= m; k++) {
            t.add(zeta.multiplicar(t.get(k - 1)).sumar(t.get(k - 2).multiplicar(Racional.UNO.negar())));
        }
        Univariado q = Univariado.de(z, List.of(u.coef(m)));
        for (int k = 1; k <= m; k++) {
            q = q.sumar(t.get(k).multiplicar(u.coef(m + k)));
        }

        List<Racional> raices = new ArrayList<>();
        Univariado resto = q;
        boolean encontro = true;
        while (resto.grado() >= 1 && encontro) {
            encontro = false;
            Optional<List<Racional>> candidatos = RaicesRacionales.candidatos(resto);
            if (candidatos.isEmpty()) {
                break;
            }
            for (Racional r : candidatos.get()) {
                if (resto.evaluar(r).esCero()) {
                    Univariado divisor = Univariado.de(z, List.of(Racional.de(r.num()).negar(), Racional.de(r.den())));
                    resto = resto.cocienteExacto(divisor).orElseThrow();
                    raices.add(r);
                    encontro = true;
                    break;
                }
            }
        }
        String enZ = Formato.polinomio(q.aPolinomio());
        if (raices.isEmpty()) {
            throw noAplica("Con " + z + " = " + x + " + 1/" + x + " queda " + enZ + ", que no tiene raíces racionales");
        }

        List<Factor> factores = new ArrayList<>();
        List<Factor> enZeta = new ArrayList<>();
        for (Racional r : raices) {
            Racional d = Racional.de(r.den());
            Racional n = Racional.de(r.num());
            factores.add(Factor.de(Univariado.de(x, List.of(d, n.negar(), d)).aPolinomio()));
            enZeta.add(Factor.de(Univariado.de(z, List.of(n.negar(), d)).aPolinomio()));
        }
        int j = resto.grado();
        Univariado x2mas1 = Univariado.de(x, List.of(Racional.UNO, Racional.CERO, Racional.UNO));
        Univariado regreso = Univariado.de(x, List.of(Racional.CERO));
        for (int i = 0; i <= j; i++) {
            Univariado termino = potencia(x2mas1, i).multiplicar(monomio(x, j - i)).multiplicar(resto.coef(i));
            regreso = regreso.sumar(termino);
        }
        factores.add(Factor.de(regreso.aPolinomio()));
        enZeta.add(Factor.de(resto.aPolinomio()));

        String dividido = agrupado(u, m, x);
        String factorizadoZ = CambioDeVariable.texto(ordenar(enZeta));
        String regresado = CambioDeVariable.texto(ordenar(factores));
        String xm = Formato.monomio(pe.mathnova.domain.algebra.Monomio.de(x, m));
        var trabajo = new TrabajoIntermedio.Sustitucion(String.valueOf(z), z + " = " + x + " + 1/" + x,
                "Se divide entre " + xm + ": " + dividido + " = " + enZ,
                enZ + " = " + factorizadoZ,
                xm + "·" + factorizadoZ + " = " + regresado);
        String justificacion = "Los coeficientes " + String.join(", ", coefs) + " se leen igual de ida y de vuelta: "
                + "se divide entre " + xm + " y se usa " + z + " = " + x + " + 1/" + x + " (" + x + "² + 1/" + x
                + "² = " + z + "² − 2)";
        return resultado(justificacion, p, factores, trabajo);
    }

    private static List<String> deMayorAMenor(Univariado u) {
        return u.deMayorAMenor().stream().map(Formato::numero).toList();
    }

    private static List<Factor> ordenar(List<Factor> factores) {
        return factores.stream().filter(f -> !(f.polinomio().esConstante()
                && f.polinomio().constante().equals(Racional.UNO))).toList();
    }

    private static String agrupado(Univariado u, int m, char x) {
        StringBuilder sb = new StringBuilder();
        for (int k = m; k >= 1; k--) {
            Racional c = u.coef(m + k);
            if (c.esCero()) {
                continue;
            }
            String par = "(" + potenciaTexto(x, k) + " + 1/" + potenciaTexto(x, k) + ")";
            sb.append(signo(c, sb.isEmpty())).append(coeficiente(c)).append(par);
        }
        Racional c0 = u.coef(m);
        if (!c0.esCero()) {
            sb.append(signo(c0, sb.isEmpty())).append(Formato.numero(c0.signo() < 0 ? c0.negar() : c0));
        }
        return sb.toString();
    }

    private static String signo(Racional c, boolean primero) {
        if (primero) {
            return c.signo() < 0 ? Formato.MENOS : "";
        }
        return c.signo() < 0 ? " " + Formato.MENOS + " " : " + ";
    }

    private static String coeficiente(Racional c) {
        Racional abs = c.signo() < 0 ? c.negar() : c;
        return abs.equals(Racional.UNO) ? "" : Formato.numero(abs);
    }

    private static String potenciaTexto(char x, int k) {
        return k == 1 ? String.valueOf(x) : x + Formato.superindice(k);
    }

    private static Univariado potencia(Univariado b, int n) {
        Univariado r = Univariado.de(b.variable(), List.of(Racional.UNO));
        for (int i = 0; i < n; i++) {
            r = r.multiplicar(b);
        }
        return r;
    }

    private static Univariado monomio(char x, int e) {
        List<Racional> c = new ArrayList<>();
        for (int i = 0; i < e; i++) {
            c.add(Racional.CERO);
        }
        c.add(Racional.UNO);
        return Univariado.de(x, c);
    }

    @Override
    public String pista() {
        return "Los coeficientes se leen igual de izquierda a derecha que de derecha a izquierda.";
    }
}
