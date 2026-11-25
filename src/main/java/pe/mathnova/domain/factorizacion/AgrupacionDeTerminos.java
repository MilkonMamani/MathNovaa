package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * F04: agrupación de términos. Prueba TODAS las particiones del conjunto de términos, primero las
 * de términos seguidos (como antes) y después el resto:
 * <ul>
 *   <li>4 términos: las 3 formas de 2+2; luego las 4 de 3+1 (un trinomio cuadrado perfecto y un
 *       cuadrado, que dejan una diferencia de cuadrados).</li>
 *   <li>5 términos: las 10 de 2+3. 6 términos: las 15 de 2+2+2 y las 10 de 3+3.</li>
 * </ul>
 * En las agrupaciones con factor común, cada grupo se escribe como c·m·(q) y todos deben compartir q.
 * Si ninguna lo logra, se acepta que el q de un grupo sea una diferencia de cuadrados que contiene al
 * de los demás: 2(x⁴ − 1) − 5x(x² − 1), con x⁴ − 1 = (x² + 1)(x² − 1). Solo se muestra la agrupación
 * que funcionó, con los grupos en el orden en que quedaron.
 */
public final class AgrupacionDeTerminos extends MetodoBase {

    public AgrupacionDeTerminos() {
        super("F04");
    }

    /** Solo con 4 términos o más. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.terminos().size() >= 4;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        List<Termino> ts = Termino.de(p);
        int n = ts.size();
        if (n < 4) {
            throw noAplica("Tiene " + n + " términos; la agrupación necesita 4 o más");
        }
        List<String> probadas = new ArrayList<>();
        List<List<List<Integer>>> particiones = particiones(n);
        for (List<List<Integer>> particion : particiones) {
            probadas.add(nombre(particion));
            Optional<ResultadoMetodo> r = conFactorComun(p, ts, particion, false);
            if (r.isPresent()) {
                return r.get();
            }
        }
        if (n == 4) {
            // El suelto al final y al principio, como antes; después en el medio.
            for (int suelto : new int[] {3, 0, 1, 2}) {
                probadas.add(suelto == 0 ? "1+3" : "3+1");
                List<Termino> tres = new ArrayList<>(ts);
                tres.remove(suelto);
                Termino t = ts.get(suelto);
                Optional<ResultadoMetodo> r = switch (suelto) {
                    case 3 -> trinomioMasCuadrado(p, tres, t, false);
                    case 0 -> trinomioMasCuadrado(p, tres, t, true);
                    default -> trinomioMasCuadrado(p, tres, t, false).or(() -> trinomioMasCuadrado(p, tres, t, true));
                };
                if (r.isPresent()) {
                    return r.get();
                }
            }
        }
        for (List<List<Integer>> particion : particiones) {
            Optional<ResultadoMetodo> r = conFactorComun(p, ts, particion, true);
            if (r.isPresent()) {
                return r.get();
            }
        }
        throw noAplica("Ninguna agrupación (" + String.join(", ", probadas.stream().distinct().toList())
                + ") deja un factor común");
    }

    /** Índices de cada grupo; primero las de términos seguidos. */
    static List<List<List<Integer>>> particiones(int n) {
        List<List<List<Integer>>> r = new ArrayList<>();
        switch (n) {
            case 4 -> r.addAll(todas(4, List.of(2, 2)));
            case 5 -> {
                r.add(consecutivos(2, 3));
                r.add(consecutivos(3, 2));
                r.addAll(todas(5, List.of(2, 3)));
            }
            case 6 -> {
                r.add(consecutivos(2, 2, 2));
                r.add(consecutivos(3, 3));
                r.addAll(todas(6, List.of(2, 2, 2)));
                r.addAll(todas(6, List.of(3, 3)));
            }
            default -> {
                if (n % 2 == 0) {
                    r.add(consecutivos(Collections.nCopies(n / 2, 2).stream().mapToInt(i -> i).toArray()));
                }
                if (n % 3 == 0) {
                    r.add(consecutivos(Collections.nCopies(n / 3, 3).stream().mapToInt(i -> i).toArray()));
                }
            }
        }
        return r.stream().distinct().toList();
    }

    /**
     * Todas las particiones de {0..n−1} en grupos con esos tamaños: cada grupo en orden y los grupos
     * ordenados por su primer índice.
     */
    private static List<List<List<Integer>>> todas(int n, List<Integer> tamanos) {
        List<List<List<Integer>>> r = new ArrayList<>();
        repartir(IntStream.range(0, n).boxed().toList(), tamanos, new ArrayList<>(), r);
        return r;
    }

    private static void repartir(List<Integer> libres, List<Integer> tamanos, List<List<Integer>> actual,
                                 List<List<List<Integer>>> r) {
        if (libres.isEmpty()) {
            r.add(List.copyOf(actual));
            return;
        }
        int primero = libres.getFirst();
        List<Integer> resto = libres.subList(1, libres.size());
        for (Integer t : tamanos.stream().distinct().toList()) {
            for (List<Integer> otros : combinaciones(resto, t - 1)) {
                List<Integer> grupo = new ArrayList<>();
                grupo.add(primero);
                grupo.addAll(otros);
                List<Integer> quedan = new ArrayList<>(resto);
                quedan.removeAll(otros);
                List<Integer> tamanosQuedan = new ArrayList<>(tamanos);
                tamanosQuedan.remove(t);
                actual.add(grupo);
                repartir(quedan, tamanosQuedan, actual, r);
                actual.removeLast();
            }
        }
    }

    private static List<List<Integer>> combinaciones(List<Integer> de, int k) {
        List<List<Integer>> r = new ArrayList<>();
        if (k == 0) {
            r.add(List.of());
            return r;
        }
        for (int i = 0; i <= de.size() - k; i++) {
            for (List<Integer> cola : combinaciones(de.subList(i + 1, de.size()), k - 1)) {
                List<Integer> c = new ArrayList<>();
                c.add(de.get(i));
                c.addAll(cola);
                r.add(c);
            }
        }
        return r;
    }

    private static List<List<Integer>> consecutivos(int... tamanos) {
        List<List<Integer>> grupos = new ArrayList<>();
        int i = 0;
        for (int t : tamanos) {
            List<Integer> g = new ArrayList<>();
            for (int k = 0; k < t; k++) {
                g.add(i++);
            }
            grupos.add(g);
        }
        return grupos;
    }

    private static String nombre(List<List<Integer>> particion) {
        return String.join("+", particion.stream().map(g -> String.valueOf(g.size())).toList());
    }

    /** Un grupo escrito como c·m·(q). */
    private record Grupo(List<Termino> terminos, Racional c, Monomio m, Polinomio q) {
    }

    /**
     * Cada grupo es c·m·(q) con el mismo q: p = q·(Σ c·m). Con {@code conDiferencia}, el q de un
     * grupo puede ser una diferencia de cuadrados (a + b)(a − b) donde a ± b es el q común.
     */
    private Optional<ResultadoMetodo> conFactorComun(Polinomio p, List<Termino> ts, List<List<Integer>> particion,
                                                     boolean conDiferencia) {
        List<Grupo> grupos = new ArrayList<>();
        for (List<Integer> indices : particion) {
            List<Termino> grupo = indices.stream().map(ts::get).toList();
            Polinomio g = Termino.suma(grupo);
            Racional c = Contenido.de(g);
            Monomio m = Contenido.monomioComun(g);
            grupos.add(new Grupo(grupo, c, m, Contenido.dividir(g, c, m)));
        }
        List<Polinomio> distintos = grupos.stream().map(Grupo::q).distinct().toList();
        if (!conDiferencia) {
            Polinomio comun = distintos.getFirst();
            if (distintos.size() > 1 || comun.terminos().size() < 2) {
                return Optional.empty();
            }
            Polinomio otro = Polinomio.CERO;
            List<String> lineas = new ArrayList<>();
            for (Grupo g : grupos) {
                otro = otro.sumar(Polinomio.termino(g.c(), g.m()));
                lineas.add(Formato.suma(g.terminos()) + " = " + Formato.productoComun(g.c(), g.m(), g.q()));
            }
            return Optional.of(armar(p, grupos, comun, otro, lineas));
        }
        if (distintos.size() < 2) {
            return Optional.empty();
        }
        for (Polinomio comun : distintos) {
            if (comun.terminos().size() < 2) {
                continue;
            }
            Polinomio otro = Polinomio.CERO;
            List<String> lineas = new ArrayList<>();
            boolean sirve = true;
            for (Grupo g : grupos) {
                String inicio = Formato.suma(g.terminos()) + " = " + Formato.productoComun(g.c(), g.m(), g.q());
                Polinomio cm = Polinomio.termino(g.c(), g.m());
                if (g.q().equals(comun)) {
                    otro = otro.sumar(cm);
                    lineas.add(inicio);
                    continue;
                }
                Optional<Polinomio> conjugado = DiferenciaDeCuadrados.conjugado(g.q(), comun);
                if (conjugado.isEmpty()) {
                    sirve = false;
                    break;
                }
                otro = otro.sumar(cm.multiplicar(conjugado.get()));
                lineas.add(inicio + " = " + Formato.productoComun(g.c(), g.m(), conjugado.get())
                        + Formato.parentesis(comun));
            }
            if (sirve) {
                return Optional.of(armar(p, grupos, comun, otro, lineas));
            }
        }
        return Optional.empty();
    }

    private ResultadoMetodo armar(Polinomio p, List<Grupo> grupos, Polinomio comun, Polinomio otro,
                                  List<String> lineas) {
        List<String> textosGrupo = grupos.stream().map(g -> "(" + Formato.suma(g.terminos()) + ")").toList();
        String lista = textosGrupo.size() == 2
                ? textosGrupo.get(0) + " y " + textosGrupo.get(1)
                : String.join(", ", textosGrupo.subList(0, textosGrupo.size() - 1)) + " y " + textosGrupo.getLast();
        String justificacion = "Los grupos " + lista + " comparten " + Formato.parentesis(comun);
        return resultado(justificacion, p, List.of(Factor.de(comun), Factor.de(otro)),
                new TrabajoIntermedio.Agrupacion(lineas));
    }

    /**
     * 3+1: (trinomio cuadrado perfecto r²) − s² = (r + s)(r − s).
     * 1+3: s² − (trinomio cuadrado perfecto r²) = (s + r)(s − r).
     */
    private Optional<ResultadoMetodo> trinomioMasCuadrado(Polinomio p, List<Termino> tres, Termino suelto,
                                                          boolean sueltoPrimero) {
        Polinomio trinomio = Termino.suma(tres);
        Polinomio cuadrado = sueltoPrimero ? trinomio.negar() : trinomio;
        Termino s2 = sueltoPrimero ? suelto : suelto.negar();
        if (!s2.positivo()) {
            return Optional.empty();
        }
        Optional<Polinomio> r = TrinomioCuadradoPerfecto.raizDe(cuadrado);
        Optional<Termino> s = s2.raiz(2);
        if (r.isEmpty() || s.isEmpty()) {
            return Optional.empty();
        }
        Polinomio rp = r.get();
        Polinomio sp = s.get().aPolinomio();
        List<Factor> factores = sueltoPrimero
                ? List.of(Factor.de(sp.sumar(rp)), Factor.de(sp.restar(rp)))
                : List.of(Factor.de(rp.sumar(sp)), Factor.de(rp.restar(sp)));
        String justificacion = Formato.parentesis(cuadrado) + " = " + Formato.parentesis(rp) + "² y "
                + Formato.termino(s2) + " = " + Formato.base(s.get()) + "²: queda una diferencia de cuadrados";
        String diferencia = sueltoPrimero
                ? Formato.base(s.get()) + "² − " + Formato.parentesis(rp) + "²"
                : Formato.parentesis(rp) + "² − " + Formato.base(s.get()) + "²";
        var trabajo = new TrabajoIntermedio.Agrupacion(List.of(
                Formato.polinomio(cuadrado) + " = " + Formato.parentesis(rp) + "²",
                Formato.polinomio(p) + " = " + diferencia));
        return Optional.of(resultado(justificacion, p, factores, trabajo));
    }

    /** Sprint 9, parte D: orienta sin decir el nombre del método. */
    @Override
    public String pista() {
        return "Mira si al juntar los términos de dos en dos aparece algo que se repite.";
    }
}
