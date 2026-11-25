package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.calculo.FactorComun;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * Cuando un límite, una derivada o una integral necesitan factorizar (L02 en un límite; P01, "factorizar y
 * simplificar" antes de derivar o integrar un cociente con un factor común; fracciones parciales), la Pizarra
 * separa las dos cosas para que el alumno distinga el ejercicio del método:
 * <ol>
 *   <li>"Factorizar el numerador con Ruffini": un paso auxiliar ({@link #AUXILIAR}) con la igualdad
 *       x³ − 3x − 2 = (x − 2)(x + 1)² y, como subpasos, los métodos que usó el motor;</li>
 *   <li>"Sustituir en el límite / la derivada / la integral" ({@link #SUSTITUIR}): el ejercicio completo con la
 *       expresión factorizada en su lugar (el factor que se cancela primero) y, en el renglón siguiente, la
 *       cancelación con su restricción (x ≠ 3);</li>
 *   <li>y después, los pasos del motor tal cual (la evaluación, la integración…).</li>
 * </ol>
 * Todo sale de los pasos reales: la igualdad resumen compone los resultados de los subpasos (el factor que
 * factoriza cada uno se reemplaza en el resultado del anterior). Sin JavaFX.
 */
final class ReorganizarPasos {

    /** Un paso auxiliar de factorización; sus hijos son los métodos (F…). */
    static final String AUXILIAR = "AUX";
    /** El ejercicio vuelto a escribir con la expresión factorizada. */
    static final String SUSTITUIR = "SUST";
    /** L02: "Factorizar y cancelar". */
    private static final String CANCELAR = "L02";

    private ReorganizarPasos() {
    }

    static List<VistaPaso> de(List<VistaPaso> pasos, RenglonesPaso.Marco m) {
        if (m.operacion() == RenglonesPaso.Operacion.GENERAL) {
            return pasos;
        }
        List<VistaPaso> r = new ArrayList<>();
        for (int i = 0; i < pasos.size(); i++) {
            VistaPaso p = pasos.get(i);
            if (cancela(p, m) && p.antes() instanceof Cociente c) {
                // 1 → Factorizar (una vez por parte), 2 → Sustituir en la operación y cancelar, 3 → la operación
                Map<String, List<VistaPaso>> partes = porParte(p.hijos());
                Expresion num = c.numerador();
                Expresion den = c.denominador();
                Map<String, Expresion[]> igualdades = new LinkedHashMap<>();
                for (var e : partes.entrySet()) {
                    Expresion antes = e.getValue().getFirst().antes();
                    Expresion despues = componer(e.getValue());
                    igualdades.put(e.getKey(), new Expresion[] {antes, despues});
                    if (e.getKey().equals("denominador")) {
                        den = despues;
                    } else {
                        num = despues;
                    }
                }
                // El factor que se cancela va primero arriba y abajo: x² − 9 = (x − 3)(x + 3)
                Expresion[] ordenados = comunesPrimero(num, den);
                for (var e : partes.entrySet()) {
                    boolean abajo = e.getKey().equals("denominador");
                    // Un dibujo (aspa, Ruffini) escribe los factores en su propio orden: ese se respeta
                    Expresion despues = sinDibujo(e.getValue()) ? (abajo ? ordenados[1] : ordenados[0])
                            : igualdades.get(e.getKey())[1];
                    if (abajo) {
                        den = despues;
                    } else {
                        num = despues;
                    }
                    r.add(auxiliar(e.getKey(), e.getValue(), igualdades.get(e.getKey())[0], despues));
                }
                if (partes.isEmpty()) {
                    num = ordenados[0];
                    den = ordenados[1];
                }
                String titulo = switch (m.operacion()) {
                    case LIMITE -> "Sustituir en el límite";
                    case DERIVADA -> "Sustituir en la derivada";
                    default -> "Sustituir en la integral";
                };
                String porque = m.operacion() == RenglonesPaso.Operacion.INTEGRAL
                        ? p.justificacion() + ". La integral se calcula en cada intervalo del dominio"
                        : p.justificacion();
                List<String> restricciones = restricciones(p);
                r.add(new VistaPaso(0, SUSTITUIR, titulo, titulo, porque, new Cociente(num, den),
                        restricciones.isEmpty() ? null : new TrabajoIntermedio.Agrupacion(restricciones),
                        p.despues()));
                continue;
            }
            if (m.operacion() == RenglonesPaso.Operacion.INTEGRAL && factorizaciones(p) && p.antes() != null
                    && p.despues() != null) {
                Map<String, List<VistaPaso>> partes = porParte(p.hijos());
                String parte = partes.size() == 1 ? partes.keySet().iterator().next() : "denominador";
                r.add(auxiliar(parte, p.hijos(), p.antes(), p.despues()));
                VistaPaso siguiente = i + 1 < pasos.size() ? pasos.get(i + 1) : null;
                if (siguiente != null && siguiente.antes() instanceof Cociente c && c.denominador().equals(p.antes())) {
                    r.add(new VistaPaso(0, SUSTITUIR, "Sustituir en la integral", "Sustituir en la integral",
                            "El denominador se escribe factorizado; la integral no cambia", c, null,
                            new Cociente(c.numerador(), p.despues())));
                }
                continue;
            }
            r.add(p);
        }
        List<VistaPaso> numerados = new ArrayList<>();
        for (VistaPaso p : r) {
            numerados.add(renumerar(p, numerados.size() + 1));
        }
        return numerados;
    }

    /**
     * "Factorizar y cancelar" antes de la operación: L02 en un límite, P01 ({@link FactorComun}) en una derivada
     * o una integral. Si el ejercicio ya venía factorizado no hay métodos: se sustituye directamente.
     */
    private static boolean cancela(VistaPaso p, RenglonesPaso.Marco m) {
        boolean codigo = switch (m.operacion()) {
            case LIMITE -> CANCELAR.equals(p.codigo());
            case DERIVADA, INTEGRAL -> FactorComun.SIMPLIFICAR.equals(p.codigo());
            default -> false;
        };
        return codigo && (p.hijos().isEmpty() || factorizaciones(p));
    }

    /** "x ≠ −2", "x ≠ 3": los renglones del motor que dicen dónde vale la cancelación. */
    static List<String> restricciones(VistaPaso p) {
        if (!(p.trabajo() instanceof TrabajoIntermedio.Agrupacion a)) {
            return List.of();
        }
        return a.grupos().stream().filter(l -> l.contains("≠") && !l.contains("=")).toList();
    }

    /**
     * El numerador y el denominador factorizados con los factores comunes delante, en el mismo orden arriba y
     * abajo: ((x + 3)(x − 3))/(x − 3) se escribe ((x − 3)(x + 3))/(x − 3). Solo cambia el orden.
     */
    static Expresion[] comunesPrimero(Expresion num, Expresion den) {
        List<Expresion> arriba = factores(num);
        List<Expresion> abajo = factores(den);
        List<Expresion> comunes = arriba.stream().filter(f -> abajo.stream().anyMatch(g -> base(g).equals(base(f))))
                .toList();
        return new Expresion[] {ordenar(num, arriba, comunes), ordenar(den, abajo, comunes)};
    }

    /** Un solo método cuya igualdad escribe la Pizarra (no un dibujo con su propio orden de factores). */
    private static boolean sinDibujo(List<VistaPaso> metodos) {
        return metodos.size() == 1 && (metodos.getFirst().trabajo() == null
                || metodos.getFirst().trabajo() instanceof TrabajoIntermedio.Comparacion);
    }

    private static List<Expresion> factores(Expresion e) {
        return e instanceof Producto p ? p.factores() : List.of(e);
    }

    private static Expresion base(Expresion f) {
        return f instanceof Potencia p ? p.base() : f;
    }

    private static Expresion ordenar(Expresion e, List<Expresion> fs, List<Expresion> comunes) {
        if (!(e instanceof Producto)) {
            return e;
        }
        List<Expresion> r = new ArrayList<>();
        fs.stream().filter(f -> f instanceof Constante).forEach(r::add);
        for (Expresion c : comunes) {
            fs.stream().filter(f -> !(f instanceof Constante) && base(f).equals(base(c)) && !r.contains(f))
                    .forEach(r::add);
        }
        fs.stream().filter(f -> !r.contains(f)).forEach(r::add);
        return new Producto(r);
    }

    /** Todos los hijos son métodos de factorización (F01…F30). */
    private static boolean factorizaciones(VistaPaso p) {
        return !p.hijos().isEmpty() && p.hijos().stream().allMatch(h -> h.codigo().matches("F\\d+"));
    }

    /** Los subpasos agrupados por la parte que factorizan ("numerador", "denominador"), en orden. */
    static Map<String, List<VistaPaso>> porParte(List<VistaPaso> hijos) {
        Map<String, List<VistaPaso>> r = new LinkedHashMap<>();
        for (VistaPaso h : hijos) {
            r.computeIfAbsent(parte(h.titulo()), k -> new ArrayList<>()).add(h);
        }
        return r;
    }

    /** "numerador" de "Ruffini · numerador"; "expresión" si no lo dice. */
    static String parte(String titulo) {
        int k = titulo.lastIndexOf(" · ");
        return k < 0 ? "expresión" : titulo.substring(k + 3).strip();
    }

    /** "Ruffini" de "Ruffini · numerador". */
    static String metodo(String titulo) {
        int k = titulo.lastIndexOf(" · ");
        return k < 0 ? titulo : titulo.substring(0, k).strip();
    }

    private static VistaPaso auxiliar(String parte, List<VistaPaso> metodos, Expresion antes, Expresion despues) {
        String titulo = titulo(parte, metodos.stream().map(h -> metodo(h.titulo())).distinct().toList());
        StringBuilder porque = new StringBuilder();
        for (VistaPaso h : metodos) {
            if (!porque.isEmpty()) {
                porque.append(' ');
            }
            porque.append(h.justificacion().strip());
            if (!h.justificacion().strip().endsWith(".")) {
                porque.append('.');
            }
        }
        List<VistaPaso> hijos = new ArrayList<>();
        for (VistaPaso h : metodos) {
            if (metodos.size() == 1 && h.despues() != null && despues != null && !h.despues().equals(despues)
                    && juntar(h.despues()) instanceof Producto && despues instanceof Producto) {
                // Un solo método: su igualdad es la del resumen, con los factores en el mismo orden (el que se
                // cancela primero)
                h = new VistaPaso(h.orden(), h.codigo(), h.titulo(), h.ayuda(), h.justificacion(), h.antes(),
                        h.trabajo(), despues, h.grupo(), h.hijos(), h.desarrollo());
            }
            hijos.add(renumerar(h, hijos.size() + 1));
        }
        return new VistaPaso(0, AUXILIAR, titulo, titulo, porque.toString(), antes, null, despues, null, hijos);
    }

    /** "Factorizar el numerador con Ruffini y trinomio cuadrado perfecto", "… por diferencia de cuadrados". */
    static String titulo(String parte, List<String> metodos) {
        String que = switch (parte) {
            case "numerador" -> "el numerador";
            case "denominador" -> "el denominador";
            default -> "la expresión";
        };
        List<String> nombres = metodos.stream().map(ReorganizarPasos::minuscula).toList();
        String lista = nombres.size() == 1 ? nombres.getFirst()
                : String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.getLast();
        String prep = nombres.getFirst().startsWith("Ruffini") || nombres.getFirst().startsWith("aspa") ? "con" : "por";
        return "Factorizar " + que + " " + prep + " " + lista;
    }

    private static String minuscula(String s) {
        if (s.isEmpty() || s.startsWith("Ruffini") || s.startsWith("Cardano")) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * La factorización completa de una parte: el resultado del primer método y, en él, cada factor que
     * factorizó un método siguiente reemplazado por su resultado; los factores iguales se juntan en una potencia:
     * (x − 2)(x² + 2x + 1) con x² + 2x + 1 = (x + 1)² da (x − 2)(x + 1)².
     */
    static Expresion componer(List<VistaPaso> metodos) {
        Expresion r = metodos.getFirst().despues();
        for (int i = 1; i < metodos.size(); i++) {
            VistaPaso h = metodos.get(i);
            if (h.antes() != null && h.despues() != null) {
                r = reemplazar(r, h.antes(), h.despues());
            }
        }
        return juntar(r);
    }

    static Expresion reemplazar(Expresion e, Expresion que, Expresion por) {
        if (e.equals(que)) {
            return por;
        }
        return switch (e) {
            case Producto p -> new Producto(p.factores().stream().map(f -> reemplazar(f, que, por)).toList());
            case Potencia p -> new Potencia(reemplazar(p.base(), que, por), p.exponente());
            case Negacion n -> new Negacion(reemplazar(n.argumento(), que, por));
            default -> e;
        };
    }

    /** Un producto plano con los factores iguales juntos: (x + 1)(x + 1)(x − 1) → (x + 1)²(x − 1). */
    static Expresion juntar(Expresion e) {
        if (e instanceof Negacion n) {
            return new Negacion(juntar(n.argumento()));
        }
        if (!(e instanceof Producto)) {
            return e;
        }
        Map<Expresion, Integer> bases = new LinkedHashMap<>();
        List<Expresion> constantes = new ArrayList<>();
        aplanar(e, bases, constantes);
        List<Expresion> f = new ArrayList<>(constantes);
        bases.forEach((b, k) -> f.add(k == 1 ? b : new Potencia(b, Constante.de(k))));
        return f.size() == 1 ? f.getFirst() : new Producto(f);
    }

    private static void aplanar(Expresion e, Map<Expresion, Integer> bases, List<Expresion> constantes) {
        switch (e) {
            case Producto p -> p.factores().forEach(x -> aplanar(x, bases, constantes));
            case Constante c -> constantes.add(c);
            case Potencia p when p.exponente() instanceof Constante k && k.valor().esEntero()
                    && k.valor().signo() > 0 -> bases.merge(p.base(), k.valor().num().intValueExact(), Integer::sum);
            default -> bases.merge(e, 1, Integer::sum);
        }
    }

    private static VistaPaso renumerar(VistaPaso p, int orden) {
        return new VistaPaso(orden, p.codigo(), p.titulo(), p.ayuda(), p.justificacion(), p.antes(), p.trabajo(),
                p.despues(), p.grupo(), p.hijos(), p.desarrollo());
    }
}
