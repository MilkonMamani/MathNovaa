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

@Service
public class FactorizacionService {

    private static final Logger LOG = LoggerFactory.getLogger(FactorizacionService.class);

    public record Metodo(String codigo, String nombre) {
    }

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

        public boolean iguales() {
            return nota != null;
        }
    }

    public static final String MISMA_FRACCION = "El resultado es el mismo en Q, R y C: el factor común de dos "
            + "polinomios con coeficientes racionales no cambia al pasar a R o C";

    private final OrquestadorFactorizacion motor;
    private final FactorizadorFunciones funciones;
    private final MotorSugerencias sugeridor;

    public FactorizacionService(OrquestadorFactorizacion motor) {
        this.motor = motor;
        this.funciones = new FactorizadorFunciones(motor);
        this.sugeridor = new MotorSugerencias(seResuelve(motor));
    }

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
            case "EXTREMOS" -> extremos.resolver((pe.mathnova.domain.expresion.Extremos) Parser.parse(s.plantilla()))
                    != null;
            case "INTEGRAL_MULTIPLE" -> multiples.resolver(
                    (pe.mathnova.domain.expresion.IntegralMultiple) Parser.parse(s.plantilla())).resuelta();
            default -> true;
        };
    }

    public Resolucion factorizar(String texto) {
        return factorizar(texto, null, fase -> { });
    }

    public Resolucion factorizar(String texto, Consumer<FaseFactorizacion> avance) {
        return factorizar(texto, null, avance);
    }

    public Resolucion factorizar(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        return factorizar(texto, forzado, Campo.Q, avance);
    }

    public Resolucion factorizar(String texto, String forzado, Campo campo, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion entrada = Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return motor.resolver(entrada, campo, forzado);
    }

    public Simplificacion simplificar(String texto, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion entrada = Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return new SimplificadorFracciones(motor).simplificar(entrada);
    }

    public List<Sugerencia> sugerencias(Resolucion r) {
        if (r.metodoForzado() != null) {
            return List.of();
        }
        return EjercicioResuelto.de(r).map(sugeridor::sugerir).orElse(List.of());
    }

    public List<Sugerencia> sugerencias(ResolucionFunciones r) {
        return EjercicioResuelto.de(r).map(sugeridor::sugerir).orElse(List.of());
    }

    public List<Sugerencia> sugerencias(Simplificacion s) {
        return EjercicioResuelto.de(s).map(sugeridor::sugerir).orElse(List.of());
    }

    public Optional<Asintotas> asintotas(Simplificacion s) {
        return Asintotas.de(s);
    }

    public boolean esFraccion(String texto) {
        try {
            return SimplificadorFracciones.esFraccion(Parser.parse(texto));
        } catch (ErrorSintaxis e) {
            return false;
        }
    }

    public List<Metodo> metodosForzables() {
        return motor.metodosForzables().stream().map(c -> new Metodo(c, Tecnicas.nombre(c))).toList();
    }

    public Verificacion verificar(Resolucion r) {
        return Verificacion.de(r);
    }

    public Verificacion verificar(Simplificacion s) {
        return Verificacion.de(s);
    }

    public ResolucionFunciones factorizarFunciones(String texto, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Expresion entrada = Parser.parse(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return funciones.resolver(entrada);
    }

    public Verificacion verificar(ResolucionFunciones r) {
        return Verificacion.de(r);
    }

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
