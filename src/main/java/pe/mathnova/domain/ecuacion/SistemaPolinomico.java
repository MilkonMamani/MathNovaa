package pe.mathnova.domain.ecuacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.Anidados;
import pe.mathnova.domain.calculo.Polinomios;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

public final class SistemaPolinomico {

    private static final int PROFUNDIDAD = 12;

    private final OrquestadorFactorizacion orquestador;
    private final ResolvedorEcuaciones ecuaciones;

    public SistemaPolinomico(OrquestadorFactorizacion orquestador) {
        this.orquestador = orquestador;
        this.ecuaciones = new ResolvedorEcuaciones(orquestador);
    }

    public record Resultado(List<Map<Character, Racional>> soluciones, List<PasoLimite> pasos, List<String> avisos,
                            boolean completo) {
    }

    public Resultado resolver(List<Polinomio> sistema, List<Character> incognitas) {
        Estado e = new Estado();
        List<Map<Character, Racional>> r = resolver(sistema, incognitas, e.pasos, e, 0);
        List<Map<Character, Racional>> unicas = new ArrayList<>();
        for (Map<Character, Racional> s : r) {
            Map<Character, Racional> ordenada = new LinkedHashMap<>();
            incognitas.forEach(v -> ordenada.put(v, s.get(v)));
            if (!unicas.contains(ordenada)) {
                unicas.add(ordenada);
            }
        }
        Comparator<Map<Character, Racional>> orden = (a, b) -> {
            for (char v : incognitas) {
                int c = a.get(v).compareTo(b.get(v));
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        };
        unicas.sort(orden);
        return new Resultado(unicas, e.pasos, List.copyOf(e.avisos), e.completo);
    }

    private static final class Estado {
        final List<PasoLimite> pasos = new ArrayList<>();
        final LinkedHashSet<String> avisos = new LinkedHashSet<>();
        boolean completo = true;
    }

    private static String p(Polinomio q) {
        return ImpresoraPizarra.imprimir(q.aExpresion());
    }

    private static String igualCero(Polinomio q) {
        return p(q) + " = 0";
    }

    private static PasoLimite paso(String titulo, String porque, List<String> lineas) {
        return new PasoLimite(Anidados.ECUACION, titulo, porque, null, null, new TrabajoIntermedio.Agrupacion(lineas));
    }

    private static PasoLimite paso(String titulo, String porque, List<String> lineas, String grupo,
                                   List<PasoLimite> hijos) {
        return new PasoLimite(Anidados.ECUACION, titulo, porque, null, null, new TrabajoIntermedio.Agrupacion(lineas),
                hijos.isEmpty() ? null : grupo, hijos);
    }

    private List<Map<Character, Racional>> resolver(List<Polinomio> sistema, List<Character> vars,
                                                   List<PasoLimite> pasos, Estado e, int profundidad) {
        List<Polinomio> eqs = new ArrayList<>();
        for (Polinomio q : sistema) {
            if (!q.esCero() && !eqs.contains(q)) {
                eqs.add(q);
            }
        }
        for (Polinomio q : eqs) {
            if (q.esConstante()) {
                pasos.add(paso("Sin solución", "Queda " + igualCero(q) + ", que es falso: esta rama no da puntos",
                        List.of(igualCero(q))));
                return List.of();
            }
        }
        if (vars.isEmpty() || eqs.isEmpty()) {
            if (!vars.isEmpty()) {
                e.completo = false;
                pasos.add(paso("Infinitas soluciones", "Ya no quedan ecuaciones y sí incógnitas ("
                        + nombres(vars) + "): hay infinitos puntos", List.of()));
                return List.of();
            }
            return List.of(new LinkedHashMap<>());
        }
        if (profundidad > PROFUNDIDAD) {
            e.completo = false;
            return List.of();
        }

        for (Polinomio q : eqs) {
            if (q.variables().size() == 1) {
                return conUnaIncognita(q, eqs, vars, pasos, e, profundidad);
            }
        }
        Optional<Despeje> d = despeje(eqs, vars);
        if (d.isPresent()) {
            return sustituir(d.get(), eqs, vars, pasos, e, profundidad);
        }
        for (Polinomio q : eqs) {
            List<Polinomio> factores = factores(q);
            if (factores.size() > 1) {
                return porFactores(q, factores, eqs, vars, pasos, e, profundidad);
            }
        }
        for (int i = 0; i < eqs.size(); i++) {
            for (int j = i + 1; j < eqs.size(); j++) {
                Polinomio resta = eqs.get(i).restar(eqs.get(j));
                List<Polinomio> factores = resta.esConstante() ? List.of() : factores(resta);
                if (factores.size() > 1) {
                    pasos.add(paso("Restar dos ecuaciones", "Se resta " + igualCero(eqs.get(j)) + " de "
                            + igualCero(eqs.get(i)) + ": la resta se factoriza", List.of(igualCero(resta))));
                    List<Polinomio> nuevas = new ArrayList<>(eqs);
                    nuevas.set(i, resta);
                    return porFactores(resta, factores, nuevas, vars, pasos, e, profundidad);
                }
            }
        }
        e.completo = false;
        List<String> lineas = new ArrayList<>();
        eqs.forEach(q -> lineas.add(igualCero(q)));
        pasos.add(paso("No se pudo seguir", "Ninguna ecuación tiene una sola incógnita, una incógnita lineal ni se "
                + "factoriza: con sustitución y factorización no se resuelve", lineas));
        return List.of();
    }

    private List<Map<Character, Racional>> conUnaIncognita(Polinomio q, List<Polinomio> eqs, List<Character> vars,
                                                          List<PasoLimite> pasos, Estado e, int profundidad) {
        char v = q.variables().first();
        ResolucionEcuacion r = ecuaciones.resolver(new Ecuacion(q.aExpresion(), Constante.de(0)));
        List<String> lineas = new ArrayList<>();
        lineas.add(igualCero(q));
        List<Racional> raices = r.soluciones().stream().map(ResolucionEcuacion.Solucion::valor).toList();
        for (Polinomio sin : r.sinSolucion()) {
            if (tieneRaizReal(sin)) {
                e.avisos.add(igualCero(sin) + " tiene raíces irracionales: esos puntos no tienen coordenadas exactas "
                        + "y no se incluyen");
            }
        }
        if (raices.isEmpty()) {
            lineas.add("No tiene raíces racionales");
        } else {
            lineas.add(String.join(", ", raices.stream().map(x -> v + " = " + ImpresoraPizarra.numero(x)).toList()));
        }
        pasos.add(paso("Resolver " + igualCero(q), "Tiene una sola incógnita, " + v + ": se resuelve factorizando",
                lineas, "Resolver la ecuación", Anidados.deEcuacion(r)));
        List<Character> resto = new ArrayList<>(vars);
        resto.remove((Character) v);
        List<Map<Character, Racional>> soluciones = new ArrayList<>();
        for (Racional raiz : raices) {
            List<Polinomio> otras = new ArrayList<>();
            List<String> sustituidas = new ArrayList<>();
            for (Polinomio o : eqs) {
                if (o == q) {
                    continue;
                }
                Polinomio s = Polinomios.sustituir(o, v, raiz);
                otras.add(s);
                sustituidas.add(igualCero(o) + "  →  " + igualCero(s));
            }
            List<PasoLimite> rama = pasos;
            if (raices.size() > 1 && !otras.isEmpty()) {
                List<PasoLimite> hijos = new ArrayList<>();
                List<Map<Character, Racional>> sub = resolver(otras, resto, hijos, e, profundidad + 1);
                pasos.add(paso("Caso " + v + " = " + ImpresoraPizarra.numero(raiz), "Se reemplaza " + v + " = "
                        + ImpresoraPizarra.numero(raiz) + " en las demás ecuaciones", sustituidas,
                        "Lo que queda", hijos));
                agregar(soluciones, sub, v, raiz);
                continue;
            }
            if (!otras.isEmpty()) {
                rama.add(paso("Reemplazar " + v + " = " + ImpresoraPizarra.numero(raiz), "Se reemplaza en las demás "
                        + "ecuaciones", sustituidas));
            }
            agregar(soluciones, resolver(otras, resto, rama, e, profundidad + 1), v, raiz);
        }
        return soluciones;
    }

    private static void agregar(List<Map<Character, Racional>> soluciones, List<Map<Character, Racional>> sub,
                                char v, Racional valor) {
        for (Map<Character, Racional> s : sub) {
            Map<Character, Racional> c = new LinkedHashMap<>(s);
            c.put(v, valor);
            soluciones.add(c);
        }
    }

    private static boolean tieneRaizReal(Polinomio q) {
        if (q.grado() % 2 == 1) {
            return true;
        }
        char v = q.variables().first();
        Racional lider = q.coeficiente(Monomio.de(v, q.grado()));
        for (int i = -40; i <= 40; i++) {
            Racional x = Racional.de(i, 4);
            Racional y = q.evaluar(Map.of(v, x));
            if (y.signo() != 0 && y.signo() != lider.signo()) {
                return true;
            }
        }
        return false;
    }

    private record Despeje(Polinomio ecuacion, char variable, Polinomio valor) {
    }

    private static Optional<Despeje> despeje(List<Polinomio> eqs, List<Character> vars) {
        Despeje mejor = null;
        int costo = Integer.MAX_VALUE;
        for (Polinomio q : eqs) {
            for (char v : vars) {
                if (q.gradoEn(v) != 1) {
                    continue;
                }
                TreeMap<Integer, Polinomio> pv = Polinomios.enPotenciasDe(q, v);
                Polinomio c = pv.get(1);
                if (!c.esConstante()) {
                    continue;
                }
                Polinomio resto = pv.getOrDefault(0, Polinomio.CERO);
                Polinomio valor = resto.negar().multiplicar(c.constante().inverso());
                int k = q.terminos().size() * 10 + valor.grado() + (Character.isLetter(v) && v >= 'α' ? 5 : 0);
                if (k < costo) {
                    costo = k;
                    mejor = new Despeje(q, v, valor);
                }
            }
        }
        return Optional.ofNullable(mejor);
    }

    private List<Map<Character, Racional>> sustituir(Despeje d, List<Polinomio> eqs, List<Character> vars,
                                                    List<PasoLimite> pasos, Estado e, int profundidad) {
        char v = d.variable();
        List<String> lineas = new ArrayList<>();
        lineas.add(igualCero(d.ecuacion()) + "  →  " + v + " = " + p(d.valor()));
        List<Polinomio> otras = new ArrayList<>();
        for (Polinomio o : eqs) {
            if (o == d.ecuacion()) {
                continue;
            }
            Polinomio s = Polinomios.sustituir(o, v, d.valor());
            otras.add(s);
            if (o.gradoEn(v) > 0) {
                lineas.add(igualCero(o) + "  →  " + igualCero(s));
            }
        }
        pasos.add(paso("Despejar " + v + " y reemplazar", v + " aparece lineal en " + igualCero(d.ecuacion())
                + ": se despeja y se reemplaza en las demás ecuaciones", lineas));
        List<Character> resto = new ArrayList<>(vars);
        resto.remove((Character) v);
        List<Map<Character, Racional>> sub = resolver(otras, resto, pasos, e, profundidad + 1);
        List<Map<Character, Racional>> r = new ArrayList<>();
        List<String> regreso = new ArrayList<>();
        for (Map<Character, Racional> s : sub) {
            Map<Character, Racional> c = new LinkedHashMap<>(s);
            Racional valor = d.valor().evaluar(s);
            c.put(v, valor);
            r.add(c);
            if (!d.valor().esConstante()) {
                regreso.add(v + " = " + p(d.valor()) + " = " + ImpresoraPizarra.numero(valor) + " con "
                        + String.join(", ", s.entrySet().stream().map(x -> x.getKey() + " = "
                                + ImpresoraPizarra.numero(x.getValue())).toList()));
            }
        }
        if (!regreso.isEmpty()) {
            pasos.add(paso("Calcular " + v, "Se regresa a " + v + " = " + p(d.valor()) + " con los valores hallados",
                    regreso));
        }
        return r;
    }

    private List<Polinomio> factores(Polinomio q) {
        try {
            FormaFactorizada f = orquestador.resolver(q.aExpresion(), Campo.Q).resultado();
            List<Polinomio> r = new ArrayList<>();
            for (Factor fa : f.factores()) {
                if (!fa.polinomio().esConstante() && !r.contains(fa.polinomio())) {
                    r.add(fa.polinomio());
                }
            }
            return r;
        } catch (RuntimeException e) {
            return List.of(q);
        }
    }

    private List<Map<Character, Racional>> porFactores(Polinomio q, List<Polinomio> factores, List<Polinomio> eqs,
                                                      List<Character> vars, List<PasoLimite> pasos, Estado e,
                                                      int profundidad) {
        String producto = String.join("·", factores.stream().map(f -> "(" + p(f) + ")").toList());
        pasos.add(paso("Factorizar " + igualCero(q), "Un producto es cero solo si alguno de sus factores es cero: "
                + "se sigue por cada factor", List.of(producto + " = 0")));
        List<Map<Character, Racional>> r = new ArrayList<>();
        for (Polinomio f : factores) {
            List<Polinomio> otras = new ArrayList<>();
            otras.add(f);
            eqs.stream().filter(o -> o != q).forEach(otras::add);
            List<PasoLimite> hijos = new ArrayList<>();
            List<Map<Character, Racional>> sub = resolver(otras, vars, hijos, e, profundidad + 1);
            pasos.add(paso("Caso " + igualCero(f), "Se resuelve el sistema con este factor", List.of(), "Lo que queda",
                    hijos));
            r.addAll(sub);
        }
        return r;
    }

    private static String nombres(List<Character> vs) {
        return String.join(", ", vs.stream().map(String::valueOf).toList());
    }
}
