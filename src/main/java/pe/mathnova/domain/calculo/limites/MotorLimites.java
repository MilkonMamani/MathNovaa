package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.FactorComun;
import pe.mathnova.domain.calculo.derivadas.Derivador;
import pe.mathnova.domain.calculo.derivadas.PasoDerivada;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.NumeroAlgebraico;

/**
 * El motor de límites (HU11, HU24; RF23–RF25, RF47). El orden de los métodos es el de RN21 y RN35:
 * <ol>
 *   <li>L01 evaluación directa: si da un número, se termina; si da k/0, se pasa a los laterales.</li>
 *   <li>Se clasifica la indeterminación (0/0, ∞/∞, ∞ − ∞, 0·∞, 1^∞, 0⁰, ∞⁰).</li>
 *   <li>0/0 en una fracción de polinomios: L02, factorizar con el orquestador y cancelar.</li>
 *   <li>0/0 con raíces: L03, multiplicar por el conjugado.</li>
 *   <li>∞/∞ algebraico: L04, comparar grados (dividir entre la mayor potencia).</li>
 *   <li>∞ − ∞: término principal, conjugado o común denominador, y se vuelve a clasificar.</li>
 *   <li>L06 límites notables, antes que L'Hôpital.</li>
 *   <li>L07 L'Hôpital solo con 0/0 o ∞/∞, verificado antes de cada aplicación, como mucho 5 veces;
 *       0·∞ pasa antes a cociente y 1^∞, 0⁰, ∞⁰ a logaritmo.</li>
 *   <li>L05 laterales: con k/0 se estudia el signo del denominador a cada lado.</li>
 * </ol>
 * Si ningún método funciona se dice "No se pudo calcular con los métodos disponibles": nunca se
 * inventa un valor.
 */
public final class MotorLimites {

    public static final int MAXIMO_LHOPITAL = 5;
    private static final int PROFUNDIDAD_MAXIMA = 30;
    /** Derivadas que se prueban para el signo del denominador cerca del punto (L05). */
    private static final int ORDEN_MAXIMO = 6;

    private final OrquestadorFactorizacion factorizador;

    public MotorLimites(OrquestadorFactorizacion factorizador) {
        this.factorizador = Objects.requireNonNull(factorizador, "factorizador");
    }

    public ResolucionLimite resolver(Limite limite) {
        return resolver(limite, null);
    }

    /**
     * Igual, pero ante la primera indeterminación se prueba antes {@code forzado} ("L02", "L03",
     * "L04", "L06" o "L07"; "Probar" en Exploración). Si no aplica, sigue la cascada normal: para saber
     * si se usó, se mira si sus pasos tienen ese código.
     */
    public ResolucionLimite resolver(Limite limite, String forzado) {
        Calculo c = new Calculo(limite);
        c.forzado = forzado;
        ResultadoLimite r = c.ejecutar();
        if (!(r instanceof ResultadoLimite.Finito || r instanceof ResultadoLimite.Infinito
                || r instanceof ResultadoLimite.NoExiste)) {
            c.metodo = null;
        }
        return new ResolucionLimite(limite, r, c.pasos, c.indeterminacion, c.metodo);
    }

    /**
     * Práctica (Sprint 9, parte D): por qué la técnica {@code codigo} no es el paso que toca con {@code f} en este
     * límite. Usa las mismas condiciones que la cascada y que forzar un método ("Probar"): vacío si sí aplica, así
     * una técnica que también funciona (L'Hôpital con 0/0) nunca se presenta como un error.
     *
     * @param f          la expresión a la que se le aplica el paso
     * @param primerPaso true si todavía no se evaluó nada: entonces solo vale evaluar
     */
    public Optional<String> noAplica(Limite limite, Expresion f, String codigo, boolean primerPaso) {
        return new Calculo(limite).noAplica(f, codigo, primerPaso);
    }

    private static String texto(Expresion e) {
        return ImpresoraTexto.imprimir(e);
    }

    /** Entre paréntesis solo si es una suma o lleva signo. */
    private static String par(Expresion e) {
        return e instanceof Suma || e instanceof Negacion ? "(" + texto(e) + ")" : texto(e);
    }

    private static String texto(Valor v) {
        return switch (v) {
            case Valor.Numero n -> texto(n.valor().aExpresion());
            case Valor.Infinito i -> i.signo() > 0 ? "∞" : "−∞";
            case Valor.SobreCero s -> texto(s.k().aExpresion()) + "/0";
            case Valor.Indeterminado i -> i.tipo().texto();
            case Valor.Indefinido i -> "sin valor";
        };
    }

    /** Un cálculo: guarda los pasos, la primera indeterminación y el último método que decidió. */
    private final class Calculo {

        private final Limite limite;
        private final char x;
        private final Limite.Lado lado;
        private final List<PasoLimite> pasos = new ArrayList<>();
        private Valor punto;
        private Indeterminacion indeterminacion;
        private String metodo;
        private int profundidad;
        private int lhopital;
        /** El método que se prueba primero ante la primera indeterminación; null en automático. */
        private String forzado;

        Calculo(Limite limite) {
            this.limite = limite;
            this.x = limite.variable();
            this.lado = limite.lado();
        }

        ResultadoLimite ejecutar() {
            Expresion f = limite.cuerpo();
            var otras = new java.util.TreeSet<>(Expresiones.variables(f));
            otras.remove(x);
            if (!otras.isEmpty()) {
                return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO + ": por ahora los límites son de "
                        + "una sola letra, y aparece " + otras.first());
            }
            if (limite.punto() instanceof Limite.Infinito inf) {
                punto = new Valor.Infinito(inf.positivo() ? 1 : -1);
            } else {
                Expresion a = ((Limite.Finito) limite.punto()).valor();
                Optional<NumeroAlgebraico> v = Evaluador.constante(a);
                if (v.isEmpty()) {
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
                }
                punto = new Valor.Numero(v.get());
            }
            return calcular(f);
        }

        private boolean alInfinito() {
            return punto instanceof Valor.Infinito;
        }

        private boolean enCero() {
            return punto instanceof Valor.Numero n && n.valor().esCero();
        }

        private String textoPunto() {
            return switch (limite.punto()) {
                case Limite.Infinito i -> i.positivo() ? "∞" : "−∞";
                case Limite.Finito f -> texto(f.valor()) + (lado == Limite.Lado.DERECHA ? "⁺"
                        : lado == Limite.Lado.IZQUIERDA ? "⁻" : "");
            };
        }

        private Valor evaluar(Expresion f) {
            return Evaluador.evaluar(f, x, punto);
        }

        /** Ver {@link MotorLimites#noAplica}. */
        Optional<String> noAplica(Expresion f, String codigo, boolean primerPaso) {
            if (limite.punto() instanceof Limite.Infinito inf) {
                punto = new Valor.Infinito(inf.positivo() ? 1 : -1);
            } else {
                Optional<NumeroAlgebraico> v = Evaluador.constante(((Limite.Finito) limite.punto()).valor());
                if (v.isEmpty()) {
                    return Optional.empty();
                }
                punto = new Valor.Numero(v.get());
            }
            String a = x + " → " + textoPunto();
            if (primerPaso) {
                return codigo.equals("L01") ? Optional.empty()
                        : Optional.of("Todavía no sabes qué sale: primero se reemplaza " + x + " por " + textoPunto()
                                + ". Solo si queda una indeterminación hace falta transformar la expresión.");
            }
            Valor v = evaluar(f);
            Indeterminacion t = v instanceof Valor.Indeterminado i ? i.tipo() : null;
            String sale = switch (v) {
                case Valor.Indeterminado i -> i.tipo().texto();
                default -> texto(v);
            };
            if (codigo.equals("L01")) {
                return t == null ? Optional.empty()
                        : Optional.of("Reemplazar " + x + " directamente da " + sale + ", una indeterminación: "
                                + "primero hay que transformar la expresión.");
            }
            if (t == null && !(v instanceof Valor.SobreCero)) {
                return Optional.of("Si reemplazas " + x + " por " + textoPunto() + " sale " + sale
                        + ": no queda ninguna indeterminación, así que no hace falta transformar nada.");
            }
            boolean ceroSobreCero = t == Indeterminacion.CERO_SOBRE_CERO;
            return switch (codigo) {
                case "L02" -> {
                    if (!ceroSobreCero) {
                        yield Optional.of("Factorizar y cancelar resuelve 0/0, y aquí sale " + sale + ".");
                    }
                    if (!SimplificadorFracciones.esFraccion(f) || FactorComun.buscar(f, factorizador).isEmpty()) {
                        yield Optional.of("No es una fracción de polinomios con un factor común arriba y abajo que se "
                                + "pueda cancelar.");
                    }
                    yield Optional.empty();
                }
                case "L03" -> {
                    boolean raiz = f instanceof Cociente c && (tieneRaiz(c.numerador()) || tieneRaiz(c.denominador()))
                            || f instanceof Suma su && su.terminos().stream().anyMatch(MotorLimites::tieneRaiz);
                    yield raiz && (ceroSobreCero || t == Indeterminacion.INFINITO_MENOS_INFINITO) ? Optional.empty()
                            : Optional.of("No hay ninguna raíz cuadrada: no hay un conjugado por el cual multiplicar.");
                }
                case "L04" -> t == Indeterminacion.INFINITO_SOBRE_INFINITO || t == Indeterminacion.INFINITO_MENOS_INFINITO
                        ? Optional.empty()
                        : Optional.of("Comparar los grados sirve cuando " + x + " tiende a infinito y sale ∞/∞; con "
                                + a + " sale " + sale + ".");
                case "L05" -> v instanceof Valor.SobreCero ? Optional.empty()
                        : Optional.of("Los límites laterales se estudian cuando sale k/0 con k distinto de 0; aquí sale "
                                + sale + ".");
                case "L06" -> (ceroSobreCero || t == Indeterminacion.UNO_A_LA_INFINITO)
                        && LimitesNotables.buscar(f, x, enCero(), punto instanceof Valor.Infinito i && i.signo() > 0)
                        .isPresent() ? Optional.empty()
                        : Optional.of("No tiene la forma de ningún límite notable, como sen(u)/u o (1 + 1/u)^u.");
                case "L07" -> (ceroSobreCero || t == Indeterminacion.INFINITO_SOBRE_INFINITO) && f instanceof Cociente
                        ? Optional.empty()
                        : Optional.of("L'Hôpital solo se aplica a un cociente con 0/0 o ∞/∞, y aquí sale " + sale + ".");
                default -> Optional.empty();
            };
        }

        // -------------------------------------------------------------- cascada

        ResultadoLimite calcular(Expresion f) {
            if (++profundidad > PROFUNDIDAD_MAXIMA) {
                return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
            }
            Valor v = evaluacionDirecta(f);
            return switch (v) {
                case Valor.Numero n -> {
                    elegir("L01");
                    yield new ResultadoLimite.Finito(n.valor());
                }
                case Valor.Infinito i -> {
                    elegir("L01");
                    yield new ResultadoLimite.Infinito(i.signo() > 0);
                }
                case Valor.SobreCero s -> laterales(f, s.k());
                case Valor.Indeterminado i -> {
                    if (indeterminacion == null) {
                        indeterminacion = i.tipo();
                    }
                    yield resolverIndeterminacion(f, i.tipo());
                }
                case Valor.Indefinido i -> i.noExiste() ? new ResultadoLimite.NoExiste(i.motivo())
                        : new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
            };
        }

        /** El primer método es L01; después cuenta el último que se aplicó. */
        private void elegir(String codigo) {
            if (metodo == null) {
                metodo = codigo;
            }
        }

        private void usar(String codigo) {
            metodo = codigo;
        }

        /** L01: reemplaza x por el punto y dice qué sale. */
        private Valor evaluacionDirecta(Expresion f) {
            Valor v = evaluar(f);
            List<String> lineas = new ArrayList<>();
            String a = textoPunto();
            if (limite.punto() instanceof Limite.Finito fin) {
                Expresion sustituida = Sustitucion.sustituir(f, x, fin.valor());
                lineas.add(texto(sustituida) + " = " + texto(v));
            } else if (f instanceof Cociente c) {
                lineas.add("Cuando " + x + " → " + a + ": el numerador tiende a " + texto(evaluar(c.numerador()))
                        + " y el denominador a " + texto(evaluar(c.denominador())));
            } else {
                lineas.add("Cuando " + x + " → " + a + ": " + texto(f) + " → " + texto(v));
            }
            String justificacion = switch (v) {
                case Valor.Numero n -> "Se reemplaza " + x + " por " + a + ": el límite vale " + texto(v);
                case Valor.Infinito i -> "Cuando " + x + " → " + a + ", la expresión crece sin cota: el límite es "
                        + (i.signo() > 0 ? "+∞" : "−∞");
                case Valor.SobreCero s -> "Evaluación directa: " + texto(v) + ". El denominador se anula y el "
                        + "numerador no: se estudian los límites laterales";
                case Valor.Indeterminado i -> "Evaluación directa: " + i.tipo().texto() + ", indeterminación";
                case Valor.Indefinido i -> "La evaluación directa no decide: " + i.motivo();
            };
            Expresion despues = v instanceof Valor.Numero n ? n.valor().aExpresion() : null;
            pasos.add(new PasoLimite("L01", Tecnicas.nombre("L01"), justificacion, f, despues,
                    new TrabajoIntermedio.Agrupacion(lineas)));
            return v;
        }

        private ResultadoLimite resolverIndeterminacion(Expresion f, Indeterminacion t) {
            if (f instanceof Negacion n) {
                return negar(calcular(n.argumento()));
            }
            if (forzado != null) {
                String metodo = forzado;
                forzado = null; // solo la primera vez
                Optional<ResultadoLimite> r = probarPrimero(metodo, f, t);
                if (r.isPresent()) {
                    return r.get();
                }
            }
            switch (t) {
                case CERO_SOBRE_CERO -> {
                    if (SimplificadorFracciones.esFraccion(f)) {
                        Optional<ResultadoLimite> r = factorizarYCancelar(f);
                        if (r.isPresent()) {
                            return r.get();
                        }
                    }
                    if (f instanceof Cociente c && (tieneRaiz(c.numerador()) || tieneRaiz(c.denominador()))) {
                        Optional<Expresion> g = racionalizar(c);
                        if (g.isPresent()) {
                            return calcular(g.get());
                        }
                    }
                    Optional<ResultadoLimite> notable = notable(f);
                    return notable.orElseGet(() -> lhopital(f));
                }
                case INFINITO_SOBRE_INFINITO -> {
                    Optional<ResultadoLimite> r = grados(f);
                    if (r.isPresent()) {
                        return r.get();
                    }
                    return lhopital(f);
                }
                case INFINITO_MENOS_INFINITO -> {
                    Optional<ResultadoLimite> r = grados(f);
                    if (r.isPresent()) {
                        return r.get();
                    }
                    if (f instanceof Suma s && s.terminos().stream().anyMatch(MotorLimites::tieneRaiz)) {
                        Optional<Expresion> g = conjugadoDeSuma(s);
                        if (g.isPresent()) {
                            return calcular(g.get());
                        }
                    }
                    if (f instanceof Suma s && s.terminos().stream().anyMatch(MotorLimites::esFraccion)) {
                        return calcular(comunDenominador(s));
                    }
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
                }
                case CERO_POR_INFINITO -> {
                    if (f instanceof Producto p) {
                        Optional<Expresion> g = aCociente(p);
                        if (g.isPresent()) {
                            return calcular(g.get());
                        }
                    }
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
                }
                default -> {
                    if (t == Indeterminacion.UNO_A_LA_INFINITO) {
                        Optional<ResultadoLimite> notable = notable(f);
                        if (notable.isPresent()) {
                            return notable.get();
                        }
                    }
                    if (f instanceof Potencia p) {
                        return logaritmo(p, t);
                    }
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
                }
            }
        }

        /** El método forzado, solo si sus condiciones se cumplen; vacío si no aplica. */
        private Optional<ResultadoLimite> probarPrimero(String metodo, Expresion f, Indeterminacion t) {
            boolean ceroSobreCero = t == Indeterminacion.CERO_SOBRE_CERO;
            switch (metodo) {
                case "L02" -> {
                    if (ceroSobreCero && SimplificadorFracciones.esFraccion(f)) {
                        return factorizarYCancelar(f);
                    }
                }
                case "L03" -> {
                    if (ceroSobreCero && f instanceof Cociente c && (tieneRaiz(c.numerador()) || tieneRaiz(c.denominador()))) {
                        return racionalizar(c).map(this::calcular);
                    }
                    if (t == Indeterminacion.INFINITO_MENOS_INFINITO && f instanceof Suma s
                            && s.terminos().stream().anyMatch(MotorLimites::tieneRaiz)) {
                        return conjugadoDeSuma(s).map(this::calcular);
                    }
                }
                case "L04" -> {
                    if (t == Indeterminacion.INFINITO_SOBRE_INFINITO || t == Indeterminacion.INFINITO_MENOS_INFINITO) {
                        return grados(f);
                    }
                }
                case "L06" -> {
                    if (ceroSobreCero || t == Indeterminacion.UNO_A_LA_INFINITO) {
                        return notable(f);
                    }
                }
                case "L07" -> {
                    if ((ceroSobreCero || t == Indeterminacion.INFINITO_SOBRE_INFINITO) && f instanceof Cociente) {
                        return Optional.of(lhopital(f));
                    }
                }
                default -> {
                    // L01 y L05 no se fuerzan: dependen de lo que da la evaluación.
                }
            }
            return Optional.empty();
        }

        private ResultadoLimite negar(ResultadoLimite r) {
            return switch (r) {
                case ResultadoLimite.Finito f -> new ResultadoLimite.Finito(f.valor().negar());
                case ResultadoLimite.Infinito i -> new ResultadoLimite.Infinito(!i.positivo());
                default -> r;
            };
        }

        // -------------------------------------------------------------- L02

        private Optional<ResultadoLimite> factorizarYCancelar(Expresion f) {
            // La misma factorización y cancelación que usan las derivadas y las integrales (FactorComun)
            Optional<Simplificacion> hay = FactorComun.buscar(f, factorizador);
            if (hay.isEmpty()) {
                return Optional.empty();
            }
            Simplificacion s = hay.get();
            List<PasoLimite> hijos = FactorComun.hijos(s);
            Expresion g = s.aExpresion();
            String factorizada = FactorComun.factorizada(s);
            List<String> lineas = List.of(texto(f) + " = " + factorizada, "= " + s.fraccion());
            pasos.add(new PasoLimite("L02", Tecnicas.nombre("L02"), "Hay 0/0 en una fracción de polinomios: "
                    + "se factorizan el numerador y el denominador. " + s.justificacion()
                    + ", y la fracción que queda vale lo mismo cerca de " + x + " = " + textoPunto(), f, g,
                    new TrabajoIntermedio.Agrupacion(lineas), "Factorizar numerador y denominador", hijos));
            usar("L02");
            return Optional.of(calcular(g));
        }

        // -------------------------------------------------------------- L03

        /** (√u ± b)/D: se multiplica arriba y abajo por el conjugado. */
        private Optional<Expresion> racionalizar(Cociente c) {
            boolean arriba = tieneRaiz(c.numerador());
            Expresion conRaiz = arriba ? c.numerador() : c.denominador();
            Optional<Expresion[]> partes = partir(conRaiz);
            if (partes.isEmpty()) {
                return Optional.empty();
            }
            Expresion conjugado = Simplificador.suma(List.of(partes.get()[0], Simplificador.negar(partes.get()[1])));
            Expresion diferencia = diferenciaDeCuadrados(partes.get()[0], partes.get()[1]);
            Expresion g;
            String linea;
            if (arriba) {
                g = Simplificador.simplificar(Simplificador.cociente(fraccion(diferencia, c.denominador()), conjugado));
                linea = "= " + par(diferencia) + "/(" + par(c.denominador()) + "(" + texto(conjugado) + "))";
            } else {
                g = Simplificador.simplificar(Simplificador.producto(List.of(fraccion(c.numerador(), diferencia),
                        conjugado)));
                linea = "= " + par(c.numerador()) + "(" + texto(conjugado) + ")/" + par(diferencia);
            }
            pasos.add(new PasoLimite("L03", Tecnicas.nombre("L03"), "Hay 0/0 con una raíz: se multiplican el numerador "
                    + "y el denominador por el conjugado " + texto(conjugado) + ", porque (a + b)(a − b) = a² − b² quita "
                    + "la raíz", c, g, new TrabajoIntermedio.Agrupacion(List.of(
                    texto(c) + " · (" + texto(conjugado) + ")/(" + texto(conjugado) + ")", linea, "= " + texto(g)))));
            usar("L03");
            return Optional.of(g);
        }

        /** ∞ − ∞ con una raíz: f = (a + b)·(a − b)/(a − b) = (a² − b²)/(a − b). */
        private Optional<Expresion> conjugadoDeSuma(Suma s) {
            Optional<Expresion[]> partes = partir(s);
            if (partes.isEmpty()) {
                return Optional.empty();
            }
            Expresion conjugado = Simplificador.suma(List.of(partes.get()[0], Simplificador.negar(partes.get()[1])));
            Expresion diferencia = diferenciaDeCuadrados(partes.get()[0], partes.get()[1]);
            Expresion g = Simplificador.simplificar(new Cociente(diferencia, conjugado));
            pasos.add(new PasoLimite("L03", "Conjugado", "Hay ∞ − ∞ con una raíz: se multiplica y se divide por el "
                    + "conjugado " + texto(conjugado) + ". Arriba queda una diferencia de cuadrados sin raíz, y el "
                    + "límite se vuelve a clasificar", s, g, new TrabajoIntermedio.Agrupacion(List.of(
                    texto(s) + " = (" + texto(s) + ")(" + texto(conjugado) + ")/(" + texto(conjugado) + ")",
                    "= " + par(diferencia) + "/(" + texto(conjugado) + ")"))));
            usar("L03");
            return Optional.of(g);
        }

        /** [a, b] con a el término con raíz y b la suma del resto. */
        private Optional<Expresion[]> partir(Expresion e) {
            if (!(e instanceof Suma s)) {
                return Optional.empty();
            }
            List<Expresion> conRaiz = s.terminos().stream().filter(MotorLimites::tieneRaiz).toList();
            if (conRaiz.size() != 1) {
                return Optional.empty();
            }
            List<Expresion> resto = s.terminos().stream().filter(t -> t != conRaiz.getFirst()).toList();
            Expresion b = resto.size() == 1 ? resto.getFirst() : new Suma(resto);
            return Optional.of(new Expresion[] {conRaiz.getFirst(), b});
        }

        /** a² − b², como polinomio si se puede. */
        private Expresion diferenciaDeCuadrados(Expresion a, Expresion b) {
            Constante dos = Constante.de(2);
            Expresion d = Simplificador.simplificar(Simplificador.suma(List.of(Simplificador.potencia(a, dos),
                    Simplificador.negar(Simplificador.potencia(b, dos)))));
            return comoPolinomio(d);
        }

        private Expresion comoPolinomio(Expresion e) {
            try {
                return Polinomio.desde(e).aExpresion();
            } catch (NoEsPolinomioException | ArithmeticException noEs) {
                return e;
            }
        }

        /** a/b simplificada si las dos son polinomios (factorizando); si no, como sale. */
        private Expresion fraccion(Expresion a, Expresion b) {
            Expresion q = new Cociente(a, b);
            if (SimplificadorFracciones.esFraccion(q)) {
                return new SimplificadorFracciones(factorizador).simplificar(q).aExpresion();
            }
            return Simplificador.simplificar(q);
        }

        // -------------------------------------------------------------- L04

        /** ∞/∞ o ∞ − ∞ algebraicos al infinito: el término principal decide. */
        private Optional<ResultadoLimite> grados(Expresion f) {
            if (!(punto instanceof Valor.Infinito inf)) {
                return Optional.empty();
            }
            boolean negativo = inf.signo() < 0;
            Expresion h = negativo ? Simplificador.simplificar(Sustitucion.sustituir(f, x,
                    new Negacion(new Variable(x)))) : f;
            String cambio = negativo ? "Como " + x + " → −∞, se cambia " + x + " por −" + x + " y se calcula hacia +∞. " : "";
            if (h instanceof Cociente c) {
                Optional<TerminoPrincipal> tn = TerminoPrincipal.de(c.numerador(), x);
                Optional<TerminoPrincipal> td = TerminoPrincipal.de(c.denominador(), x);
                if (tn.isEmpty() || td.isEmpty()) {
                    return Optional.empty();
                }
                Racional p = tn.get().exponente();
                Racional q = td.get().exponente();
                NumeroAlgebraico cociente = Exactos.dividir(tn.get().coeficiente(), td.get().coeficiente());
                int comparacion = p.compareTo(q);
                ResultadoLimite r = comparacion < 0 ? new ResultadoLimite.Finito(NumeroAlgebraico.CERO)
                        : comparacion == 0 ? new ResultadoLimite.Finito(cociente)
                        : new ResultadoLimite.Infinito(cociente.signo() > 0);
                Expresion dividida = Simplificador.simplificar(new Cociente(TerminoPrincipal.dividida(c.numerador(), x, q),
                        TerminoPrincipal.dividida(c.denominador(), x, q)));
                String potencia = potencia(q);
                String conclusion = comparacion < 0 ? "el grado de abajo es mayor: el límite es 0"
                        : comparacion == 0 ? "los grados son iguales: el límite es el cociente de los coeficientes "
                        + "principales, " + texto(tn.get().coeficiente().aExpresion()) + "/"
                        + texto(td.get().coeficiente().aExpresion()) + " = " + r.texto()
                        : "el grado de arriba es mayor: el límite es " + r.texto();
                List<String> lineas = new ArrayList<>();
                if (!potencia.equals("1")) {
                    lineas.add("Se divide entre " + potencia + ": " + texto(dividida));
                }
                lineas.add("Cada término c/" + x + "ᵏ tiende a 0");
                lineas.add("→ " + r.texto());
                pasos.add(new PasoLimite("L04", Tecnicas.nombre("L04"), cambio + "Grado del numerador: " + grado(p)
                        + "; grado del denominador: " + grado(q) + ". Se divide numerador y denominador entre la mayor "
                        + "potencia del denominador, " + potencia + "; " + conclusion, h,
                        r instanceof ResultadoLimite.Finito fin ? fin.valor().aExpresion() : dividida,
                        new TrabajoIntermedio.Agrupacion(lineas)));
                usar("L04");
                return Optional.of(r);
            }
            Optional<TerminoPrincipal> t = TerminoPrincipal.de(h, x);
            if (t.isEmpty()) {
                return Optional.empty();
            }
            Racional p = t.get().exponente();
            NumeroAlgebraico c = t.get().coeficiente();
            ResultadoLimite r = p.signo() > 0 ? new ResultadoLimite.Infinito(c.signo() > 0)
                    : p.esCero() ? new ResultadoLimite.Finito(c) : new ResultadoLimite.Finito(NumeroAlgebraico.CERO);
            pasos.add(new PasoLimite("L04", Tecnicas.nombre("L04"), cambio + "Manda el término de mayor grado, "
                    + texto(c.aExpresion()) + "·" + potencia(p) + ": los demás crecen más despacio. El límite es "
                    + r.texto(), h, r instanceof ResultadoLimite.Finito fin ? fin.valor().aExpresion() : null,
                    new TrabajoIntermedio.Agrupacion(List.of(texto(h) + " = " + potencia(p) + "·("
                            + texto(TerminoPrincipal.dividida(h, x, p)) + ")", "→ " + r.texto()))));
            usar("L04");
            return Optional.of(r);
        }

        private String potencia(Racional p) {
            if (p.esCero()) {
                return "1";
            }
            return p.equals(Racional.UNO) ? String.valueOf(x) : x + "^" + (p.esEntero() ? p.toString() : "(" + p + ")");
        }

        private static String grado(Racional p) {
            return p.toString();
        }

        // -------------------------------------------------------------- L06

        private Optional<ResultadoLimite> notable(Expresion f) {
            boolean masInfinito = punto instanceof Valor.Infinito i && i.signo() > 0;
            Optional<LimitesNotables.Notable> n = LimitesNotables.buscar(f, x, enCero(), masInfinito);
            if (n.isEmpty()) {
                return Optional.empty();
            }
            ResultadoLimite r = new ResultadoLimite.Finito(n.get().valor());
            pasos.add(new PasoLimite("L06", Tecnicas.nombre("L06"), "Es un límite notable: " + n.get().forma() + ". "
                    + n.get().detalle(), f, n.get().valor().aExpresion(),
                    new TrabajoIntermedio.Agrupacion(List.of(n.get().forma(), n.get().detalle()))));
            usar("L06");
            return Optional.of(r);
        }

        // -------------------------------------------------------------- L07

        private ResultadoLimite lhopital(Expresion f) {
            // Un cociente se deriva tal como está: simplificado, ln x/(1/x) pasaría a ser un producto.
            Expresion actual = f instanceof Cociente ? f : Simplificador.simplificar(f);
            while (true) {
                if (!(actual instanceof Cociente c)) {
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
                }
                Valor vn = evaluar(c.numerador());
                Valor vd = evaluar(c.denominador());
                Indeterminacion tipo = esCero(vn) && esCero(vd) ? Indeterminacion.CERO_SOBRE_CERO
                        : esInfinito(vn) && esInfinito(vd) ? Indeterminacion.INFINITO_SOBRE_INFINITO : null;
                if (tipo == null) {
                    // RN35: se verifica antes de cada aplicación.
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
                }
                if (lhopital >= MAXIMO_LHOPITAL) {
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO
                            + ": L'Hôpital se aplicó " + MAXIMO_LHOPITAL + " veces sin llegar a un resultado");
                }
                lhopital++;
                Derivador.Resultado dn;
                Derivador.Resultado dd;
                try {
                    dn = Derivador.derivar(c.numerador(), x);
                    dd = Derivador.derivar(c.denominador(), x);
                } catch (RuntimeException e) {
                    return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
                }
                Expresion g = Simplificador.simplificar(new Cociente(dn.derivada(), dd.derivada()));
                List<PasoLimite> hijos = new ArrayList<>();
                dn.pasos().forEach(p -> hijos.add(deDerivada(p, "numerador")));
                dd.pasos().forEach(p -> hijos.add(deDerivada(p, "denominador")));
                pasos.add(new PasoLimite("L07", Tecnicas.nombre("L07") + (lhopital > 1 ? " (" + lhopital + ".ª vez)" : ""),
                        "Hay " + tipo.texto() + " (comprobado): se derivan el numerador y el denominador por separado, "
                                + "no con la regla del cociente", actual, g,
                        new TrabajoIntermedio.Agrupacion(List.of(
                                "(" + texto(c.numerador()) + ")' = " + texto(dn.derivada()),
                                "(" + texto(c.denominador()) + ")' = " + texto(dd.derivada()))),
                        "Derivar numerador y denominador", hijos));
                usar("L07");
                Valor v = evaluar(g);
                if (v instanceof Valor.Indeterminado i && i.tipo().esCociente() && g instanceof Cociente) {
                    evaluacionDirecta(g);
                    actual = g;
                    continue;
                }
                return calcular(g);
            }
        }

        private static PasoLimite deDerivada(PasoDerivada p, String parte) {
            return new PasoLimite(p.codigo(), p.nombre() + " · " + parte, p.regla(), p.antes(), p.despues(), null);
        }

        private static boolean esCero(Valor v) {
            return v instanceof Valor.Numero n && n.esCero();
        }

        private static boolean esInfinito(Valor v) {
            return v instanceof Valor.Infinito || v instanceof Valor.SobreCero;
        }

        /** 0·∞: f·g = g/(1/f), con f el factor que tiende a 0, para usar L'Hôpital. */
        private Optional<Expresion> aCociente(Producto p) {
            List<Expresion> aCero = new ArrayList<>();
            List<Expresion> otros = new ArrayList<>();
            for (Expresion f : p.factores()) {
                (esCero(evaluar(f)) ? aCero : otros).add(f);
            }
            if (aCero.isEmpty() || otros.isEmpty()) {
                return Optional.empty();
            }
            Expresion cero = aCero.size() == 1 ? aCero.getFirst() : new Producto(aCero);
            Expresion infinito = otros.size() == 1 ? otros.getFirst() : new Producto(otros);
            Expresion g = new Cociente(infinito, Simplificador.simplificar(Simplificador.cociente(Constante.de(1), cero)));
            pasos.add(new PasoLimite("L07", "Pasar a cociente", "Hay 0·∞: L'Hôpital solo se aplica a 0/0 o ∞/∞, así que "
                    + "el producto se escribe como cociente: " + texto(infinito) + "·" + texto(cero) + " = "
                    + texto(infinito) + "/(1/" + texto(cero) + ")", p, g, null));
            return Optional.of(g);
        }

        /** 1^∞, 0⁰, ∞⁰: y = uᵛ, ln y = v·ln u; se calcula ese límite y al final y = e^(límite). */
        private ResultadoLimite logaritmo(Potencia p, Indeterminacion t) {
            Expresion g = Simplificador.producto(List.of(p.exponente(), new Funcion(TipoFuncion.LN, p.base())));
            pasos.add(new PasoLimite("L07", "Tomar logaritmo", "Hay " + t.texto() + ": se toma logaritmo. Si y = "
                    + texto(p) + ", entonces ln y = " + texto(g) + "; se calcula ese límite y al final y = e^(límite)",
                    p, g, null));
            ResultadoLimite interno = calcular(g);
            ResultadoLimite r = switch (interno) {
                case ResultadoLimite.Finito f -> new ResultadoLimite.Finito(Exactos.exp(f.valor()));
                case ResultadoLimite.Infinito i -> i.positivo() ? new ResultadoLimite.Infinito(true)
                        : new ResultadoLimite.Finito(NumeroAlgebraico.CERO);
                default -> interno;
            };
            if (r instanceof ResultadoLimite.Finito || r instanceof ResultadoLimite.Infinito) {
                pasos.add(new PasoLimite("L07", "Deshacer el logaritmo", "ln y → " + interno.texto() + ", así que y → "
                        + r.texto(), g, r instanceof ResultadoLimite.Finito f ? f.valor().aExpresion() : null, null));
                usar("L07");
            }
            return r;
        }

        // -------------------------------------------------------------- ∞ − ∞ con fracciones

        private Expresion comunDenominador(Suma s) {
            Expresion num = Constante.de(0);
            Expresion den = Constante.de(1);
            for (Expresion t : s.terminos()) {
                Expresion[] nd = numeradorYDenominador(t);
                num = Simplificador.suma(List.of(Simplificador.producto(List.of(num, nd[1])),
                        Simplificador.producto(List.of(nd[0], den))));
                den = Simplificador.producto(List.of(den, nd[1]));
            }
            Expresion g = new Cociente(comoPolinomio(Simplificador.simplificar(num)), comoPolinomio(den));
            pasos.add(new PasoLimite("L02", "Común denominador", "Hay ∞ − ∞ con fracciones: se pasa a común "
                    + "denominador y el límite se vuelve a clasificar", s, g, null));
            return g;
        }

        private Expresion[] numeradorYDenominador(Expresion t) {
            if (t instanceof Cociente c) {
                return new Expresion[] {c.numerador(), c.denominador()};
            }
            if (t instanceof Negacion n && n.argumento() instanceof Cociente c) {
                return new Expresion[] {Simplificador.negar(c.numerador()), c.denominador()};
            }
            return new Expresion[] {t, Constante.de(1)};
        }

        // -------------------------------------------------------------- L05

        private ResultadoLimite laterales(Expresion f, NumeroAlgebraico k) {
            if (!(punto instanceof Valor.Numero a)) {
                return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
            }
            String p = texto(((Limite.Finito) limite.punto()).valor());
            int derecha = lado == Limite.Lado.IZQUIERDA ? 0 : signoCerca(f, a.valor(), 1);
            int izquierda = lado == Limite.Lado.DERECHA ? 0 : signoCerca(f, a.valor(), -1);
            if (lado != Limite.Lado.IZQUIERDA && derecha == 0 || lado != Limite.Lado.DERECHA && izquierda == 0) {
                return new ResultadoLimite.NoCalculado(ResultadoLimite.NO_CALCULADO);
            }
            Expresion denominador = f instanceof Cociente c ? c.denominador() : null;
            List<String> lineas = new ArrayList<>();
            if (lado != Limite.Lado.DERECHA) {
                lineas.add(linea("izquierda", x + " < " + p, "⁻", p, izquierda, denominador, k));
            }
            if (lado != Limite.Lado.IZQUIERDA) {
                lineas.add(linea("derecha", x + " > " + p, "⁺", p, derecha, denominador, k));
            }
            ResultadoLimite r;
            String conclusion;
            if (lado == Limite.Lado.DERECHA) {
                r = new ResultadoLimite.Infinito(derecha > 0);
                conclusion = "Por la derecha el límite es " + r.texto();
            } else if (lado == Limite.Lado.IZQUIERDA) {
                r = new ResultadoLimite.Infinito(izquierda > 0);
                conclusion = "Por la izquierda el límite es " + r.texto();
            } else if (derecha == izquierda) {
                r = new ResultadoLimite.Infinito(derecha > 0);
                conclusion = "Los dos lados dan " + r.texto() + ": el límite es " + r.texto();
            } else {
                String motivo = "lím por la izquierda = " + (izquierda > 0 ? "+∞" : "−∞") + " y por la derecha = "
                        + (derecha > 0 ? "+∞" : "−∞");
                r = new ResultadoLimite.NoExiste(motivo);
                conclusion = "No existe: " + motivo;
            }
            lineas.add(conclusion);
            pasos.add(new PasoLimite("L05", Tecnicas.nombre("L05"), "Con k/0 se estudia el signo "
                    + (denominador != null ? "del denominador " + texto(denominador) : "de la expresión")
                    + " a cada lado de " + x + " = " + p + ". " + conclusion, f, null,
                    new TrabajoIntermedio.Agrupacion(lineas)));
            usar("L05");
            return r;
        }

        private String linea(String nombre, String condicion, String marca, String p, int signo, Expresion denominador,
                             NumeroAlgebraico k) {
            String inf = signo > 0 ? "+∞" : "−∞";
            if (denominador == null) {
                return "Por la " + nombre + " (" + x + " → " + p + marca + "): la expresión es "
                        + (signo > 0 ? "positiva" : "negativa") + " y crece sin cota → " + inf;
            }
            int signoDen = signo * k.signo();
            return "Por la " + nombre + " (" + condicion + "): " + texto(denominador) + " es "
                    + (signoDen > 0 ? "positivo" : "negativo") + " y el numerador tiende a " + texto(k.aExpresion())
                    + " → " + inf;
        }

        /**
         * El signo de f a un lado del punto, cuando |f| → ∞. Si f es un cociente, sale del signo del
         * numerador y del primer término no nulo de Taylor del denominador: si D⁽ᵐ⁾(a) ≠ 0, D tiene el
         * signo de D⁽ᵐ⁾(a) a la derecha y ese signo por (−1)ᵐ a la izquierda. Si no, se evalúa f de forma
         * exacta en a ± 10⁻⁶ y a ± 10⁻¹⁰. 0 si no se pudo decidir.
         */
        private int signoCerca(Expresion f, NumeroAlgebraico a, int lado) {
            if (f instanceof Cociente c && evaluar(c.numerador()) instanceof Valor.Numero n && !n.esCero()) {
                Expresion d = c.denominador();
                for (int m = 1; m <= ORDEN_MAXIMO; m++) {
                    try {
                        d = Derivador.derivar(d, x).derivada();
                    } catch (RuntimeException e) {
                        break;
                    }
                    if (Evaluador.evaluar(d, x, new Valor.Numero(a)) instanceof Valor.Numero dm && !dm.esCero()) {
                        int s = dm.valor().signo() * (lado < 0 && m % 2 == 1 ? -1 : 1);
                        return s * n.valor().signo();
                    }
                    if (!(Evaluador.evaluar(d, x, new Valor.Numero(a)) instanceof Valor.Numero)) {
                        break;
                    }
                }
            }
            int signo = 0;
            for (int digitos : new int[] {6, 10}) {
                NumeroAlgebraico h = NumeroAlgebraico.de(new Racional(java.math.BigInteger.ONE,
                        java.math.BigInteger.TEN.pow(digitos)));
                NumeroAlgebraico cerca = lado > 0 ? a.sumar(h) : a.restar(h);
                if (!(Evaluador.evaluar(f, x, new Valor.Numero(cerca)) instanceof Valor.Numero v) || v.esCero()) {
                    return 0;
                }
                int s = v.valor().signo();
                if (signo != 0 && s != signo) {
                    return 0;
                }
                signo = s;
            }
            return signo;
        }
    }

    // ------------------------------------------------------------------ consultas

    static boolean tieneRaiz(Expresion e) {
        return switch (e) {
            case Raiz r -> true;
            case Funcion f -> f.tipo() == TipoFuncion.SQRT || tieneRaiz(f.argumento());
            case Suma s -> s.terminos().stream().anyMatch(MotorLimites::tieneRaiz);
            case Producto p -> p.factores().stream().anyMatch(MotorLimites::tieneRaiz);
            case Cociente c -> tieneRaiz(c.numerador()) || tieneRaiz(c.denominador());
            case Negacion n -> tieneRaiz(n.argumento());
            case Potencia p -> tieneRaiz(p.base()) || p.exponente() instanceof Cociente
                    || p.exponente() instanceof Constante k && !k.valor().esEntero();
            default -> false;
        };
    }

    private static boolean esFraccion(Expresion e) {
        return e instanceof Cociente || e instanceof Negacion n && n.argumento() instanceof Cociente;
    }
}
