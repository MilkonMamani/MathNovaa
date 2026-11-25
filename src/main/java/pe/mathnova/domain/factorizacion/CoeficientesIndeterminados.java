package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

public final class CoeficientesIndeterminados extends MetodoBase {

    private static final String LETRAS_A = "ab";
    private static final String LETRAS_A3 = "abc";

    public CoeficientesIndeterminados() {
        super("F20");
    }

    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() >= 4;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        int n = u.grado();
        if (n < 4) {
            throw noAplica("Es de grado " + n + "; se usa desde grado 4");
        }
        if (!u.enteros() || u.coef(0).esCero()) {
            throw noAplica("No tiene término independiente");
        }
        Optional<Racional> raiz = AspaDobleEspecial.primeraRaiz(u);
        if (raiz.isPresent()) {
            throw noAplica(u.variable() + " = " + Formato.numero(raiz.get()) + " es raíz: corresponde Ruffini");
        }
        BigInteger limite = BigInteger.valueOf(1_000_000L);
        BigInteger p1 = u.evaluar(Racional.UNO).num().abs();
        BigInteger pm1 = u.evaluar(Racional.UNO.negar()).num().abs();
        if (p1.compareTo(limite) > 0 || pm1.compareTo(limite) > 0 || u.coef(0).num().abs().compareTo(limite) > 0
                || u.principal().num().abs().compareTo(limite) > 0) {
            throw noAplica("Los coeficientes son demasiado grandes para buscar los valores");
        }
        Optional<ResultadoMetodo> r = buscar(p, u, 2);
        if (r.isEmpty() && n == 6) {
            r = buscar(p, u, 3);
        }
        return r.orElseThrow(() -> noAplica("El sistema de " + (n + 1)
                + " ecuaciones no tiene solución entera: no hay un factor de grado 2" + (n == 6 ? " ni 3" : "")));
    }

    private Optional<ResultadoMetodo> buscar(Polinomio p, Univariado u, int k) {
        char x = u.variable();
        long an = u.principal().num().longValueExact();
        long a0 = u.coef(0).num().longValueExact();
        long p1 = u.evaluar(Racional.UNO).num().longValueExact();
        long pm1 = u.evaluar(Racional.UNO.negar()).num().longValueExact();
        for (long l : RaicesRacionales.divisores(Math.abs(an))) {
            for (long b0 : AspaDobleEspecial.conSigno(RaicesRacionales.divisores(Math.abs(a0)))) {
                for (long d1 : AspaDobleEspecial.conSigno(RaicesRacionales.divisores(Math.abs(p1)))) {
                    for (long d2 : AspaDobleEspecial.conSigno(RaicesRacionales.divisores(Math.abs(pm1)))) {
                        Optional<Univariado> factor = candidato(x, k, l, b0, d1, d2);
                        if (factor.isEmpty()) {
                            continue;
                        }
                        Optional<Univariado> cof = u.cocienteExacto(factor.get());
                        if (cof.isPresent() && cof.get().enteros()) {
                            return Optional.of(armar(p, u, factor.get(), cof.get()));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Univariado> candidato(char x, int k, long l, long b0, long d1, long d2) {
        if (k == 2) {
            long a = d1 - l - b0;
            if (l - a + b0 != d2) {
                return Optional.empty();
            }
            return Optional.of(Univariado.de(x, List.of(Racional.de(b0), Racional.de(a), Racional.de(l))));
        }
        long suma = d1 - l - b0;
        long resta = d2 + l - b0;
        if ((suma + resta) % 2 != 0) {
            return Optional.empty();
        }
        long a = (suma + resta) / 2;
        long b = (suma - resta) / 2;
        return Optional.of(Univariado.de(x, List.of(Racional.de(b0), Racional.de(b), Racional.de(a), Racional.de(l))));
    }

    private ResultadoMetodo armar(Polinomio p, Univariado u, Univariado factor, Univariado cofactor) {
        char x = u.variable();
        int k = factor.grado();
        int m = cofactor.grado();
        String letrasA = k == 2 ? LETRAS_A : LETRAS_A3;
        List<String> nombresA = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            nombresA.add(String.valueOf(letrasA.charAt(k - 1 - i)));
        }
        List<String> nombresB = new ArrayList<>();
        char siguiente = (char) (letrasA.charAt(k - 1) + 1);
        for (int i = 0; i < m; i++) {
            nombresB.add(String.valueOf((char) (siguiente + (m - 1 - i))));
        }
        List<Simbolo> sa = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            sa.add(new Simbolo(nombresA.get(i), factor.coef(i)));
        }
        sa.add(new Simbolo(null, factor.principal()));
        List<Simbolo> sb = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            sb.add(new Simbolo(nombresB.get(i), cofactor.coef(i)));
        }
        sb.add(new Simbolo(null, cofactor.principal()));

        String planteo = forma(x, sa) + forma(x, sb);
        List<TrabajoIntermedio.SistemaCoeficientes.Ecuacion> ecuaciones = new ArrayList<>();
        for (int e = k + m - 1; e >= 0; e--) {
            List<String> productos = new ArrayList<>();
            for (int i = Math.min(k, e); i >= 0; i--) {
                int j = e - i;
                if (j > m) {
                    continue;
                }
                productos.add(producto(sa.get(i), sb.get(j)));
            }
            String potencia = e == 0 ? "término independiente" : Formato.monomio(pe.mathnova.domain.algebra.Monomio.de(x, e));
            ecuaciones.add(new TrabajoIntermedio.SistemaCoeficientes.Ecuacion(potencia,
                    String.join(" + ", productos) + " = " + Formato.numero(u.coef(e))));
        }
        List<String> solucion = new ArrayList<>();
        for (Simbolo s : sa) {
            if (s.nombre() != null) {
                solucion.add(s.nombre() + " = " + Formato.numero(s.valor()));
            }
        }
        for (Simbolo s : sb) {
            if (s.nombre() != null) {
                solucion.add(s.nombre() + " = " + Formato.numero(s.valor()));
            }
        }
        solucion.sort(null);
        Polinomio fa = factor.aPolinomio();
        Polinomio fb = cofactor.aPolinomio();
        String resultadoTexto = Formato.parentesis(fa) + Formato.parentesis(fb);
        var trabajo = new TrabajoIntermedio.SistemaCoeficientes(planteo, ecuaciones, solucion, resultadoTexto);
        String justificacion = "No tiene raíces racionales: se plantea " + planteo + " y el sistema de ecuaciones "
                + "de los coeficientes tiene la solución entera " + String.join(", ", solucion);
        return resultado(justificacion, p, List.of(Factor.de(fa), Factor.de(fb)), trabajo);
    }

    private record Simbolo(String nombre, Racional valor) {
    }

    private static String forma(char x, List<Simbolo> s) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = s.size() - 1; i >= 0; i--) {
            Simbolo c = s.get(i);
            String potencia = i == 0 ? "" : Formato.monomio(pe.mathnova.domain.algebra.Monomio.de(x, i));
            if (c.nombre() == null) {
                Racional v = c.valor();
                sb.append(v.equals(Racional.UNO) ? "" : Formato.numero(v)).append(potencia);
            } else {
                sb.append(" + ").append(c.nombre()).append(potencia);
            }
        }
        return sb.append(")").toString();
    }

    private static String producto(Simbolo a, Simbolo b) {
        if (a.nombre() == null && b.nombre() == null) {
            return Formato.numero(a.valor().multiplicar(b.valor()));
        }
        if (a.nombre() == null) {
            return coeficiente(a.valor()) + b.nombre();
        }
        if (b.nombre() == null) {
            return coeficiente(b.valor()) + a.nombre();
        }
        return a.nombre() + b.nombre();
    }

    private static String coeficiente(Racional v) {
        if (v.equals(Racional.UNO)) {
            return "";
        }
        if (v.equals(Racional.UNO.negar())) {
            return Formato.MENOS;
        }
        return Formato.numero(v);
    }

    @Override
    public String pista() {
        return "Supón que se parte en dos factores con coeficientes que aún no conoces, multiplícalos e iguala término a término.";
    }
}
