package pe.mathnova.application.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.derivadas.MotorDerivadas;
import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.calculo.integrales.MotorIntegrales;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.parciales.MotorParciales;
import pe.mathnova.domain.calculo.parciales.ResolucionParcial;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Direccional;
import pe.mathnova.domain.expresion.Gradiente;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.VariablePrincipal;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.FactorizadorFunciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

/**
 * El botón "Resolver" (antes "Factorizar") y el resumen del resultado en una línea: decide qué hacer con
 * lo que se escribió (factorizar, simplificar, resolver la ecuación, calcular el límite, la derivada o la
 * integral) y lo calcula.
 */
@Service
public class ResolverService {

    static final String SOLO_POR_AHORA = "Por ahora se factorizan polinomios y expresiones con funciones de x, se "
            + "simplifican fracciones de polinomios y se resuelven ecuaciones polinómicas de una incógnita.";
    static final String OPERACIONES_PRONTO = "Esta operación todavía no se resuelve.";
    static final String COEFICIENTE_DERIVADA = "Escribe el número dentro de la derivada: d/dx(3·…).";
    static final String IMPLICITA = "Para derivarla implícitamente escribe d/dx(…) con la ecuación adentro: "
            + "d/dx(x^2 + y^2 = 25).";

    /** Qué hará "Resolver". */
    public sealed interface Plan {

        /** Texto del botón. */
        default String boton() {
            return "Resolver";
        }

        /** Si el botón queda habilitado. */
        default boolean resoluble() {
            return false;
        }

        record Vacio() implements Plan {
        }

        record Invalido(String mensaje, int posicion) implements Plan {
        }

        record Factorizar() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** HU23: polinómica en funciones de x (T01–T05), o para decir por qué no lo es. */
        record FactorizarFunciones() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        record Simplificar() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        record ResolverEcuacion() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** HU11, HU24: lim(x→a, f) con la cascada de métodos. */
        record CalcularLimite() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** HU12, HU25: d/dx(f), dⁿ/dxⁿ(f) o la derivación implícita d/dx(F = G). */
        record Derivar() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** HU13: ∫(f, x), ∫(f, x, a, b) o k·∫(…). */
        record Integrar() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /**
         * HU28 (Sprint 7): ∂/∂x(f), grad(f), Du(f, v, P) o la regla de la cadena
         * "d/dt(f); x = t^2, y = sen t".
         */
        record Parciales() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** HU29 (Sprint 8): lim((x, y) → (a, b), f). */
        record LimiteDoble() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** HU30 (Sprint 8): extremos(f) o extremos(f; g = c). */
        record Extremos() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** HU31 (Sprint 8): ∬ y ∭, con o sin cambio de coordenadas. */
        record IntegralMultiple() implements Plan {
            @Override
            public boolean resoluble() {
                return true;
            }
        }

        /** Una operación que se reconoce pero no se resuelve (un número delante de una derivada). */
        record Operacion(String mensaje) implements Plan {
        }

        record NoSoportado(String mensaje) implements Plan {
        }
    }

    private final FactorizacionService factorizacion;
    private final OrquestadorFactorizacion motor;
    private final ResolvedorEcuaciones ecuaciones;
    private final LimiteService limites;
    private final MotorDerivadas derivadas;
    private final MotorIntegrales integrales;
    private final MotorParciales parciales = new MotorParciales();
    private final pe.mathnova.domain.calculo.limites.MotorLimitesDobles limitesDobles =
            new pe.mathnova.domain.calculo.limites.MotorLimitesDobles();
    private final pe.mathnova.domain.calculo.extremos.MotorExtremos extremos;
    private final pe.mathnova.domain.calculo.multiples.MotorIntegralesMultiples multiples;

    /** Sin historial de límites (pruebas). */
    public ResolverService(FactorizacionService factorizacion, OrquestadorFactorizacion motor,
                           ResolvedorEcuaciones ecuaciones) {
        this(factorizacion, motor, ecuaciones, new LimiteService(motor, null));
    }

    @Autowired
    public ResolverService(FactorizacionService factorizacion, OrquestadorFactorizacion motor,
                           ResolvedorEcuaciones ecuaciones, LimiteService limites) {
        this.factorizacion = factorizacion;
        this.motor = motor;
        this.ecuaciones = ecuaciones;
        this.limites = limites;
        this.derivadas = new MotorDerivadas(motor);
        this.integrales = new MotorIntegrales(motor);
        this.extremos = new pe.mathnova.domain.calculo.extremos.MotorExtremos(motor);
        this.multiples = new pe.mathnova.domain.calculo.multiples.MotorIntegralesMultiples(motor);
    }

    // ------------------------------------------------------------------ Sprint 8

    /** HU29: un límite doble. */
    public pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble resolverLimiteDoble(String texto,
                                                                                       Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion e = Parser.parse(texto);
        if (!(e instanceof pe.mathnova.domain.expresion.LimiteDoble l)) {
            throw new IllegalArgumentException("No es un límite doble: " + texto);
        }
        avance.accept(FaseFactorizacion.METODOS);
        return limitesDobles.resolver(l);
    }

    /** HU30: puntos críticos y extremos, con el Hessiano o con Lagrange. */
    public pe.mathnova.domain.calculo.extremos.ResolucionExtremos resolverExtremos(String texto,
                                                                                  Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion e = Parser.parse(texto);
        if (!(e instanceof pe.mathnova.domain.expresion.Extremos x)) {
            throw new IllegalArgumentException("No es un cálculo de extremos: " + texto);
        }
        avance.accept(FaseFactorizacion.METODOS);
        return extremos.resolver(x);
    }

    /**
     * HU31: una integral doble o triple.
     *
     * @param forzado ORDEN, POLARES, CILINDRICAS o ESFERICAS (Exploración), o null
     */
    public pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple resolverIntegralMultiple(
            String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion e = Parser.parse(texto);
        if (!(e instanceof pe.mathnova.domain.expresion.IntegralMultiple i)) {
            throw new IllegalArgumentException("No es una integral múltiple: " + texto);
        }
        avance.accept(FaseFactorizacion.METODOS);
        return multiples.resolver(i, forzado);
    }

    public List<CaminoCalculo> caminos(pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple r) {
        return multiples.caminos(r);
    }

    // ------------------------------------------------------------------ derivadas e integrales (Sprint 6)

    /** @param forzado un camino de Exploración, o null */
    public ResolucionDerivada resolverDerivada(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion e = Parser.parse(texto);
        if (!(e instanceof Derivada d)) {
            throw new IllegalArgumentException("No es una derivada: " + texto);
        }
        avance.accept(FaseFactorizacion.METODOS);
        return derivadas.resolver(d, forzado);
    }

    /** @param forzado el método que se prueba primero, o null */
    public ResolucionIntegral resolverIntegral(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion e = Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return integrales.resolver(e, forzado);
    }

    /** HU28: una parcial, un gradiente, una derivada direccional o la regla de la cadena (con ";"). */
    public ResolucionParcial resolverParciales(String texto, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Optional<MotorParciales.ReglaCadena> cadena = MotorParciales.leerCadena(texto);
        Expresion e = cadena.isPresent() ? null : Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return cadena.isPresent() ? parciales.cadena(cadena.get()) : parciales.resolver(e);
    }

    public List<CaminoCalculo> caminos(ResolucionDerivada r) {
        return derivadas.caminos(r);
    }

    public List<CaminoCalculo> caminos(ResolucionIntegral r) {
        return integrales.caminos(r);
    }

    /** HU11: el límite con sus pasos, guardado en el historial. */
    public ResolucionLimite resolverLimite(String texto, Consumer<FaseFactorizacion> avance) {
        return limites.resolver(texto, avance);
    }

    /** @param forzado el método que se prueba primero ("Probar" en Exploración), o null */
    public ResolucionLimite resolverLimite(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        return limites.resolver(texto, forzado, avance);
    }

    public LimiteService limites() {
        return limites;
    }

    public Plan decidir(String texto) {
        if (texto == null || texto.isBlank()) {
            return new Plan.Vacio();
        }
        Expresion e;
        try {
            e = Parser.parse(texto);
        } catch (ErrorSintaxis error) {
            if (texto.indexOf(';') < 0) {
                return new Plan.Invalido(error.getMessage(), error.getPosicion());
            }
            // "d/dt(f); x = t^2, y = sen t": el ";" va fuera de la derivada (Sprint 7)
            try {
                MotorParciales.leerCadena(texto);
                return new Plan.Parciales();
            } catch (ErrorSintaxis otro) {
                return new Plan.Invalido(otro.getMessage(), otro.getPosicion());
            } catch (IllegalArgumentException otro) {
                return new Plan.NoSoportado(otro.getMessage());
            }
        }
        if (e instanceof pe.mathnova.domain.expresion.LimiteDoble) {
            return new Plan.LimiteDoble();
        }
        if (e instanceof pe.mathnova.domain.expresion.Extremos) {
            return new Plan.Extremos();
        }
        if (e instanceof pe.mathnova.domain.expresion.IntegralMultiple) {
            return new Plan.IntegralMultiple();
        }
        if (e instanceof Limite) {
            return new Plan.CalcularLimite();
        }
        if (e instanceof Derivada) {
            return new Plan.Derivar();
        }
        if (e instanceof DerivadaParcial || e instanceof Gradiente || e instanceof Direccional) {
            return new Plan.Parciales();
        }
        if (e instanceof Integral || e instanceof Producto(List<Expresion> fs) && fs.size() == 2
                && fs.get(0) instanceof Constante && fs.get(1) instanceof Integral) {
            return new Plan.Integrar();
        }
        if (e instanceof Producto(List<Expresion> fs) && fs.size() == 2 && fs.get(1) instanceof Derivada) {
            return new Plan.Operacion(COEFICIENTE_DERIVADA);
        }
        if (Expresiones.esOperacion(e)) {
            return new Plan.Operacion(OPERACIONES_PRONTO);
        }
        if (e instanceof Ecuacion ec) {
            if (esEcuacionPolinomica(ec)) {
                return new Plan.ResolverEcuacion();
            }
            return new Plan.NoSoportado(Expresiones.variables(ec).size() == 2 ? SOLO_POR_AHORA + " " + IMPLICITA
                    : SOLO_POR_AHORA);
        }
        if (SimplificadorFracciones.esFraccion(e)) {
            return new Plan.Simplificar();
        }
        try {
            Polinomio.desde(e);
            return new Plan.Factorizar();
        } catch (NoEsPolinomioException | ArithmeticException noEs) {
            if (FactorizadorFunciones.tieneFunciones(e) && soloX(e)) {
                return new Plan.FactorizarFunciones();
            }
            return new Plan.NoSoportado(SOLO_POR_AHORA);
        }
    }

    /** Las expresiones con funciones son de una sola variable, x. */
    private static boolean soloX(Expresion e) {
        SortedSet<Character> letras = VariablePrincipal.de(e).variables();
        return letras.isEmpty() || letras.size() == 1 && letras.first() == 'x';
    }

    private static boolean esEcuacionPolinomica(Ecuacion ec) {
        try {
            Polinomio izq = Polinomio.desde(ec.izquierda());
            Polinomio der = Polinomio.desde(ec.derecha());
            SortedSet<Character> letras = new TreeSet<>(izq.variables());
            letras.addAll(der.variables());
            return letras.size() <= 1;
        } catch (NoEsPolinomioException | ArithmeticException noEs) {
            return false;
        }
    }

    /**
     * HU38. Avisa las mismas fases que la factorización para el indicador de carga.
     *
     * @param forzado el método con que empieza la factorización (HU08), o null en automático
     */
    public ResolucionEcuacion resolverEcuacion(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion e = Parser.parse(texto);
        if (!(e instanceof Ecuacion ec)) {
            throw new IllegalArgumentException("No es una ecuación: " + texto);
        }
        avance.accept(FaseFactorizacion.METODOS);
        return ecuaciones.resolver(ec, forzado);
    }

    public Verificacion verificar(ResolucionEcuacion r) {
        return r.factorizacion() == null ? null : factorizacion.verificar(r.factorizacion());
    }

    // ------------------------------------------------------------------ resumen en una línea (antes RF71)

    /**
     * El resultado en una línea, en texto de la sección 9: "= (x - 2)(x - 3)",
     * "x₁ = -1/2 ≈ -0.5, x₂ = 2", "= 5/3 ≈ 1.6667". Vacío si no hay nada que mostrar: entrada
     * incompleta o inválida, una operación que todavía no se resuelve, o nada nuevo que decir.
     *
     * <p>Antes se mostraba en gris bajo el editor (HU37, RF71). Esa línea se retiró de la pantalla: el
     * resultado depende del método y del campo, y adelantarlo confundía. La interfaz ya no llama a este
     * método; queda porque las pruebas de los sprints S4–S8 lo usan para comparar resultados.
     */
    public Optional<String> enVivo(String texto) {
        return enVivo(texto, Campo.Q);
    }

    /** Igual, con la factorización en el campo elegido (R o C muestran radicales e i). */
    public Optional<String> enVivo(String texto, Campo campo) {
        Plan plan = decidir(texto);
        try {
            return switch (plan) {
                case Plan.Factorizar f -> factorizacionEnVivo(Parser.parse(texto), campo);
                case Plan.FactorizarFunciones f -> {
                    ResolucionFunciones r = new FactorizadorFunciones(motor).resolver(Parser.parse(texto));
                    yield r.factorizable() ? Optional.of("= " + r.formula()) : Optional.empty();
                }
                case Plan.Simplificar s -> Optional.of("= " + new SimplificadorFracciones(motor)
                        .simplificar(Parser.parse(texto)).fraccion());
                case Plan.ResolverEcuacion r -> Optional.of(solucionesEnVivo(
                        ecuaciones.resolver((Ecuacion) Parser.parse(texto))));
                case Plan.CalcularLimite l -> limiteEnVivo(limites.calcular(texto).resultado());
                case Plan.Derivar d -> {
                    ResolucionDerivada r = derivadas.resolver((Derivada) Parser.parse(texto));
                    yield Optional.of(r.esImplicita() ? r.texto() : "= " + r.texto());
                }
                case Plan.Integrar i -> integralEnVivo(integrales.resolver(Parser.parse(texto)));
                case Plan.Parciales p -> Optional.of("= " + resolverParciales(texto, f -> { }).texto());
                case Plan.LimiteDoble l -> {
                    var r = resolverLimiteDoble(texto, f -> { });
                    yield r.estado() == pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Estado.NO_DECIDIDO
                            ? Optional.empty() : Optional.of(r.existe() ? "= " + r.texto() : r.texto());
                }
                case Plan.Extremos x -> {
                    var r = resolverExtremos(texto, f -> { });
                    yield r.puntos().isEmpty() ? Optional.empty() : Optional.of(r.texto());
                }
                case Plan.IntegralMultiple i -> {
                    var r = resolverIntegralMultiple(texto, null, f -> { });
                    yield r.resuelta() ? Optional.of("= " + r.texto()) : Optional.empty();
                }
                default -> Optional.empty();
            };
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private Optional<String> factorizacionEnVivo(Expresion e, Campo campo) {
        Polinomio p = Polinomio.desde(e);
        if (p.esConstante()) {
            Racional valor = p.constante();
            return Optional.of("= " + conDecimal(valor));
        }
        Resolucion r = motor.resolver(e, campo);
        boolean enCampo = r.extension() != null && !r.extension().pasos().isEmpty();
        if (r.pasos().isEmpty() && !enCampo) {
            return Optional.empty();
        }
        return Optional.of((r.extension() != null && r.extension().aproximada() ? "" : "= ") + r.texto());
    }

    private static String solucionesEnVivo(ResolucionEcuacion r) {
        if (r.tipo() != ResolucionEcuacion.Tipo.CON_SOLUCIONES) {
            return r.resumen();
        }
        List<String> partes = new ArrayList<>();
        List<ResolucionEcuacion.Solucion> s = r.soluciones();
        for (int i = 0; i < s.size(); i++) {
            String nombre = s.size() == 1 ? String.valueOf(r.variable()) : r.variable() + subindice(i + 1);
            String linea = nombre + " = " + conDecimal(s.get(i).valor());
            if (s.get(i).multiplicidad() > 1) {
                linea += " (" + s.get(i).nombreMultiplicidad() + ")";
            }
            partes.add(linea);
        }
        return String.join(", ", partes);
    }

    /** "= 4", "= 3/2 ≈ 1.5", "= e ≈ 2.7183", "= +∞", "No existe"; nada si no se pudo calcular. */
    static Optional<String> limiteEnVivo(ResultadoLimite r) {
        return switch (r) {
            case ResultadoLimite.Finito f -> {
                NumeroAlgebraico z = f.valor();
                if (z.esRacional()) {
                    yield Optional.of("= " + conDecimal(z.aRacional()));
                }
                BigDecimal d = EvaluacionNumerica.evaluar(z, new MathContext(20)).re()
                        .setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
                yield Optional.of("= " + f.texto() + " ≈ " + d.toPlainString());
            }
            case ResultadoLimite.Infinito i -> Optional.of("= " + i.texto());
            case ResultadoLimite.NoExiste n -> Optional.of("No existe");
            case ResultadoLimite.NoCalculado n -> Optional.empty();
        };
    }

    /** "= x³/3 + C", "= 1/3 ≈ 0.3333", "= ln 2 ≈ 0.6931"; nada si ningún método aplicó. */
    static Optional<String> integralEnVivo(ResolucionIntegral r) {
        if (!r.resuelta()) {
            return Optional.empty();
        }
        if (!r.definida()) {
            return Optional.of("= " + r.texto());
        }
        Racional q = r.valorRacional();
        if (q != null) {
            return Optional.of("= " + conDecimal(q).replace("-", ImpresoraPizarra.MENOS));
        }
        if (r.aproximado() == null) {
            return Optional.of("= " + r.texto());
        }
        String d = r.aproximado().setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return Optional.of("= " + r.texto() + " ≈ " + d.replace("-", ImpresoraPizarra.MENOS));
    }

    /** "3", o "5/3 ≈ 1.6667" si no es entero. */
    static String conDecimal(Racional r) {
        if (r.esEntero()) {
            return r.toString();
        }
        BigDecimal d = new BigDecimal(r.num()).divide(new BigDecimal(r.den()), new MathContext(10, RoundingMode.HALF_UP))
                .setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        return r + " ≈ " + d.toPlainString();
    }

    private static String subindice(int n) {
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(n).toCharArray()) {
            sb.append((char) ('₀' + (c - '0')));
        }
        return sb.toString();
    }
}
