package pe.mathnova.application.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.FactorizadorFunciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.domain.sugerencias.EjercicioResuelto;
import pe.mathnova.domain.sugerencias.MotorSugerencias;
import pe.mathnova.domain.sugerencias.Sugerencia;

/**
 * Factoriza lo que se escribió en la pantalla de entrada (HU04) en Q, R o C (HU20, HU21), con un método
 * forzado si se pidió (HU08), y la guarda en el historial (RN24). Simplifica fracciones de polinomios
 * (HU09, solo en Q) y factoriza expresiones con funciones (HU23).
 */
@Service
public class FactorizacionService {

    private static final Logger LOG = LoggerFactory.getLogger(FactorizacionService.class);

    /** Un método del menú "Método": "F16" y "Ruffini (divisores binómicos)". */
    public record Metodo(String codigo, String nombre) {
    }

    /**
     * La comparación de la pestaña Exploración: el LaTeX del resultado en cada campo, o el motivo por
     * el que no hay uno ("Solo en Q").
     */
    public record Comparacion(Map<Campo, String> latex, Map<Campo, String> avisos, String nota,
                              Map<Campo, Resolucion> resoluciones) {
        public Comparacion {
            latex = Map.copyOf(latex);
            avisos = Map.copyOf(avisos);
            resoluciones = Map.copyOf(resoluciones);
        }

        public Comparacion(Map<Campo, String> latex, Map<Campo, String> avisos, String nota) {
            this(latex, avisos, nota, Map.of());
        }

        public Comparacion(Map<Campo, String> latex, Map<Campo, String> avisos) {
            this(latex, avisos, null);
        }

        /** Q, R y C dan lo mismo: se muestra una sola vez, con {@link #nota()}. */
        public boolean iguales() {
            return nota != null;
        }
    }

    /** La nota de la comparación cuando una fracción simplificada es la misma en los tres campos. */
    public static final String MISMA_FRACCION = "El resultado es el mismo en Q, R y C: el factor común de dos "
            + "polinomios con coeficientes racionales no cambia al pasar a R o C";

    private final OrquestadorFactorizacion motor;
    private final FactorizadorFunciones funciones;
    private final HistorialFactorizacionService historial;
    private final MotorSugerencias sugeridor;

    public FactorizacionService(OrquestadorFactorizacion motor, HistorialFactorizacionService historial) {
        this.motor = motor;
        this.funciones = new FactorizadorFunciones(motor);
        this.historial = historial;
        this.sugeridor = new MotorSugerencias(seResuelve(motor));
    }

    /**
     * RN20: una integral o una derivada sugerida solo queda disponible si su plantilla se resuelve de verdad
     * (una integral que necesita otro método se muestra desactivada).
     */
    static java.util.function.Predicate<pe.mathnova.domain.sugerencias.Sugerencia> seResuelve(
            OrquestadorFactorizacion motor) {
        var integrales = new pe.mathnova.domain.calculo.integrales.MotorIntegrales(motor);
        var derivadas = new pe.mathnova.domain.calculo.derivadas.MotorDerivadas(motor);
        var extremos = new pe.mathnova.domain.calculo.extremos.MotorExtremos(motor);
        var multiples = new pe.mathnova.domain.calculo.multiples.MotorIntegralesMultiples(motor);
        return s -> switch (s.operacion()) {
            case "INTEGRAR" -> integrales.resolver(Parser.parse(s.plantilla())).resuelta();
            case "PUNTOS_CRITICOS" -> derivadas.resolver(
                    (pe.mathnova.domain.expresion.Derivada) Parser.parse(s.plantilla())) != null;
            // Sprint 8: los extremos se buscan en polinomios; una integral doble, si la iterada se resuelve
            case "EXTREMOS" -> extremos.resolver((pe.mathnova.domain.expresion.Extremos) Parser.parse(s.plantilla()))
                    != null;
            case "INTEGRAL_MULTIPLE" -> multiples.resolver(
                    (pe.mathnova.domain.expresion.IntegralMultiple) Parser.parse(s.plantilla())).resuelta();
            default -> true;
        };
    }

    /**
     * @throws pe.mathnova.domain.parser.ErrorSintaxis si el texto está mal escrito
     * @throws pe.mathnova.domain.algebra.NoEsPolinomioException si no es un polinomio
     */
    public Resolucion factorizar(String texto) {
        return factorizar(texto, null, fase -> { });
    }

    public Resolucion factorizar(String texto, Consumer<FaseFactorizacion> avance) {
        return factorizar(texto, null, avance);
    }

    /**
     * Igual que {@link #factorizar(String)}, con un método forzado y avisando cada fase.
     *
     * @param forzado código del método forzado, o null en automático
     * @param avance  recibe {@link FaseFactorizacion#LECTURA} y {@link FaseFactorizacion#METODOS}
     * @throws pe.mathnova.domain.factorizacion.MetodoNoAplicable si el método forzado no aplica
     */
    public Resolucion factorizar(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        return factorizar(texto, forzado, Campo.Q, avance);
    }

    /** En R o C primero se factoriza en Q y luego se parten los factores irreducibles (RN27). */
    public Resolucion factorizar(String texto, String forzado, Campo campo, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion entrada = Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        Resolucion r = motor.resolver(entrada, campo, forzado);
        try {
            historial.guardar(texto, r, sugerencias(r));
        } catch (RuntimeException e) {
            // Si el historial falla, la resolución se muestra igual.
            LOG.warn("No se pudo guardar la factorización de {} en el historial", texto, e);
        }
        return r;
    }

    /** P01 (HU09). Desde el Sprint 5 se guarda en el historial con sus sugerencias. */
    public Simplificacion simplificar(String texto, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion entrada = Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        Simplificacion s = new SimplificadorFracciones(motor).simplificar(entrada);
        try {
            historial.guardarSimplificacion(texto, s, sugerencias(s));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo guardar la simplificación de {} en el historial", texto, e);
        }
        return s;
    }

    /**
     * HU10: las sugerencias de la factorización de un polinomio de una letra (RN18). Las reglas no
     * dependen del campo: corren igual en Q, R y C.
     */
    public List<Sugerencia> sugerencias(Resolucion r) {
        if (r.metodoForzado() != null) {
            return List.of();
        }
        return EjercicioResuelto.de(r).map(sugeridor::sugerir).orElse(List.of());
    }

    /** HU10 para una expresión con funciones ya factorizada (HU23); vacío si no se factorizó. */
    public List<Sugerencia> sugerencias(ResolucionFunciones r) {
        return EjercicioResuelto.de(r).map(sugeridor::sugerir).orElse(List.of());
    }

    /** HU10: las sugerencias de una fracción simplificada (RN15–RN17, RN19). */
    public List<Sugerencia> sugerencias(Simplificacion s) {
        return EjercicioResuelto.de(s).map(sugeridor::sugerir).orElse(List.of());
    }

    /** RF26: las asíntotas de una fracción simplificada; vacío si no es de una letra. */
    public Optional<Asintotas> asintotas(Simplificacion s) {
        return Asintotas.de(s);
    }

    /** Si el texto es una fracción de polinomios que se puede simplificar. */
    public boolean esFraccion(String texto) {
        try {
            return SimplificadorFracciones.esFraccion(Parser.parse(texto));
        } catch (ErrorSintaxis e) {
            return false;
        }
    }

    /** Los métodos del menú "Método" (HU08), en orden. */
    public List<Metodo> metodosForzables() {
        return motor.metodosForzables().stream().map(c -> new Metodo(c, Tecnicas.nombre(c))).toList();
    }

    /** HU18: expande el resultado y lo compara con la entrada. */
    public Verificacion verificar(Resolucion r) {
        return Verificacion.de(r);
    }

    /** HU18 para fracciones: multiplica en cruz. */
    public Verificacion verificar(Simplificacion s) {
        return Verificacion.de(s);
    }

    /** HU23: una expresión con funciones (T01–T05). No se guarda en el historial. */
    public ResolucionFunciones factorizarFunciones(String texto, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion entrada = Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return funciones.resolver(entrada);
    }

    /** Entrada y resultado evaluados en cinco puntos con 40 cifras. */
    public Verificacion verificar(ResolucionFunciones r) {
        return Verificacion.de(r);
    }

    /**
     * La tabla Q | R | C de Exploración. Una fracción se simplifica en Q (el factor común no cambia en
     * R ni en C) y lo que queda arriba y abajo se factoriza en cada campo. Las expresiones con
     * funciones solo se factorizan en Q. Si en un campo no hay resultado, ese campo no está en
     * {@link Comparacion#latex()} y la fila no se muestra: nunca "No se pudo calcular".
     */
    public Comparacion comparar(String texto) {
        Map<Campo, String> latex = new EnumMap<>(Campo.class);
        Map<Campo, String> avisos = new EnumMap<>(Campo.class);
        Expresion e = Parser.parse(texto);
        if (SimplificadorFracciones.esFraccion(e)) {
            Simplificacion s = new SimplificadorFracciones(motor).simplificar(e);
            String enQ = ImpresoraLatex.imprimir(s.aExpresion());
            latex.put(Campo.Q, enQ);
            String formaQ = latexFraccion(s, Campo.Q);
            boolean iguales = true;
            for (Campo c : List.of(Campo.R, Campo.C)) {
                try {
                    String forma = latexFraccion(s, c);
                    // Lo mismo que en Q se muestra igual que en Q.
                    latex.put(c, forma.equals(formaQ) ? enQ : forma);
                    iguales &= forma.equals(formaQ);
                } catch (RuntimeException ex) {
                    iguales = false;
                    LOG.debug("La fracción {} no se pudo factorizar en {}", texto, c, ex);
                }
            }
            return new Comparacion(latex, avisos, iguales ? MISMA_FRACCION : null);
        }
        Map<Campo, Resolucion> resoluciones = new EnumMap<>(Campo.class);
        for (Campo c : Campo.values()) {
            try {
                if (FactorizadorFunciones.tieneFunciones(e)) {
                    if (c == Campo.Q) {
                        ResolucionFunciones r = funciones.resolver(e);
                        if (r.factorizable()) {
                            latex.put(c, ImpresoraLatex.imprimir(r.resultado()));
                        } else {
                            avisos.put(c, r.mensaje());
                        }
                    } else {
                        avisos.put(c, "Las expresiones con funciones se factorizan en Q");
                    }
                } else {
                    Resolucion r = motor.resolver(e, c);
                    latex.put(c, latex(r));
                    resoluciones.put(c, r);
                }
            } catch (RuntimeException ex) {
                LOG.debug("{} no se pudo factorizar en {}", texto, c, ex);
            }
        }
        return new Comparacion(latex, avisos, null, resoluciones);
    }

    /**
     * La fracción simplificada con lo de arriba y lo de abajo factorizado en {@code c}:
     * (x^2 - 2)/(x - 1) en R es (x − √2)(x + √2)/(x − 1). Si en R o C las raíces solo se aproximan, esa
     * parte queda como en Q.
     */
    private String latexFraccion(Simplificacion s, Campo c) {
        String arriba = parteEnCampo(s.arriba(), c);
        if (s.abajo().factores().isEmpty()) {
            return arriba;
        }
        String abajo = parteEnCampo(s.abajo(), c);
        boolean negativo = arriba.startsWith("-");
        String frac = "\\frac{" + (negativo ? arriba.substring(1).trim() : arriba) + "}{" + abajo + "}";
        return negativo ? "-" + frac : frac;
    }

    private String parteEnCampo(pe.mathnova.domain.factorizacion.FormaFactorizada parte, Campo c) {
        if (parte.factores().isEmpty()) {
            return ImpresoraLatex.imprimir(Parser.parse(parte.aTexto()));
        }
        if (c == Campo.Q) {
            return ImpresoraLatex.imprimir(parte.aExpresion());
        }
        Resolucion r = motor.resolver(parte.aExpresion(), c);
        if (r.extension() == null || r.extension().aproximada() || r.extension().pasos().isEmpty()) {
            return ImpresoraLatex.imprimir(parte.aExpresion());
        }
        return ImpresoraLatex.imprimir(r.extension().resultado().aExpresion());
    }

    /** El resultado en LaTeX; en R o C con ≈ y las raíces si se aproximaron (RN30). */
    public static String latex(Resolucion r) {
        if (r.extension() == null) {
            return ImpresoraLatex.imprimir(r.resultado().aExpresion());
        }
        String forma = ImpresoraLatex.imprimir(r.extension().resultado().aExpresion());
        if (!r.extension().aproximada()) {
            return forma;
        }
        String raices = String.join(",\\; ", r.extension().aproximadas().stream()
                .map(t -> "x \\approx " + t.replace("i", "\\,i")).toList());
        return forma + "\\quad " + raices;
    }
}
