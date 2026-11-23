package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.MetodoFactorizacion.Aplicabilidad;
import pe.mathnova.domain.factorizacion.MetodoFactorizacion.ResultadoMetodo;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio.TablaRuffini;
import pe.mathnova.domain.parser.Parser;

/**
 * Motor de factorización en Q, R y C (Chain of Responsibility). Sobre cada factor, de forma recursiva:
 * <ol>
 *   <li>Preprocesamiento: la entrada pasa a Polinomio (reducido y ordenado).</li>
 *   <li>F03, solo si la entrada ya viene agrupada como x²(x − 3) + 5(x − 3): se mira el árbol antes de expandir.</li>
 *   <li>F01 (con el signo, RN05 y RN06) o F02 si hay coeficientes fraccionarios.</li>
 *   <li>Según el número de términos: 2 → F05, F08/F09, F11 (grado ≥ 5); 3 → F07 y luego F12 o F13 si es
 *       de grado 2; 4 → F10 y F04; 5 o más → F04.</li>
 *   <li>En una sola letra y desde grado 3: F14, F15, F18, F16 (seguido de F17 si la raíz se repite),
 *       F19, F20 y, al final, A4 (Kronecker), que encuentra un factor si existe.</li>
 * </ol>
 * Con dos o más letras (HU27, sección 2.13) la cadena es otra: M01 factor común, M03 productos notables,
 * M04 aspa simple homogénea, M05 aspa doble, M06 homogéneo con t = x/y, M02 agrupación, M07 Ruffini
 * respecto de una letra y M08 coeficientes indeterminados de respaldo. En un homogéneo de grado ≥ 3 cuyo
 * polinomio en t tiene una raíz repetida, M07 va antes que M02 porque deja ver la multiplicidad.
 * Los factores finales en varias letras se certifican con A6 (reducción de Kronecker).
 * Lo que ningún método parte queda como factor final, con su análisis de irreducibilidad (HU07).
 * Con un método forzado (HU08), ese método se aplica a la entrada y el resto sigue en automático.
 */
public final class OrquestadorFactorizacion implements Factorizador {

    private static final int PROFUNDIDAD_MAXIMA = 64;
    /** Orden de los métodos de grado ≥ 3 en una letra (después de los del número de términos). */
    private static final List<String> AVANZADOS = List.of("F14", "F15", "F18", "F16", "F19", "F20", "A4");
    /** No se ofrecen como "también se puede resolver con" ni en el menú de forzar. */
    private static final List<String> NO_FORZABLES = List.of("A4");

    private final Map<String, MetodoFactorizacion> metodos = new LinkedHashMap<>();
    private final FactorComunPolinomio f03;
    private final RuffiniRepetido f17;
    /** Los métodos de varias variables por código (M01…M08); vacío en los orquestadores de prueba. */
    private final Map<String, MetodoFactorizacion> varias = new LinkedHashMap<>();

    /** Con todos los métodos de Q. */
    public OrquestadorFactorizacion() {
        this(List.of(FactorComun.monomio(), FactorComun.fraccionario(), new DiferenciaDeCuadrados(),
                new TrinomioCuadradoPerfecto(), new TrinomioSimple(), new TrinomioAspa(),
                new SumaODiferenciaDeCubos(true), new SumaODiferenciaDeCubos(false), new CuboPerfecto(),
                new PotenciasEnesimas(), new AgrupacionDeTerminos(), new CambioDeVariable(), new QuitaYPon(), new Reciproco(), new Ruffini(),
                new RuffiniRepetido(), new AspaDobleEspecial(), new CoeficientesIndeterminados(),
                new VerificacionIrreducibilidad()),
                new FactorComunPolinomio());
        for (String[] par : new String[][] {{"M01", "F01"}, {"M01F", "F02"}, {"M03-F05", "F05"}, {"M03-F07", "F07"},
                {"M03-F08", "F08"}, {"M03-F09", "F09"}, {"M03-F10", "F10"}, {"M03-F11", "F11"}, {"M02", "F04"}}) {
            varias.put(par[0], new EnVariasVariables(par[0].substring(0, 3), metodos.get(par[1])));
        }
        varias.put("M04", new AspaSimpleHomogenea());
        varias.put("M05", new AspaDobleVariables());
        varias.put("M06", new Homogeneo());
        varias.put("M07", new RuffiniVariables());
        varias.put("M08", new CoeficientesIndeterminadosVariables());
    }

    /** Solo con los métodos dados; f03 puede ser null. */
    OrquestadorFactorizacion(List<MetodoFactorizacion> lista, FactorComunPolinomio f03) {
        lista.forEach(m -> metodos.put(m.codigo(), m));
        this.f03 = f03;
        this.f17 = metodos.get("F17") instanceof RuffiniRepetido r ? r : null;
    }

    /**
     * Texto en el formato del banco. Una fracción de polinomios se simplifica (P01):
     * "(x + 2)/(x + 3); x ≠ -3, x ≠ 2".
     */
    @Override
    public String factorizar(String entrada, Campo campo) {
        Expresion e = Parser.parse(entrada);
        if (SimplificadorFracciones.esFraccion(e)) {
            if (campo != Campo.Q) {
                throw new UnsupportedOperationException("Por ahora solo se simplifica en Q, no en " + campo);
            }
            return new SimplificadorFracciones(this).simplificar(e).texto();
        }
        return resolver(e, campo).texto();
    }

    /** Códigos de los métodos que se pueden forzar (HU08), en orden. */
    public List<String> metodosForzables() {
        List<String> codigos = new ArrayList<>();
        if (f03 != null) {
            codigos.add(FactorComunPolinomio.CODIGO);
        }
        metodos.keySet().stream().filter(c -> !NO_FORZABLES.contains(c)).forEach(codigos::add);
        codigos.sort(null);
        return List.copyOf(codigos);
    }

    public Resolucion resolver(Expresion entrada, Campo campo) {
        return resolver(entrada, campo, null);
    }

    /**
     * @param forzado código del método que se aplica primero (HU08), o null en automático
     * En R o C primero se factoriza en Q y luego se parten los factores irreducibles (RN27).
     *
     * @throws pe.mathnova.domain.algebra.NoEsPolinomioException si la entrada no es un polinomio
     * @throws MetodoNoAplicable si el método forzado no aplica, con el motivo
     */
    public Resolucion resolver(Expresion entrada, Campo campo, String forzado) {
        Polinomio p = Polinomio.desde(entrada);
        Proceso proceso = new Proceso();
        if (forzado != null) {
            proceso.registrar(forzar(entrada, p, forzado), 1, forzado.equals(FactorComunPolinomio.CODIGO), 0);
        } else {
            Optional<ResultadoMetodo> agrupada = f03 == null ? Optional.empty() : f03.detectar(entrada);
            if (agrupada.isPresent()) {
                proceso.registrar(agrupada.get(), 1, true, 0);
            } else {
                proceso.factorizar(p, 1, true, 0);
            }
        }
        FormaFactorizada resultado = FormaFactorizada.normalizada(proceso.constante, proceso.finales);
        List<Irreducibilidad> irreducibles = resultado.factores().stream()
                .filter(f -> f.polinomio().terminos().size() > 1)
                .map(f -> AnalisisIrreducibilidad.analizar(f.polinomio()))
                .toList();
        String primero = proceso.pasos.isEmpty() ? null : proceso.pasos.getFirst().codigoTecnica();
        Resolucion enQ = new Resolucion(entrada, proceso.pasos, proceso.descartados, resultado, irreducibles,
                alternativas(entrada, p, primero), forzado);
        // Los métodos de R y C (F21–F30, A5) son de una letra. Un factor en varias letras que quedó de Q
        // ya no lo parte ninguno: en un factor lineal, como x + 2y + 3 tras el aspa doble, es irreducible.
        boolean variasLetras = resultado.factores().stream().anyMatch(f -> f.polinomio().variables().size() > 1);
        if (campo == Campo.Q || variasLetras) {
            return enQ;
        }
        return enQ.conExtension(ExtensionCampo.extender(enQ, campo));
    }

    private ResultadoMetodo forzar(Expresion entrada, Polinomio p, String codigo) {
        if (codigo.equals(FactorComunPolinomio.CODIGO) && f03 != null) {
            return f03.detectar(entrada).orElseThrow(() -> new MetodoNoAplicable(codigo,
                    "La expresión no viene escrita como una suma de términos con un mismo paréntesis"));
        }
        MetodoFactorizacion m = metodos.get(codigo);
        if (m == null || NO_FORZABLES.contains(codigo)) {
            throw new IllegalArgumentException("No hay un método " + codigo + " para forzar");
        }
        Aplicabilidad a = m.puedeAplicarse(p, Campo.Q);
        if (!a.aplica()) {
            throw new MetodoNoAplicable(codigo, a.motivo());
        }
        return m.aplicar(p, Campo.Q);
    }

    /** Otros métodos que también aplican a la entrada, sin contar el primero que se usó. */
    private List<String> alternativas(Expresion entrada, Polinomio p, String usado) {
        if (usado == null || p.terminos().size() < 2) {
            return List.of();
        }
        List<String> r = new ArrayList<>();
        if (f03 != null && !usado.equals(FactorComunPolinomio.CODIGO) && f03.detectar(entrada).isPresent()) {
            r.add(FactorComunPolinomio.CODIGO);
        }
        if (!varias.isEmpty() && p.variables().size() >= 2) {
            for (MetodoFactorizacion m : varias.values()) {
                String c = m.codigo();
                if (c.equals(usado) || r.contains(c)) {
                    continue;
                }
                try {
                    if (m.esCandidato(p) && m.puedeAplicarse(p, Campo.Q).aplica()) {
                        r.add(c);
                    }
                } catch (RuntimeException e) {
                    // no aplica
                }
            }
            return r;
        }
        for (MetodoFactorizacion m : metodos.values()) {
            String c = m.codigo();
            if (c.equals(usado) || NO_FORZABLES.contains(c) || c.equals("F02") && usado.equals("F01")) {
                continue;
            }
            if (m.esCandidato(p) && m.puedeAplicarse(p, Campo.Q).aplica()) {
                r.add(c);
            }
        }
        return r;
    }

    private List<MetodoFactorizacion> candidatos(Polinomio p) {
        List<Termino> ts = Termino.de(p);
        List<String> codigos = new ArrayList<>();
        switch (ts.size()) {
            case 2 -> {
                codigos.add("F05");
                codigos.add(ts.get(1).positivo() ? "F08" : "F09");
                if (p.grado() >= 5) {
                    codigos.add("F11");
                }
            }
            case 3 -> {
                codigos.add("F07");
                if (p.grado() == 2 && p.variables().size() == 1) {
                    codigos.add(Contenido.principal(p).equals(Racional.UNO) ? "F12" : "F13");
                }
            }
            case 4 -> {
                codigos.add("F10");
                codigos.add("F04");
            }
            default -> codigos.add("F04");
        }
        if (p.variables().size() == 1 && p.grado() >= 3) {
            codigos.addAll(AVANZADOS);
        }
        return codigos.stream().map(metodos::get).filter(m -> m != null).toList();
    }

    /** La cadena de varias variables (sección 2.13), en orden. */
    private List<MetodoFactorizacion> candidatosVarias(Polinomio p) {
        List<String> codigos = new ArrayList<>();
        List<Termino> ts = Termino.de(p);
        switch (ts.size()) {
            case 2 -> {
                codigos.add("M03-F05");
                codigos.add(ts.get(1).positivo() ? "M03-F08" : "M03-F09");
                if (p.grado() >= 5) {
                    codigos.add("M03-F11");
                }
            }
            case 3 -> codigos.add("M03-F07");
            case 4 -> codigos.add("M03-F10");
            default -> {
            }
        }
        codigos.add("M04");
        codigos.add("M05");
        codigos.add("M06");
        boolean homogeneo = Multivariable.homogeneo(p) && p.grado() >= 3;
        if (homogeneo) {
            codigos.add("M07");
        }
        codigos.add("M02");
        if (!homogeneo) {
            codigos.add("M07");
        }
        codigos.add("M08");
        return codigos.stream().map(varias::get).filter(m -> m != null).toList();
    }

    /** Estado de una factorización. */
    private final class Proceso {

        private Racional constante = Racional.UNO;
        private final List<Factor> finales = new ArrayList<>();
        private final List<Paso> pasos = new ArrayList<>();
        private final List<Paso> descartados = new ArrayList<>();

        /**
         * @param raiz true para la entrada y para los factores de F03: solo en ellos se anota F01
         *             como descartado (los factores que salen de otros métodos ya son primitivos)
         */
        void factorizar(Polinomio p, int multiplicidad, boolean raiz, int profundidad) {
            if (p.esConstante()) {
                constante = constante.multiplicar(p.constante().potencia(multiplicidad));
                return;
            }
            if (p.terminos().size() == 1 || profundidad > PROFUNDIDAD_MAXIMA) {
                finales.add(new Factor(p, multiplicidad));
                return;
            }
            boolean enVarias = !varias.isEmpty() && p.variables().size() >= 2;
            MetodoFactorizacion contenido = enVarias ? varias.get(Contenido.tieneFracciones(p) ? "M01F" : "M01")
                    : metodos.get(Contenido.tieneFracciones(p) ? "F02" : "F01");
            if (contenido != null) {
                Aplicabilidad a = contenido.puedeAplicarse(p, Campo.Q);
                if (a.aplica()) {
                    registrar(contenido.aplicar(p, Campo.Q), multiplicidad, false, profundidad);
                    return;
                }
                if (raiz) {
                    descartar(contenido, p, a.motivo());
                }
            }
            if (p.grado() == 1) {
                finales.add(new Factor(p, multiplicidad));
                return;
            }
            for (MetodoFactorizacion m : enVarias ? candidatosVarias(p) : candidatos(p)) {
                if (!m.esCandidato(p)) {
                    continue; // por su forma no tiene sentido probarlo: ni se prueba ni se lista
                }
                Aplicabilidad a = m.puedeAplicarse(p, Campo.Q);
                if (a.aplica()) {
                    registrar(m.aplicar(p, Campo.Q), multiplicidad, false, profundidad);
                    return;
                }
                descartar(m, p, a.motivo());
            }
            finales.add(new Factor(p, multiplicidad));
        }

        void registrar(ResultadoMetodo r, int multiplicidad, boolean factoresRaiz, int profundidad) {
            pasos.add(r.paso().conOrden(pasos.size() + 1));
            // F16 → F17: si la raíz vuelve a anular el cociente, se sigue con la misma raíz.
            TablaRuffini tabla = r.paso().codigoTecnica().equals("F16")
                    && r.paso().trabajo() instanceof TablaRuffini t ? t : null;
            for (int i = 0; i < r.factores().size(); i++) {
                Factor f = r.factores().get(i);
                int m = multiplicidad * f.multiplicidad();
                if (tabla != null && f17 != null && i == r.factores().size() - 1) {
                    Optional<ResultadoMetodo> repetida = f17.continuar(f.polinomio(), tabla);
                    if (repetida.isPresent()) {
                        registrar(repetida.get(), m, false, profundidad + 1);
                        continue;
                    }
                }
                factorizar(f.polinomio(), m, factoresRaiz, profundidad + 1);
            }
        }

        void descartar(MetodoFactorizacion m, Polinomio p, String motivo) {
            descartados.add(Paso.descartado(m.codigo(), p, motivo).conOrden(descartados.size() + 1));
        }
    }
}
